package org.cirruslabs.anka.controller

import com.google.common.util.concurrent.ListeningExecutorService
import com.google.common.util.concurrent.MoreExecutors
import com.google.common.util.concurrent.ThreadFactoryBuilder
import io.dropwizard.Application
import io.dropwizard.setup.Environment
import io.grpc.ServerBuilder
import io.grpc.ServerInterceptor
import io.grpc.ServerInterceptors
import org.cirruslabs.anka.controller.auth.AccessTokenServerInterceptor
import org.cirruslabs.anka.controller.config.AuthApplicationConfiguration
import org.cirruslabs.anka.controller.health.ManagerHealthCheck
import org.cirruslabs.anka.controller.health.ThreadHealthCheck
import org.cirruslabs.anka.controller.resources.HealthzResource
import org.cirruslabs.anka.sdk.AnkaCommunicator
import org.cirruslabs.anka.sdk.AnkaVMManager
import java.net.URL
import java.util.concurrent.Executors

fun main(vararg args: String) {
  ControllerApplication().run(*args)
}

class ControllerApplication : Application<AuthApplicationConfiguration>() {
  val mainExecutor: ListeningExecutorService = MoreExecutors.listeningDecorator(
    Executors.newCachedThreadPool(
      ThreadFactoryBuilder()
        .setNameFormat("cirrus-main-pool-%d")
        .build()
    )
  )

  override fun run(configuration: AuthApplicationConfiguration, environment: Environment) {
    val grpcConfig = configuration.grpc ?: throw IllegalStateException("grpc config should be provided!")

    val env = System.getenv()

    // for backward compatibility
    val urlViaObsoleteVars = if (env.containsKey("ANKA_HOST")) {
      "http://" + listOfNotNull(env["ANKA_HOST"], env["ANKA_PORT"]).joinToString(separator = ":") + "/"
    } else {
      null
    }

    val communicator = AnkaCommunicator(
      (env["CONTROLLER_URL"] ?: urlViaObsoleteVars)?.let { URL(it) }
        ?: throw IllegalStateException("CONTROLLER_URL environment variable should be defined!"),
      env["AUTH_USERNAME"],
      env["AUTH_PASSWORD"]
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

    environment.jersey().apply {
      val healthzResource = HealthzResource(mainExecutor, environment.healthChecks())
      register(healthzResource)
    }

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
