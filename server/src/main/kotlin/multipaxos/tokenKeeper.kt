package multipaxos

import kotlinx.coroutines.channels.Channel
import zookeeper.kotlin.ZKPaths
import zookeeper.kotlin.ZooKeeperKt
import zookeeper.kotlin.createflags.Ephemeral
import zookeeper.kotlin.createflags.Sequential


class TokenKeeperLeader private constructor(private val zk: ZooKeeperKt, val id : Int) {

    companion object {
        suspend fun make(zk: ZooKeeperKt, id: Int): TokenKeeperLeader {
            val zk = zk
                .usingNamespace("/leaders")
                .usingNamespace("/tokenKeeper")
                .usingNamespace("/leaders")
            return TokenKeeperLeader(zk, id)
        }
    }


    var myPath: String? = null

    // return true if it's ok to make node. False otherwise
    suspend fun checkShardFriend( id : Int) : Boolean{
        if (id == 8001){
            try {
                val (exists, _) = zk.exists("/8004")
            } catch (e : Exception){
                return true
            }
            return false
        } else if (id == 8004) {
            try {
                val (exists, _) = zk.exists("/8001")
            } catch (e : Exception){
                return true
            }
            return false
        } else if (id == 8002) {
            try {
                val (exists, _) = zk.exists("/8005")
            } catch (e : Exception){
                return true
            }
            return false
        } else if (id == 8005) {
            try {
                val (exists, _) = zk.exists("/8002")
            } catch (e : Exception){
                return true
            }
            return false
        } else if (id == 8003) {
            try {
                val (exists, _) = zk.exists("/8006")
            } catch (e : Exception){
                return true
            }
            return false
        } else if (id == 8006) {
            try {
                val (exists, _) = zk.exists("/8003")
            } catch (e : Exception){
                return true
            }
            return false
        }
        return false
    }

    suspend fun lead() {
        // I can create only if the other friend is not inside yet
        while (true){
            if (checkShardFriend(id))
                break
        }

        // can create znode!!
        myPath = zk.create("/$id") {
            flags = Ephemeral
        }.first

        val leaderWait: Channel<Unit> = Channel(1)

        val children = zk.getChildren("/").first
        for (child in children){
           /* val (exists, _) = zk.exists(child) {
                watchers += { _, _, _ -> leaderWait.send(Unit) }
            }*/
            println("child = $child")
        }

    }

    suspend fun unlock() {
        zk.delete("/id")
    }

    suspend fun getLeader() : String{
        val seqNos = zk.getChildren("/").first
            .map { Pair(ZKPaths.extractSequentialSuffix(it)!!, it.substring(5,it.length-11)) }
            .sortedBy { ZKPaths.extractSequentialSuffix(it.first)!! }
        println(seqNos[0].second)
        return seqNos[0].first
    }

}


class TokenKeeperLiveliness private constructor(private val zk: ZooKeeperKt){
    companion object {
        suspend fun make(zk: ZooKeeperKt): TokenKeeperLiveliness {
            val zk = zk
                .usingNamespace("/leaders")
                .usingNamespace("/tokenKeeper")
                .usingNamespace("/liveliness")
            return TokenKeeperLiveliness(zk)
        }
    }
    var mySeqNo: String? = null


    suspend fun rotate(){
        mySeqNo = zk.create("/rot-") {
            flags = Ephemeral and Sequential
        }.first.let { ZKPaths.extractSequentialSuffix(it)!! }
        val seqNo = mySeqNo!!
    }

    suspend fun unlock() {
        zk.delete("/rot-${mySeqNo}")
    }

    suspend fun getFirst() : String {
        val seqNos = zk.getChildren("/").first
            .map { ZKPaths.extractSequentialSuffix(it)!! }
            .sorted()
        return seqNos[0]
    }

}