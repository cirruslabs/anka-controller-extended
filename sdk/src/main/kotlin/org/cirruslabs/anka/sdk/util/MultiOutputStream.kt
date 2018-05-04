package org.cirruslabs.anka.sdk.util

import java.io.OutputStream

class MultiOutputStream(val defaultStream: OutputStream, defaultAdditionalStreams: List<OutputStream> = emptyList()) : OutputStream() {
  private val additionalStreams = defaultAdditionalStreams.toMutableList()

  fun addStream(stream: OutputStream): Boolean {
    return additionalStreams.add(stream)
  }

  override fun write(b: Int) {
    return defaultStream.write(b).also {
      additionalStreams.forEach {
        it.write(b)
      }
    }
  }
}
