package rest_api

import org.junit.Test
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import rest_api.repository.model.Transaction
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString
import java.net.http.HttpResponse

class rest_api_Test{
    @Test
    fun basicTransactionTest(){

        val tx = Transaction("test1", listOf("1"), listOf("1"), listOf("2","1"), listOf(20,80))
        val json = Json.encodeToString(tx)

        val client = HttpClient.newBuilder().build()
        val request = HttpRequest.newBuilder()
            .uri(URI.create("http://localhost:8080/submit_transaction"))
            .POST(HttpRequest.BodyPublishers.ofString(json))
            .setHeader("Content-Type","application/json")
            .build()
        val response = client.send(request, HttpResponse.BodyHandlers.ofString())
        println(response.body())
        println(response.statusCode())
    }
}