package org.cirruslabs.anka.controller

import io.grpc.Channel
import io.grpc.Metadata
import io.grpc.stub.MetadataUtils
import org.cirruslabs.anka.controller.grpc.ControllerGrpc

object ClientFactory {
  val AUTHORIZATION_HEADER: Metadata.Key<String> = Metadata.Key.of("authorization", Metadata.ASCII_STRING_MARSHALLER)

  fun create(channel: Channel, token: String? = null): ControllerGrpc.ControllerFutureStub {
    val futureStub = ControllerGrpc.newFutureStub(channel)
    return if (token != null) {
      MetadataUtils.attachHeaders(
        futureStub,
        Metadata().apply {
          put(AUTHORIZATION_HEADER, "Bearer $token")
        }
      )
    } else {
      futureStub
    }
  }
}
