package org.cirruslabs.anka.controller

import org.cirruslabs.anka.sdk.AnkaCommunicator
import io.dropwizard.Application
import io.dropwizard.setup.Environment
import io.grpc.ServerBuilder
import io.grpc.ServerInterceptor
import io.grpc.ServerInterceptors
import org.cirruslabs.anka.controller.auth.AccessTokenServerInterceptor
import org.cirruslabs.anka.controller.config.AuthApplicationConfiguration
import org.cirruslabs.anka.controller.health.FutureHealthCheck
import org.cirruslabs.anka.controller.health.ManagerHealthCheck
import org.cirruslabs.anka.controller.health.ScheduleHealthCheck
import org.cirruslabs.anka.sdk.AnkaVMManager
import java.util.*
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit

fun main(vararg args: String) {
  ControllerApplication().run(*args)
}

class ControllerApplication : Application<AuthApplicationConfiguration>() {
  val scheduledExecutorService: ScheduledExecutorService = Executors.newScheduledThreadPool(16)

  override fun run(configuration: AuthApplicationConfiguration, environment: Environment) {
    val grpcConfig = configuration.grpc ?: throw IllegalStateException("grpc config should be provided!")

    val env = System.getenv()
    val communicator = AnkaCommunicator(
      env["ANKA_HOST"] ?: throw IllegalStateException("ANKA_HOST environment variable should be defined!"),
      env["ANKA_PORT"] ?: throw IllegalStateException("ANKA_PORT environment variable should be defined!")
    )

    val vmManager = AnkaVMManager(communicator)

    val serviceImpl = ControllerServiceImpl(vmManager)

    val interceptors = mutableListOf<ServerInterceptor>()

    env["ACCESS_TOKEN"]?.also {
      interceptors.add(AccessTokenServerInterceptor(it))
    }

    ServerBuilder.forPort(grpcConfig.port)
      .addService(ServerInterceptors.intercept(serviceImpl, interceptors))
      .build()
      .start()

    // available at /healthcheck
    val healthCheck = ManagerHealthCheck(vmManager)
    environment.healthChecks().register("vm-manager", healthCheck)
    environment.healthChecks().register("scheduling", ScheduleHealthCheck(vmManager))

    println("Started GRPC server on ${grpcConfig.port} port...")
    println("Current health: ${healthCheck.execute()}")


    val tryToSchedule = scheduledExecutorService.scheduleWithFixedDelay(
      {
        try {
          while(vmManager.tryToSchedule()) {
            println("Scheduled!")
          }
        } catch (e: Throwable) {
          System.err.println("Failed to schedule: ${e.message}")
          e.printStackTrace(System.err)
        }
      },
      30L + Random().nextInt(15).toLong(),
      15,
      TimeUnit.SECONDS
    )

    environment.healthChecks().register("try-to-schedule-future", FutureHealthCheck(tryToSchedule))
  }
}
