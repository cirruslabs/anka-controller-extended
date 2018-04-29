package org.cirruslabs.anka.sdk

import org.cirruslabs.anka.sdk.exceptions.AnkaException

import java.io.IOException

interface AnkaVm {

  val id: String

  val name: String

  val connectionIp: String?

  val connectionPort: Int

  val isRunning: Boolean

  val info: String

  @Throws(InterruptedException::class, IOException::class, AnkaException::class)
  fun waitForBoot(): String

  fun terminate()

}
