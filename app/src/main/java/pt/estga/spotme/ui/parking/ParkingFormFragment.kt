package pt.estga.spotme.ui.parking

import android.Manifest
import android.annotation.SuppressLint
import android.app.TimePickerDialog
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import pt.estga.spotme.R
import pt.estga.spotme.databinding.FragmentParkingFormBinding
import pt.estga.spotme.entities.Parking
import pt.estga.spotme.ui.BaseFragment
import java.util.Calendar
import kotlinx.coroutines.*
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import pt.estga.spotme.database.repository.ParkingRepository
import java.util.TimeZone

class ParkingFormFragment : BaseFragment() {

    private var _binding: FragmentParkingFormBinding? = null
    private val binding get() = _binding!!
    private lateinit var viewModel: ParkingFormViewModel
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private var startTimeCalendar: Calendar? = null
    private lateinit var repository: ParkingRepository

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentParkingFormBinding.inflate(inflater, container, false)
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity())
        repository = ParkingRepository(requireContext())
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel = androidx.lifecycle.ViewModelProvider(this)[ParkingFormViewModel::class.java]

        viewModel.parking.observe(viewLifecycleOwner) { parking ->
            if (parking == null) {
                viewModel.setParking(Parking())
            } else {
                with(binding) {
                    editTextTitle.setText(parking.title)
                    editTextLatitude.setText(parking.latitude.toString())
                    editTextLongitude.setText(parking.longitude.toString())
                    editTextDuration.setText((parking.allowedTime / 60000).toString())
                    editTextDescription.setText(parking.description)
                    if (parking.startTime != 0L) {
                        textViewStartTime.text = "Hora de início: ${parking.startTime}"
                    }
                }
            }
        }

        binding.btnGetLocation.setOnClickListener { requestLocation() }
        binding.btnSelectStartTime.setOnClickListener { showTimePicker() }
        binding.btnSave.setOnClickListener { saveParking() }
    }

    private fun saveParking() {
        val parking = viewModel.parking.value ?: Parking()

        val userId = userViewModel.userId.value
        if (userId == null || userId == -1L) {
            Toast.makeText(requireContext(), "Utilizador não está loggedIn!", Toast.LENGTH_SHORT).show()
            return
        }

        try {
            with(binding) {
                parking.title = editTextTitle.text.toString()
                parking.latitude = editTextLatitude.text.toString().toDouble()
                parking.longitude = editTextLongitude.text.toString().toDouble()
                parking.allowedTime = editTextDuration.text.toString().toLong() * 60 * 1000
                parking.description = editTextDescription.text.toString()
            }
            parking.userId = userId
            parking.startTime = startTimeCalendar?.timeInMillis ?: parking.startTime

            lifecycleScope.launch {
                withContext(Dispatchers.IO) {
                    if (parking.id == null) repository.insertParking(parking)
                    else repository.updateParking(parking)
                }
                Toast.makeText(requireContext(), "Estacionamento salvo!", Toast.LENGTH_SHORT).show()
                findNavController().navigate(R.id.nav_home)
            }
        } catch (e: Exception) {
            Toast.makeText(requireContext(), "Erro ao salvar: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun requestLocation() {
        if (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
            ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissions(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), LOCATION_PERMISSION_REQUEST_CODE)
            return
        }
        getLastLocation()
    }

    @SuppressLint("MissingPermission")
    private fun getLastLocation() {
        fusedLocationClient.lastLocation.addOnSuccessListener { location ->
            if (location != null) {
                val latitude = location.latitude
                val longitude = location.longitude
                binding.editTextLatitude.setText(latitude.toString())
                binding.editTextLongitude.setText(longitude.toString())

                viewModel.parking.value?.let {
                    it.latitude = latitude
                    it.longitude = longitude
                    viewModel.setParking(it)
                }
            } else {
                Toast.makeText(requireContext(), "Não foi possível obter a localização", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun showTimePicker() {
        val calendar = Calendar.getInstance(TimeZone.getDefault())
        val hour = calendar[Calendar.HOUR_OF_DAY]
        val minute = calendar[Calendar.MINUTE]

        TimePickerDialog(requireContext(), { _, selectedHour, selectedMinute ->
            val selectedTime = Calendar.getInstance(TimeZone.getDefault())

            selectedTime.set(Calendar.HOUR_OF_DAY, selectedHour)
            selectedTime.set(Calendar.MINUTE, selectedMinute)

            startTimeCalendar = selectedTime
            binding.textViewStartTime.text = "Hora de início: %02d:%02d".format(selectedHour, selectedMinute)
        }, hour, minute, true).show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        private const val LOCATION_PERMISSION_REQUEST_CODE = 1001
    }
}
