class ThreadController(private val lock: Any) {

    private val queue = mutableMapOf<Int, Thread>()
    lateinit var onQueueComplete : () -> Unit
    lateinit var onInterrupted : () -> Unit

    fun submit(block: () -> Unit) {
        val key = queue.size + 1

        val thread = Thread {
            block.invoke()
            synchronized(lock) {
                queue.remove(key)
                if (queue.isEmpty()) onQueueComplete.invoke()
            }
        }

        queue[key] = thread
        thread.start()
    }

    fun shutdown() {
        synchronized(lock) {
            queue.forEach { keyPair ->
                with(keyPair.value){
                    interrupt()
                    join(1000)
                }
            }
            queue.clear()
            onInterrupted.invoke()
        }
    }
}