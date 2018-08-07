package org.cirruslabs.anka.controller

import org.cirruslabs.anka.sdk.AnkaVMRequest
import org.junit.Test
import java.util.concurrent.PriorityBlockingQueue
import kotlin.test.assertEquals

class AnkaVMRequestTest {
  @Test
  fun priority() {
    val queue = PriorityBlockingQueue(100, Comparator<AnkaVMRequest> { o1, o2 -> o2.compareTo(o1) })
    queue.add(AnkaVMRequest("base", vmName = "1", priority = 101))
    queue.add(AnkaVMRequest("base", vmName = "2", priority = 102))
    queue.add(AnkaVMRequest("base", vmName = "3", priority = 200))
    queue.add(AnkaVMRequest("base", vmName = "4", priority = 300))
    queue.add(AnkaVMRequest("base", vmName = "5", priority = 105))
    assertEquals("4", queue.poll().vmName)
    assertEquals("3", queue.poll().vmName)
    assertEquals("5", queue.poll().vmName)
    assertEquals("2", queue.poll().vmName)
    assertEquals("1", queue.poll().vmName)
  }
}
