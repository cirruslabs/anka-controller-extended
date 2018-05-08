package org.cirruslabs.anka.sdk

import kotlinx.coroutines.experimental.delay
import kotlinx.coroutines.experimental.runBlocking
import org.cirruslabs.anka.sdk.exceptions.AnkaException

import java.io.IOException
import java.time.Duration
import java.time.LocalTime
import java.util.logging.Logger

class AnkaVmImpl(override val id: String, private val communicator: AnkaCommunicator, private val sshConnectionPort: Int) : AnkaVm {
  private val waitDuration = Duration.ofSeconds(2)
  private val maxRunningTimeout = Duration.ofMinutes(5)
  private val maxIpTimeout = Duration.ofMinutes(2)
  private val cacheTime = 60 * 5 * 1000 // 5 minutes
  private var cachedVmSession: AnkaVmSession? = null
  private var lastCached = 0

  private val status: String
    @Throws(AnkaException::class)
    get() {
      val session = this.communicator.showVm(this.id)
      return session!!.sessionState
    }

  private val ip: String?
    @Throws(AnkaException::class)
    get() {
      val session = this.communicator.showVm(this.id)
      if (session!!.vmInfo == null) {
        return null
      }
      val ip = session.vmInfo!!.vmIp
      return if (ip != "") {
        ip
      } else null
    }

  private val sessionInfoCache: AnkaVmSession?
    get() {
      return try {
        if (this.cachedVmSession == null || this.shouldInvalidate()) {
          val session = this.communicator.showVm(this.id)
          if (session != null) {
            this.cachedVmSession = session
          } else {
            logger.info("info for vm is null")
          }
        }
        this.cachedVmSession
      } catch (e: AnkaException) {
        e.printStackTrace()
        null
      }

    }

  override val name: String
    get() {
      val session = this.sessionInfoCache
      return session!!.vmInfo!!.name
    }

  override val connectionIp: String?
    get() {
      val session = this.sessionInfoCache
      return session?.vmInfo?.hostIp

    }

  override val connectionPort: Int
    get() {
      val session = this.sessionInfoCache

      for (rule in session!!.vmInfo!!.portForwardingRules) {
        if (rule.guestPort == this.sshConnectionPort) {
          return rule.hostPort
        }
      }
      return 0
    }

  override val isRunning: Boolean
    get() {
      val session = this.sessionInfoCache
      return session!!.sessionState == "Started" && session.vmInfo!!.status == "running"
    }

  override val info: String
    get() {
      val session = this.sessionInfoCache
      return String.format("host: %s, id: %s, machine ip: %s",
        session!!.vmInfo!!.hostIp, session.vmInfo!!.uuid, session.vmInfo!!.vmIp)
    }


  init {
    logger.info(String.format("init VM %s", id))
  }

  private fun unixTime(): Int {
    return (System.currentTimeMillis() / 1000L).toInt()
  }

  private fun shouldInvalidate(): Boolean {
    val timeNow = unixTime()
    if (timeNow - this.lastCached > this.cacheTime) {
      this.lastCached = timeNow
      return true
    }
    return false
  }

  @Throws(AnkaException::class)
  override fun waitForBoot(): String {
    return runBlocking {
      logger.info(String.format("waiting for vm %s to boot", id))
      var waitStarted = LocalTime.now()
      fun timeWaited(): Duration = Duration.between(waitStarted, LocalTime.now())

      while (status == "Scheduling") {
        delay(waitDuration.toMillis())
        logger.info(String.format("waiting for vm %s %s to start", id, timeWaited()))
      }
      waitStarted = LocalTime.now()

      if (status != "Starting" && status != "Started" && status != "Scheduled") {
        logger.info(String.format("vm %s in unexpected state %s, terminating", id, status))
        terminate()
        throw AnkaException("could not start vm")
      }

      while (status != "Started" || sessionInfoCache == null) {
        delay(waitDuration.toMillis())
        logger.info(String.format("waiting for vm %s %s to boot", id, timeWaited()))
        if (timeWaited() > maxRunningTimeout) {
          terminate()
          throw AnkaException("could not start vm")
        }
      }
      waitStarted = LocalTime.now()

      logger.info(String.format("waiting for vm %s to get an ip ", id))
      while (true) { // wait to get machine ip
        if (ip != null)
          break

        delay(waitDuration.toMillis())
        logger.info(String.format("waiting for vm %s %s to get ip ", id, timeWaited()))
        if (timeWaited() > maxIpTimeout) {
          terminate()
          throw IOException("VM started but couldn't acquire ip")
        }
      }
      ip ?: throw AnkaException("Failed to wait for instance to start!")
    }
  }

  override fun terminate() {
    try {
      this.communicator.terminateVm(this.id)
    } catch (e: AnkaException) {
      e.printStackTrace()
    }
  }

  companion object {
    private val logger = Logger.getLogger("anka-sdk")
  }
}
