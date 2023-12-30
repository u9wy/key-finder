
import org.web3j.crypto.ECKeyPair
import org.web3j.crypto.Keys
import org.web3j.utils.Numeric
import java.math.BigInteger
import java.nio.file.Paths
import java.util.*

object Ethereum : Scanner {
    private fun generatePrivateKey(index: BigInteger): String {
        // Convert the index to a hexadecimal string
        val hexIndex = index.toString(16)

        // Pad the hexadecimal string to 64 characters
        val paddedHexIndex = hexIndex.padStart(64, '0')

        return paddedHexIndex
    }

    private fun privateToAddress(privateKey: String): String {
        // Derive the ECKeyPair from the private key
        val keyPair = ECKeyPair.create(Numeric.toBigInt(privateKey))

        // Derive the Ethereum address from the public key
        val address = Keys.getAddress(keyPair.publicKey)

        // Convert the address to a hexadecimal string without a prefix
        return Keys.toChecksumAddress(address)
    }

    private fun getFirstAddressLocationFromPage(page: BigInteger) =
        page.multiply(BigInteger("100")).subtract("99".toBigInteger())

    override fun getPrivateKeyPairPage(page: BigInteger): Map<String,String> {

        val pageLocation = getFirstAddressLocationFromPage(page)

        val numberOfKeysToGenerate = 99 // You can change this value based on your requirements
        val keyPairList = mutableMapOf<String,String>()

        var i = pageLocation
        while (i <= pageLocation + BigInteger.valueOf(numberOfKeysToGenerate.toLong())) {
//            val startTime = System.currentTimeMillis()
            val privateKey = generatePrivateKey(i)

//            val privateKeyPair = PrivateKeyPair(privateKey,
//                privateToAddress(privateKey).replace("0x","").lowercase(Locale.getDefault())
//            )

            keyPairList[privateToAddress(privateKey).replace("0x","").lowercase(Locale.getDefault())] = privateKey

//            println(System.currentTimeMillis() - startTime)
//            println("Private Key ${i}: ${privateKeyPair.privateKey}")
//            println("Address ${i}: ${privateKeyPair.publicAddress}")
//            println()

            i += BigInteger.ONE
        }

        return keyPairList
    }

    override fun getDatabasePath() = Paths.get("database/ethereum")

    override fun getSaveProgressWritePath() = Paths.get("ethereum/GeneratedPageNumbers.txt")

    override fun getSaveFoundWalletsWritePath() = Paths.get("ethereum/FundedPrivateKeys.txt")
}
//data class PrivateKeyPair(val privateKey: String, val publicAddress: String)

fun main() {
    Ethereum.getPrivateKeyPairPage(BigInteger("1"))
}