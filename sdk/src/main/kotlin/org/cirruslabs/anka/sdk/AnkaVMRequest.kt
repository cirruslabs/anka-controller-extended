package org.cirruslabs.anka.sdk

data class AnkaVMRequest(
  val templateName: String,
  val tag: String? = null,
  val vmName: String? = null,
  val startupScript: String? = null,
  val priority: Long = 0,
  val creationTimestamp: Long = System.currentTimeMillis()
): Comparable<AnkaVMRequest> {
  override fun compareTo(other: AnkaVMRequest): Int {
    if (priority == other.priority) {
      return other.creationTimestamp.compareTo(creationTimestamp)
    }
    return priority.compareTo(other.priority)
  }
}
