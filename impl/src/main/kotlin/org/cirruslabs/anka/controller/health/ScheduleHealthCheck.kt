package org.cirruslabs.anka.controller.health

import com.codahale.metrics.health.HealthCheck
import org.cirruslabs.anka.sdk.AnkaVMManager

class ScheduleHealthCheck(val manager: AnkaVMManager): HealthCheck() {
  override fun check(): Result {
    return try {
      manager.tryToSchedule()
      Result.healthy("Scheduling is not hanging!")
    } catch (e: Exception) {
      Result.unhealthy(e)
    }.also {
      println("Health check: ${it.message}")
    }
  }
}
