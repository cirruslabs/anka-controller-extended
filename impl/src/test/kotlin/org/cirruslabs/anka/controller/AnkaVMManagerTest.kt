package org.cirruslabs.anka.controller

import org.cirruslabs.anka.sdk.AnkaCommunicator
import org.cirruslabs.anka.sdk.AnkaVmTemplate
import org.cirruslabs.anka.sdk.AnkaVMManager
import org.junit.Assert.*
import org.junit.Test

class AnkaVMManagerTest {
  val communicator = AnkaCommunicator("localhost", "8090")
  val manager = AnkaVMManager(communicator)

  val testTemplate = "osx-10.13-base"

  fun findTemplate(name: String): AnkaVmTemplate? =
    communicator.listTemplates().find { it.name == name }

  @Test
  fun testTemplatePresentede() {
    println(communicator.listTemplates())
    assertNotNull("Please make sure your local registry has $testTemplate template", findTemplate(testTemplate))
  }

  @Test
  fun testVMCreation() {
    val template = findTemplate(testTemplate)
    assertNotNull("Please make sure your local registry has $testTemplate template", template)
    val instanceId = manager.startVM(template!!.id)
    val vm = manager.waitForVMToStart(instanceId)

    try {
      val output = manager.execute(vm, "pwd\necho \$USER")
      assertEquals("/Users/anka\nanka\n", output)
    } finally {
      assertTrue(manager.stopVM(instanceId))
    }
  }
}