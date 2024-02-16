class ProgressBar(private val console: Console) {

    var progress = 0
    var length: Int = 50
    var title: String = "Progress"

    private val incomplete = '░' // U+2591 Unicode Character
    private val complete = '█' // U+2588 Unicode Character
    private val builder = StringBuilder("".padEnd(length, incomplete))

    fun updateProgress(percentage: Int, key: Int? = null, keep: Boolean = false, updateConsole: Boolean = true) {
        progress = if (percentage in 0..100) percentage else 0
        val progressBar = buildProgressBar()
        if (updateConsole)
            console.update("$title\n $progress% $progressBar", keep, key)
    }

    private fun buildProgressBar(): String {
        val filledLength = (length * progress) / 100
        builder.setLength(0)
        builder.append("".padEnd(filledLength, complete))
        builder.append("".padEnd(length - filledLength, incomplete))
        return builder.toString()
    }
}

class ProgressBarGroup {

    var group = mutableMapOf<Long, ProgressBar>()

    fun add(key: Long, progressBar: ProgressBar) {
        group[key] = progressBar
        group = group.toSortedMap()
    }

    fun remove(key: Long) {
        group.remove(key)
        group = group.toSortedMap()
    }

    fun removeAll() {
        group.clear()
    }

    fun update(key: Long, progress: Int, keep: Boolean, updateConsole: Boolean = true) {
        group[key]?.updateProgress(progress, key.toInt(), keep, updateConsole)
    }

    fun updateAll() {
        group.forEach {
            it.value.updateProgress(
                it.value.progress, it.key.toInt(), false, true)
        }
    }
}

fun main() {
    val console = Console()
    val progressBar = ProgressBar(console)

    // Usage example

    // Simulate progress update
    for (i in 0..100) {
        progressBar.updateProgress(i)
        console.printAll()
        Thread.sleep(50)
    }
}
