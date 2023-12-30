import java.math.BigInteger
import java.nio.file.Files
import java.nio.file.NoSuchFileException
import java.nio.file.Path
import java.nio.file.StandardOpenOption
import java.rmi.UnexpectedException
import java.security.SecureRandom
import java.util.*
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.ThreadPoolExecutor

@Volatile private var shutdown = false

private val generatedNumbers = mutableSetOf<BigInteger>()
@Volatile private lateinit var fundedMap: Map<String, String>


@Volatile private var scannedPages =  BigInteger.ZERO
@Volatile private var isSequenceRandom = true
@Volatile private var scanUniquePages = false
@Volatile private var isAscending = true
@Volatile private lateinit var sequentialPageCounter: BigInteger
@Volatile private lateinit var maxValue: BigInteger
@Volatile private lateinit var minValue: BigInteger
private lateinit var type: Type
private lateinit var scanner: Scanner

fun generateRandomBigInteger(): BigInteger {

    val minLength = 1
    val maxLength = 77
    val random = SecureRandom()

    var value: String

    fun generateNumber(): BigInteger {
        do {
            val length = random.nextInt(maxLength - minLength + 1) + minLength

            value = buildString {
                repeat(length) {
                    append(random.nextInt(10)) // Append a random digit
                }
            }
        } while (value.isEmpty() || value.toBigInteger().let { it < minValue || it > maxValue })

        return when {
            scanUniquePages && generatedNumbers.contains(value.toBigInteger()) -> generateNumber()
            else -> {
                generatedNumbers.add(value.toBigInteger())
                value.toBigInteger()
            }
        }
    }
    return generateNumber()
}

private fun paginate(): BigInteger {
    generatedNumbers.add(sequentialPageCounter)

    when {
        sequentialPageCounter == maxValue || sequentialPageCounter == minValue -> {
            return if (isAscending) {
                sequentialPageCounter += BigInteger.ONE
                minValue
            } else {
                sequentialPageCounter -= BigInteger.ONE
                maxValue
            }
        }

        isAscending &&
                sequentialPageCounter < maxValue -> sequentialPageCounter += BigInteger.ONE

        !isAscending &&
                sequentialPageCounter > minValue -> sequentialPageCounter -= BigInteger.ONE
    }

    return sequentialPageCounter
}

private fun getPageNumber() =
    when {
        isSequenceRandom -> generateRandomBigInteger()
        else -> paginate()
    }

private fun crossCheckAddresses() {
    while (!shutdown) {
        val page = getPageNumber()
        val addressList = scanner.getPrivateKeyPairPage(page)

        addressList.forEach { (publicAddress, privateKey) ->
            fundedMap[publicAddress]?.let {
                val resultString =
                    "\nPage Number: $page \n" +
                            "Wallet Balance: $it \n" +
                            "Wallet Address: $publicAddress \n" +
                            "Wallet Private Key: ${privateKey}\n" +
                            "-----\n".trim()

                writeToFile(scanner.getSaveFoundWalletsWritePath(), resultString)
                println(resultString)
            }
        }
        scannedPages += BigInteger.ONE
    }
}

private val consoleThreadPool = Executors.newFixedThreadPool(1)
private fun clearAndPrint() {
    consoleThreadPool.submit(Thread {
        while (!shutdown) {
            if (scannedPages > BigInteger.ZERO) {
                clearLine(99)
                printUpdate()
                Thread.sleep(1000)
            }
            Thread.sleep(1000)
        }
        clearLine(99)
        printUpdate()
    })
}

private fun printUpdate() {
    print("Scanned Pages: $scannedPages ")
}

private fun clearLine(length: Int) {
    for (i in 0..length) {
        print("\b")
    }
}

private fun loadGeneratedPages() {
    generatedNumbers
        .addAll(
            readStringFromFile(scanner.getSaveProgressWritePath())
                .split("\n")
                .map { s -> s.replace("\n", "").trim() }
                .filter { s -> s.isNotEmpty() }
                .map { s -> BigInteger(s) })
}

private fun setToString(set: Set<BigInteger>): String {
    val stringBuilder = StringBuilder()

    for (element in set) {
        stringBuilder.append(element.toString()).append("\n")
    }

    return stringBuilder.toString()
}

fun readStringFromFile(filePath: Path): String {
    return try {
        Files.readString(filePath)
    } catch (ex: NoSuchFileException) {
        ""
    }
}

private val fileThreadPool = Executors.newFixedThreadPool(1)
fun writeToFile(filePath: Path, content: String, spawnThread: Boolean = true) {

    fun write() {
        var writeableString = ""
        if (readStringFromFile(filePath).isNotEmpty())
            writeableString = "\n"

        writeableString += content

        try {
            if (!Files.exists(filePath)) {
                Files.createDirectories(filePath.parent)
                Files.createFile(filePath)
                println("\nFile created at: ${filePath.toFile().path}")
            }

            println("\nWriting data to file")
            Files.write(
                filePath,
                writeableString.toByteArray(),
                StandardOpenOption.CREATE,
                StandardOpenOption.APPEND
            )
        } catch (e: Exception) {
            println("\nAn error occurred while writing to the file: ${e.message}")
        }
    }

    if (spawnThread)
        fileThreadPool.submit(
            Thread {
                write()
            })
    else write()
}

private lateinit var mainThreadPool: ExecutorService
private fun start(threadMultiplier: Int) {

    println("Loading $type Database....")
    fundedMap = Database.getData(type, scanner.getDatabasePath())

    println("Loading Scanned Page Data...")
    loadGeneratedPages()

    val availableCores = Runtime.getRuntime().availableProcessors()
    val runnableThreads = availableCores * threadMultiplier

    println("Number of available processor cores: $availableCores")
    println("Spawning $runnableThreads threads")
    mainThreadPool = Executors.newFixedThreadPool(runnableThreads)

    println("Scanning Addresses...")
    clearAndPrint()
    for (i in 0 until runnableThreads) {
        mainThreadPool.submit(Thread {
            crossCheckAddresses()
        })
    }
}

fun main(args: Array<String>) {
    Runtime.getRuntime().addShutdownHook(thread)

    val scannerType = args[0]
    val threadMultiplier = args[1].toInt()
    val isSequential = args[2].toBoolean()
    val ascending = args[3].toBoolean()
    val crossCheckScannedPages = args[4].toBoolean()
    val startPage = args[5].toBigInteger()
    val endPage = args[6].toBigInteger()

    type = when (scannerType.lowercase(Locale.getDefault())) {
        "eth" -> {
            scanner = Ethereum; Type.ETH
        }

        "btc" -> {
            scanner = Bitcoin; Type.BTC
        }

        else -> {
            throw UnexpectedException("Unexpected Type")
        }
    }
    isSequenceRandom = !isSequential
    isAscending = ascending
    scanUniquePages = crossCheckScannedPages
    maxValue = endPage
    minValue = startPage

    sequentialPageCounter = if (isAscending) minValue else maxValue

    start(threadMultiplier)
}

val thread = Thread {
    shutdown = true
    if((mainThreadPool as ThreadPoolExecutor).poolSize > 0)
        mainThreadPool.shutdownNow()
    Thread.sleep(5000)
    writeToFile(scanner.getSaveProgressWritePath(), setToString(generatedNumbers), false)
}