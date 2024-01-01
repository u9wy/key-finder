import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

object Database {

    private fun readLargeTSVFile(file: File, block: (line: String?) -> Pair<String, String>?): Map<String, String> {
        val lines = mutableMapOf<String, String>()

        try {
            file.useLines { linesSequence ->
                linesSequence.forEach { line ->
                    block.invoke(line)?.let { pair ->
                        lines[pair.first] = pair.second
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return lines
    }

    private fun getEthereumData(line: String?): Pair<String, String>? {
        var parsedString: Pair<String, String>? = null
        line?.let {
            val balance = it.substring(it.lastIndexOf("\t"), it.length).trim()
            if (balance.length >= 17 && !it.contains("nonce")) { // excluding addresses with less than 0.01 eth and excluding headers
                parsedString = Pair(it.substring(0, it.indexOf("\t")).trim(), balance)
            }
        }
        return parsedString
    }

    private fun getBitcoinData(line: String?): Pair<String, String>? {
        var parsedString: Pair<String, String>? = null
        line?.let { parsedString = Pair(line.trim(), "Unknown")}
        return parsedString
    }

    private fun getParserMethod(type: Type) =
        when (type) {
            Type.ETH -> {
                val block: (line: String?) -> Pair<String, String>? = { line -> getEthereumData(line) }; block
            }

            Type.BTC -> {
                val block: (line: String?) -> Pair<String, String>? = { line -> getBitcoinData(line) }; block
            }
        }


    fun getData(type: Type, path: Path): Map<String, String> {
        val data = mutableMapOf<String, String>()
        if (Files.exists(path)) {
            path.toFile().listFiles()
                ?.filter { file -> file.extension == "tsv" }
                ?.forEach { file -> data.putAll(readLargeTSVFile(file, getParserMethod(type))) }

        } else {
            Files.createDirectory(path)
            getData(type, path)
        }

        println("Funded Wallets: ${data.size}")

        return data
    }
}

fun main() {
    Database.getData(Type.ETH, Paths.get("database/ethereum"))
    Database.getData(Type.BTC, Paths.get("database/bitcoin"))
}