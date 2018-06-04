package org.cirruslabs.anka.sdk

import com.jcabi.ssh.SshByPassword
import org.cactoos.io.DeadInput
import org.cirruslabs.anka.sdk.exceptions.AnkaException
import org.cirruslabs.anka.sdk.util.MultiOutputStream
import java.io.ByteArrayOutputStream
import java.nio.charset.StandardCharsets


class AnkaVMManager(val communicator: AnkaCommunicator) {
  fun startVM(templateName: String, tag: String? = null, vmName: String? = null, startupScript: String? = null): String {
    val template = (communicator.listTemplates().find { it.name == templateName }
      ?: throw AnkaException("Template with name $templateName not found!"))
    return communicator.startVm(template.id, tag, vmName,startupScript)
  }

  fun stopVM(instanceId: String): Boolean {
    return communicator.terminateVm(instanceId)
  }

  suspend fun waitForVMToStart(instanceId: String): AnkaVm {
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
    val output = ByteArrayOutputStream()
    println("Executing script...")
    shell.exec(
      script,
      DeadInput().stream(),
      MultiOutputStream(output, listOf(System.out)),
      MultiOutputStream(output, listOf(System.err))
    )
    println("Finished script execution!")
    return output.toString(StandardCharsets.UTF_8.toString())
  }
}
