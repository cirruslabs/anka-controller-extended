package com.veertu.ankaMgmtSdk;

import org.json.JSONObject;

public class AnkaNodeInfo {

  private final String ipAddress;
  private final String nodeId;
  private final String state;

  public AnkaNodeInfo(JSONObject jsonObject) {
    this.ipAddress = jsonObject.getString("ip_address");
    this.nodeId = jsonObject.getString("node_id");
    this.state = jsonObject.getString("state");
  }

  @Override
  public String toString() {
    return "AnkaNodeInfo{" +
      "ipAddress='" + ipAddress + '\'' +
      ", nodeId='" + nodeId + '\'' +
      ", state='" + state + '\'' +
      '}';
  }
}
