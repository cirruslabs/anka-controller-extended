package org.cirruslabs.anka.sdk

import org.cirruslabs.anka.sdk.exceptions.AnkaException

import java.io.IOException

class ConcAnkaVm(override val id: String, private val communicator: AnkaCommunicator, private val sshConnectionPort: Int) : AnkaVm {
  private val waitUnit = 4000
  private val maxRunningTimeout = waitUnit * 20
  private val maxIpTimeout = waitUnit * 20
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
      try {
        if (this.cachedVmSession == null || this.shouldInvalidate()) {
          val session = this.communicator.showVm(this.id)
          if (session != null) {
            this.cachedVmSession = session
          } else {
            logger.info("info for vm is null")
          }
        }
        return this.cachedVmSession
      } catch (e: AnkaException) {
        e.printStackTrace()
        return null
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

  @Throws(InterruptedException::class, IOException::class, AnkaException::class)
  override fun waitForBoot(): String {
    logger.info(String.format("waiting for vm %s to boot", this.id))
    var timeWaited = 0

    while (status == "Scheduling") {
      Thread.sleep(waitUnit.toLong())
      logger.info(String.format("waiting for vm %s %d to start", this.id, timeWaited))
    }

    if (status != "Starting" && status != "Started" && status != "Scheduled") {
      logger.info(String.format("vm %s in unexpected state %s, terminating", this.id, status))
      this.terminate()
      throw IOException("could not start vm")
    }

    while (status != "Started" || sessionInfoCache == null) {
      // wait for the vm to spin up TODO: put this in const
      Thread.sleep(waitUnit.toLong())
      timeWaited += waitUnit
      logger.info(String.format("waiting for vm %s %d to boot", this.id, timeWaited))
      if (timeWaited > maxRunningTimeout) {
        this.terminate()
        throw IOException("could not start vm")

      }
    }

    var ip: String?
    timeWaited = 0
    logger.info(String.format("waiting for vm %s to get an ip ", this.id))
    while (true) { // wait to get machine ip

      ip = this.ip
      if (ip != null)
        return ip

      Thread.sleep(waitUnit.toLong())
      timeWaited += waitUnit
      logger.info(String.format("waiting for vm %s %d to get ip ", this.id, timeWaited))
      if (timeWaited > maxIpTimeout) {
        this.terminate()
        throw IOException("VM started but couldn't acquire ip")
      }
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

    private val logger = java.util.logging.Logger.getLogger("anka-sdk")
  }
}
