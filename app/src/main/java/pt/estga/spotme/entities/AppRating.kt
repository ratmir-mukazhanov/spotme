package pt.estga.spotme.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey
import java.io.Serializable
import java.util.Date

@Entity(
    tableName = "app_ratings",
    foreignKeys = [ForeignKey(
        entity = User::class,
        parentColumns = ["id"],
        childColumns = ["userId"],
        onDelete = ForeignKey.CASCADE
    )]
)
class AppRating : Serializable {
    @PrimaryKey(autoGenerate = true)
    var id: Long? = null
    var rating: Float = 0.0f
    var feedback: String? = null
    var timestamp: Long = 0
    var isSubmittedToServer: Boolean = false
    @ColumnInfo(name = "userId", index = true)
    var userId: Long? = null

    constructor()

    constructor(
        rating: Float,
        feedback: String?,
        timestamp: Long,
        userId: Long?,
        isSubmittedToServer: Boolean = false
    ) {
        this.rating = rating
        this.feedback = feedback
        this.timestamp = timestamp
        this.userId = userId
        this.isSubmittedToServer = isSubmittedToServer
    }

    fun isPositiveRating(): Boolean {
        return rating >= 4.0f
    }
}