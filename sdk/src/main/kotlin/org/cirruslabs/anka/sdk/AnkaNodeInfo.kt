package org.cirruslabs.anka.sdk

import org.json.JSONObject

data class AnkaNodeInfo(
  val ipAddress: String,
  val nodeId: String,
  val state: String,
  val vmCount: Int,
  val capacity: Int
) {
  val hasCapacity: Boolean
    get() = vmCount < capacity

  constructor(jsonObject: JSONObject) : this(
    jsonObject.getString("ip_address"),
    jsonObject.getString("node_id"),
    jsonObject.getString("state"),
    jsonObject.getInt("vm_count"),
    jsonObject.getInt("capacity")
  )
}
