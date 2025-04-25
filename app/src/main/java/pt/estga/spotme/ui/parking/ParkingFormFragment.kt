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
import pt.estga.spotme.utils.UserPreferences
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

        updateLocationButtonState()

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

        // Validar campos obrigatórios
        if (!validateFields()) {
            return
        }

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
                Toast.makeText(requireContext(), "Estacionamento guardado com sucesso!", Toast.LENGTH_SHORT).show()
                findNavController().navigate(R.id.nav_home)
            }
        } catch (e: Exception) {
            Toast.makeText(requireContext(), "Erro ao salvar: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun validateFields(): Boolean {
        var isValid = true

        with(binding) {
            // Validar título
            if (editTextTitle.text.isNullOrEmpty()) {
                editTextTitle.error = "Título é obrigatório"
                isValid = false
            }

            // Validar latitude
            if (editTextLatitude.text.isNullOrEmpty()) {
                editTextLatitude.error = "Latitude é obrigatória"
                isValid = false
            } else {
                try {
                    editTextLatitude.text.toString().toDouble()
                } catch (e: NumberFormatException) {
                    editTextLatitude.error = "Formato de latitude inválido"
                    isValid = false
                }
            }

            // Validar longitude
            if (editTextLongitude.text.isNullOrEmpty()) {
                editTextLongitude.error = "Longitude é obrigatória"
                isValid = false
            } else {
                try {
                    editTextLongitude.text.toString().toDouble()
                } catch (e: NumberFormatException) {
                    editTextLongitude.error = "Formato de longitude inválido"
                    isValid = false
                }
            }

            // Validar duração
            if (editTextDuration.text.isNullOrEmpty()) {
                editTextDuration.error = "Duração é obrigatória"
                isValid = false
            } else {
                try {
                    val duration = editTextDuration.text.toString().toLong()
                    if (duration <= 0) {
                        editTextDuration.error = "A duração deve ser maior que zero"
                        isValid = false
                    }
                } catch (e: NumberFormatException) {
                    editTextDuration.error = "Formato de duração inválido"
                    isValid = false
                }
            }

            // Validar hora de início (opcional)
            if (startTimeCalendar == null) {
                Toast.makeText(requireContext(), "Recomendado definir uma hora de início", Toast.LENGTH_SHORT).show()
            }
        }

        if (!isValid) {
            Toast.makeText(requireContext(), "Por favor, preencha todos os campos obrigatórios", Toast.LENGTH_LONG).show()
        }

        return isValid
    }

    private fun requestLocation() {
        // Verificar primeiro se o usuário habilitou a localização nas configurações
        val userPreferences = UserPreferences.getInstance(requireContext())

        if (!userPreferences.isLocationEnabled()) {
            Toast.makeText(
                requireContext(),
                "Serviços de localização estão desativados nas configurações do aplicativo.",
                Toast.LENGTH_LONG
            ).show()
            return
        }

        // Continuar com a verificação de permissões do sistema
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

        getLastLocation()
    }

    @SuppressLint("MissingPermission")
    private fun getLastLocation() {
        val userPreferences = UserPreferences.getInstance(requireContext())

        // Verificação dupla para garantir que a localização está habilitada
        if (!userPreferences.isLocationEnabled()) {
            return
        }

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
                Toast.makeText(
                    requireContext(),
                    "Não foi possível obter a localização",
                    Toast.LENGTH_SHORT
                ).show()
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

    private fun updateLocationButtonState() {
        val userPreferences = UserPreferences.getInstance(requireContext())

        // Se a localização estiver desativada, podemos desabilitar o botão ou torná-lo menos visível
        binding.btnGetLocation.isEnabled = userPreferences.isLocationEnabled()

        // Opcionalmente, adicionar uma dica visual
        if (!userPreferences.isLocationEnabled()) {
            binding.btnGetLocation.alpha = 0.5f
            binding.btnGetLocation.setOnClickListener {
                Toast.makeText(
                    requireContext(),
                    "Ative os serviços de localização nas configurações do aplicativo primeiro.",
                    Toast.LENGTH_LONG
                ).show()
            }
        } else {
            binding.btnGetLocation.alpha = 1.0f
            binding.btnGetLocation.setOnClickListener { requestLocation() }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        private const val LOCATION_PERMISSION_REQUEST_CODE = 1001
    }
}
