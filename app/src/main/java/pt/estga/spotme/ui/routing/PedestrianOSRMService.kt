package pt.estga.spotme.ui.routing

import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Path

interface PedestrianOSRMService {
    @GET("route/v1/foot/{coordinates}?overview=full&geometries=polyline6")
    fun getRoute(@Path("coordinates", encoded = false) coordinates: String): Call<OSRMResponse>
}