package org.cirruslabs.anka.sdk

data class AnkaVMRequest(
  val templateName: String,
  val tag: String? = null,
  val vmName: String? = null,
  val startupScript: String? = null,
  val priority: Long = 0
): Comparable<AnkaVMRequest> {
  override fun compareTo(other: AnkaVMRequest): Int {
    return priority.compareTo(other.priority)
  }
}
