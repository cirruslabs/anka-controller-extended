package org.cirruslabs.anka.sdk.exceptions

class AnkaException(message: String, e: Throwable? = null) : Exception(message, e) {
  constructor(e: Throwable) : this(e.message ?: "", e)
}
