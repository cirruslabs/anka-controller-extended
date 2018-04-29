package org.cirruslabs.anka.sdk

import org.json.JSONObject

data class PortForwardingRule(
  val hostPort: Int,
  val guestPort: Int
) {
  constructor(portForwardingJsonObj: JSONObject) : this(
    portForwardingJsonObj.getInt("host_port"),
    portForwardingJsonObj.getInt("guest_port")
  )
}
