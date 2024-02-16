import java.lang.Double
import java.math.BigDecimal
import java.math.BigInteger
import java.math.MathContext
import java.math.RoundingMode
import java.util.*
import kotlin.Exception
import kotlin.IllegalArgumentException
import kotlin.Int
import kotlin.Long
import kotlin.div
import kotlin.inc
import kotlin.math.*
import kotlin.minus
import kotlin.plus
import kotlin.rem
import kotlin.require
import kotlin.times
import kotlin.toBigDecimal
import kotlin.toBigInteger

object MathUtil {

    fun log(value: BigDecimal, base: BigDecimal = BigDecimal(Math.E), precision: Int = 50): BigDecimal {
        require(value > BigDecimal.ZERO) { "Value must be greater than zero" }
        require(base > BigDecimal.ZERO) { "Base must be greater than zero" }

        val lnValue = BigDecimal(ln(value.toDouble()))
        val lnBase = BigDecimal(ln(base.toDouble()))

        return lnValue.divide(lnBase).setScale(precision,RoundingMode.HALF_UP)
    }

    fun power(base: BigDecimal, exponent: BigDecimal, precision: Int = 20): BigDecimal {
        require(exponent >= BigDecimal.ZERO) { "Exponent must be non-negative" }


        var result = BigDecimal.ONE
        var exp = exponent

        var currentBase = base
        while (exp > BigDecimal.ZERO) {
            if (exp.remainder(BigDecimal("2")) != BigDecimal.ZERO) {
                result = result.multiply(currentBase, MathContext(precision))
            }
            currentBase = currentBase.multiply(currentBase, MathContext(precision))
            exp = exp.divideToIntegralValue(BigDecimal("2"))
        }


        return result.setScale(precision)
    }

    fun exp(value: BigDecimal, precision: Int = 20): BigDecimal {

        val mc = MathContext(precision, RoundingMode.HALF_UP)
        var result = BigDecimal.ONE
        var term = BigDecimal.ONE
        var n = BigDecimal.ONE

        var i = 0
        while (i < precision) {
            result = result.add(term)
            term = term.multiply(value).divide(n, mc).setScale(precision + 10, RoundingMode.HALF_UP)
            n = n.add(BigDecimal.ONE)
            i++
        }

        return result.setScale(precision, BigDecimal.ROUND_HALF_UP)
    }

    fun ln(value: BigDecimal, mathContext: MathContext): BigDecimal {
        require(value > BigDecimal.ZERO) { "Value must be greater than zero" }

        var result = BigDecimal.ZERO
        var term = (value.subtract(BigDecimal.ONE)).divide(value.add(BigDecimal.ONE), mathContext)

        var n = BigDecimal.ONE
        var sign = BigDecimal.ONE

        var i = 0
        while (i < mathContext.precision) {
            result = result.add(term)
            term = term.multiply((value.subtract(BigDecimal.ONE)).divide(value.add(BigDecimal.ONE), mathContext))
                .multiply((value.subtract(BigDecimal.ONE)).divide(value.add(BigDecimal.ONE), mathContext))
            term = term.setScale(mathContext.precision + 10, RoundingMode.HALF_UP)
            term = term.divide(n.add(BigDecimal.ONE), mathContext)
            n = n.add(BigDecimal.ONE)
            sign = sign.negate()
            i++
        }

        return result.multiply(BigDecimal(2), mathContext)
    }

    fun closestDivisor(number: BigInteger, reference: BigInteger): BigInteger {
        if (number <= BigInteger.ONE) {
            throw Exception("dumb fuck")
        }

        var closestDivisor = reference
        var minDifference = BigInteger.valueOf(Long.MAX_VALUE)

        val sqrt = number.sqrt()

        var i = reference.min(sqrt)
        while (i <= reference && i <= sqrt) {
            if (number % i == BigInteger.ZERO) {
                val complement = number / i

                val difference = (reference - i).abs()
                if (difference < minDifference) {
                    closestDivisor = i
                    minDifference = difference
                }

                val complementDifference = (reference - complement).abs()
                if (complementDifference < minDifference) {
                    closestDivisor = complement
                    minDifference = complementDifference
                }
            }
            i++
        }

        return closestDivisor
    }

    fun findAllFactors(number: BigInteger): List<BigInteger> {
        val factors = mutableListOf<BigInteger>()

        if (number <= BigInteger.ONE) {
            return factors
        }

        var num = number
        var i = BigInteger.TWO
        while (i * i <= num) {
            while (num % i == BigInteger.ZERO) {
                factors.add(i)
                num /= i
            }
            i += BigInteger.ONE
        }

        if (num > BigInteger.ONE) {
            factors.add(num)
        }

        return factors
    }

    fun findAllDivisors(number: BigInteger): List<BigInteger> {
        val factors = findAllFactors(number)
        val divisors = mutableListOf<BigInteger>()

        // Start with 1 as the first divisor
        divisors.add(BigInteger.ONE)

        // Generate divisors by combining factors
        for (i in 0 until (1 shl factors.size)) {
            var divisor = BigInteger.ONE
            for (j in factors.indices) {
                if ((i and (1 shl j)) > 0) {
                    divisor *= factors[j]
                }
            }
            divisors.add(divisor)
        }

        return divisors.distinct().sorted()
    }

    fun cartesianProduct(lists: List<List<BigInteger>>): List<List<BigInteger>> {
        var result = listOf(emptyList<BigInteger>())
        for (list in lists) {
            val temp = mutableListOf<List<BigInteger>>()
            for (item in result) {
                for (element in list) {
                    temp.add(item + element)
                }
            }
            result = temp
        }
        return result
    }

}

data class Tuple(
    val minValue: BigInteger,
    val maxValue: BigInteger,
    val pageCounter: BigInteger = sequentialPageCounter
)

object ConsoleUtil {

    fun clearScreen() {
        val osName = System.getProperty("os.name").lowercase(Locale.getDefault())

        when {
            osName.contains("win") -> {
                ProcessBuilder("cmd", "/c", "cls").inheritIO().start().waitFor()
            }
            else -> {
                print("\u001b[H\u001b[2J")
                System.out.flush()
            }
        }
    }
}

fun MutableList<BigInteger>.average(): BigInteger {
    var sum = BigInteger.ZERO
    forEach { sum = sum.add(it) }
    return sum.div(size.toBigInteger())
}

fun List<BigDecimal>.average(): BigDecimal {
    var sum = BigDecimal.ZERO
    forEach { sum = sum.add(it).setScale(20, RoundingMode.UNNECESSARY) }
    return sum.div(size.toBigDecimal().setScale(20, RoundingMode.UNNECESSARY))
}


fun main() {
    for (i in 0 until 10) {
        println(i + 1)
    }
//    ConsoleUtil.clearLinesFromBottomToTop(5)
//    ConsoleUtil.clearLines(1,5)
    ConsoleUtil.clearScreen()

}