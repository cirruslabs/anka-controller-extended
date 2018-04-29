package org.cirruslabs.anka.controller

import io.grpc.stub.StreamObserver
import kotlinx.coroutines.experimental.async
import org.cirruslabs.anka.controller.grpc.ControllerGrpc
import org.cirruslabs.anka.controller.grpc.StartVMRequest
import org.cirruslabs.anka.controller.grpc.StartVMResponse
import org.cirruslabs.anka.controller.manager.AnkaVMManager

class ControllerServiceImpl(val manager: AnkaVMManager) : ControllerGrpc.ControllerImplBase() {
  override fun startVM(request: StartVMRequest, responseObserver: StreamObserver<StartVMResponse>) {
    try {
      val instanceId = manager.startVM(request.templateId)
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
      responseObserver.onError(e)
    }
  }
}
