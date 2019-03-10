package org.cirruslabs.anka.sdk

import com.google.common.cache.CacheBuilder
import com.jcabi.ssh.SshByPassword
import org.cactoos.io.DeadInput
import org.cirruslabs.anka.sdk.exceptions.AnkaException
import org.cirruslabs.anka.sdk.util.MultiOutputStream
import java.io.ByteArrayOutputStream
import java.nio.charset.StandardCharsets


class AnkaVMManager(val communicator: AnkaCommunicator) {
  private val queue = RequestQueue()

  private val instanceIdCache = CacheBuilder.newBuilder()
    .maximumSize(10000)
    .build<String, String>()

  val queueSize: Int
    get() = queue.size

  fun startVM(templateName: String, tag: String? = null, vmName: String? = null, startupScript: String? = null): String {
    println("Starting VM $templateName:$tag with name $vmName...")
    val template = (communicator.listTemplates().find { it.name == templateName }
      ?: throw AnkaException("Template with name $templateName not found!"))
    val instanceId = communicator.startVm(template.id, tag, vmName, startupScript)
    println("Started VM $templateName:$tag with name $vmName! Instance id is $instanceId")
    if (vmName != null) {
      instanceIdCache.put(vmName, instanceId)
    }
    return instanceId
  }

  fun stopVM(instanceId: String): Boolean {
    println("Stopping instance $instanceId")
    return communicator.terminateVm(instanceId)
  }

  fun stopVMByName(name: String): Boolean {
    println("Stopping vm $name")
    val queuedRequest = queue.findByVMName(name)
    if (queuedRequest != null) {
      println("Removed vm $name from the queue")
      return queue.remove(queuedRequest)
    }
    var instanceId = instanceIdCache.getIfPresent(name)

    if (instanceId != null) {
      println("Got instance id $instanceId for $name vm from cache!")
    } else {
      println("Trying to find instance for $name vm via API: $instanceId...")
      instanceId = communicator.listInstances().find { it.vmInfo?.name == name }?.id
      println("Tried to find instance for $name vm via API: $instanceId")
    }

    if (instanceId == null) {
      System.err.println("Failed to find instance for $instanceId/$name!")
      return true
    }

    instanceIdCache.invalidate(name)
    println("Stopping instance $instanceId for vm $name!")
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

  fun vmStatusByName(name: String): String {
    println("Check $name VM status...")
    if (queue.findByVMName(name) != null) {
      println("VM $name is still in the queue...")
      return "Scheduling"
    }
    val instanceId = instanceIdCache.getIfPresent(name)

    if (instanceId != null) {
      println("Got instance id $instanceId for $name vm from cache!")
      val session = communicator.listInstances().find {
        it.id == instanceId
      }
      val status = session?.vmInfo?.status ?: session?.sessionState
      return status ?: throw AnkaException("VM with name $name not found!")
    } else {
      println("Tried to find instance status for $name vm via API: $instanceId")
      val session = communicator.listInstances().find {
        it.vmInfo?.name == name
      }
      val status = session?.vmInfo?.status ?: session?.sessionState
      return status ?: throw AnkaException("VM with name $name not found!")
    }
  }

  fun vmStatusByNames(vmNames: Set<String>): Map<String, String> {
    val statusesFromQueue = queue.vmNames.toSet().mapNotNull { vmName ->
      if (vmNames.contains(vmName)) Pair(vmName, "Scheduling") else null
    }.toMap()
    if (statusesFromQueue.keys.containsAll(vmNames)) {
      return statusesFromQueue
    }
    val statusesFromApi = communicator.listInstances().mapNotNull { session ->
      val instanceVmName = session.vmInfo?.name
      when {
        instanceVmName == null -> null
        vmNames.contains(instanceVmName) -> {
          val status = session.vmInfo?.status ?: session.sessionState
          Pair(instanceVmName, status)
        }
        else -> null
      }
    }.toMap()
    return statusesFromQueue + statusesFromApi
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
    return queue.offer(vmRequest)
  }

  fun tryToScheduleRemainingVMs() {
    println("Trying to schedule a VM...")
    if (queue.isEmpty) {
      println("Nothing to schedule...")
      return
    }
    val clusterRemainingCapacity = communicator.listNodes().sumBy {
      it.remainingCapacity
    }
    if (clusterRemainingCapacity == 0) {
      println("Cluster doesn't have capacity")
      return
    }
    repeat(clusterRemainingCapacity) {
      if (!tryToScheduleSingleVM()) return@repeat
    }
  }

  private fun tryToScheduleSingleVM(): Boolean {
    val request = queue.poll()
    if (request == null) {
      println("No requests to schedule!")
      return false
    }
    startVM(request.templateName, request.tag, request.vmName, request.startupScript)
    return true
  }
}
