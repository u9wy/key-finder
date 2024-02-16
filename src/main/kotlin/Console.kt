class Console {
    private val consoleText = mutableMapOf<String, Pair<String, Boolean>>()
    var lastPrintMillis = System.currentTimeMillis()

    fun remove(key : Int){
        consoleText.remove(key.toString())
    }

    fun removeAll(){
        consoleText.clear()
    }

    fun update(text: String, keep: Boolean = true,key : Int? = null) {
        consoleText[(key?.toString() ?: ("${consoleText.size} + A"))] = Pair(text, keep)
    }

    fun updateAndPrint(text: String, keep: Boolean = true) {
        ConsoleUtil.clearScreen()
        update(text,keep)
        printAll()
    }

    fun printAll() {
        ConsoleUtil.clearScreen()
        val deleteList = mutableListOf<String>()
        consoleText.forEach {
            println(it.value.first)
            if (!it.value.second) {
                deleteList.add(it.key)
            }
        }
        lastPrintMillis = System.currentTimeMillis()
        deleteList.forEach { consoleText.remove(it) }
    }
}