package org.cirruslabs.anka.sdk

import com.google.common.util.concurrent.ThreadFactoryBuilder
import org.cirruslabs.anka.sdk.exceptions.AnkaException
import org.json.JSONObject
import java.io.IOException
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Duration
import java.util.*
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit

class AnkaCommunicator @Throws(AnkaException::class)
constructor(private val host: String, private val port: String) {
  private val API_TIMEOUT = Duration.ofMinutes(2)

  private val scheme: String = "http"

  private val httpClient =
    HttpClient.newBuilder()
      .build()

  private val executor: ScheduledExecutorService = Executors.newScheduledThreadPool(
    2,
    ThreadFactoryBuilder()
      .setNameFormat("anka-controller-pool-%d")
      .setDaemon(true)
      .build()
  )

  @Throws(AnkaException::class)
  fun listTemplates(): List<AnkaVmTemplate> {
    val templates = ArrayList<AnkaVmTemplate>()
    val url = String.format("%s://%s:%s/api/v1/registry/vm", this.scheme, this.host, this.port)
    try {
      val jsonResponse = this.doRequest(RequestMethod.GET, url)
      val logicalResult = jsonResponse!!.getString("status")
      if (logicalResult == "OK") {
        val vmsJson = jsonResponse.getJSONArray("body")
        for (j in vmsJson) {
          val jsonObj = j as JSONObject
          val vmId = jsonObj.getString("id")
          val name = jsonObj.getString("name")
          val vm = AnkaVmTemplate(vmId, name)
          templates.add(vm)
        }
      }
    } catch (e: IOException) {
      return templates
    }

    return templates
  }

  @Throws(AnkaException::class)
  fun listNodes(): List<AnkaNodeInfo> {
    val templates = ArrayList<AnkaNodeInfo>()
    val url = String.format("%s://%s:%s/api/v1/node", this.scheme, this.host, this.port)
    try {
      val jsonResponse = this.doRequest(RequestMethod.GET, url)
      val logicalResult = jsonResponse!!.getString("status")
      if (logicalResult == "OK") {
        val nodesJson = jsonResponse.getJSONArray("body")
        for (j in nodesJson) {
          val jsonObj = j as JSONObject
          templates.add(AnkaNodeInfo(jsonObj))
        }
      }
    } catch (e: IOException) {
      return templates
    }

    return templates
  }

  @Throws(AnkaException::class)
  fun listInstances(): List<AnkaVmSession> {
    val templates = ArrayList<AnkaVmSession>()
    val url = String.format("%s://%s:%s/api/v1/vm", this.scheme, this.host, this.port)
    try {
      val jsonResponse = this.doRequest(RequestMethod.GET, url)
      val logicalResult = jsonResponse!!.getString("status")
      if (logicalResult == "OK") {
        val nodesJson = jsonResponse.getJSONArray("body")
        for (j in nodesJson) {
          val jsonObj = j as JSONObject
          templates.add(AnkaVmSession(jsonObj.getString("instance_id"), jsonObj.getJSONObject("vm")))
        }
      }
    } catch (e: IOException) {
      return templates
    }

    return templates
  }

  @Throws(AnkaException::class)
  fun getTemplateTags(templateId: String): List<String> {
    val tags = ArrayList<String>()
    val url = String.format("%s://%s:%s/api/v1/registry/vm?id=%s", this.scheme, this.host, this.port, templateId)
    try {
      val jsonResponse = this.doRequest(RequestMethod.GET, url)
      val logicalResult = jsonResponse!!.getString("status")
      if (logicalResult == "OK") {
        val templateVm = jsonResponse.getJSONObject("body")
        val vmsJson = templateVm.getJSONArray("versions")
        for (j in vmsJson) {
          val jsonObj = j as JSONObject
          val tag = jsonObj.getString("tag")
          tags.add(tag)
        }
      }
    } catch (e: IOException) {
      System.err.printf("Exception trying to access: '%s'", url)
    } catch (e: org.json.JSONException) {
      System.err.printf("Exception trying to parse response: '%s'", url)
    }

    return tags
  }

  @Throws(AnkaException::class)
  fun startVm(templateId: String, tag: String?, nameTemplate: String?, startupScript: String?): String {
    val url = String.format("%s://%s:%s/api/v1/vm", this.scheme, this.host, this.port)
    val jsonObject = JSONObject()
    jsonObject.put("vmid", templateId)
    if (tag != null)
      jsonObject.put("tag", tag)
    if (nameTemplate != null)
      jsonObject.put("name_template", nameTemplate)
    if (startupScript != null)
      jsonObject.put("startup_script", Base64.getEncoder().encodeToString(startupScript.toByteArray()))
    var jsonResponse: JSONObject? = null
    try {
      jsonResponse = this.doRequest(RequestMethod.POST, url, jsonObject)
    } catch (e: IOException) {
      e.printStackTrace()
      throw AnkaException(e)
    }

    val logicalResult = jsonResponse!!.getString("status")
    if (logicalResult == "OK") {
      val uuidsJson = jsonResponse.getJSONArray("body")
      if (uuidsJson.length() >= 1) {
        return uuidsJson.getString(0)
      }
    }
    throw AnkaException("Failed to create a VM! $jsonResponse")
  }

  @Throws(AnkaException::class)
  fun showVm(sessionId: String): AnkaVmSession? {
    val url = String.format("%s://%s:%s/api/v1/vm?id=%s", this.scheme, this.host, this.port, sessionId)
    try {
      val jsonResponse = this.doRequest(RequestMethod.GET, url)
      val logicalResult = jsonResponse!!.getString("status")
      if (logicalResult == "OK") {
        val body = jsonResponse.getJSONObject("body")
        return AnkaVmSession(sessionId, body)
      }
      return null
    } catch (e: IOException) {
      e.printStackTrace()
      return null
    }

  }

  @Throws(AnkaException::class)
  fun terminateVm(sessionId: String): Boolean {
    val url = String.format("%s://%s:%s/api/v1/vm", this.scheme, this.host, this.port)
    return try {
      val jsonObject = JSONObject()
      jsonObject.put("id", sessionId)
      val jsonResponse = this.doRequest(RequestMethod.DELETE, url, jsonObject)
      val logicalResult = jsonResponse!!.getString("status")
      logicalResult == "OK"
    } catch (e: IOException) {
      e.printStackTrace()
      false
    }

  }


  @Throws(AnkaException::class)
  private fun list(): List<String> {
    val vmIds = ArrayList<String>()
    val url = String.format("%s://%s:%s/list", this.scheme, this.host, this.port)
    return try {
      val jsonResponse = this.doRequest(RequestMethod.GET, url)
      val logicalResult = jsonResponse!!.getString("result")
      if (logicalResult == "OK") {
        val vmsJson = jsonResponse.getJSONArray("instance_id")
        for (i in 0 until vmsJson.length()) {
          val vmId = vmsJson.getString(i)
          vmIds.add(vmId)
        }
      }
      vmIds
    } catch (e: IOException) {
      vmIds
    }

  }

  @Throws(IOException::class, AnkaException::class)
  private fun doRequest(method: RequestMethod, url: String, requestBody: JSONObject? = null): JSONObject? {
    println("Making $method request to $url with body: $requestBody")

    val request: HttpRequest = when (method) {
      AnkaCommunicator.RequestMethod.POST -> {
        HttpRequest.newBuilder()
          .uri(URI.create(url))
          .POST(HttpRequest.BodyPublishers.ofString(requestBody?.toString() ?: ""))
          .timeout(API_TIMEOUT)
          .build()
      }
      AnkaCommunicator.RequestMethod.DELETE -> {
        HttpRequest.newBuilder()
          .uri(URI.create(url))
          .method("DELETE", HttpRequest.BodyPublishers.ofString(requestBody?.toString() ?: ""))
          .timeout(API_TIMEOUT)
          .build()
      }
      AnkaCommunicator.RequestMethod.GET ->
        HttpRequest.newBuilder()
          .uri(URI.create(url))
          .GET()
          .timeout(API_TIMEOUT)
          .build()
    }

    try {
      val responseF = httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
      val response = responseF.get(2 * API_TIMEOUT.seconds, TimeUnit.SECONDS)
      val responseCode = response.statusCode()
      if (responseCode != 200) {
        println(response.toString())
        return null
      }
      return JSONObject(response.body())
    } catch (e: Exception) {
      e.printStackTrace()
      throw AnkaException(e)
    }
  }

  private enum class RequestMethod {
    GET, POST, DELETE
  }
}
