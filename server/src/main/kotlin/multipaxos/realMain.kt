package multipaxos

import com.google.protobuf.ByteString
import io.grpc.ManagedChannelBuilder
import io.grpc.ServerBuilder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.withContext

fun getShardIDs (id : Int) : List<Int>{
    val shard = id % numShards
    return if (shard == 0){
        listOf(8001,8004)
    } else if (shard == 1){
        listOf(8002,8005)
    } else{
        listOf(8003,8006)
    }
}


suspend fun main(args: Array<String>) = mainWith(args) {_, zk ->
    id = args[0].toInt()
    awaitAll(
        async { shardProcess(zk) },
       async { ringProcess(zk) }
    )
    //shardProcess(id, zk)
    /*withContext(Dispatchers.IO) { // Operations that block the current thread should be in a IO context
        System.`in`.read()
    }*/
    //ringProcess(id,zk)
    /*var currLeader: MutableList<Int> = mutableListOf(id)
    val shard = "shard" + (id % numShards).toString()
    val zkLeader = LeaderElection.make(zk, shard)
    val learnerService = LearnerService(this)
    val learnerService2 = LearnerService(this)
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
            if (id == 8001 || id == 8002 || id == 8003)
                addService(learnerService2)
            println(id)
        }
        .build()
    val atomicBroadcast = object : AtomicBroadcast<String>(learnerService, biSerializer) {
        // These are dummy implementations
        override suspend fun _send(byteString: ByteString) {
            //println("SENDING STUFF IS IMPORTANT")
        }
        override fun _deliver(byteString: ByteString) = listOf(this.biSerializer(byteString))
    }
    val atomicBroadcast2 = object : AtomicBroadcast<String>(learnerService2, biSerializer) {
        // These are dummy implementations
        override suspend fun _send(byteString: ByteString) {
            //println("SENDING STUFF IS IMPORTANT")
        }
        override fun _deliver(byteString: ByteString) = listOf(this.biSerializer(byteString))
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
    val proposer = Proposer(
        id = id, omegaFD = omega, scope = this, acceptors = chans,
        thisLearner = learnerService,
    )
    var ringChans : List<Int> = listOf()
    if (id == 8001){
        ringChans = listOf<Int>(8002,8003)
    }else if (id == 8002){
        ringChans = listOf<Int>(8001,8003)
    }else if (id == 8003){
        ringChans = listOf<Int>(8001,8002)
    }
    val chans2 = ringChans.associateWith {
        ManagedChannelBuilder.forAddress("localhost", it).usePlaintext().build()!!
    }
    learnerService2.learnerChannels = chans2.filterKeys { it != id }.values.toList()
    val proposer2 = Proposer(
        id = id, omegaFD = omega, scope = this, acceptors = chans2,
        thisLearner = learnerService2,
    )

    proposer.start()
    proposer2.start()
    awaitAll(
        async {shardProcessNew(atomicBroadcast,currLeader, zkLeader, id, proposer)},
        async {ringProcessNew(atomicBroadcast2,id,proposer2, numShards, token=1)})
*/
}