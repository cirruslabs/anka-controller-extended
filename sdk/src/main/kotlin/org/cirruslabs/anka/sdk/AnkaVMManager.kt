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

  private val templateIdCache = CacheBuilder.newBuilder()
    .maximumSize(10000)
    .build<String, String>()

  private val failureCache = CacheBuilder.newBuilder()
    .maximumSize(1000)
    .build<String, String>()

  val queueSize: Int
    get() = queue.size

  fun startVM(templateName: String, tag: String? = null, vmName: String? = null, startupScript: String? = null): String {
    println("Starting VM $templateName:$tag with name $vmName...")
    val templateId = findTemplateByName(templateName)
    val instanceId = communicator.startVm(templateId, tag, vmName, startupScript)
    println("Started VM $templateName:$tag with name $vmName! Instance id is $instanceId")
    if (vmName != null) {
      instanceIdCache.put(vmName, instanceId)
    }
    return instanceId
  }

  private fun findTemplateByName(templateName: String): String {
    return templateIdCache.get(templateName) {
      (communicator.listTemplates().find { it.name == templateName }?.id
        ?: throw AnkaException("Template with name $templateName not found!"))
    }
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
      println("Failed to find instance for $$name!")
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
    val failedStatuses = vmNames.mapNotNull { vmName ->
      failureCache.getIfPresent(vmName)?.let { Pair(vmName, "Failed") }
    }.toMap()
    val allInMemoryStatuses = statusesFromQueue + failedStatuses
    if (allInMemoryStatuses.keys.containsAll(vmNames)) {
      return allInMemoryStatuses
    }

    val instances = communicator.listInstances()
    val statusesFromApiByName = instances.mapNotNull { session ->
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

    val statusesFromApiByVmId = vmNames.mapNotNull { instanceVmName ->
      instanceIdCache.getIfPresent(instanceVmName)?.let { instanceId ->
        instances.find { it.vmId == instanceId }?.let { session ->
          val status = session.vmInfo?.status ?: session.sessionState
          Pair(instanceVmName, status)
        }
      }
    }.toMap()
    return statusesFromQueue + statusesFromApiByName + statusesFromApiByVmId
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
    val schedulingInstances = communicator.listInstances().count { it.sessionState == "Scheduling" }
    val clusterRemainingCapacity = communicator.listNodes().sumBy {
      it.remainingCapacity
    }
    println("Cluster remaining capacity is $clusterRemainingCapacity and there are $schedulingInstances scheduling instances...")
    if ((clusterRemainingCapacity - schedulingInstances) <= 0) {
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
    try {
      startVM(request.templateName, request.tag, request.vmName, request.startupScript)
    } catch (e: Exception) {
      println("Failed to schedule VM ${request.vmName}: ${e.message}")
      val vmName = request.vmName
      if (vmName != null) {
        failureCache.put(vmName, e.message ?: "Failed to start a VM!")
      }
    }
    return true
  }
}
