import com.google.gson.Gson
import java.math.BigDecimal
import java.math.BigInteger
import java.math.RoundingMode
import java.nio.file.*
import java.rmi.UnexpectedException
import java.security.SecureRandom
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicInteger

@Volatile
private var shutdown = false

@Volatile
private var restart = false

@Volatile
private lateinit var scannedRanges: MutableSet<Pair<BigInteger, BigInteger>>

@Volatile
private var scannedCounter = BigInteger.ONE

private val generatedRanges = mutableSetOf<Pair<BigInteger, BigInteger>>()

@Volatile
private lateinit var fundedMap: Map<String, String>

@Volatile
private var allPagesScanned = BigInteger.ZERO

@Volatile
private var scannedRangeCounter = BigInteger.ZERO

@Volatile
lateinit var sequentialPageCounter: BigInteger

@Volatile
private var threadProgress: MutableMap<Long, () -> Int> = mutableMapOf()

private lateinit var config: Config
private lateinit var scanner: Scanner
private lateinit var threadController: ThreadController
private lateinit var progressBarGroup: ProgressBarGroup
private var threadCount = 0
private val lock = Any()
private val consoleLock = Any()
private val console = Console()
private lateinit var endingStride: BigInteger
private lateinit var currentStride: BigInteger // might have to change this appraoch if threads stop waiting for all to complete before starting a new range
private var strideCounter = BigInteger.ZERO
private fun generateRandomBigInteger(min: BigInteger, max: BigInteger): BigInteger {
    require(min <= max) { "Min value must be less than or equal to max value" }

    val random = SecureRandom()
    val range = max - min + BigInteger.ONE

    val bitLength = range.bitLength()

    val byteArray = ByteArray((bitLength + 7) / 8)
    random.nextBytes(byteArray)

    val randomValue = BigInteger(byteArray)

    val result = min + randomValue.mod(range)

    return if (result <= max) result else max
}

fun generateRandomBigInteger(
    seed: BigInteger,
    increment: BigInteger,
    lowerLimit: BigInteger,
    upperLimit: BigInteger
): BigInteger {

    val multiplier = BigInteger("1664525")
    val modulus = BigInteger("2").pow(32)


    var currentValue = System.nanoTime().toBigInteger() + seed
    currentValue = ((multiplier * currentValue + increment) % modulus + modulus) % modulus

    val scaledValue = BigDecimal(currentValue).divide(BigDecimal(modulus))
    val mappedValue =
        lowerLimit.toBigDecimal() + (scaledValue * (upperLimit.toBigDecimal() - lowerLimit.toBigDecimal()))

    return mappedValue.setScale(0, RoundingMode.HALF_UP).toBigInteger()
}

private fun paginate(minValue: BigInteger, maxValue: BigInteger, pageCounter: BigInteger): BigInteger {

    var counter = pageCounter
    when {

        config.isAscending &&
                pageCounter <= maxValue -> counter += if (config.sequenceType.isStride()) currentStride else BigInteger.ONE

        !config.isAscending &&
                pageCounter >= minValue -> counter -= if (config.sequenceType.isStride()) currentStride else BigInteger.ONE
    }

    if (config.sequenceType.isSequential() || config.sequenceType.isStride()) sequentialPageCounter = counter

    return counter
}

private fun crossCheckAddresses(tuple: Tuple) {
    var threadPagesScanned = BigInteger.ZERO
    var page =
        if (config.sequenceType.isSequential() || config.sequenceType.isStride()) sequentialPageCounter else tuple.pageCounter

    fun processFoundKey(balance: String, publicAddress: String, privateKey: String) {
        val resultString =
            "\nPage Number: $page \n" +
                    "Wallet Balance: $balance \n" +
                    "Wallet Address: $publicAddress \n" +
                    "Wallet Private Key: ${privateKey}\n" +
                    "-----\n".trim()

        writeToFile(scanner.getSaveFoundWalletsWritePath(), resultString, false, false)
        console.updateAndPrint(resultString)
    }

    fun searchByPage() {
        val addressList = scanner.getPrivateKeyPairPage(page)

        addressList.forEach { (publicAddress, privateKey) ->
            fundedMap[publicAddress]?.let {
                processFoundKey(it, publicAddress, privateKey)
            }
        }
    }

    fun searchByAddress() {
        val publicPrivateKey = scanner.getPublicPrivateKey(page)

        fundedMap[publicPrivateKey.first]?.let {
            processFoundKey(it, publicPrivateKey.first, publicPrivateKey.second)
        }
    }

    while (!shutdown && !restart && !Thread.currentThread().isInterrupted) {

        if ((page > tuple.maxValue || page < tuple.minValue)) {
            scannedRangeCounter += BigInteger.ONE
            break
        }

        if (config.searchByPage) searchByPage() else searchByAddress()

        synchronized(lock) {
            allPagesScanned += BigInteger.ONE //todo probably not needed should use sequentialPageCounter instead
            // todo only needed for sequential mode
            page = paginate(tuple.minValue, tuple.maxValue, page)

            threadPagesScanned += BigInteger.ONE
            threadProgress[Thread.currentThread().id] = {
                updateThreadProgress(
                    tuple.maxValue - tuple.minValue,
                    if (config.sequenceType.isSequential()) allPagesScanned
                    else if (config.sequenceType.isStride()) page - tuple.minValue
                    else threadPagesScanned
                )
            }
        }
    }
    synchronized(lock) {
        if (::scannedRanges.isInitialized)
            if (!restart && !shutdown && !Thread.currentThread().isInterrupted)
                scannedRanges.add(Pair(tuple.minValue, tuple.maxValue))
            else {
                scannedRanges.add(Pair(tuple.minValue, page))
            }
    }
}

private fun updateThreadProgress(rangeSize: BigInteger, pagesScanned: BigInteger) =
    (BigDecimal("100").setScale(50, RoundingMode.HALF_UP) /
            rangeSize.toBigDecimal() * pagesScanned.toBigDecimal()).toInt()

lateinit var consoleThread: Thread
private fun clearAndPrint() {
    consoleThread = Thread {
//        var lastAllPagesScanned = BigInteger.ZERO
//        val pagesScanned = mutableListOf<BigInteger>()
        while (!shutdown && !Thread.currentThread().isInterrupted) {
            synchronized(lock) {
                threadProgress.forEach {
                    val progress = it.value.invoke()
                    if (config.sequenceType.isSequential() || config.sequenceType.isStride())
                        progressBarGroup.group.entries.first().value.updateProgress(
                            progress,
                            keep = false,
                            updateConsole = false
                        )
                    else
                        progressBarGroup.update(it.key, progress, false, false)
                }
                console.update(updateText, false)
//                pagesScanned.add(allPagesScanned - lastAllPagesScanned)
//                lastAllPagesScanned = allPagesScanned
//                console.update("Pages Per Second: ${pagesScanned.average()}",false, -1)
//                if(pagesScanned.size >= 10) pagesScanned.clear(); pagesScanned.add(allPagesScanned - lastAllPagesScanned)
                if (!config.sequenceType.isSequential() || config.sequenceType.isStride())
                    progressBarGroup.updateAll()
                console.printAll()
                threadProgress.clear()
            }
            Thread.sleep(250)
        }
    }.apply { start() }
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

private fun readSavedProgress(): Parent? =
    Gson().fromJson(readStringFromFile(scanner.getSaveProgressWritePath()), Parent::class.java)

private fun loadProgress() {
    val progressCounter = readSavedProgress()?.savedProgress?.progressList?.find {
        it.startingRange == config.startPage && it.endingRange == config.endPage
    }
    scannedCounter = progressCounter?.progress ?: BigInteger.ONE
}

private fun setToString(set: Set<Pair<BigInteger, BigInteger>>): String {
    val stringBuilder = StringBuilder()

    for (element in set) {
        stringBuilder.append("${element.first}-${element.second}").append("\n")
    }

    return stringBuilder.toString()
}

private fun toString(set: Set<String>): String {
    val stringBuilder = StringBuilder()

    for (element in set) {
        stringBuilder.append("$element").append("\n")
    }

    return stringBuilder.toString()
}

private fun listToSTring(set: List<BigDecimal>): String {
    val stringBuilder = StringBuilder()

    for (element in set) {
        stringBuilder.append("$element").append("\n")
    }

    return stringBuilder.toString()
}

private fun listToString(set: List<String>): String {
    val stringBuilder = StringBuilder()

    for (element in set) {
        stringBuilder.append("$element").append("\n")
    }

    return stringBuilder.toString()
}

private fun progressToString() =
    readSavedProgress()?.savedProgress?.progressList?.let { list ->
        val progress = list.find { it.startingRange == config.startPage && it.endingRange == config.endPage }

        if (progress != null)
            progress.copy(progress = scannedCounter).let { Gson().toJson(SavedProgress(mutableListOf(it))) }
        else
            Gson().toJson(
                SavedProgress(
                    mutableListOf(
                        Progress(
                            config.startPage,
                            config.endPage,
                            scannedCounter,
                            config.isAscending
                        )
                    )
                )
            )

    } ?: Gson().toJson(
        SavedProgress(
            mutableListOf(
                Progress(
                    config.startPage,
                    config.endPage,
                    scannedCounter,
                    config.isAscending
                )
            )
        )
    )
fun readStringFromFile(filePath: Path): String {
    return try {
        Files.readString(filePath)
    } catch (ex: NoSuchFileException) {
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
                console.updateAndPrint("\nFile created at: ${filePath.toFile().path}")
            }

            console.updateAndPrint("\nWriting data to file")
            Files.write(
                filePath,
                writeableString.toByteArray(),
                *options
            )
        } catch (e: Exception) {
            console.updateAndPrint("\nAn error occurred while writing to the file: ${e.message}")
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
    if (config.sequenceType.isSequential() ||
        config.sequenceType.isRandom() && allPagesScanned >= config.endPage.subtract(config.startPage) ||
        config.sequenceType.isPercentage() && percentageCounter.get() >= (config.percentages?.size ?: -1) ||
        config.sequenceType.isStride() && strideCounter >= endingStride
    ) {
        console.updateAndPrint("Scan Complete")
        shutdown = true
        consoleThread.interrupt()
    } else {
        restart = true
        progressBarGroup.removeAll()
        threadProgress.clear()
//        console.updateAndPrint("Starting new Ranges", false)
        start(config.threadMultiplier)
    }
}

private fun start(threadMultiplier: Int) {

    if (!::fundedMap.isInitialized) {
        console.updateAndPrint("Loading ${config.scannerType} ${if (config.useDatabase) "Database...." else "Address..."}")
        fundedMap =
            if (config.useDatabase) Database.getData(config.scannerType, scanner.getDatabasePath())
            else mapOf(Pair(config.searchAddress.trim(), "Unknown"))
    }

    if (!::scannedRanges.isInitialized) {
//        if (scannedCounter == BigInteger.ONE) {
        println("Loading Scanned Page Data...")
//        loadProgress()
        loadGeneratedPages()
    }

    val availableCores = Runtime.getRuntime().availableProcessors()
    threadCount = availableCores * threadMultiplier

    if (!restart) {
        console.updateAndPrint("Number of available processor cores: $availableCores")
        console.updateAndPrint("Spawning $threadCount threads")
        console.updateAndPrint("Scanning Addresses in ${config.sequenceType} Mode...")
    }

    shutdown = false
    restart = false

    if (config.sequenceType.isStride()) {
        if (!::endingStride.isInitialized) {
            if (config.isAscending) {
                endingStride = (config.endPage - config.startPage) / BigInteger.TWO
                currentStride = config.stride
            } else {
                currentStride = (config.endPage - config.startPage) / BigInteger.TWO
                endingStride = config.stride
            }
            val recoveredStride = Gson().fromJson(readStringFromFile(Paths.get("bitcoin/Stride.json")),String::class.java)
            if(!recoveredStride.isNullOrEmpty())
                currentStride = recoveredStride.toBigInteger()
        } else {
            currentStride += if (config.isAscending) BigInteger.ONE else -BigInteger.ONE
            strideCounter += BigInteger.ONE
        }
    }

    progressBarGroup = ProgressBarGroup()
    for (i in 0 until threadCount) {

        val tuple = when (config.sequenceType) {
            SequenceType.SEQUENTIAL -> Tuple(config.startPage, config.endPage, sequentialPageCounter)
            SequenceType.RANDOM -> generateRandomRange()
            SequenceType.PERCENTAGE -> generatePercentageRange()
            SequenceType.STRIDE -> generateStrideRange(currentStride)
        }

        if (tuple == null) continue

        val threadId = threadController.submit { crossCheckAddresses(tuple) }
        if (((config.sequenceType.isSequential() || config.sequenceType.isStride()) && i == 0) || !config.sequenceType.isSequential() && !config.sequenceType.isStride())
            progressBarGroup.add(threadId, ProgressBar(console).apply {
                length = 100
                title = "Range: ${tuple.minValue} - ${tuple.maxValue}"
            })
    }
    if (threadController.queue.isEmpty()) stopAndRestart() //todo change shutdown boolean?
}

private fun generateRandomRange(): Tuple? {
    val userRangeSizeDifference = config.endPage - config.startPage
    val roundedRangeSize = MathUtil.closestDivisor(userRangeSizeDifference, config.randomRangeSize)
    val maxNumberOfRanges = userRangeSizeDifference / roundedRangeSize

    var foundRange: Pair<BigInteger, BigInteger>? = null
    var foundGeneratedRange: Pair<BigInteger, BigInteger>? = null

    var attemptCounter = 0
    var endingRange: BigInteger
    var startingRange: BigInteger

    do {
        val randomRangeIndex =
            generateRandomBigInteger(scannedCounter, BigInteger.ONE, BigInteger.ONE, maxNumberOfRanges)
        endingRange = config.startPage + randomRangeIndex * roundedRangeSize
        startingRange = endingRange - roundedRangeSize

        synchronized(lock) {
            foundRange = scannedRanges.find { pair -> (startingRange >= pair.first) && (endingRange <= pair.second) }
            foundGeneratedRange =
                generatedRanges.find { pair -> (startingRange >= pair.first) && (endingRange <= pair.second) }
            attemptCounter += 1
            scannedCounter += BigInteger.ONE
        }

        if (attemptCounter.toBigInteger() >= roundedRangeSize) {
            return null
        }

    } while ((foundRange != null || foundGeneratedRange != null))


    val minValue = startingRange
    val maxValue = if (endingRange <= config.endPage) endingRange else config.endPage

    generatedRanges.add(Pair(minValue, maxValue))

    return Tuple(minValue, maxValue, if (config.isAscending) minValue else maxValue)
}


var percentageCounter = AtomicInteger(0) //todo store progress
private fun generatePercentageRange(): Tuple? {

    if (config.percentages.isNullOrEmpty()) throw UnexpectedException("Percentages array is empty")

    var currentCounter = percentageCounter.get()

    if (currentCounter >= (config.percentages?.size!!) || currentCounter < 0) return null

    val percentage =
        config.percentages!![if (config.isAscending) currentCounter else (config.percentages?.size!! - 1) - currentCounter]
    val difference = config.endPage.subtract(config.startPage).toBigDecimal().setScale(20, RoundingMode.HALF_UP)


    val calculatedValue = if (difference == BigDecimal.ZERO) {
        config.startPage
    } else {
//        val adjustedDifference = if (difference < config.startPage.toBigDecimal()) (config.startPage.toBigDecimal() - difference) else difference
        val adjustedDifference = difference
        val value = (adjustedDifference / BigDecimal(100).setScale(0, RoundingMode.HALF_UP)) * percentage
        value.setScale(0, RoundingMode.HALF_UP).toBigInteger()
    }

    var startingRange = (config.startPage + calculatedValue) - (config.randomRangeSize / BigInteger.TWO)
    var endingRange = (config.startPage + calculatedValue) + (config.randomRangeSize / BigInteger.TWO)


    when {
        startingRange < config.startPage && endingRange > config.endPage -> {
            startingRange = config.startPage
            endingRange = config.endPage
        }

        startingRange < config.startPage -> {
            endingRange += (config.startPage - startingRange)
            startingRange = config.startPage
        }

        endingRange > config.endPage -> {
            startingRange += (config.endPage - endingRange)
            endingRange = config.endPage
        }
    }

    percentageCounter.incrementAndGet()

    return Tuple(startingRange, endingRange, if (config.isAscending) startingRange else endingRange)
}

private fun generateStrideRange(stride: BigInteger): Tuple {

    val closestStartMultiple = (stride * (
            config.startPage.toBigDecimal().divide(stride.toBigDecimal(), 10, RoundingMode.HALF_UP)
                .setScale(10, RoundingMode.HALF_UP)
                .toBigInteger() + BigInteger.ONE))
        .run { if (config.isAscending) this.subtract(stride) else this.add(stride) }

    sequentialPageCounter = closestStartMultiple

    return Tuple(closestStartMultiple, config.endPage, sequentialPageCounter)
}

private fun generateRandomStrideRange(stride:BigInteger): Tuple { //todo

    val closestStartMultiple = (stride * (
            config.startPage.toBigDecimal().divide(stride.toBigDecimal(), 10, RoundingMode.HALF_UP)
                .setScale(10, RoundingMode.HALF_UP)
                .toBigInteger() + BigInteger.ONE))
        .run { if (config.isAscending) this.subtract(stride) else this.add(stride) }


    return Tuple(closestStartMultiple, config.endPage, closestStartMultiple)
}

private fun generatePercentageRanges() {
    if (config.percentages.isNullOrEmpty()) throw UnexpectedException("Percentages array is empty")


    val difference = config.endPage.subtract(config.startPage).toBigDecimal().setScale(50)
    val percentageValue = ("100".toBigDecimal().setScale(50)) / difference * config.randomRangeSize.toBigDecimal().setScale(50)


    var percentageCounter = "0".toBigDecimal().setScale(50)
    println(percentageValue)
    val percentages = mutableListOf<BigDecimal>()
    while (percentageCounter != "100".toBigDecimal()) {
        percentages.add(percentageCounter)
        percentageCounter += percentageValue
    }

    percentages.forEach { println(it) }
    percentages.shuffle()
    println()
    percentages.forEach { println(it) }

    writeToFile(Paths.get("shuffled.txt"), Gson().toJson(percentages), true, false)
}

private fun generatePercentageStrides(){

    val list = config.percentages?.filter { it <= "50".toBigDecimal() }?.map {

        val difference = config.endPage.subtract(config.startPage).toBigDecimal().setScale(50)
        val percentageValue = (difference / "100".toBigDecimal().setScale(50)) * it
        println(percentageValue)
        "20000000000000000:3ffffffffffffffff:${percentageValue.toBigInteger()}"
    }?.shuffled()

    writeToFile(Paths.get("shuffled.txt"), listToString(list!!), true, false)
}

fun main() {
    Runtime.getRuntime().addShutdownHook(thread)

    config = Config.readConfig() ?: throw UnexpectedException("Config not found at expected path")
    if (!config.useDatabase && config.searchAddress.isEmpty()) throw UnexpectedException("No Address set to search for")
    scanType = if (config.searchByPage) "Scanned Pages:" else "Scanned Keys:"
    scanner = when (config.scannerType) {
        ScannerType.ETH -> {
            Ethereum
        }

        ScannerType.BTC -> {
            Bitcoin
        }
    }

    sequentialPageCounter = if (config.isAscending) config.startPage else config.endPage

    threadController = ThreadController(lock).apply {
        onQueueComplete = { stopAndRestart() }//todo make onThreadComplete and call there instead
        onInterrupted = { tearDown() }
    }
    scannedRanges = mutableSetOf()

    start(config.threadMultiplier)
    clearAndPrint()
}

private val thread = Thread {
    shutdown = true
    restart = false
    threadController.shutdown()
    consoleThread.interrupt()
}

private fun tearDown() {
    consoleThread.interrupt()
    synchronized(lock) {
//        writeToFile(
//            scanner.getSaveProgressWritePath(),
//            progressToString(),
//            overwrite = true,
//            spawnThread = false
//        )
        writeToFile(
            scanner.getSaveProgressWritePath(),
            setToString(scannedRanges),
            overwrite = true,
            spawnThread = false
        )
        writeToFile(
            Paths.get("bitcoin/Stride.json"),
            Gson().toJson(currentStride),
            overwrite = true,
            spawnThread = false
        )
    }
}

private val updateText
    get() =
        if (config.sequenceType.isSequential())
            "$scanType $allPagesScanned"
        else if (config.sequenceType.isStride())
            "$scanType $allPagesScanned - Range Completions: $strideCounter - Current Stride: $currentStride"
        else
            "$scanType $allPagesScanned - Scanned Ranges $scannedRangeCounter"

private lateinit var scanType: String