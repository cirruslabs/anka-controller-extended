package org.cirruslabs.anka.controller

import org.cirruslabs.anka.sdk.AnkaCommunicator
import io.dropwizard.Application
import io.dropwizard.setup.Environment
import io.grpc.ServerBuilder
import io.grpc.ServerInterceptors
import org.cirruslabs.anka.controller.config.AuthApplicationConfiguration
import org.cirruslabs.anka.controller.health.ManagerHealthCheck
import org.cirruslabs.anka.sdk.AnkaVMManager

fun main(vararg args: String) {
  ControllerApplication().run(*args)
}

class ControllerApplication : Application<AuthApplicationConfiguration>() {
  override fun run(configuration: AuthApplicationConfiguration, environment: Environment) {
    val grpcConfig = configuration.grpc ?: throw IllegalStateException("grpc config should be provided!")

    val communicator = AnkaCommunicator(
      System.getenv()["ANKA_HOST"] ?: throw IllegalStateException("ANKA_HOST environment variable should be defined!"),
      System.getenv()["ANKA_PORT"] ?: throw IllegalStateException("ANKA_PORT environment variable should be defined!")
    )

    val vmManager = AnkaVMManager(communicator)

    val serviceImpl = ControllerServiceImpl(vmManager)

    ServerBuilder.forPort(grpcConfig.port)
      .addService(ServerInterceptors.intercept(serviceImpl))
      .build()
      .start()

    // available at /healthcheck
    val healthCheck = ManagerHealthCheck(vmManager)
    environment.healthChecks().register("vm-manager", healthCheck)

    println("Started GRPC server on ${grpcConfig.port} port...")
    println("Current health: ${healthCheck.execute()}")
  }
}
