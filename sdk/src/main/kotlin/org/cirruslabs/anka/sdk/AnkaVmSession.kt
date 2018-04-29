package org.cirruslabs.anka.sdk

import org.json.JSONObject

data class AnkaVmSession(
  val id: String,
  val sessionState: String,
  val vmId: String,
  var vmInfo: AnkaVmInfo? = null
) {

  constructor(id: String, jsonObject: JSONObject) : this(
    id,
    jsonObject.getString("instance_state"),
    jsonObject.getString("vmid"),
    if (jsonObject.has("vminfo")) {
      AnkaVmInfo(jsonObject.getJSONObject("vminfo"))
    } else {
      null
    }
  )
}


