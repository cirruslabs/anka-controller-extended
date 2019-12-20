package org.cirruslabs.anka.controller

import io.grpc.ManagedChannelBuilder
import org.cirruslabs.anka.controller.grpc.VMStatusRequest
import org.junit.Test
import kotlin.test.assertEquals

/**
 * A useful test for checking connection to an Anka Controller Extended
 */
abstract class ClientFactoryTest {
  @Test
  fun testConnection() {
    val channel = ManagedChannelBuilder.forTarget("IP:8239")
      .usePlaintext()
      .build()
    val client = ClientFactory.create(channel)
    val request = VMStatusRequest.newBuilder()
      .setVmId("not-exists")
      .build()

    assertEquals("NotFound", client.vmStatus(request).get().status)
  }
}
