package org.cirruslabs.anka.sdk

import org.junit.Assert.*
import org.junit.Test
import java.util.concurrent.PriorityBlockingQueue

class RequestQueueTest {
  @Test
  fun priority() {
    val queue = RequestQueue()
    queue.offer(AnkaVMRequest("base", vmName = "1", priority = 100, creationTimestamp = 1))
    queue.offer(AnkaVMRequest("base", vmName = "2", priority = 100, creationTimestamp = 2))
    queue.offer(AnkaVMRequest("base", vmName = "3", priority = 200))
    queue.offer(AnkaVMRequest("base", vmName = "4", priority = 300))
    queue.offer(AnkaVMRequest("base", vmName = "5", priority = 100, creationTimestamp = 5))
    kotlin.test.assertEquals(listOf("4", "3", "1", "2", "5"), queue.vmNames)
    kotlin.test.assertEquals("4", queue.poll()?.vmName)
    kotlin.test.assertEquals("3", queue.poll()?.vmName)
    kotlin.test.assertEquals("1", queue.poll()?.vmName)
    kotlin.test.assertEquals("2", queue.poll()?.vmName)
    kotlin.test.assertEquals("5", queue.poll()?.vmName)
  }
}
