package org.cirruslabs.anka.controller.config

import io.dropwizard.Configuration

class AuthApplicationConfiguration : Configuration() {
  var grpc: GrpcConfig? = null
}
