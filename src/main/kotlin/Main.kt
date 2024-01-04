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
import kotlin.system.exitProcess

@Volatile
private var shutdown = false

@Volatile
private var restart = false

@Volatile
private lateinit var scannedRanges: MutableSet<Pair<BigInteger, BigInteger>>

@Volatile
private lateinit var fundedMap: Map<String, String>

@Volatile
private var scannedPages = "-1".toBigInteger()

@Volatile
private var scannedRangeCounter = BigInteger.ZERO

@Volatile
private lateinit var sequentialPageCounter: BigInteger

@Volatile
private lateinit var maxValue: BigInteger

@Volatile
private lateinit var minValue: BigInteger

private lateinit var config: Config
private lateinit var scanner: Scanner
private lateinit var threadController: ThreadController
private val lock = Any()

fun generateRandomBigInteger(min: BigInteger, max: BigInteger): BigInteger {

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
        } while (value.isEmpty() || value.toBigInteger().let { it < min || it > max })

        return value.toBigInteger()
    }


    return generateNumber()
}

private fun paginate(): BigInteger {

    when {
        config.isAscending &&
                sequentialPageCounter <= maxValue -> sequentialPageCounter += BigInteger.ONE

        !config.isAscending &&
                sequentialPageCounter >= minValue -> sequentialPageCounter -= BigInteger.ONE
    }

    return sequentialPageCounter
}

private fun crossCheckAddresses() {
    while (!shutdown && !restart && !Thread.currentThread().isInterrupted) {

        val page = paginate()

        if ((page > maxValue || page < minValue))
            return

        val addressList = scanner.getPrivateKeyPairPage(page)

        addressList.forEach { (publicAddress, privateKey) ->
            fundedMap[publicAddress]?.let {
                val resultString =
                    "\nPage Number: $page \n" +
                            "Wallet Balance: $it \n" +
                            "Wallet Address: $publicAddress \n" +
                            "Wallet Private Key: ${privateKey}\n" +
                            "-----\n".trim()

                writeToFile(scanner.getSaveFoundWalletsWritePath(), resultString, false)
                println(resultString)
            }
        }
        scannedPages += BigInteger.ONE
    }
}


private val consoleThreadPool = Executors.newFixedThreadPool(1)
private fun clearAndPrint() {
    fun print() {
        clearLine(updateText.length)
        print(updateText)
    }
    consoleThreadPool.submit(Thread {
        while (!shutdown) {
            if (scannedPages > BigInteger.ZERO) {
                print()
                Thread.sleep(1000)
            }
            Thread.sleep(1000)
        }
        print()
    })
}


private fun clearLine(length: Int) {
    for (i in 0..length) {
        print("\b")
    }
}

private fun loadGeneratedPages() {
    scannedRanges = mutableSetOf()
    scannedRanges
        .addAll(
            readStringFromFile(scanner.getSaveProgressWritePath())
                .split("\n")
                .map { s -> s.replace("\n", "").trim() }
                .filter { s -> s.isNotEmpty() }
                .map { s -> Pair(s.split("-")[0].toBigInteger(), s.split("-")[1].toBigInteger()) })
}

private fun setToString(set: Set<Pair<BigInteger, BigInteger>>): String {
    val stringBuilder = StringBuilder()

    for (element in set) {
        stringBuilder.append("${element.first}-${element.second}").append("\n")
    }

    return stringBuilder.toString()
}

fun readStringFromFile(filePath: Path): String {
    return try {
        Files.readString(filePath)
    } catch (ex: NoSuchFileException) {
        println("Error reading file ${filePath.toFile().path}: {ex.message}")
        ""
    }
}

private val fileThreadPool = Executors.newFixedThreadPool(1)
fun writeToFile(filePath: Path, content: String, overwrite: Boolean, spawnThread: Boolean = true) {

    fun write() {
        var writeableString = ""
        if (readStringFromFile(filePath).isNotEmpty())
            writeableString = "\n"

        writeableString += content

        val options =
            if (overwrite)
                arrayOf(StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE)
            else
                arrayOf(StandardOpenOption.CREATE, StandardOpenOption.APPEND)

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
                *options
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

private fun stopAndRestart() {
    if (config.isSequential) {
        println("Scan Complete")
        shutdown = true
        exitProcess(0)
    } else {
        restart = true
        scannedRanges.add(Pair(minValue, maxValue))
        start(config.threadMultiplier)
    }
}

private fun start(threadMultiplier: Int) {

    if (!::fundedMap.isInitialized) {
        println("Loading ${config.type} Database....")
        fundedMap = Database.getData(config.type, scanner.getDatabasePath())
    }

    if (!::scannedRanges.isInitialized) {
        println("Loading Scanned Page Data...")
        loadGeneratedPages()
    }

    val availableCores = Runtime.getRuntime().availableProcessors()
    val runnableThreads = availableCores * threadMultiplier

    if (!restart) {
        println("Number of available processor cores: $availableCores")
        println("Spawning $runnableThreads threads")
        println("Scanning Addresses...")
    }

    shutdown = false
    restart = false

    if (!config.isSequential)
        generateRandomRange()


    clearAndPrint()
    for (i in 0 until runnableThreads) {
        threadController.submit { crossCheckAddresses() }
    }
}

private fun generateRandomRange() { //todo fix recursive loop issue in small ranges
    val startingRange = generateRandomBigInteger(config.startPage, config.endPage)

    val foundRange = scannedRanges.find { pair -> (startingRange >= pair.first) && startingRange <= pair.second }

    if (foundRange != null)
        generateRandomRange()


    val endingRange = when {
        (startingRange + config.randomRangeSize) <= config.endPage -> startingRange + config.randomRangeSize
        else -> config.endPage
    }

    minValue = startingRange
    maxValue = endingRange
    sequentialPageCounter = if (config.isAscending) minValue else maxValue
    scannedRangeCounter += BigInteger.ONE
}

fun main(args: Array<String>) {
    Runtime.getRuntime().addShutdownHook(thread)

    val type = when (args[0].lowercase(Locale.getDefault())) {
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

    config = Config(
        type,
        args[1].toInt(),
        args[2].toBoolean(),
        args[3].toBoolean(),
        args[4].toBigInteger(),
        args[5].toBigInteger(),
    )
    config = config.copy(randomRangeSize = args.getOrNull(6)?.toBigInteger() ?: config.randomRangeSize)

    maxValue = config.startPage
    minValue = config.endPage

    sequentialPageCounter = if (config.isAscending) minValue else maxValue

    threadController = ThreadController(lock).apply {
        onQueueComplete = { stopAndRestart() }
        onInterrupted = { tearDown() }
    }
    start(config.threadMultiplier)
}

private val thread = Thread {
    shutdown = true
    restart = false
    threadController.shutdown()
}

private fun tearDown() {
    synchronized(lock) {
        if (::scannedRanges.isInitialized) {
            if (config.isAscending)
                scannedRanges.add(Pair(minValue, sequentialPageCounter))
            else
                scannedRanges.add(Pair(maxValue, sequentialPageCounter))

            writeToFile(
                scanner.getSaveProgressWritePath(),
                setToString(scannedRanges),
                overwrite = true,
                spawnThread = false
            )
        }
    }
}

private val updateText
    get() =
        if (config.isSequential)
            "Scanned Pages: $scannedPages"
        else
            "Scanned Pages: $scannedPages - Scanned Ranges $scannedRangeCounter"

/**
 * TODO add stop code to random mode
 */