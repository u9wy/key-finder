import java.math.BigInteger
import java.nio.file.Path

interface Scanner {

    fun getPrivateKeyPairPage(page : BigInteger) : Map<String,String>
    fun getDatabasePath() : Path
    fun getSaveProgressWritePath() : Path
    fun getSaveFoundWalletsWritePath() : Path

}