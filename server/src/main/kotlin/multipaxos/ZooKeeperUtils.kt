package multipaxos

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.runBlocking
import org.apache.log4j.BasicConfigurator
import org.apache.zookeeper.Watcher
import org.apache.zookeeper.ZooKeeper
import zookeeper.kotlin.ZooKeeperKt
import zookeeper.kotlin.ZookeeperKtClient

fun makeConnectionString(sockets: List<Pair<String, Int>>) =
    sockets.joinToString(separator = ",") { (hostname, port) ->
        "${hostname}:${port}"
    }

suspend fun withZooKeeper(
    zkConnectionString: String,
    block: suspend (client: ZooKeeperKt) -> Unit,
) {
    println("--- Connecting to ZooKeeper @ $zkConnectionString")
    val chan = Channel<Unit>()
    val zk = ZooKeeper(zkConnectionString, 1000) { event ->
        if (event.state == Watcher.Event.KeeperState.SyncConnected &&
            event.type == Watcher.Event.EventType.None
        ) {
            runBlocking { chan.send(Unit) }
        }
    }
    chan.receive()
    println("--- Connected to ZooKeeper")
    block(ZookeeperKtClient(zk))
    zk.close()
}


typealias MainFunction =suspend CoroutineScope.(Array<String>, client: ZooKeeperKt) ->Unit

fun mainWith(args:Array<String> = emptyArray(), the_main: MainFunction) = runBlocking {
    //BasicConfigurator.configure()

    val zkSockets = (1..3).map { Pair("127.0.0.1", 2180 + it) }
    val zkConnectionString = makeConnectionString(zkSockets)

    withZooKeeper(zkConnectionString) {
        the_main(args, it)
    }
}