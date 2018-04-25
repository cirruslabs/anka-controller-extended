package org.cirruslabs.anka.controller

import com.veertu.ankaMgmtSdk.AnkaMgmtCommunicator
import com.veertu.ankaMgmtSdk.AnkaVmSession
import kotlinx.coroutines.experimental.delay
import kotlinx.coroutines.experimental.runBlocking

class AnkaVMManager(val communicator: AnkaMgmtCommunicator) {
  fun startVM(templateId: String, tag: String? = null): String {
    return communicator.startVm(templateId, tag, null)
  }
  fun stopVM(instanceId: String): Boolean {
    return communicator.terminateVm(instanceId)
  }

  fun waitForVMToStart(instanceId: String): AnkaVmSession {
    return runBlocking {
      var vmInfo = vmInfo(instanceId)
      while(true) {
        println(vmInfo)
        if (vmInfo.sessionState == "Scheduling") {
          delay(10_000)
          vmInfo = vmInfo(instanceId)
        } else {
          break
        }
      }
      vmInfo
    }
  }

  fun vmInfo(instanceId: String): AnkaVmSession {
    return communicator.showVm(instanceId)
  }
}
