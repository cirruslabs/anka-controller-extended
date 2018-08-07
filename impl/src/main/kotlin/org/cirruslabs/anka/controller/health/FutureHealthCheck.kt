package org.cirruslabs.anka.controller.health

import com.codahale.metrics.health.HealthCheck
import java.util.concurrent.Future

class FutureHealthCheck<V>(val future: Future<V>) : HealthCheck() {
  override fun check(): Result {
    return when {
      future.isCancelled -> Result.unhealthy("Future is canceled!")
      future.isDone -> Result.unhealthy("Future is done!")
      else -> Result.healthy("Future is executing")
    }
  }
}
