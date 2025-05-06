package pt.estga.spotme.ui.routing

import com.google.gson.annotations.SerializedName
import retrofit2.Call
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Path

interface OSRMService {
    @GET("route/v1/driving/{coordinates}?overview=full&geometries=polyline6")
    fun getRoute(@Path("coordinates", encoded = false) coordinates: String): Call<OSRMResponse>
}

data class OSRMResponse(
    val code: String,
    val routes: List<Route>
)

data class Route(
    val distance: Double,
    val duration: Double,
    val geometry: String,
    val legs: List<Leg>
)

data class Leg(
    val steps: List<Step>,
    val distance: Double,
    val duration: Double
)

data class Step(
    val distance: Double,
    val duration: Double,
    val name: String,
    val maneuver: Maneuver
)

data class Maneuver(
    val location: List<Double>,
    @SerializedName("type") val type: String,
    @SerializedName("modifier") val modifier: String?
)