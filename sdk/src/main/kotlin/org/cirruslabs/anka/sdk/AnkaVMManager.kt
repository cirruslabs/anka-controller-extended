package org.cirruslabs.anka.sdk

import com.jcabi.ssh.SshByPassword
import org.cactoos.io.DeadInput
import org.cirruslabs.anka.sdk.exceptions.AnkaException
import org.cirruslabs.anka.sdk.util.MultiOutputStream
import java.io.ByteArrayOutputStream
import java.nio.charset.StandardCharsets
import java.util.*


class AnkaVMManager(val communicator: AnkaCommunicator) {
  private val queue: PriorityQueue<AnkaVMRequest> = PriorityQueue()
  val queueSize: Int
    get() = queue.size

  fun startVM(templateName: String, tag: String? = null, vmName: String? = null, startupScript: String? = null): String {
    val template = (communicator.listTemplates().find { it.name == templateName }
      ?: throw AnkaException("Template with name $templateName not found!"))
    return communicator.startVm(template.id, tag, vmName,startupScript)
  }

  fun stopVM(instanceId: String): Boolean {
    return communicator.terminateVm(instanceId)
  }

  fun stopVMByName(name: String): Boolean {
    val queuedRequest = queue.find { it.vmName == name }
    if (queuedRequest != null) {
      return queue.remove(queuedRequest)
    }
    val session = communicator.listInstances().find { it.vmInfo?.name == name } ?: return false
    return communicator.terminateVm(session.id)
  }

  suspend fun waitForVMToStart(instanceId: String): AnkaVm {
    val vm = AnkaVmImpl(instanceId, communicator, 22)
    vm.waitForBoot()
    return vm
  }

  fun vmInfo(instanceId: String): AnkaVmSession? {
    return communicator.showVm(instanceId)
  }

  fun vmStatusByName(name: String): String {
    queue.find { it.vmName == name } ?: return "Scheduling"
    return communicator.listInstances().find {
      it.vmInfo?.name == name
    }?.vmInfo?.status ?: throw AnkaException("VM with name $name not found!")
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

  fun scheduleVM(templateName: String, tag: String? = null, vmName: String? = null, startupScript: String? = null, priority: Long = 0): Int {
    val vmRequest = AnkaVMRequest(templateName, tag, vmName, startupScript, priority)
    queue.offer(vmRequest)
    tryToSchedule()
    return Math.max(0, queue.indexOf(vmRequest) + 1)
  }

  fun tryToSchedule(): Boolean {
    if (queue.isEmpty()) {
      return false
    }
    val instanceWaitingScheduling = communicator.listInstances().find {
      it.vmInfo?.status?.toLowerCase() == "scheduling" || it.vmInfo?.status?.toLowerCase() == "scheduled"
    }
    if (instanceWaitingScheduling != null) {
      return false
    }
    val request = synchronized(queue) {
      try {
        queue.remove()
      } catch (e: Exception) {
        null
      }
    } ?: return false
    startVM(request.templateName, request.tag, request.vmName, request.startupScript)
    return true
  }
}
