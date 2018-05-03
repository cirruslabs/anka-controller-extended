package org.cirruslabs.anka.sdk

import com.jcabi.ssh.Shell
import com.jcabi.ssh.SshByPassword
import org.cirruslabs.anka.sdk.exceptions.AnkaException
import java.io.ByteArrayInputStream


class AnkaVMManager(val communicator: AnkaCommunicator) {
  fun startVM(templateName: String, tag: String? = null): String {
    val template = (communicator.listTemplates().find { it.name == templateName }
      ?: throw AnkaException("Template with name $templateName not found!"))
    return communicator.startVm(template.id, tag, template.name)
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
    shell.exec(
      "cat > /tmp/cirrus-build.sh",
      ByteArrayInputStream(script.toByteArray()),
      System.out,
      System.err
    )
    return Shell.Plain(shell).exec("chmod +x /tmp/cirrus-build.sh && /bin/bash /tmp/cirrus-build.sh")
  }
}
