package org.cirruslabs.anka.controller.health

import com.codahale.metrics.health.HealthCheck

class ThreadHealthCheck(val thread: Thread) : HealthCheck() {
  override fun check(): Result {
    return when {
      !thread.isAlive -> Result.unhealthy("Thread is not alive!")
      thread.isInterrupted -> Result.unhealthy("Thread was interrupted!")
      else -> Result.healthy("Thread is executing")
    }
  }
}
