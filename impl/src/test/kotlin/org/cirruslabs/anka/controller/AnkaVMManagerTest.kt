package org.cirruslabs.anka.controller

import com.veertu.ankaMgmtSdk.AnkaMgmtCommunicator
import com.veertu.ankaMgmtSdk.AnkaVmTemplate
import org.junit.Assert.*
import org.junit.Test

class AnkaVMManagerTest {
  val communicator = AnkaMgmtCommunicator("localhost", "8090")
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
    manager.waitForVMToStart(instanceId)
    assertTrue(manager.stopVM(instanceId))
  }
}
