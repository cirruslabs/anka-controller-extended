package org.cirruslabs.anka.controller

import kotlinx.coroutines.runBlocking
import org.cirruslabs.anka.sdk.AnkaCommunicator
import org.cirruslabs.anka.sdk.AnkaVMManager
import org.cirruslabs.anka.sdk.AnkaVmTemplate
import org.junit.Assert.*
import org.junit.Test

class AnkaVMManagerTest {
  val communicator = AnkaCommunicator("10.254.55.2", "80")
  val manager = AnkaVMManager(communicator)

  val testTemplate = "high-sierra-base"

  fun findTemplate(name: String): AnkaVmTemplate? =
    communicator.listTemplates().find { it.name == name }

//  @Test
  fun testCapacity() {
    println(communicator.listNodes())
  }

//  @Test
  fun testTemplatePresented() {
    println(communicator.listTemplates())
    assertNotNull("Please make sure your local registry has $testTemplate template", findTemplate(testTemplate))
  }

//  @Test
  fun testVMCreation() {
    assertNotNull("Please make sure your local registry has $testTemplate template", findTemplate(testTemplate))
    val instanceId = manager.startVM(testTemplate)
    val vm = runBlocking {
      manager.waitForVMToStart(instanceId)
    }

    try {
      val output = manager.execute(vm, "pwd\necho \$USER")
      assertEquals("/Users/anka\nanka\n", output)
    } finally {
      assertTrue(manager.stopVM(instanceId))
    }
  }

//  @Test
  fun testVMScheduling() {
    assertNotNull("Please make sure your local registry has $testTemplate template", findTemplate(testTemplate))
    val vmName = "test2"
    manager.scheduleVM(testTemplate, vmName = vmName)
    assertTrue(manager.stopVMByName(vmName))
  }
}
