package pt.estga.spotme.ui.parking

import android.Manifest
import android.annotation.SuppressLint
import android.app.TimePickerDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.FileProvider
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
import pt.estga.spotme.ui.parking.ParkingFormFragment.Companion.LOCATION_PERMISSION_REQUEST_CODE
import pt.estga.spotme.utils.UserPreferences
import java.util.TimeZone
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.toString

class ParkingFormFragment : BaseFragment() {

    private var _binding: FragmentParkingFormBinding? = null
    private val binding get() = _binding!!
    private lateinit var viewModel: ParkingFormViewModel
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private var startTimeCalendar: Calendar? = null
    private lateinit var repository: ParkingRepository
    private var photoUri: Uri? = null

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
                binding.switchEnableTimer.isChecked = false
            } else {
                with(binding) {
                    editTextTitle.setText(parking.title)
                    editTextLatitude.setText(parking.latitude.toString())
                    editTextLongitude.setText(parking.longitude.toString())
                    editTextDuration.setText((parking.allowedTime / 60000).toString())
                    editTextDescription.setText(parking.description)
                    if (parking.startTime != 0L) {
                        textViewStartTime.text = getString(R.string.start_time_text)+": ${parking.startTime}"
                    }
                    if (parking.allowedTime > 0 || parking.startTime > 0) {
                        switchEnableTimer.isChecked = true
                        layoutTimeControls.visibility = View.VISIBLE
                    } else {
                        switchEnableTimer.isChecked = false
                        layoutTimeControls.visibility = View.GONE
                    }
                }
            }
        }

        binding.switchEnableTimer.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                binding.layoutTimeControls.visibility = View.VISIBLE
            } else {
                binding.layoutTimeControls.visibility = View.GONE
                // Opcional: limpar os valores de tempo quando desativado
                startTimeCalendar = null
                binding.textViewStartTime.text = getString(R.string.start_time_not_selected_text)
                binding.editTextDuration.setText("")
            }
        }

        binding.btnGetLocation.setOnClickListener { requestLocation() }
        binding.btnSelectStartTime.setOnClickListener { showTimePicker() }
        binding.btnTakePhoto.setOnClickListener { checkCameraPermissionAndTakePhoto() }
        binding.btnSelectPhoto.setOnClickListener { selectPhoto() }
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
            Toast.makeText(requireContext(), "Utilizador não está logado!", Toast.LENGTH_SHORT).show()
            return
        }

        try {
            with(binding) {
                parking.title = editTextTitle.text.toString()
                parking.title = editTextTitle.text.toString()
                parking.latitude = editTextLatitude.text.toString().toDouble()
                parking.longitude = editTextLongitude.text.toString().toDouble()

                // Considerar o estado do toggle
                if (switchEnableTimer.isChecked) {
                    parking.allowedTime = editTextDuration.text.toString().toLong() * 60 * 1000
                    parking.startTime = startTimeCalendar?.timeInMillis ?: System.currentTimeMillis()
                } else {
                    // Se o temporizador estiver desativado, definimos valores padrão ou zeros
                    parking.allowedTime = 0
                    parking.startTime = System.currentTimeMillis() // Definimos pelo menos a data atual
                }

                parking.description = editTextDescription.text.toString()
            }
            parking.userId = userId
            parking.photoUri = photoUri?.toString()

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
                editTextTitle.error = getString(R.string.validate_title)
                isValid = false
            }

            // Validar latitude
            if (editTextLatitude.text.isNullOrEmpty()) {
                editTextLatitude.error = getString(R.string.validate_latitude)
                isValid = false
            } else {
                try {
                    editTextLatitude.text.toString().toDouble()
                } catch (e: NumberFormatException) {
                    editTextLatitude.error = getString(R.string.validate_formato_latitude)
                    isValid = false
                }
            }

            // Validar longitude
            if (editTextLongitude.text.isNullOrEmpty()) {
                editTextLongitude.error = getString(R.string.validate_longitude)
                isValid = false
            } else {
                try {
                    editTextLongitude.text.toString().toDouble()
                } catch (e: NumberFormatException) {
                    editTextLongitude.error = getString(R.string.validate_formato_longitude)
                    isValid = false
                }
            }

            // Validar campos de temporização apenas se o toggle estiver ativado
            if (switchEnableTimer.isChecked) {
                // Validar duração
                if (editTextDuration.text.isNullOrEmpty()) {
                    editTextDuration.error = getString(R.string.validate_textDuration)
                    isValid = false
                } else {
                    try {
                        val duration = editTextDuration.text.toString().toLong()
                        if (duration <= 0) {
                            editTextDuration.error = getString(R.string.validate_textDuration_zero)
                            isValid = false
                        }
                    } catch (e: NumberFormatException) {
                        editTextDuration.error = getString(R.string.validate_textDuration_format)
                        isValid = false
                    }
                }

                // Validar hora de início (agora obrigatória)
                if (startTimeCalendar == null) {
                    Toast.makeText(requireContext(), "Hora de início é obrigatória quando a temporização está ativada", Toast.LENGTH_LONG).show()
                    isValid = false
                }
            }
        }

        if (!isValid) {
            Toast.makeText(requireContext(), "Por favor, preencha todos os campos obrigatórios", Toast.LENGTH_LONG).show()
        }

        return isValid
    }

    private fun requestLocation() {
        // Verificar primeiro se o utilizador habilitou a localização nas configurações
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
            binding.textViewStartTime.text = "${getString(R.string.start_time_text)}: %02d:%02d".format(selectedHour, selectedMinute)
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

    private fun takePhoto() {
        val takePictureIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        if (takePictureIntent.resolveActivity(requireActivity().packageManager) != null) {
            val photoFile = createImageFile()
            photoFile?.let {
                val photoURI = FileProvider.getUriForFile(
                    requireContext(),
                    "${requireContext().packageName}.fileprovider",
                    it
                )
                photoUri = photoURI
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI)
                startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE)
            }
        } else {
            Toast.makeText(requireContext(), "Não foi possível abrir a câmera", Toast.LENGTH_SHORT).show()
        }
    }

    private fun checkCameraPermissionAndTakePhoto() {
        if (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(arrayOf(Manifest.permission.CAMERA), CAMERA_PERMISSION_REQUEST_CODE)
        } else {
            takePhoto()
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == CAMERA_PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                takePhoto()
            } else {
                Toast.makeText(requireContext(), "Permissão de câmera negada", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun selectPhoto() {
        try {
            val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
            intent.type = "image/*"
            startActivityForResult(intent, REQUEST_SELECT_PHOTO)
        } catch (e: Exception) {
            Toast.makeText(requireContext(), "Não foi possível abrir a galeria: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun createImageFile(): File? {
        return try {
            val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val storageDir = requireContext().getExternalFilesDir(Environment.DIRECTORY_PICTURES)
            File.createTempFile("JPEG_${timeStamp}_", ".jpg", storageDir)
        } catch (ex: IOException) {
            Toast.makeText(requireContext(), "Erro ao criar arquivo de imagem", Toast.LENGTH_SHORT).show()
            null
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == AppCompatActivity.RESULT_OK) {
            when (requestCode) {
                REQUEST_IMAGE_CAPTURE -> {
                    photoUri?.let {
                        binding.imageViewParking.setImageURI(it)
                        binding.textViewNoImage.visibility = View.GONE
                    }
                }
                REQUEST_SELECT_PHOTO -> {
                    data?.data?.let { uri ->
                        val newFile = createImageFile()
                        if (newFile != null) {
                            try {
                                requireContext().contentResolver.openInputStream(uri)?.use { inputStream ->
                                    newFile.outputStream().use { outputStream ->
                                        inputStream.copyTo(outputStream)
                                    }
                                }
                                photoUri = FileProvider.getUriForFile(
                                    requireContext(),
                                    "${requireContext().packageName}.fileprovider",
                                    newFile
                                )
                                binding.imageViewParking.setImageURI(photoUri)
                                binding.textViewNoImage.visibility = View.GONE
                            } catch (e: IOException) {
                                Toast.makeText(requireContext(), "Erro ao salvar imagem", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        private const val LOCATION_PERMISSION_REQUEST_CODE = 1001
        private const val CAMERA_PERMISSION_REQUEST_CODE = 1002
        private const val REQUEST_IMAGE_CAPTURE = 1
        private const val REQUEST_SELECT_PHOTO = 2
    }
}
