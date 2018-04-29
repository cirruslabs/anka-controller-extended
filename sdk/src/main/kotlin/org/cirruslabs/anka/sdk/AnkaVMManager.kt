package org.cirruslabs.anka.sdk

import com.jcabi.ssh.Shell
import com.jcabi.ssh.SshByPassword


class AnkaVMManager(val communicator: AnkaCommunicator) {
  fun startVM(templateId: String, tag: String? = null): String {
    return communicator.startVm(templateId, tag, null)
  }

  fun stopVM(instanceId: String): Boolean {
    return communicator.terminateVm(instanceId)
  }

  fun waitForVMToStart(instanceId: String): AnkaVm {
    val vm = AnkaVmImpl(instanceId, communicator, 22)
    vm.waitForBoot()
    return vm
  }

  fun vmInfo(instanceId: String): AnkaVmSession? {
    return communicator.showVm(instanceId)
  }

  fun execute(vm: AnkaVm, script: String): String {
    val shell = SshByPassword(vm.connectionIp, vm.connectionPort, "anka", "admin")
    // todo: investigate how to do fire and forget
    return Shell.Plain(shell).exec(script)
  }
}
