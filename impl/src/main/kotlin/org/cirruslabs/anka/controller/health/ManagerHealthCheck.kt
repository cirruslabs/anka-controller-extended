package org.cirruslabs.anka.controller.health

import com.codahale.metrics.health.HealthCheck
import org.cirruslabs.anka.controller.manager.AnkaVMManager

class ManagerHealthCheck(val manager: AnkaVMManager): HealthCheck() {
  override fun check(): Result {
    return try {
      val templates = manager.communicator.listTemplates()
      if (templates.isNotEmpty()) {
        Result.healthy("Number of templates available: ${templates.size}")
      } else {
        Result.unhealthy("Controller returned 0 templates!")
      }
    } catch (e: Exception) {
      Result.unhealthy(e)
    }
  }
}
