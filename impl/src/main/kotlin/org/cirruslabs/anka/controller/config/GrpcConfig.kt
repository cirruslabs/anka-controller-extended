package org.cirruslabs.anka.controller.config

data class GrpcConfig(var port: Int) {
  constructor() : this(8239)
}
