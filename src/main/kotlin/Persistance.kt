import com.google.gson.annotations.SerializedName
import java.math.BigInteger

data class Parent(val savedProgress: SavedProgress)

data class SavedProgress(
    @SerializedName("progressList")
    val progressList: List<Progress>
)

data class Progress(
    @SerializedName("startingRange")
    val startingRange: BigInteger,
    @SerializedName("endingRange")
    val endingRange: BigInteger,
    @SerializedName("progress")
    val progress: BigInteger,
    @SerializedName("isAscending")
    val isAscending: Boolean
)

