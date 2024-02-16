import com.google.gson.Gson
import org.bitcoinj.core.Address
import org.bitcoinj.core.ECKey
import org.bitcoinj.params.MainNetParams
import org.bitcoinj.script.Script
import java.math.BigDecimal
import java.math.BigInteger
import java.math.MathContext
import java.math.RoundingMode
import java.nio.file.Paths
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.*
import kotlin.math.ln
import kotlin.math.log10

object Bitcoin : Scanner {
    fun generatePrivateKey(index: BigInteger): ECKey {
        val keyBytes = index.toByteArray()
        val key = ECKey.fromPrivate(keyBytes, true)
        return key
    }

    fun privateKeyToWIF(privateKey: ECKey): String {
        return privateKey.getPrivateKeyEncoded(MainNetParams.get()).toString()
    }

    fun privateKeyToPublicAddress(privateKey: ECKey): String {
        val address = Address.fromKey(MainNetParams.get(), privateKey, Script.ScriptType.P2PKH)
        return address.toString()
    }

    private fun getFirstAddressLocationFromPage(page: BigInteger) =
        page.multiply(BigInteger("100")).subtract("99".toBigInteger())

    override fun getPublicPrivateKey(index: BigInteger): Pair<String, String> {
        val privateKey = generatePrivateKey(if(index == BigInteger.ONE) "2".toBigInteger() else index) //todo fix
        val publicKey = privateKeyToPublicAddress(privateKey)
        return Pair (publicKey,privateKey.privateKeyAsHex)
    }
    override fun getPrivateKeyPairPage(page: BigInteger): Map<String, String> {

        val pageLocation = getFirstAddressLocationFromPage(page)

        val keysToGenerate = "99".toBigInteger()
        val startIndex = pageLocation
        val endIndex = startIndex + keysToGenerate

        val keyPairList = mutableMapOf<String, String>()

        var i = startIndex
        while (i <= endIndex) {
            if (i <= "1".toBigInteger()) i = "2".toBigInteger() //TODO FIX WORK AROUND

            val privateKey = generatePrivateKey(i)
            val wif = privateKeyToWIF(privateKey)
            val address = privateKeyToPublicAddress(privateKey)

            keyPairList[address] = wif

//        println("${privateKey.privKey}\t${privateKey.privateKeyAsHex}\t$address")

//        println("Index: $i")
//        println("Private Key Decimal: ${privateKey.privKey}")
//        println("Private Key Hex: ${privateKey.privateKeyAsHex}")
//        println("WIF: $wif")
//        println("Public Address: $address")
//        println("------------")
            i += BigInteger.ONE
        }

        return keyPairList
    }

    override fun getDatabasePath() = Paths.get("database/bitcoin")

    override fun getSaveProgressWritePath() = Paths.get("bitcoin/ScannedPageRanges.txt")

    override fun getSaveFoundWalletsWritePath() = Paths.get("bitcoin/FundedPrivateKeys.txt")

}

fun main(){
    Bitcoin.getPrivateKeyPairPage(BigInteger("1157920892373161954235709850086879078528375642790749043826051631415181614944"))
}