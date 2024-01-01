import java.math.BigInteger

data class Config(
    val type: Type,
    val threadMultiplier : Int,
    val isSequential : Boolean,
    val isAscending : Boolean,
    val startPage : BigInteger,
    val endPage : BigInteger,
    val randomRangeSize : BigInteger = "1000000".toBigInteger()
)
