package org.cirruslabs.anka.controller

import io.dropwizard.Application
import io.dropwizard.setup.Environment
import io.grpc.ServerBuilder
import io.grpc.ServerInterceptor
import io.grpc.ServerInterceptors
import org.cirruslabs.anka.controller.auth.AccessTokenServerInterceptor
import org.cirruslabs.anka.controller.config.AuthApplicationConfiguration
import org.cirruslabs.anka.controller.health.ManagerHealthCheck
import org.cirruslabs.anka.controller.health.ThreadHealthCheck
import org.cirruslabs.anka.sdk.AnkaCommunicator
import org.cirruslabs.anka.sdk.AnkaVMManager

fun main(vararg args: String) {
  ControllerApplication().run(*args)
}

class ControllerApplication : Application<AuthApplicationConfiguration>() {

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

    println("Started GRPC server on ${grpcConfig.port} port...")
    println("Current health: ${healthCheck.execute()}")

    val schedulingThread = SchedulingThread(vmManager)
    schedulingThread.isDaemon = true
    schedulingThread.setUncaughtExceptionHandler { _, ex ->
      System.err.println("Scheduling thread errored with ${ex.message}")
    }
    schedulingThread.start()

    environment.healthChecks().register("try-to-schedule-thread", ThreadHealthCheck(schedulingThread))
  }
}
