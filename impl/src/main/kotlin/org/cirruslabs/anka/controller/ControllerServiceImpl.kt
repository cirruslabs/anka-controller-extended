package org.cirruslabs.anka.controller

import io.grpc.stub.StreamObserver
import kotlinx.coroutines.experimental.async
import org.cirruslabs.anka.controller.grpc.*
import org.cirruslabs.anka.sdk.AnkaVMManager

class ControllerServiceImpl(val manager: AnkaVMManager) : ControllerGrpc.ControllerImplBase() {
  override fun startVM(request: StartVMRequest, responseObserver: StreamObserver<StartVMResponse>) {
    try {
      println("Starting VM ${request.template}:${request.tag}...")
      val instanceId = manager.startVM(
        request.template,
        if (request.tag.isNullOrEmpty()) null else request.tag,
        if (request.vmName.isNullOrEmpty()) null else request.vmName
      )
      println("Started VM $instanceId from ${request.template}:${request.tag}!")
      val response = StartVMResponse.newBuilder()
        .setVmId(instanceId)
        .build()
      responseObserver.onNext(response)
      responseObserver.onCompleted()

      async {
        val vm = manager.waitForVMToStart(instanceId)
        manager.execute(vm, request.script)
      }
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
      println("Stopping VM ${request.vmId}...")
      val response = StopVMResponse.newBuilder()
        .setSuccess(manager.stopVM(request.vmId))
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
      println("Getting status for VM ${request.vmId}...")
      val status = manager.vmInfo(request.vmId)?.vmInfo?.status ?: "NotFound"
      println("Status for VM ${request.vmId}: $status")
      val response = VMStatusResponse.newBuilder()
        .setStatus(status)
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
}
