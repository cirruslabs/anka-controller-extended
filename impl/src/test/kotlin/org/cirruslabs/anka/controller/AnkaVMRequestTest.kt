package org.cirruslabs.anka.controller

import org.cirruslabs.anka.sdk.AnkaVMRequest
import org.junit.Test
import java.util.Comparator
import java.util.concurrent.PriorityBlockingQueue
import kotlin.test.assertEquals

class AnkaVMRequestTest {
  @Test
  fun priority() {
    val queue = PriorityBlockingQueue(100, Comparator<AnkaVMRequest> { o1, o2 -> -o1.compareTo(o2) })
    queue.add(AnkaVMRequest("base", vmName = "1", priority = 100, creationTimestamp = 1))
    queue.add(AnkaVMRequest("base", vmName = "2", priority = 100, creationTimestamp = 2))
    queue.add(AnkaVMRequest("base", vmName = "3", priority = 200))
    queue.add(AnkaVMRequest("base", vmName = "4", priority = 300))
    queue.add(AnkaVMRequest("base", vmName = "5", priority = 100, creationTimestamp = 5))
    assertEquals(listOf("4", "3", "1", "2", "5"), queue.map { it.vmName })
    assertEquals("4", queue.remove().vmName)
    assertEquals("3", queue.remove().vmName)
    assertEquals("1", queue.remove().vmName)
    assertEquals("2", queue.remove().vmName)
    assertEquals("5", queue.remove().vmName)

  }
}
