package multipaxos

import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import zookeeper.kotlin.ZKPaths
import zookeeper.kotlin.ZooKeeperKt
import zookeeper.kotlin.createflags.Ephemeral
import zookeeper.kotlin.createflags.Sequential


fun main(args: Array<String>) = mainWith(args) { _, zk ->
    val zkleader = LeaderElection.make(zk, "shard1")
    println("Waiting for the election")
    zkleader.volunteer()
    println("I Am groot")
    delay(7_000)
    println("I Am not groot")
    zkleader.unlock()
}



class LeaderElection private constructor(private val zk: ZooKeeperKt, val electionName : String) {

    companion object {
        suspend fun make(zk: ZooKeeperKt, shard: String): LeaderElection {
            val zk = zk
                .usingNamespace("/leaders")
                .usingNamespace("/$shard")
            return LeaderElection(zk, shard)
        }
    }


    var mySeqNo: String? = null

    // need?????
    val id: String get() = mySeqNo!!

    suspend fun volunteer() {
        mySeqNo = zk.create("/guid-") {
            flags = Ephemeral and Sequential
        }.first.let { ZKPaths.extractSequentialSuffix(it)!! }
        val seqNo = mySeqNo!!

        // need???
        val leaderWait: Channel<Unit> = Channel(1)
        while (true) {
            // get all nodes
            val seqNos = zk.getChildren("/").first
                .map { ZKPaths.extractSequentialSuffix(it)!! }
                .sorted()

            if (seqNo == seqNos[0]) { // I am the minimal- my turn to be the leader
                break
            } else{
                val nextSeqNo = seqNos[1]
                val (exists, _) = zk.exists("/guid-$nextSeqNo") {
                    watchers += { _, _, _ -> leaderWait.send(Unit) }
                }
                if (!exists) {
                    continue
                } else{
                    if (nextSeqNo == mySeqNo) {
                        leaderWait.send(Unit)
                    }
                    leaderWait.receive()
                }
            }
        }
    }

    suspend fun unlock() {
        zk.delete("/guid-${mySeqNo}")
    }

}