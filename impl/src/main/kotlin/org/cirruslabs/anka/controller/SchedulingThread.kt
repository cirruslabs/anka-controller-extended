package org.cirruslabs.anka.controller

import org.cirruslabs.anka.sdk.AnkaVMManager
import java.time.Duration

class SchedulingThread(
  val vmManager: AnkaVMManager,
  val sleepDuration: Duration = Duration.ofSeconds(15)
) : Thread("vm-scheduler") {
  override fun run() {
    while (true) {
      try {
        sleep(sleepDuration.toMillis())
        vmManager.tryToScheduleRemainingVMs()
      } catch (e: Throwable) {
        System.err.println("Failed to schedule: ${e.message}")
        e.printStackTrace(System.err)
      }
    }
  }
}
