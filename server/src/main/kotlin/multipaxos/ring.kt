package multipaxos

import com.google.protobuf.ByteString
import com.google.protobuf.kotlin.toByteStringUtf8
import com.google.protobuf.type
import io.grpc.ManagedChannelBuilder
import io.grpc.ServerBuilder
import kotlinx.coroutines.*
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import rest_api.controller.TransactionController
import rest_api.repository.model.Transaction
import rest_api.service.TransactionService
import rest_api.service.createTransactionOut
import rest_api.service.ledger
import rest_api.service.tx_stream


private fun parseToTx(msg: String) : Transaction{
    var de = biSerializer.deserialize(msg.split("\n")[1].toByteStringUtf8())
    de = de.replace("\",\"","\"-\"")
    val reg = "[0-9],[0-9]".toRegex()
    var found = reg.find(de)
    while(found != null){
        de = de.replace(found.value,found.value[0]+"$"+found.value[2])
        found = found.next()
    }
    val json = de.split(",")

    val txID = json[0].split(":")[1].toLong()

    var inputTxIdStr = json[1].split(":")[1]
    inputTxIdStr = inputTxIdStr.substring(1,inputTxIdStr.length-1)
    var inputAddrStr = json[2].split(":")[1]
    inputAddrStr = inputAddrStr.substring(1,inputAddrStr.length-1)
    val inputTxId = mutableListOf<Long>()
    for (txId in inputTxIdStr.split("$")){
        inputTxId.add(txId.toLong())
    }
    val inputAddr = mutableListOf<String>()
    for (addr in inputAddrStr.split("-")){
        inputAddr.add(addr.substring(1,addr.length-1))
    }

    var outputAddrStr = json[3].split(":")[1]
    outputAddrStr = outputAddrStr.substring(1,outputAddrStr.length-1)
    val outputAddr = mutableListOf<String>()
    for (addr in outputAddrStr.split("-")){
        outputAddr.add(addr.substring(1,addr.length-1))
    }

    var outputCoinsStr = json[4].split(":")[1]
    outputCoinsStr = outputCoinsStr.substring(1, outputCoinsStr.length-2)
    val outputCoins = mutableListOf<Long>()
    for (coins in outputCoinsStr.split("$")){
        outputCoins.add(coins.toLong())
    }

    val tx = Transaction(txID,inputTxId,inputAddr,outputAddr,outputCoins)
    return tx
}

fun addOtherShardTx(tx : Transaction){
    if (!ledger.txMap.contains(tx.tx_id)){ // check that the tx is not already exists
        val result = createTransactionOut(tx)
        println(result)
    }
}

/*private fun CoroutineScope.restAPI (controller: TransactionController){
    runApplication<SpringBootBoilerplateApplication>()
}*/

fun main(args: Array<String>) = mainWith(args) {_, zk ->

    //org.apache.log4j.BasicConfigurator.configure()

    val id = args[0].toInt()

    //restAPI(controller)

    val learnerService = LearnerService(this)
    val acceptorService = AcceptorService(id)

    var token = 1
    val numOfShards = 3

    val server = ServerBuilder.forPort(id)
        .apply {
            if (id > 0) // Apply your own logic: who should be an acceptor
                addService(acceptorService)
            println(id)
        }
        .apply {
            if (id > 0) // Apply your own logic: who should be a learner
                addService(learnerService)
            println(id)
        }
        .build()


    // Use the atomic broadcast adapter to use the learner service as an atomic broadcast service
    val atomicBroadcast = object : AtomicBroadcast<String>(learnerService, biSerializer) {
        // These are dummy implementations
        // TODO: add real implementations
        override suspend fun _send(byteString: ByteString) {
            //println("SENDING STUFF IS IMPORTANT")
        }
        override fun _deliver(byteString: ByteString) = listOf(biSerializer(byteString))
    }

    withContext(Dispatchers.IO) { // Operations that block the current thread should be in a IO context
        server.start()
    }

    val chans = listOf(8001, 8002, 8003).associateWith {
        ManagedChannelBuilder.forAddress("localhost", it).usePlaintext().build()!!
    }

    learnerService.learnerChannels = chans.filterKeys { it != id }.values.toList()

    val omega = object : OmegaFailureDetector<ID> {
        override val leader: ID get() = id
        //override val leader: ID get() = 8001
        override fun addWatcher(observer: suspend () -> Unit) {
            // TODO: replace the fallen leader in my shard
        }
    }

    // Create a proposer, note that the proposers id's and
    // the acceptors id's must be all unique (they break symmetry)
    val proposer = Proposer(
        id = id, omegaFD = omega, scope = this, acceptors = chans,
        thisLearner = learnerService,
    )

    proposer.start()

    startRecievingMessages(atomicBroadcast, id, proposer, numOfShards)

    // "Key press" barrier so only one propser sends messages
    withContext(Dispatchers.IO) { // Operations that block the current thread should be in a IO context
        System.`in`.read()
    }
    startGeneratingMessages(id, proposer, token, numOfShards)
    withContext(Dispatchers.IO) { // Operations that block the current thread should be in a IO context
        server.awaitTermination()
    }
}

    private fun CoroutineScope.startGeneratingMessages(
        id: Int,
        proposer: Proposer,
        token: Int,
        num0fShards: Int
    ) {
        while (true){
            if ((id - 8000) % num0fShards == token % num0fShards)
                break
        }
        launch {
            println("Started Generating Messages")
            /*while (true){
                println(service.ledger)
                delay(10000)
            }*/
            /*(1..1).forEach {
                val tx = Transaction(id.toLong(), listOf(1), listOf("1"), listOf("2","1"), listOf(20,80))
                val json = Json.encodeToString(tx)
                val str = "tx\n" + json + "\n id = $id"
                val prop = str.toByteStringUtf8()
                    .also { println("Adding Proposal ${it.toStringUtf8()!!}")}
                /*val prop = "[Value no $it from $id]".toByteStringUtf8()
                    .also { println("Adding Proposal ${it.toStringUtf8()!!}") }*/
                proposer.addProposal(prop)
                delay(20000)
            }*/
            delay(5000)
            val stream = tx_stream
            for (tx in stream){
                val json = Json.encodeToString(tx)
                val str = "tx\n" + json + "\n id = $id"
                val prop = str.toByteStringUtf8()
                proposer.addProposal(prop)
            }
            tx_stream.clear()
            val tokenMsg = ("token " + token.toString()).toByteStringUtf8()
            proposer.addProposal(tokenMsg)
        }
    }

    // token message: "token x"
    private fun CoroutineScope.startRecievingMessages(
        atomicBroadcast: AtomicBroadcast<String>, id: Int,
        proposer: Proposer, numOfShards: Int) {
        launch {
            for ((`seq#`, msg) in atomicBroadcast.stream) {
                println("Message: #$`seq#`: \n $msg \n  received!")
                if (msg.split(" ")[0] == "token") {
                    val token = msg.split(" ")[1].toInt()
                    val a = (token + 1) % numOfShards
                    val b = (id - 8000) % numOfShards
                    // println(a)
                    // println(b)
                    if (a == b) {
                        println("Start because of token")
                        startGeneratingMessages(id, proposer, token + 1, numOfShards)
                    }
                } else if (msg.split("\n")[0] == "tx"){
                    val tx = parseToTx(msg)
                    //println(tx)
                    addOtherShardTx(tx)
                }
            }
        }
    }


