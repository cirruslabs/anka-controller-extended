package org.cirruslabs.anka.controller

import com.google.protobuf.Empty
import io.grpc.stub.StreamObserver
import org.cirruslabs.anka.controller.converter.toProto
import org.cirruslabs.anka.controller.grpc.*
import org.cirruslabs.anka.sdk.AnkaVMManager

class ControllerServiceImpl(val manager: AnkaVMManager) : ControllerGrpc.ControllerImplBase() {
  override fun startVM(request: StartVMRequest, responseObserver: StreamObserver<StartVMResponse>) {
    try {
      println("Starting VM ${request.template}:${request.tag}...")
      val instanceId = manager.startVM(
        request.template,
        if (request.tag.isNullOrEmpty()) null else request.tag,
        if (request.vmName.isNullOrEmpty()) null else request.vmName,
        if (request.startupScript.isNullOrEmpty()) null else request.startupScript
      )
      println("Started VM $instanceId from ${request.template}:${request.tag}!")
      val response = StartVMResponse.newBuilder()
        .setVmId(instanceId)
        .build()
      responseObserver.onNext(response)
      responseObserver.onCompleted()
    } catch (e: Exception) {
      e.printStackTrace()
      val response = StartVMResponse.newBuilder()
        .setErrorMessage(e.message)
        .build()
      responseObserver.onNext(response)
      responseObserver.onCompleted()
    }
  }

  override fun stopVM(request: StopVMRequest, responseObserver: StreamObserver<StopVMResponse>) {
    try {
      println("Stopping VM ${request.vmId}${request.vmName}...")
      val (success, nodeId) = when (request.identifierCase) {
        StopVMRequest.IdentifierCase.VM_ID -> manager.stopVM(request.vmId)
        StopVMRequest.IdentifierCase.VM_NAME -> manager.stopVMByName(request.vmName)
        else -> throw IllegalStateException("No vmId or vmName is provided!")
      }
      val response = StopVMResponse.newBuilder()
        .setSuccess(success)
        .setNodeId(nodeId ?: "")
        .build()
      println("Stopped VM ${request.vmId}!")
      responseObserver.onNext(response)
      responseObserver.onCompleted()
    } catch (e: Exception) {
      e.printStackTrace()
      val response = StopVMResponse.newBuilder()
        .setErrorMessage(e.message)
        .build()
      responseObserver.onNext(response)
      responseObserver.onCompleted()
    }
  }

  override fun vmStatus(request: VMStatusRequest, responseObserver: StreamObserver<VMStatusResponse>) {
    try {
      println("Getting status for VM ${request.vmId}${request.vmName}...")
      val status = when (request.identifierCase) {
        VMStatusRequest.IdentifierCase.VM_ID ->
          manager.vmInfo(request.vmId)?.let { session ->
            session.vmInfo?.status ?: session.sessionState
          }
        VMStatusRequest.IdentifierCase.VM_NAME -> {
          manager.vmStatusByName(request.vmName)
        }
        else -> throw IllegalStateException("No vmId or vmName is provided!")
      }
      println("Status for VM ${request.vmId}${request.vmName}: $status")
      val response = VMStatusResponse.newBuilder()
        .setStatus(status ?: "NotFound")
        .build()
      responseObserver.onNext(response)
      responseObserver.onCompleted()
    } catch (e: Exception) {
      e.printStackTrace()
      val response = VMStatusResponse.newBuilder()
        .setErrorMessage(e.message)
        .build()
      responseObserver.onNext(response)
      responseObserver.onCompleted()
    }
  }

  override fun batchedVmStatus(request: BatchedVMStatusRequest, responseObserver: StreamObserver<BatchedVMStatusResponse>) {
    try {
      println("Getting status for VMs ${request.vmNamesList}")
      val statuses = manager.vmStatusByNames(request.vmNamesList.toSet())
      val response = BatchedVMStatusResponse.newBuilder()
        .putAllStatuses(statuses)
        .build()
      responseObserver.onNext(response)
      responseObserver.onCompleted()
    } catch (e: Exception) {
      e.printStackTrace()
      val response = BatchedVMStatusResponse.newBuilder()
        .setErrorMessage(e.message)
        .build()
      responseObserver.onNext(response)
      responseObserver.onCompleted()
    }
  }

  override fun scheduleVM(request: ScheduleVMRequest, responseObserver: StreamObserver<ScheduleVMResponse>) {
    try {
      println("Scheduling VM ${request.vmName} from ${request.template}:${request.tag}...")
      val index = manager.scheduleVM(
        request.template,
        if (request.tag.isNullOrEmpty()) null else request.tag,
        if (request.vmName.isNullOrEmpty()) null else request.vmName,
        if (request.startupScript.isNullOrEmpty()) null else request.startupScript,
        request.priority
      )
      println("Added VM ${request.vmName} from ${request.template}:${request.tag} in queue with number $index!")
      val response = ScheduleVMResponse.newBuilder()
        .setQueueNumber(index.toLong())
        .setQueueSize(manager.queueSize.toLong())
        .build()
      responseObserver.onNext(response)
      responseObserver.onCompleted()
    } catch (e: Exception) {
      e.printStackTrace()
      val response = ScheduleVMResponse.newBuilder()
        .setErrorMessage(e.message)
        .build()
      responseObserver.onNext(response)
      responseObserver.onCompleted()
    }
  }

  override fun listVMs(request: Empty, responseObserver: StreamObserver<VMListResponse>) {
    try {
      val instances = manager.communicator.listInstances().mapNotNull { it.vmInfo }
      val response = VMListResponse.newBuilder()
        .addAllInstances(instances.map { it.toProto() })
        .build()
      responseObserver.onNext(response)
      responseObserver.onCompleted()
    } catch (e: Exception) {
      e.printStackTrace()
      responseObserver.onError(e)
    }
  }

  override fun getStats(request: Empty, responseObserver: StreamObserver<GetStatsResponse>) {
    try {
      val instances = manager.communicator.listInstances()
      val nodes = manager.communicator.listNodes()

      val response = GetStatsResponse.newBuilder()
        .setQueueSize(manager.queueSize.toLong())
        .setInstancesSize(instances.size.toLong())
        .setActiveNodesSize(nodes.count { it.state?.toLowerCase() == "active" }.toLong())
        .setOfflineNodesSize(nodes.count { it.state?.toLowerCase() == "offline" }.toLong())
        .putAllInstanceCountByStatuses(
          instances
            .groupBy { it.sessionState ?: "Undefined" }
            .mapValues { (_, instances) -> instances.size.toLong() }
        )
        .setRemainingCapacity(nodes.map { it.remainingCapacity }.sum().toLong())
        .build()
      responseObserver.onNext(response)
      responseObserver.onCompleted()
    } catch (e: Exception) {
      e.printStackTrace()
      responseObserver.onError(e)
    }
  }
}
