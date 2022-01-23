package multipaxos

import kotlinx.coroutines.*

fun main(args: Array<String>){
    println("start running")
    runBlocking {
        val timer = CountDownTimer()
        timer.startTimer()
    }
}


open class CountDownTimer() {

    private val job = SupervisorJob()
    private val scope = CoroutineScope(Dispatchers.Default + job)
    var active = true

    private fun startCoroutineTimer(delayMillis: Long, repeatMillis: Long, action: () -> Unit) = scope.launch {
        //delay(delayMillis)
        var counter = 5
        if (repeatMillis > 0) {
            while (counter > 0) {
                //action()
                delay(repeatMillis)
                println(counter)
                counter--
            }
        } else {
            action()
        }
        active = false
    }
    private val timer: Job = startCoroutineTimer(delayMillis = 0, repeatMillis = 1000) {
        //Log.d(TAG, "Background - tick")
        //doSomethingBackground()
        println("started")
        scope.launch(Dispatchers.Main) {
            //Log.d(TAG, "Main thread - tick")
            //doSomethingMainThread()
            println("tick")
        }
    }

    fun startTimer() {
        timer.start()
    }

    fun cancelTimer() {
        timer.cancel()
    }

}
