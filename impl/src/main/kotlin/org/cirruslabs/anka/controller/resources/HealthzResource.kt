package org.cirruslabs.anka.controller.resources

import com.codahale.metrics.health.HealthCheckRegistry
import java.util.concurrent.ExecutorService
import java.util.concurrent.atomic.AtomicBoolean
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse
import javax.ws.rs.GET
import javax.ws.rs.InternalServerErrorException
import javax.ws.rs.Path
import javax.ws.rs.Produces
import javax.ws.rs.core.Context
import javax.ws.rs.core.MediaType

@Path("/healthz")
@Produces(MediaType.TEXT_PLAIN)
class HealthzResource(val executor: ExecutorService, val healthChecks: HealthCheckRegistry) {
  private val lastIsHealthyCheckResult = AtomicBoolean(true)

  val wasHealthyDuringLastCheck: Boolean
    get() = lastIsHealthyCheckResult.get()

  @GET
  fun check(@Context req: HttpServletRequest, @Context resp: HttpServletResponse): String {
    val failedChecks = healthChecks.runHealthChecks(executor).filterValues { !it.isHealthy }
    if (failedChecks.isNotEmpty()) {
      val message = "${failedChecks.size} health checks failed: ${failedChecks.keys.joinToString()}!"
      failedChecks.forEach { name, details -> println("$name failed with: ${details.message}") }
      lastIsHealthyCheckResult.set(false)
      throw InternalServerErrorException(message)
    }
    lastIsHealthyCheckResult.set(true)
    return "Healthy!"
  }
}
