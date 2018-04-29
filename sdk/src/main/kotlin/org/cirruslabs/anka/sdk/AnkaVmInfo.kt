package org.cirruslabs.anka.sdk

import org.json.JSONObject

data class AnkaVmInfo(
  val uuid: String,
  val name: String,
  val status: String,
  val vmIp: String,
  val hostIp: String,
  val portForwardingRules: List<PortForwardingRule>
) {
  constructor(jsonObject: JSONObject) : this(
    jsonObject.getString("uuid"),
    jsonObject.getString("name"),
    jsonObject.getString("status"),
    jsonObject.getString("ip"),
    jsonObject.getString("host_ip"),
    if (!jsonObject.isNull("port_forwarding")) {
      jsonObject.getJSONArray("port_forwarding").map {
        PortForwardingRule(it as JSONObject)
      }
    } else {
      emptyList<PortForwardingRule>()
    }
  )
}
