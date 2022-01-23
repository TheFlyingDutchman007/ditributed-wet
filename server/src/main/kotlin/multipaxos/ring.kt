package multipaxos

import antlr.Token
import com.google.protobuf.ByteString
import com.google.protobuf.kotlin.toByteStringUtf8
import io.grpc.ManagedChannelBuilder
import io.grpc.ServerBuilder
import kotlinx.coroutines.*
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import rest_api.repository.model.Transaction


suspend fun main(args: Array<String>) = mainWith(args) {_, zk ->

    org.apache.log4j.BasicConfigurator.configure()


    val id = args[0].toInt()

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

    // Create channels with clients
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

    startRecievingMessages(atomicBroadcast, id, proposer)

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
            (1..2).forEach {
                delay(2000)
                /*val tx = Transaction("test1", listOf("1"), listOf("1"), listOf("2","1"), listOf(20,80))
                val json = Json.encodeToString(tx)
                val str = json + "\n id = $id"
                val prop = str.toByteStringUtf8()
                    .also { println("Adding Proposal ${it.toStringUtf8()!!}")}*/
                val prop = "[Value no $it from $id]".toByteStringUtf8()
                    .also { println("Adding Proposal ${it.toStringUtf8()!!}") }
                proposer.addProposal(prop)
            }
            val tokenMsg = ("token " + token.toString()).toByteStringUtf8()
            proposer.addProposal(tokenMsg)
        }
    }

    // token message: "token x"
    private fun CoroutineScope.startRecievingMessages(atomicBroadcast: AtomicBroadcast<String>, id: Int,
                                                      proposer: Proposer) {
        launch {
            for ((`seq#`, msg) in atomicBroadcast.stream) {
                println("Message: #$`seq#`: \n $msg \n  received!")
                if (msg.split(" ")[0] == "token") {
                    val token = msg.split(" ")[1].toInt()
                    val a = (token + 1) % 3
                    val b = (id - 8000) % 3
                    println(a)
                    println(b)
                    if (a == b) {
                        println("Start because of token")
                        startGeneratingMessages(id, proposer, token + 1, 3)
                    }
                }
            }
        }
    }


