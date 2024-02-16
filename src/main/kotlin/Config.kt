import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import java.math.BigDecimal
import java.math.BigInteger
import java.nio.file.Path
import java.nio.file.Paths

data class Config(
    @SerializedName("ScannerType") val scannerType: ScannerType,
    @SerializedName("ThreadMultiplier") val threadMultiplier: Int,
    @SerializedName("SequenceType") val sequenceType: SequenceType,
    @SerializedName("IsAscending") val isAscending:Boolean,
    @SerializedName("StartPage") val startPage: BigInteger,
    @SerializedName("EndPage") val endPage: BigInteger,
    @SerializedName("ScanningRangeSize") val randomRangeSize: BigInteger = "1000000".toBigInteger(),
    @SerializedName("Percentages") val percentages: List<BigDecimal>? = null,
    @SerializedName("Stride") val stride: BigInteger = "100000000".toBigInteger(),
    @SerializedName("UseDatabase") val useDatabase : Boolean = false,
    @SerializedName("SearchAddress") val searchAddress : String = "",
    @SerializedName("SearchByPage") val searchByPage : Boolean = true
){
    companion object {
        fun readConfig() : Config? =
            Gson().fromJson(readStringFromFile(Paths.get("config.json")),Config::class.java)
    }
}
enum class ScannerType(val type : String) {
    @SerializedName("btc") BTC("btc"),
    @SerializedName("eth") ETH("eth")
}

enum class SequenceType(val type : String) {
    @SerializedName("sequential") SEQUENTIAL("sequential"),
    @SerializedName("random") RANDOM("random"),
    @SerializedName("percentage") PERCENTAGE("percentage"),
    @SerializedName("stride") STRIDE("stride");

    fun isSequential() = type == "sequential"
    fun isStride() = type == "stride"
    fun isPercentage() = type == "percentage"
    fun isRandom() = type == "random"
}

