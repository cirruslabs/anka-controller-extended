package org.cirruslabs.anka.sdk

import java.util.*

class RequestQueue {
  private val queue = PriorityQueue<AnkaVMRequest>(Comparator<AnkaVMRequest> { o1, o2 -> -o1.compareTo(o2) })

  val size: Int
    @Synchronized get() = queue.size

  val isEmpty: Boolean
    @Synchronized get() = queue.isEmpty()

  val vmNames: List<String>
    @Synchronized get() = queue.mapNotNull { it.vmName }

  @Synchronized
  fun findByVMName(name: String): AnkaVMRequest? {
    return queue.find { it.vmName == name }
  }

  @Synchronized
  fun remove(request: AnkaVMRequest): Boolean {
    return queue.remove(request)
  }

  @Synchronized
  fun poll(): AnkaVMRequest? {
    return try {
      queue.poll()
    } catch (ex: Throwable) {
      null
    }
  }

  @Synchronized
  fun offer(vmRequest: AnkaVMRequest): Int {
    queue.offer(vmRequest)
    return Math.max(0, queue.indexOf(vmRequest) + 1)
  }
}
