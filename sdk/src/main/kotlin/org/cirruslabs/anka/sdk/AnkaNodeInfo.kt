package org.cirruslabs.anka.sdk

import org.json.JSONObject
import kotlin.math.max

data class AnkaNodeInfo(
  val ipAddress: String,
  val nodeId: String,
  val state: String,
  val vmCount: Int,
  val capacity: Int
) {
  val hasCapacity: Boolean
    get() = vmCount < capacity

  val remainingCapacity: Int
    get() = max(0, capacity - vmCount)

  constructor(jsonObject: JSONObject) : this(
    jsonObject.getString("ip_address"),
    jsonObject.getString("node_id"),
    jsonObject.getString("state"),
    jsonObject.getInt("vm_count"),
    jsonObject.getInt("capacity")
  )
}
