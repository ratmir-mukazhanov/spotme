package pt.estga.spotme.ui.routing

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.preference.PreferenceManager
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.fragment.app.Fragment
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.ItemizedIconOverlay
import org.osmdroid.views.overlay.OverlayItem
import org.osmdroid.views.overlay.Polyline
import pt.estga.spotme.R
import pt.estga.spotme.databinding.FragmentMapRouteBinding
import pt.estga.spotme.entities.Parking
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class MapRouteFragment : Fragment() {

    private var _binding: FragmentMapRouteBinding? = null
    private val binding get() = _binding!!

    private lateinit var map: MapView
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private var parking: Parking? = null

    companion object {
        private const val LOCATION_PERMISSION_REQUEST_CODE = 1001

        fun newInstance(parking: Parking): MapRouteFragment {
            val fragment = MapRouteFragment()
            val args = Bundle()
            args.putSerializable("parking", parking)
            fragment.arguments = args
            return fragment
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            parking = it.getSerializable("parking") as? Parking
        }
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity())

        // Configuração do OSMDroid
        Configuration.getInstance().load(
            requireContext(),
            PreferenceManager.getDefaultSharedPreferences(requireContext())
        )
        Configuration.getInstance().userAgentValue = requireContext().packageName
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMapRouteBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupMap()
        setupListeners()

        // Solicitar localização automaticamente ao carregar
        getCurrentLocation()
    }

    private fun setupMap() {
        map = binding.map
        map.setTileSource(TileSourceFactory.MAPNIK)
        map.setMultiTouchControls(true)
        val mapController = map.controller
        mapController.setZoom(15.0)

        // Adicionar marcador para o local do estacionamento
        parking?.let { addParkingMarker(it) }
    }

    private fun setupListeners() {
        binding.fabMyLocation.setOnClickListener {
            getCurrentLocation()
        }

        binding.fabCloseInfo.setOnClickListener {
            binding.cardRouteInfo.visibility = View.GONE
            binding.fabCloseInfo.visibility = View.GONE
        }
    }

    private fun addParkingMarker(parking: Parking) {
        val parkingPoint = GeoPoint(parking.latitude, parking.longitude)

        // Usar o Marker do OSMdroid em vez de OverlayItem
        val marker = org.osmdroid.views.overlay.Marker(map)
        marker.position = parkingPoint
        marker.setAnchor(org.osmdroid.views.overlay.Marker.ANCHOR_CENTER, org.osmdroid.views.overlay.Marker.ANCHOR_BOTTOM)
        marker.icon = resources.getDrawable(R.drawable.ic_parking_marker)
        marker.title = "Estacionaste aqui"
        marker.snippet = parking.title

        // Mostrar o info window por padrão
        marker.showInfoWindow()

        // Adicionar ao mapa
        map.overlays.add(marker)
        map.controller.setCenter(parkingPoint)
    }

    private fun getCurrentLocation() {
        if (ActivityCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                requireActivity(),
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                LOCATION_PERMISSION_REQUEST_CODE
            )
            return
        }

        binding.loadingOverlay.visibility = View.VISIBLE

        fusedLocationClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, CancellationTokenSource().token)
            .addOnSuccessListener { location ->
                location?.let {
                    val currentPoint = GeoPoint(it.latitude, it.longitude)

                    // Limpar marcadores anteriores de localização atual
                    val currentLocationOverlays = map.overlays
                        .filterIsInstance<org.osmdroid.views.overlay.Marker>()
                        .filter { it.title == "Estás aqui" }
                    map.overlays.removeAll(currentLocationOverlays)

                    // Usar o Marker do OSMdroid
                    val marker = org.osmdroid.views.overlay.Marker(map)
                    marker.position = currentPoint
                    marker.setAnchor(org.osmdroid.views.overlay.Marker.ANCHOR_CENTER, org.osmdroid.views.overlay.Marker.ANCHOR_BOTTOM)
                    marker.icon = resources.getDrawable(R.drawable.ic_current_location_marker)
                    marker.title = "Estás aqui"

                    // Mostrar o info window por padrão
                    marker.showInfoWindow()

                    // Adicionar ao mapa
                    map.overlays.add(marker)

                    // Obter e desenhar a rota
                    parking?.let { parking ->
                        fetchRouteFromOSRM(currentPoint, GeoPoint(parking.latitude, parking.longitude))
                    }

                    map.invalidate()
                } ?: run {
                    binding.loadingOverlay.visibility = View.GONE
                    Toast.makeText(requireContext(), "Não foi possível obter sua localização", Toast.LENGTH_SHORT).show()
                }
            }
            .addOnFailureListener { e ->
                binding.loadingOverlay.visibility = View.GONE
                Toast.makeText(requireContext(), "Erro ao obter localização: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun fetchRouteFromOSRM(start: GeoPoint, end: GeoPoint) {
        binding.loadingOverlay.visibility = View.VISIBLE

        val coordinates = "${start.longitude},${start.latitude};${end.longitude},${end.latitude}"

        val pedestrianOsrmService = Retrofit.Builder()
            .baseUrl("https://router.project-osrm.org/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(PedestrianOSRMService::class.java)

        pedestrianOsrmService.getRoute(coordinates).enqueue(object : Callback<OSRMResponse> {
            override fun onResponse(call: Call<OSRMResponse>, response: Response<OSRMResponse>) {
                binding.loadingOverlay.visibility = View.GONE

                if (response.isSuccessful) {
                    response.body()?.let { osrmResponse ->
                        if (osrmResponse.code == "Ok" && osrmResponse.routes.isNotEmpty()) {
                            val route = osrmResponse.routes[0]
                            val routePoints = PolylineDecoder.decode(route.geometry)

                            // Distância em metros
                            val distance = route.distance

                            // Calcular duração manualmente baseado em velocidade média de caminhada (5 km/h)
                            // 5 km/h = 1.38889 m/s
                            val walkingSpeedMps = 1.38889
                            val calculatedDuration = distance / walkingSpeedMps

                            Log.d("MapRouteFragment", "Duração original: ${route.duration}s, Duração calculada: ${calculatedDuration}s")

                            activity?.runOnUiThread {
                                drawDetailedRoute(routePoints, distance, calculatedDuration)
                                showRouteInfo(distance, calculatedDuration)
                            }
                        } else {
                            showErrorToast("Não foi possível calcular a rota a pé")
                        }
                    } ?: showErrorToast("Resposta vazia da API")
                } else {
                    showErrorToast("Erro na API: ${response.code()}")
                }
            }

            override fun onFailure(call: Call<OSRMResponse>, t: Throwable) {
                binding.loadingOverlay.visibility = View.GONE
                showErrorToast("Falha ao obter rota a pé: ${t.localizedMessage}")
            }
        })
    }

    private fun showRouteInfo(distance: Double, duration: Double) {
        binding.tvDistance.text = formatDistance(distance)
        binding.tvDuration.text = formatDuration(duration) + getString(R.string.onFoot_text)
        binding.cardRouteInfo.visibility = View.VISIBLE
        binding.fabCloseInfo.visibility = View.VISIBLE
    }

    private fun formatDistance(meters: Double): String {
        return if (meters >= 1000) {
            String.format("%.1f km", meters / 1000)
        } else {
            String.format("%.0f m", meters)
        }
    }

    private fun formatDuration(seconds: Double): String {
        val hours = (seconds / 3600).toInt()
        val minutes = ((seconds % 3600) / 60).toInt()

        return if (hours > 0) {
            "$hours h $minutes min"
        } else {
            "$minutes min"
        }
    }

    private fun showErrorToast(message: String) {
        activity?.runOnUiThread {
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        }
    }

    private fun drawDetailedRoute(routePoints: List<GeoPoint>, distance: Double, duration: Double) {
        // Remover rotas anteriores
        val routeOverlays = map.overlays.filter { it is Polyline }
        map.overlays.removeAll(routeOverlays)

        // Criar uma nova linha para a rota
        val routeLine = Polyline()
        routeLine.setPoints(routePoints)
        routeLine.outlinePaint.color = Color.BLUE
        routeLine.outlinePaint.strokeWidth = 10f

        map.overlays.add(routeLine)
        map.invalidate()

        // Mostrar o botão de direções
        binding.fabDirections.visibility = View.VISIBLE

        // Configurar listener do botão de direções
        binding.fabDirections.setOnClickListener {
            parking?.let { parking ->
                openGoogleMapsDirections(GeoPoint(parking.latitude, parking.longitude))
            }
        }

        // Ajustar zoom para mostrar a rota completa
        map.zoomToBoundingBox(routeLine.bounds, true, 100)
    }

    private fun openGoogleMapsDirections(destination: GeoPoint) {
        try {
            // URI para abrir o Google Maps com direções a pé para o destino
            val uri = "google.navigation:q=${destination.latitude},${destination.longitude}&mode=w"
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(uri))
            intent.setPackage("com.google.android.apps.maps")

            // Verificar se o Google Maps está instalado
            if (intent.resolveActivity(requireActivity().packageManager) != null) {
                startActivity(intent)
            } else {
                // Alternativa para abrir no navegador se o app não estiver instalado
                val webUri = "https://www.google.com/maps/dir/?api=1&destination=${destination.latitude},${destination.longitude}&travelmode=walking"
                startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(webUri)))
            }
        } catch (e: Exception) {
            Toast.makeText(
                requireContext(),
                "Não foi possível abrir o Google Maps: ${e.localizedMessage}",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    override fun onResume() {
        super.onResume()
        map.onResume()
    }

    override fun onPause() {
        super.onPause()
        map.onPause()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                getCurrentLocation()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}