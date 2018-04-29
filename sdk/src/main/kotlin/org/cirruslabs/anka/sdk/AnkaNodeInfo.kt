package org.cirruslabs.anka.sdk

import org.json.JSONObject

data class AnkaNodeInfo(
  val ipAddress: String,
  val nodeId: String,
  val state: String
) {

  constructor(jsonObject: JSONObject) : this(
    jsonObject.getString("ip_address"),
    jsonObject.getString("node_id"),
    jsonObject.getString("state")

  )
}
