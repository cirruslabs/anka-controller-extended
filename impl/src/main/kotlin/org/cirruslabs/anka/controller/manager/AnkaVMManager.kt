package org.cirruslabs.anka.controller.manager

import com.jcabi.ssh.Shell
import com.jcabi.ssh.SshByPassword
import com.veertu.ankaMgmtSdk.AnkaMgmtCommunicator
import com.veertu.ankaMgmtSdk.AnkaMgmtVm
import com.veertu.ankaMgmtSdk.AnkaVmSession
import com.veertu.ankaMgmtSdk.ConcAnkaMgmtVm


class AnkaVMManager(val communicator: AnkaMgmtCommunicator) {
  fun startVM(templateId: String, tag: String? = null): String {
    return communicator.startVm(templateId, tag, null)
  }

  fun stopVM(instanceId: String): Boolean {
    return communicator.terminateVm(instanceId)
  }

  fun waitForVMToStart(instanceId: String): AnkaMgmtVm {
    val vm = ConcAnkaMgmtVm(instanceId, communicator, 22)
    vm.waitForBoot()
    return vm
  }

  fun vmInfo(instanceId: String): AnkaVmSession {
    return communicator.showVm(instanceId)
  }

  fun execute(vm: AnkaMgmtVm, script: String): String {
    val shell = SshByPassword(vm.connectionIp, vm.connectionPort, "anka", "admin")
    // todo: investigate how to do fire and forget
    return Shell.Plain(shell).exec(script)
  }
}
