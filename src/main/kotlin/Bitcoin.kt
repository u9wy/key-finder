
import org.bitcoinj.core.*
import org.bitcoinj.params.MainNetParams
import org.bitcoinj.script.Script
import java.math.BigInteger
import java.nio.file.Paths
import java.util.*

object Bitcoin : Scanner {
    private fun generatePrivateKey(index: BigInteger): ECKey {
        val keyBytes = index.toByteArray()
        val key = ECKey.fromPrivate(keyBytes, true)
        return key
    }

    private fun privateKeyToWIF(privateKey: ECKey): String {
        return privateKey.getPrivateKeyEncoded(MainNetParams.get()).toString()
    }

    private fun privateKeyToPublicAddress(privateKey: ECKey): String {
        val address = Address.fromKey(MainNetParams.get(), privateKey, Script.ScriptType.P2PKH)
        return address.toString()
    }

    private fun getFirstAddressLocationFromPage(page: BigInteger) =
        page.multiply(BigInteger("100")).subtract("99".toBigInteger())

    override fun getPrivateKeyPairPage(page: BigInteger) : Map<String,String> {

        val pageLocation = getFirstAddressLocationFromPage(page)

        val keysToGenerate = "99".toBigInteger()
        val startIndex = pageLocation
        val endIndex = startIndex + keysToGenerate

        val keyPairList = mutableMapOf<String,String>()

        var i = startIndex
        while (i <= endIndex) {
            if(i == "1".toBigInteger() ) continue //TODO FIX WORK AROUND

            val privateKey = generatePrivateKey(i)
            val wif = privateKeyToWIF(privateKey)
            val address = privateKeyToPublicAddress(privateKey)

            keyPairList [address.lowercase(Locale.getDefault()).trim()] = privateKey.privateKeyAsHex.lowercase(Locale.getDefault()).trim()

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

    override fun getSaveProgressWritePath() = Paths.get("bitcoin/GeneratedPageNumbers.txt")

    override fun getSaveFoundWalletsWritePath() = Paths.get("bitcoin/FundedPrivateKeys.txt")

    fun calculate() {
        val addresses =
            "0000000000000000000000000000000000000000000000000000000000000001\n" +
                    "0000000000000000000000000000000000000000000000000000000000000003\n" +
                    "0000000000000000000000000000000000000000000000000000000000000007\n" +
                    "0000000000000000000000000000000000000000000000000000000000000008\n" +
                    "0000000000000000000000000000000000000000000000000000000000000015\n" +
                    "0000000000000000000000000000000000000000000000000000000000000031\n" +
                    "000000000000000000000000000000000000000000000000000000000000004c\n" +
                    "00000000000000000000000000000000000000000000000000000000000000e0\n" +
                    "00000000000000000000000000000000000000000000000000000000000001d3\n" +
                    "0000000000000000000000000000000000000000000000000000000000000202\n" +
                    "0000000000000000000000000000000000000000000000000000000000000483\n" +
                    "0000000000000000000000000000000000000000000000000000000000000a7b\n" +
                    "0000000000000000000000000000000000000000000000000000000000001460\n" +
                    "0000000000000000000000000000000000000000000000000000000000002930\n" +
                    "00000000000000000000000000000000000000000000000000000000000068f3\n" +
                    "000000000000000000000000000000000000000000000000000000000000c936\n" +
                    "000000000000000000000000000000000000000000000000000000000001764f\n" +
                    "000000000000000000000000000000000000000000000000000000000003080d\n" +
                    "000000000000000000000000000000000000000000000000000000000005749f\n" +
                    "00000000000000000000000000000000000000000000000000000000000d2c55\n" +
                    "00000000000000000000000000000000000000000000000000000000001ba534\n" +
                    "00000000000000000000000000000000000000000000000000000000002de40f\n" +
                    "0000000000000000000000000000000000000000000000000000000000556e52\n" +
                    "0000000000000000000000000000000000000000000000000000000000dc2a04\n" +
                    "0000000000000000000000000000000000000000000000000000000001fa5ee5\n" +
                    "000000000000000000000000000000000000000000000000000000000340326e\n" +
                    "0000000000000000000000000000000000000000000000000000000006ac3875\n" +
                    "000000000000000000000000000000000000000000000000000000000d916ce8\n" +
                    "0000000000000000000000000000000000000000000000000000000017e2551e\n" +
                    "000000000000000000000000000000000000000000000000000000003d94cd64\n" +
                    "000000000000000000000000000000000000000000000000000000007d4fe747\n" +
                    "00000000000000000000000000000000000000000000000000000000b862a62e\n" +
                    "00000000000000000000000000000000000000000000000000000001a96ca8d8\n" +
                    "000000000000000000000000000000000000000000000000000000034a65911d\n" +
                    "00000000000000000000000000000000000000000000000000000004aed21170\n" +
                    "00000000000000000000000000000000000000000000000000000009de820a7c\n" +
                    "0000000000000000000000000000000000000000000000000000001757756a93\n" +
                    "00000000000000000000000000000000000000000000000000000022382facd0\n" +
                    "0000000000000000000000000000000000000000000000000000004b5f8303e9\n" +
                    "000000000000000000000000000000000000000000000000000000e9ae4933d6\n" +
                    "00000000000000000000000000000000000000000000000000000153869acc5b\n" +
                    "000000000000000000000000000000000000000000000000000002a221c58d8f\n" +
                    "000000000000000000000000000000000000000000000000000006bd3b27c591\n" +
                    "00000000000000000000000000000000000000000000000000000e02b35a358f\n" +
                    "0000000000000000000000000000000000000000000000000000122fca143c05\n" +
                    "00000000000000000000000000000000000000000000000000002ec18388d544\n" +
                    "00000000000000000000000000000000000000000000000000006cd610b53cba\n" +
                    "0000000000000000000000000000000000000000000000000000ade6d7ce3b9b\n" +
                    "000000000000000000000000000000000000000000000000000174176b015f4d\n" +
                    "00000000000000000000000000000000000000000000000000022bd43c2e9354\n" +
                    "00000000000000000000000000000000000000000000000000075070a1a009d4\n" +
                    "000000000000000000000000000000000000000000000000000efae164cb9e3c\n" +
                    "00000000000000000000000000000000000000000000000000180788e47e326c\n" +
                    "00000000000000000000000000000000000000000000000000236fb6d5ad1f43\n" +
                    "000000000000000000000000000000000000000000000000006abe1f9b67e114\n" +
                    "000000000000000000000000000000000000000000000000009d18b63ac4ffdf\n" +
                    "00000000000000000000000000000000000000000000000001eb25c90795d61c\n" +
                    "00000000000000000000000000000000000000000000000002c675b852189a21\n" +
                    "00000000000000000000000000000000000000000000000007496cbb87cab44f\n" +
                    "0000000000000000000000000000000000000000000000000fc07a1825367bbe\n" +
                    "00000000000000000000000000000000000000000000000013c96a3742f64906\n" +
                    "000000000000000000000000000000000000000000000000363d541eb611abee\n" +
                    "0000000000000000000000000000000000000000000000007cce5efdaccf6808\n" +
                    "000000000000000000000000000000000000000000000000f7051f27b09112d4\n" +
                    "000000000000000000000000000000000000000000000001a838b13505b26867"

        val addressesList = addresses.split("\n").map { s -> s.trim() }
        val averagesList = mutableListOf<DifferentBetweenKeys>()

        addressesList.forEachIndexed { index, address ->
            if (index % 2 == 0) {
                val i = index + 1
                if (i < addressesList.size) {
                    val difference = addressesList[i].toBigInteger(16) - addressesList[index].toBigInteger(16)
                    averagesList.add(DifferentBetweenKeys(addressesList[index], addressesList[i], difference))
                }
            }
        }

        averagesList.forEachIndexed { index, obj -> println("${obj.difference}") }
        addressesList.forEach { println(it) }
    }

    data class DifferentBetweenKeys(val key1: String, val key2: String, val difference: BigInteger)
}

fun main(){
//    BitcoinScanner.calculate()
//    BitcoinScanner.getPrivateKeyPairPage(BigInteger("1157920892373161954235709850086879078528375642790749043826051631415181614944"))
}