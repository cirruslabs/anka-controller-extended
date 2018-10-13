package org.cirruslabs.anka.controller.health

import com.codahale.metrics.health.HealthCheck
import org.cirruslabs.anka.sdk.AnkaVMManager

class ManagerHealthCheck(val manager: AnkaVMManager): HealthCheck() {
  override fun check(): Result {
    return try {
      val templates = manager.communicator.listNodes()
      if (templates.isNotEmpty()) {
        Result.healthy("Number of nodes available: ${templates.size}")
      } else {
        Result.unhealthy("Controller returned 0 nodes!")
      }
    } catch (e: Exception) {
      Result.unhealthy(e)
    }.also {
      println("Health check: ${it.message}")
    }
  }
}
