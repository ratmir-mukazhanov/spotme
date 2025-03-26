package pt.estga.spotme.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey
import java.io.Serializable

@Entity(
    tableName = "parking",
    foreignKeys = [ForeignKey(
        entity = User::class,
        parentColumns = ["id"],
        childColumns = ["userId"],
        onDelete = ForeignKey.CASCADE
    )]
)
class Parking : Serializable {
    @PrimaryKey(autoGenerate = true)
    var id: Long? = null
    var title: String? = null
    var latitude: Double = 0.0
    var longitude: Double = 0.0
    var allowedTime: Long = 0
    var startTime: Long = 0
    var endTime: Long = 0
    var description: String? = null
    @ColumnInfo(name = "userId", index = true)
    var userId: Long? = null

    constructor()

    constructor(
        title: String?,
        latitude: Double,
        longitude: Double,
        allowedTime: Long,
        startTime: Long,
        description: String?,
        userId: Long?
    ) {
        this.title = title
        this.latitude = latitude
        this.longitude = longitude
        this.allowedTime = allowedTime
        this.startTime = startTime
        this.endTime = startTime + allowedTime
        this.description = description
        this.userId = userId
    }

    fun updateEndTime() {
        this.endTime = this.startTime + this.allowedTime
    }
}