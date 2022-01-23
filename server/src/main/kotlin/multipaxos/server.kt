package multipaxos

import io.grpc.ServerBuilder

suspend fun main(args: Array<String>) = mainWith(args) {_, zk ->
    val id = args[0].toInt()

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
}