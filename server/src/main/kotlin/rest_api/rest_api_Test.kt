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

        val tx = Transaction(1234, listOf(1), listOf("1"), listOf("2","1"), listOf(20,80))
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

        var coinCheck = HttpRequest.newBuilder()
            .uri(URI.create("http://localhost:8080/coins/1"))
            .build()
        var responseCoins = client.send(coinCheck, HttpResponse.BodyHandlers.ofString())
        println(responseCoins.body())
        assert(responseCoins.body() == "80")

        coinCheck = HttpRequest.newBuilder()
            .uri(URI.create("http://localhost:8080/coins/2"))
            .build()
        responseCoins = client.send(coinCheck, HttpResponse.BodyHandlers.ofString())
        assert(responseCoins.body() == "120")

    }

    @Test
    fun basicTransferTest(){
        val client = HttpClient.newBuilder().build()
        val request = HttpRequest.newBuilder()
            .uri(URI.create("http://localhost:8080/transfer/2/3/110"))
            .build()
        val response = client.send(request, HttpResponse.BodyHandlers.ofString())

        var coinCheck = HttpRequest.newBuilder()
            .uri(URI.create("http://localhost:8080/coins/1"))
            .build()
        var responseCoins = client.send(coinCheck, HttpResponse.BodyHandlers.ofString())
        println(responseCoins.body())
        assert(responseCoins.body() == "80")

        coinCheck = HttpRequest.newBuilder()
            .uri(URI.create("http://localhost:8080/coins/2"))
            .build()
        responseCoins = client.send(coinCheck, HttpResponse.BodyHandlers.ofString())
        assert(responseCoins.body() == "10")

        coinCheck = HttpRequest.newBuilder()
            .uri(URI.create("http://localhost:8080/coins/3"))
            .build()
        responseCoins = client.send(coinCheck, HttpResponse.BodyHandlers.ofString())
        assert(responseCoins.body() == "210")
    }
}