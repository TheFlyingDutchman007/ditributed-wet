package multipaxos

import com.google.protobuf.ByteString
import com.google.protobuf.kotlin.toByteStringUtf8
import io.grpc.ManagedChannel
import io.grpc.ManagedChannelBuilder
import io.grpc.ServerBuilder
import kotlinx.coroutines.*
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import rest_api.repository.model.Transaction
import rest_api.service.*
import zookeeper.kotlin.ZooKeeperKt
import java.util.*

var id : Int = 0
var currLeader : MutableList<Int> = mutableListOf(id)


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

private fun addMyLeaderTx(tx : Transaction){
    if (!ledger.txMap.contains(tx.tx_id)){ // check that the tx is not already exists
        val result = createTransactionOut(tx)
        println(result)
    }
}


/*private fun CoroutineScope.restAPI (controller: TransactionController){
    runApplication<SpringBootBoilerplateApplication>()
}*/

/*suspend fun main(args: Array<String>) = mainWith(args) {_, zk ->


    id = args[0].toInt()
    currLeader = mutableListOf(id)
    val shard = "shard" + (id % numShards).toString()
    val zkLeader = LeaderElection.make(zk, shard)

    val learnerService = LearnerService(this)
    val acceptorService = AcceptorService(id)

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
        override suspend fun _send(byteString: ByteString) {
            //println("SENDING STUFF IS IMPORTANT")
        }
        override fun _deliver(byteString: ByteString) = listOf(biSerializer(byteString))
    }

    withContext(Dispatchers.IO) { // Operations that block the current thread should be in a IO context
        server.start()
    }
    val shardChans = getShardIDs(id)
    val chans = shardChans.associateWith {
        ManagedChannelBuilder.forAddress("localhost", it).usePlaintext().build()!!
    }

    learnerService.learnerChannels = chans.filterKeys { it != id }.values.toList()

    val omega = object : OmegaFailureDetector<ID> {
        override val leader: ID get() = id
        //override val leader: ID get() = 8001
        override fun addWatcher(observer: suspend () -> Unit) {}
    }

    // Create a proposer, note that the proposers id's and
    // the acceptors id's must be all unique (they break symmetry)
    val proposer = Proposer(
        id = id, omegaFD = omega, scope = this, acceptors = chans,
        thisLearner = learnerService,
    )

    proposer.start()

    startRecievingMessages(atomicBroadcast)

    // "Key press" barrier so only one propser sends messages
    withContext(Dispatchers.IO) { // Operations that block the current thread should be in a IO context
        System.`in`.read()
    }
    //println(omega.leader)
    zkLeader.volunteer()
    sendLeaderMsg(id,proposer)
    startGeneratingMessages(id, proposer)
    withContext(Dispatchers.IO) { // Operations that block the current thread should be in a IO context
        server.awaitTermination()
    }
}*/

suspend fun shardProcess(zk: ZooKeeperKt) {
    coroutineScope {
        currLeader = mutableListOf(id)
        val shard = "shard" + (id % numShards).toString()
        val zkLeader = LeaderElection.make(zk, shard)

        val learnerService = LearnerService(this)
        val acceptorService = AcceptorService(id)

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
            override suspend fun _send(byteString: ByteString) {
                //println("SENDING STUFF IS IMPORTANT")
            }
            override fun _deliver(byteString: ByteString) = listOf(biSerializer(byteString))
        }

        withContext(Dispatchers.IO) { // Operations that block the current thread should be in a IO context
            server.start()
        }
        val shardChans = getShardIDs(id)
        val chans = shardChans.associateWith {
            ManagedChannelBuilder.forAddress("localhost", it).usePlaintext().build()!!
        }

        learnerService.learnerChannels = chans.filterKeys { it != id }.values.toList()

        val omega = object : OmegaFailureDetector<ID> {
            override val leader: ID get() = id
            //override val leader: ID get() = 8001
            override fun addWatcher(observer: suspend () -> Unit) {}
        }

        // Create a proposer, note that the proposers id's and
        // the acceptors id's must be all unique (they break symmetry)
        val proposer = Proposer(
            id = id, omegaFD = omega, scope = this, acceptors = chans,
            thisLearner = learnerService,
        )

        proposer.start()

        startRecievingMessages(atomicBroadcast, proposer)
        // "Key press" barrier so only one propser sends messages
        /*withContext(Dispatchers.IO) { // Operations that block the current thread should be in a IO context
            System.`in`.read()
        }*/
        //println(omega.leader)
        zkLeader.volunteer()
        delay(30000)
        currLeader.clear()
        currLeader += id
        sendLeaderMsg(id,proposer)
        delay(4000)
        sendKeyMsg(id,proposer)
        startGeneratingMessages(proposer)
        withContext(Dispatchers.IO) { // Operations that block the current thread should be in a IO context
            server.awaitTermination()
        }
    }
}

/*suspend fun shardProcessNew(
    atomicBroadcast: AtomicBroadcast<String>, currLeader: MutableList<Int>, zkLeader : LeaderElection,
    id: Int, proposer: Proposer
    ){
    coroutineScope {
        startRecievingMessages(atomicBroadcast)
        withContext(Dispatchers.IO) { // Operations that block the current thread should be in a IO context
            System.`in`.read()
        }
        zkLeader.volunteer()
        //delay(60000)
        sendLeaderMsg(id, proposer)
        startGeneratingMessages(id, proposer)
    }
}*/

private fun CoroutineScope.startGeneratingMessages(
    proposer: Proposer
) {
    launch {
        println("Started Generating Messages")
        while(true){
            delay(2000)
            // send keys arrived from the ring to rest of my shard
            for (key in sendToShardKeys){
                val msg = buildSendPublicKeyMessage(key.key, key.value).toByteStringUtf8()
                proposer.addProposal(msg)
            }
            sendToShardKeys.clear()

            for (tx in tx_stream_leader){
                val json = Json.encodeToString(tx)
                val str = "tx\n" + json + "\n$id\n" + cryptoFun.signMessage(id.toString(),
                    Base64.getEncoder().encodeToString(cryptoFun.privateKey.encoded))
                val prop = str.toByteStringUtf8()
                proposer.addProposal(prop)
            }
            tx_stream_leader.clear()
        }
    }
}

private fun CoroutineScope.startRecievingMessages(
    atomicBroadcast: AtomicBroadcast<String>,
    proposer: Proposer,
) {
    launch {
        for ((`seq#`, msg) in atomicBroadcast.stream) {
            println("Message: #$`seq#`: \n $msg \n  received!")
            //println(currLeader[0])
            if (msg.split(" ")[0] == "Leader"){
                val leader = msg.split(" ")[1].toInt()
                if (leader == currLeader[0]) {
                    println("I am the leader")
                }else{
                    println("The leader is: " + leader.toString())
                    currLeader.clear()
                    currLeader += leader
                }
            } else if (msg.split("\n")[0] == "tx"){
                val tx = parseToTx(msg)
                val senderID = msg.split("\n")[2].toInt()
                val secretCy = msg.split("\n")[3]
                if (mapOfPublicKeys.containsKey(senderID)){
                    val publicKey = mapOfPublicKeys[senderID]
                    val secretPlain = publicKey?.let { cryptoFun.unsignMessage(secretCy, it) }
                    println("ID $senderID secret is $secretPlain")
                }
                //println(tx)
                addMyLeaderTx(tx)
            } else if (msg.split(" ")[0] == "PublicKeyMsg"){
                val senderID = msg.split(" ")[1].toInt()
                if (mapOfPublicKeys.containsKey(senderID)) continue // if we already have key, continue
                val senderKey = msg.split(" ")[2]
                mapOfPublicKeys[senderID] = senderKey
                if (senderID == currLeader[0]){
                    sendKeyMsg(id,proposer)
                }
                //val secret = msg.split(" ")[3]
                /*println("before")
                println(cryptoFun.unsignMessage(secret,senderKey))
                println("after")*/
                /*if (senderID != currLeader[0]){
                    println("I'll send the leader my key too!!")
                    sendKeyMsg(id, proposer)
                }*/
                /*for (key in mapOfPublicKeys){
                    println("Public key of " + key.key +" is: \n" + key.value)
                }*/
            }

            /*else{
                if (id != currLeader[0]) {
                    val str = "I am not the leader. id = " + id
                    val prop = str.toByteStringUtf8()
                    proposer.addProposal(prop)
                    println("SENDING REQ TO LEADER (ALL...)")
                    delay(10000)
                }
                else{
                    println("got a request from shard friends")
                }
            }*/
        }
    }
}

private fun CoroutineScope.sendLeaderMsg(id: Int, proposer: Proposer){
    launch {
        println("Send \"I am the LEADER\" msg")
        delay(2000)
        val str = "Leader " + id.toString()
        val prop = str.toByteStringUtf8()
        proposer.addProposal(prop)
    }
}

private fun CoroutineScope.sendKeyMsg(id: Int, proposer: Proposer){
    launch {
        println("Sending PUBLICKEY")
        delay(1000)
        val prop = buildSendPublicKeyMessage(id,
            Base64.getEncoder().encodeToString(cryptoFun.publicKey.encoded)).toByteStringUtf8()
        proposer.addProposal(prop)
    }
}




