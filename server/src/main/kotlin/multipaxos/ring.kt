package multipaxos

import com.google.protobuf.ByteString
import com.google.protobuf.kotlin.toByteStringUtf8
import io.grpc.ManagedChannelBuilder
import io.grpc.ServerBuilder
import kotlinx.coroutines.*
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import rest_api.repository.model.Transaction
import rest_api.service.*
import zookeeper.kotlin.ZooKeeperKt


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

    val token = 1
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
    /*withContext(Dispatchers.IO) { // Operations that block the current thread should be in a IO context
        System.`in`.read()
    }*/
    delay(30000)
    startGeneratingMessages(id, proposer, token, numOfShards, zk)
    withContext(Dispatchers.IO) { // Operations that block the current thread should be in a IO context
        server.awaitTermination()
    }
}

suspend fun ringProcess(zk : ZooKeeperKt) {
    coroutineScope {

        val learnerService = LearnerService(this)
        var ringId = id + 9
        /*if (id == 8001 || id == 8004){
            ringId = 8010
        }else if (id == 8002 || id == 8005) {
            ringId = 8011
        }else if (id == 8003 || id == 8006){
            ringId = 8012
        }*/
        println(ringId)
        val acceptorService = AcceptorService(ringId)

        val token = 1
        val numOfShards = 3

        val server = ServerBuilder.forPort(ringId)
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

        withContext(Dispatchers.IO) { // Operations that block the current thread should be in a IO context
            System.`in`.read()
        }
        val zkRinger = TokenKeeperLeader.make(zk,id)
        zkRinger.lead()

        val chans = listOf(8010, 8011, 8012, 8013, 8014, 8015).associateWith {
            ManagedChannelBuilder.forAddress("localhost", it).usePlaintext().build()!!
        }

        learnerService.learnerChannels = chans.filterKeys { it != ringId }.values.toList()

        val omega = object : OmegaFailureDetector<ID> {
            override val leader: ID get() = ringId

            //override val leader: ID get() = 8001
            override fun addWatcher(observer: suspend () -> Unit) {
                // TODO: replace the fallen leader in my shard
            }
        }

        // Create a proposer, note that the proposers id's and
        // the acceptors id's must be all unique (they break symmetry)
        val proposer = Proposer(
            id = ringId, omegaFD = omega, scope = this, acceptors = chans,
            thisLearner = learnerService,
        )

        proposer.start()

        startRecievingMessages(atomicBroadcast, ringId, proposer, numOfShards)

        // "Key press" barrier so only one propser sends messages
        /*withContext(Dispatchers.IO) { // Operations that block the current thread should be in a IO context
            System.`in`.read()
        }*/
        startGeneratingMessages(ringId, proposer, token, numOfShards, zk)

        withContext(Dispatchers.IO) { // Operations that block the current thread should be in a IO context
            server.awaitTermination()
        }
    }
}

/*suspend fun ringProcessNew(
    atomicBroadcast: AtomicBroadcast<String>, id: Int, proposer: Proposer,
    numOfShards: Int, token: Int
) {
    coroutineScope {
        println("before recv")
        startRecievingMessages(atomicBroadcast, id, proposer, numOfShards)
        // "Key press" barrier so only one propser sends messages
        /*withContext(Dispatchers.IO) { // Operations that block the current thread should be in a IO context
            System.`in`.read()
        }*/
        println("before timeout")
        withContext(Dispatchers.IO) { // Operations that block the current thread should be in a IO context
            System.`in`.read()
        }
        println("after timeout")
        startGeneratingMessages(id, proposer, token, numOfShards)
    }
}*/

private fun CoroutineScope.startGeneratingMessages(
        id: Int,
        proposer: Proposer,
        token: Int,
        num0fShards: Int,
        zk: ZooKeeperKt,
    ) {
        launch {
            val stayAlive = TokenKeeperLiveliness.make(zk)
            while(true) {
                stayAlive.rotate()
                while (true){
                    val toke = stayAlive.getFirst()
                    if (toke == stayAlive.mySeqNo && toke.toInt() % num0fShards != id % num0fShards){
                        stayAlive.unlock()
                        stayAlive.rotate()
                        continue
                    }
                    if (toke.toInt() % num0fShards == id % num0fShards && toke == stayAlive.mySeqNo)
                        break
                    //println(stayAlive.getFirst())
                    //delay(5000)
                }
                println("Started Generating Rotating Token Messages")
                delay(10000)
                val stream = tx_stream_token
                for (tx in stream) {
                    val json = Json.encodeToString(tx)
                    val str = "tx\n$json\n id = $id"
                    val prop = str.toByteStringUtf8()
                    proposer.addProposal(prop)
                }
                tx_stream_token.clear()
                val tok = stayAlive.getFirst().toInt()
                val tokenMsg = ("token $tok").toByteStringUtf8()
                proposer.addProposal(tokenMsg)
                println("SEND TOKEN TO RING")
                stayAlive.unlock()
            }
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
                    /*if (a == b) {
                        println("Start because of token")
                        //startGeneratingMessages(id, proposer, token + 1, numOfShards)
                    }*/
                } else if (msg.split("\n")[0] == "tx"){
                    val tx = parseToTx(msg)
                    //println(tx)
                    addOtherShardTx(tx)
                }
            }
        }
    }


