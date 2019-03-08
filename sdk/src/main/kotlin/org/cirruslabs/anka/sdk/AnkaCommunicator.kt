package org.cirruslabs.anka.sdk

import org.apache.http.client.config.RequestConfig
import org.apache.http.client.methods.*
import org.apache.http.client.utils.HttpClientUtils
import org.apache.http.conn.HttpHostConnectException
import org.apache.http.entity.StringEntity
import org.apache.http.impl.client.HttpClientBuilder
import org.cirruslabs.anka.sdk.exceptions.AnkaException
import org.json.JSONObject
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader
import java.io.UnsupportedEncodingException
import java.net.URI
import java.time.Duration
import java.util.*
import javax.net.ssl.SSLException

class AnkaCommunicator @Throws(AnkaException::class)
constructor(private val host: String, private val port: String) {
  private val API_TIMEOUT = Duration.ofMinutes(2)

  private var scheme: String? = null

  init {
    this.scheme = "https"
    try {
      val url = String.format("%s://%s:%s", this.scheme, this.host, this.port)
      this.doRequest(RequestMethod.GET, url)
    } catch (e: SSLException) {
      this.scheme = "http"
    } catch (e: IOException) {
      e.printStackTrace()
      throw AnkaException(e)
    }

    this.listTemplates()
  }

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
    val httpClient = HttpClientBuilder.create()
      .setDefaultRequestConfig(
        RequestConfig.custom()
          .setConnectTimeout(API_TIMEOUT.toMillis().toInt())
          .setConnectionRequestTimeout(API_TIMEOUT.toMillis().toInt())
          .build()
      )
      .build()
    println("Making $method request to $url with body: $requestBody")
    val request: HttpRequestBase  = when (method) {
      AnkaCommunicator.RequestMethod.POST -> {
        val postRequest = HttpPost(url)
        setBody(postRequest, requestBody!!)
      }
      AnkaCommunicator.RequestMethod.DELETE -> {
        val delRequest = HttpDeleteWithBody(url)
        setBody(delRequest, requestBody!!)
      }
      AnkaCommunicator.RequestMethod.GET -> HttpGet(url)
    }
    var response: CloseableHttpResponse? = null

    try {
      response = httpClient.execute(request)
      val responseCode = response.statusLine.statusCode
      if (responseCode != 200) {
        println(response.toString())
        return null
      }
      val entity = response.entity
      if (entity != null) {
        val rd = BufferedReader(InputStreamReader(entity.content))
        val responseContent = rd.readText()
        println("Response for $url with body $requestBody: $responseContent")
        return JSONObject(responseContent)
      }
    } catch (e: HttpHostConnectException) {
      throw AnkaException(e)
    } catch (e: SSLException) {
      throw e
    } catch (e: UnsupportedEncodingException) {
      e.printStackTrace()
      throw RuntimeException(e)
    } catch (e: IOException) {
      e.printStackTrace()
      throw AnkaException(e)
    } finally {
      request.releaseConnection()
      HttpClientUtils.closeQuietly(response)
      HttpClientUtils.closeQuietly(httpClient)
    }
    return null
  }

  @Throws(UnsupportedEncodingException::class)
  private fun setBody(request: HttpEntityEnclosingRequestBase, requestBody: JSONObject): HttpRequestBase {
    request.setHeader("content-type", "application/json")
    val body = StringEntity(requestBody.toString())
    request.entity = body
    return request
  }

  private enum class RequestMethod {
    GET, POST, DELETE
  }

  internal inner class HttpDeleteWithBody : HttpEntityEnclosingRequestBase {

    constructor(uri: String) : super() {
      setURI(URI.create(uri))
    }

    constructor(uri: URI) : super() {
      setURI(uri)
    }

    constructor() : super()

    override fun getMethod(): String {
      return "DELETE"
    }
  }
}
