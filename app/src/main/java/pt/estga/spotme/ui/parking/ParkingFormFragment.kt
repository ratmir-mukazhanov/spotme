package pt.estga.spotme.ui.parking

import android.Manifest
import android.annotation.SuppressLint
import android.app.TimePickerDialog
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.TimePicker
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.Navigation.findNavController
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import pt.estga.spotme.R
import pt.estga.spotme.database.AppDatabase.Companion.getInstance
import pt.estga.spotme.entities.Parking
import pt.estga.spotme.utils.UserSession
import java.util.Calendar
import java.util.concurrent.Executors

class ParkingFormFragment : Fragment() {
    private lateinit var mViewModel: ParkingFormViewModel
    private lateinit var titleEditText: EditText
    private lateinit var latitudeEditText: EditText
    private lateinit var longitudeEditText: EditText
    private lateinit var durationEditText: EditText
    private lateinit var descriptionEditText: EditText
    private lateinit var textViewStartTime: TextView
    private lateinit var btnGetLocation: Button
    private lateinit var btnSelectStartTime: Button
    private lateinit var btnSave: Button

    private var fusedLocationClient: FusedLocationProviderClient? = null
    private var startTimeCalendar: Calendar? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val root = inflater.inflate(R.layout.fragment_parking_form, container, false)

        titleEditText = root.findViewById(R.id.editTextTitle)
        latitudeEditText = root.findViewById(R.id.editTextLatitude)
        longitudeEditText = root.findViewById(R.id.editTextLongitude)
        descriptionEditText = root.findViewById(R.id.editTextDescription)
        textViewStartTime = root.findViewById(R.id.textViewStartTime)
        durationEditText = root.findViewById(R.id.editTextDuration)
        btnGetLocation = root.findViewById(R.id.btnGetLocation)
        btnSelectStartTime = root.findViewById(R.id.btnSelectStartTime)
        btnSave = root.findViewById(R.id.btnSave)

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity())

        btnGetLocation.setOnClickListener(View.OnClickListener { v: View? -> requestLocation() })
        btnSelectStartTime.setOnClickListener(View.OnClickListener { v: View? -> showTimePicker() })

        return root
    }

    @SuppressLint("SetTextI18n")
    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        mViewModel = ViewModelProvider(this)[ParkingFormViewModel::class.java]

        mViewModel.parking.observe(viewLifecycleOwner) { parking: Parking? ->
            var parking = parking
            if (parking == null) {
                // Se for null, cria um novo objeto Parking
                parking = Parking()
                mViewModel.setParking(parking)
            } else {
                // Se existir, preencher os campos do formulário
                titleEditText.setText(parking.title)
                latitudeEditText.setText(parking.latitude.toString())
                longitudeEditText.setText(parking.longitude.toString())
                durationEditText.setText(parking.allowedTime.toString())
                descriptionEditText.setText(parking.description)

                if (parking.startTime != 0L) {
                    textViewStartTime.text = "Hora de início: " + parking.startTime
                }
            }
        }

        btnSave.setOnClickListener { saveParking() }
    }

    private fun saveParking() {
        var parking = mViewModel.parking.value
        if (parking == null) {
            parking = Parking()
        }

        val userId = UserSession.getInstance(requireContext()).userId

        if (userId == -1L) {
            Toast.makeText(requireContext(), "Utilizador não está loggedIn!", Toast.LENGTH_SHORT)
                .show()
            return
        }

        // Atualizar os valores do estacionamento
        parking.title = titleEditText.text.toString()
        parking.latitude = latitudeEditText.text.toString().toDouble()
        parking.longitude = longitudeEditText.text.toString().toDouble()
        parking.allowedTime = durationEditText.text.toString().toInt().toLong() * 60 * 1000
        parking.description = descriptionEditText.text.toString()
        parking.userId = userId

        if (startTimeCalendar != null) {
            val startTimeMillis = startTimeCalendar!!.timeInMillis
            parking.startTime = startTimeMillis
        }

        val db = getInstance(requireContext())

        val finalParking: Parking = parking
        Executors.newSingleThreadExecutor().execute {
            if (finalParking.id == null) {
                db.parkingDao().insert(finalParking)
            } else {
                db.parkingDao().update(finalParking)
            }
            requireActivity().runOnUiThread {
                Toast.makeText(requireContext(), "Estacionamento salvo!", Toast.LENGTH_SHORT).show()
                // Navegar de volta para a lista de estacionamentos
                val navController =
                    findNavController(requireActivity(), R.id.nav_host_fragment_content_main)
                navController.navigate(R.id.nav_home)
            }
        }
    }

    private fun requestLocation() {
        if (ActivityCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED &&
            ActivityCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissions(
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                LOCATION_PERMISSION_REQUEST_CODE
            )
            return
        }
        lastLocation
    }

    @get:SuppressLint("MissingPermission")
    private val lastLocation: Unit
        get() {
            fusedLocationClient!!.lastLocation
                .addOnSuccessListener(
                    requireActivity()
                ) { location ->
                    if (location != null) {
                        val latitude = location.latitude
                        val longitude = location.longitude

                        latitudeEditText.setText(latitude.toString())
                        longitudeEditText.setText(longitude.toString())

                        val parking = mViewModel.parking.value
                        if (parking != null) {
                            parking.latitude = latitude
                            parking.longitude = longitude
                            mViewModel.setParking(parking)
                        }
                    } else {
                        Toast.makeText(
                            requireContext(),
                            "Não foi possível obter a localização",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
        }

    private fun showTimePicker() {
        val calendar = Calendar.getInstance()
        val hour = calendar[Calendar.HOUR_OF_DAY]
        val minute = calendar[Calendar.MINUTE]

        val timePickerDialog = TimePickerDialog(
            requireContext(),
            { view: TimePicker?, selectedHour: Int, selectedMinute: Int ->
                val selectedTime = Calendar.getInstance()
                selectedTime[Calendar.HOUR_OF_DAY] = selectedHour
                selectedTime[Calendar.MINUTE] = selectedMinute

                startTimeCalendar = selectedTime
                textViewStartTime.text =
                    "Hora de início: " + selectedHour + ":" + String.format("%02d", selectedMinute)
            }, hour, minute, true
        )
        timePickerDialog.show()
    }

    companion object {
        private const val LOCATION_PERMISSION_REQUEST_CODE = 1001
    }
}
