package org.cirruslabs.anka.controller.health

import com.codahale.metrics.health.HealthCheck
import org.cirruslabs.anka.sdk.AnkaVMManager

class ManagerHealthCheck(val manager: AnkaVMManager): HealthCheck() {
  override fun check(): Result {
    return try {
      manager.communicator.listInstances()
      Result.healthy("Healthy connection to Anka!")
    } catch (e: Exception) {
      Result.unhealthy(e)
    }.also {
      println("Health check: ${it.message}")
    }
  }
}
