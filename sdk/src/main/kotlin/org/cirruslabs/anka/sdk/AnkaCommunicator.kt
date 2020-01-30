package org.cirruslabs.anka.sdk

import com.google.common.net.HttpHeaders
import org.cirruslabs.anka.sdk.exceptions.AnkaException
import org.json.JSONObject
import java.io.IOException
import java.net.URI
import java.net.URL
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Duration
import java.util.*
import java.util.concurrent.TimeUnit

class AnkaCommunicator @Throws(AnkaException::class)
constructor(
  private val controllerURL: URL,
  private val username: String? = null,
  private val password: String? = null
) {
  private val API_TIMEOUT = Duration.ofMinutes(2)

  private val httpClient =
    HttpClient.newBuilder()
      .build()

  @Throws(AnkaException::class)
  fun listTemplates(): List<AnkaVmTemplate> {
    val templates = ArrayList<AnkaVmTemplate>()
    val endpoint = String.format("api/v1/registry/vm")
    try {
      val jsonResponse = this.doRequest(RequestMethod.GET, endpoint)
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
    val endpoint = String.format("api/v1/node")
    try {
      val jsonResponse = this.doRequest(RequestMethod.GET, endpoint)
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
    val endpoint = String.format("api/v1/vm")
    try {
      val jsonResponse = this.doRequest(RequestMethod.GET, endpoint)
      val logicalResult = jsonResponse!!.getString("status")
      if (logicalResult == "OK") {
        val nodesJson = jsonResponse.getJSONArray("body")
        for (j in nodesJson) {
          val jsonObj = j as JSONObject
          try {
            templates.add(AnkaVmSession(jsonObj.getString("instance_id"), jsonObj.getJSONObject("vm")))
          } catch (e: Exception) {
            e.printStackTrace()
          }
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
    val endpoint = String.format("api/v1/registry/vm?id=%s", templateId)
    try {
      val jsonResponse = this.doRequest(RequestMethod.GET, endpoint)
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
      System.err.printf("Exception trying to access: '%s'", endpoint)
    } catch (e: org.json.JSONException) {
      System.err.printf("Exception trying to parse response: '%s'", endpoint)
    }

    return tags
  }

  @Throws(AnkaException::class)
  fun startVm(templateId: String, tag: String?, nameTemplate: String?, startupScript: String?): String {
    val endpoint = String.format("api/v1/vm")
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
      jsonResponse = this.doRequest(RequestMethod.POST, endpoint, jsonObject)
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
    val endpoint = String.format("api/v1/vm?id=%s", sessionId)
    try {
      val jsonResponse = this.doRequest(RequestMethod.GET, endpoint)
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
    val endpoint = String.format("api/v1/vm")
    return try {
      val jsonObject = JSONObject()
      jsonObject.put("id", sessionId)
      val jsonResponse = this.doRequest(RequestMethod.DELETE, endpoint, jsonObject)
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
    val endpoint = String.format("list")
    return try {
      val jsonResponse = this.doRequest(RequestMethod.GET, endpoint)
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
  private fun doRequest(method: RequestMethod, endpoint: String, requestBody: JSONObject? = null): JSONObject? {
    println("Making $method request to $endpoint with body: $requestBody")

    val url = URI.create("$controllerURL$endpoint")
    
    val requestBuilder: HttpRequest.Builder = when (method) {
      AnkaCommunicator.RequestMethod.POST -> {
        HttpRequest.newBuilder()
          .uri(url)
          .POST(HttpRequest.BodyPublishers.ofString(requestBody?.toString() ?: ""))
          .timeout(API_TIMEOUT)
      }
      AnkaCommunicator.RequestMethod.DELETE -> {
        HttpRequest.newBuilder()
          .uri(url)
          .method("DELETE", HttpRequest.BodyPublishers.ofString(requestBody?.toString() ?: ""))
          .timeout(API_TIMEOUT)
      }
      AnkaCommunicator.RequestMethod.GET ->
        HttpRequest.newBuilder()
          .uri(url)
          .GET()
          .timeout(API_TIMEOUT)
    }

    if (username != null && password != null) {
      val encodedAuth = Base64.getEncoder().encodeToString("$username:$password".toByteArray())
      requestBuilder.header(HttpHeaders.AUTHORIZATION, "Basic $encodedAuth")
    }

    try {
      val response =
        httpClient.sendAsync(requestBuilder.build(), HttpResponse.BodyHandlers.ofString())
          .orTimeout(2 * API_TIMEOUT.seconds, TimeUnit.SECONDS)
          .get()
      val responseCode = response.statusCode()
      if (responseCode != 200) {
        println("Bad response $responseCode for $endpoint: $response")
        return null
      }
      val body = response.body()
      println("Response $responseCode for $endpoint: $body")
      return JSONObject(body)
    } catch (e: Exception) {
      e.printStackTrace()
      throw AnkaException(e)
    }
  }

  private enum class RequestMethod {
    GET, POST, DELETE
  }
}
