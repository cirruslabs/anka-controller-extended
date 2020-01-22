package org.cirruslabs.anka.controller.converter

import org.cirruslabs.anka.controller.grpc.VmInfo
import org.cirruslabs.anka.sdk.AnkaVmInfo

fun AnkaVmInfo.toProto(): VmInfo {
  return VmInfo.newBuilder()
    .setUuid(uuid)
    .setName(name)
    .setNodeId(nodeId)
    .setStatus(status)
    .build()
}
