package com.veertu.ankaMgmtSdk;

public class AnkaVmTemplate extends AnkaVMRepresentation {

  public AnkaVmTemplate(String uuid, String name) {
    this.id = uuid;
    this.name = name;
  }

  @Override
  public String toString() {
    return "AnkaVmTemplate{" +
      "id='" + id + '\'' +
      ", name='" + name + '\'' +
      '}';
  }
}
