package org.cirruslabs.anka.controller.auth

import io.grpc.*
import org.cirruslabs.anka.controller.ClientFactory

class AccessTokenServerInterceptor(val token: String) : ServerInterceptor {
  override fun <ReqT : Any, RespT : Any> interceptCall(
    call: ServerCall<ReqT, RespT>,
    metadata: Metadata,
    next: ServerCallHandler<ReqT, RespT>
  ): ServerCall.Listener<ReqT> {
    val providedToken = metadata.get(ClientFactory.AUTHORIZATION_HEADER)?.substring("Bearer ".length)
      ?: throw StatusException(Status.UNAUTHENTICATED)
    if (providedToken != token) {
      throw StatusException(Status.UNAUTHENTICATED)
    }
    return next.startCall(call, metadata)
  }
}
