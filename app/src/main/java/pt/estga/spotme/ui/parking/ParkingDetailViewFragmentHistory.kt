package pt.estga.spotme.ui.parking

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.os.CountDownTimer
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.net.toUri
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.Navigation.findNavController
import kotlinx.coroutines.launch
import pt.estga.spotme.R
import pt.estga.spotme.database.AppDatabase
import pt.estga.spotme.database.ParkingDao
import pt.estga.spotme.databinding.FragmentParkingDetailViewHistoryBinding
import pt.estga.spotme.entities.Parking
import pt.estga.spotme.ui.BaseFragment
import pt.estga.spotme.utils.ClipboardUtils
import pt.estga.spotme.utils.DateFormatter
import pt.estga.spotme.utils.GeocodingHelper
import pt.estga.spotme.utils.ParkingDetailHelper
import java.util.concurrent.Executors

class ParkingDetailViewFragmentHistory : BaseFragment() {

    private var _binding: FragmentParkingDetailViewHistoryBinding? = null
    private val binding get() = _binding!!

    private lateinit var parking: Parking
    private lateinit var countDownTimer: CountDownTimer
    private lateinit var parkingDao: ParkingDao

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentParkingDetailViewHistoryBinding.inflate(inflater, container, false)
        parkingDao = AppDatabase.getInstance(requireContext()).parkingDao()
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        arguments?.getSerializable("parking")?.let {
            parking = it as Parking
            setupUI(parking)
        } ?: Toast.makeText(requireContext(), "Erro ao carregar o estacionamento", Toast.LENGTH_SHORT).show()

        setupListeners()
    }

    private fun setupUI(parking: Parking) {
        // Configurar título e localização
        binding.tvTitle.text = parking.title ?: getString(R.string.my_parking_text)

        // Configurar data do estacionamento
        binding.tvParkingDate.text = DateFormatter.formatDate(parking.startTime)

        lifecycleScope.launch {
            val address = GeocodingHelper.getAddressFromCoordinates(
                requireContext(),
                parking.latitude,
                parking.longitude
            )
            binding.tvCoordinates.text = address
        }

        // Verificar se há temporizador
        if (parking.allowedTime <= 0) {
            // Não tem temporizador definido
            binding.cardTimer.visibility = View.GONE
            binding.tvTimerNotDefined.visibility = View.VISIBLE
            binding.chipDuration.visibility = View.GONE

            // Ocultar seções de hora de início e fim
            val startTimeParent = (binding.tvStartTime.parent as View).parent as View
            startTimeParent.visibility = View.GONE

            val endTimeParent = (binding.tvEndTime.parent as View).parent as View
            endTimeParent.visibility = View.GONE
        } else {
            // Tem temporizador definido
            binding.cardTimer.visibility = View.VISIBLE
            binding.tvTimerNotDefined.visibility = View.GONE

            // Mostrar seções de hora de início e fim
            val startTimeParent = (binding.tvStartTime.parent as View).parent as View
            startTimeParent.visibility = View.VISIBLE

            val endTimeParent = (binding.tvEndTime.parent as View).parent as View
            endTimeParent.visibility = View.VISIBLE

            // Configurar horário de início
            binding.tvStartTime.text = DateFormatter.formatTime(parking.startTime)

            // Configurar horário de fim
            val currentTime = System.currentTimeMillis()
            if (parking.endTime != null && parking.endTime > 0) {
                // Se tem um endTime registrado, usa esse valor
                binding.tvEndTime.text = DateFormatter.formatTime(parking.endTime!!)
            } else {
                // Caso contrário, sempre mostra o horário calculado (startTime + allowedTime)
                binding.tvEndTime.text = DateFormatter.formatTime(parking.startTime + parking.allowedTime)
            }

            // Configurar duração
            val durationMinutes = (parking.allowedTime / (1000 * 60)).toInt()
            binding.chipDuration.text = "${getString(R.string.duration_text)}: $durationMinutes min"
            binding.chipDuration.visibility = View.VISIBLE

            // Status do estacionamento
            if (currentTime > (parking.startTime + parking.allowedTime)) {
                binding.tvStatus.text = getString(R.string.concluded_text)
            } else {
                binding.tvStatus.text = getString(R.string.ongoing_text)
            }
        }

        binding.tilNotes.visibility = View.VISIBLE
        binding.btnEditNotes.visibility = View.VISIBLE

        if (!parking.description.isNullOrEmpty()) {
            binding.etNotes.setText(parking.description)
        } else {
            binding.etNotes.setText("")
        }

        // Verificar se há foto
        if (parking.photoUri.isNullOrEmpty()) {
            binding.btnViewPhoto.visibility = View.GONE
        } else {
            binding.btnViewPhoto.visibility = View.VISIBLE
        }
    }

    private fun setupListeners() {
        binding.btnCopyCoordinates.setOnClickListener {
            ClipboardUtils.copyText(
                requireContext(),
                "Coordenadas",
                "${parking.latitude}, ${parking.longitude}"
            )
        }

        binding.btnShare.setOnClickListener {
            ParkingDetailHelper.share(requireContext(), parking)
        }

        binding.btnViewPhoto.setOnClickListener { viewPhoto() }

        binding.btnRoute.setOnClickListener {
            ParkingDetailHelper.openInMaps(requireContext(), parking.latitude, parking.longitude)
        }

        binding.btnEditLocation.setOnClickListener { editParkingName() }

        binding.btnEditNotes.setOnClickListener { editNotes() }

        binding.btnDelete.setOnClickListener { deleteParking() }
    }

    private fun editParkingName() {
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_edit_parking_name, null)
        val etEditName = dialogView.findViewById<android.widget.EditText>(R.id.et_edit_parking_name)
        val btnSaveName = dialogView.findViewById<View>(R.id.btn_save_parking_name)
        val btnCancelName = dialogView.findViewById<View>(R.id.btnCancel)
        val btnClose = dialogView.findViewById<View>(R.id.btnClose)

        val alertDialog = android.app.AlertDialog.Builder(requireContext())
            .setView(dialogView)
            .create()

        btnCancelName.setOnClickListener {
            alertDialog.dismiss()
        }

        btnClose.setOnClickListener {
            alertDialog.dismiss()
        }

        etEditName.setText(parking.title)

        alertDialog.show()

        btnSaveName.setOnClickListener {
            val newName = etEditName.text.toString()
            parking.title = newName
            Executors.newSingleThreadExecutor().execute {
                parkingDao.update(parking)
                requireActivity().runOnUiThread {
                    binding.tvTitle.text = newName
                    Toast.makeText(requireContext(), "Nome atualizado com sucesso", Toast.LENGTH_SHORT).show()
                    alertDialog.dismiss()
                }
            }
        }
    }

    private fun editNotes() {
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_edit_notes, null)
        val etEditNotes = dialogView.findViewById<android.widget.EditText>(R.id.et_edit_notes)
        val btnSaveNotes = dialogView.findViewById<View>(R.id.btn_save_notes)
        val btnCancelNotes = dialogView.findViewById<View>(R.id.btnCancel)
        val btnClose = dialogView.findViewById<View>(R.id.btnClose)

        val alertDialog = android.app.AlertDialog.Builder(requireContext())
            .setView(dialogView)
            .create()

        btnCancelNotes.setOnClickListener {
            alertDialog.dismiss()
        }

        btnClose.setOnClickListener {
            alertDialog.dismiss()
        }

        etEditNotes.setText(parking.description)

        alertDialog.show()

        btnSaveNotes.setOnClickListener {
            val newNotes = etEditNotes.text.toString()
            parking.description = newNotes
            Executors.newSingleThreadExecutor().execute {
                parkingDao.update(parking)
                requireActivity().runOnUiThread {
                    binding.etNotes.setText(newNotes)
                    Toast.makeText(requireContext(), "Notas atualizadas com sucesso", Toast.LENGTH_SHORT).show()
                    alertDialog.dismiss()
                }
            }
        }
    }

    private fun deleteParking() {
        android.app.AlertDialog.Builder(requireContext(), R.style.CustomAlertDialogTheme)
            .setTitle(getString(R.string.delete_parking_title))
            .setMessage(getString(R.string.delete_parking_message))
            .setPositiveButton(getString(R.string.delete_yes)) { _, _ ->
                Executors.newSingleThreadExecutor().execute {
                    parkingDao.delete(parking)
                    requireActivity().runOnUiThread {
                        Toast.makeText(requireContext(), getString(R.string.delete_success), Toast.LENGTH_SHORT).show()
                        val navController = findNavController(requireActivity(), R.id.nav_host_fragment_content_main)
                        navController.navigate(R.id.nav_parking_history)
                    }
                }
            }
            .setNegativeButton(getString(R.string.delete_cancel), null)
            .show()
    }

    @SuppressLint("SetTextI18n")
    private fun startTimer(parking: Parking) {
        val currentTime = System.currentTimeMillis()
        val timeLeft = (parking.startTime + parking.allowedTime) - currentTime

        if (timeLeft > 0) {
            var warningShown = false

            countDownTimer = object : CountDownTimer(timeLeft, 1000) {
                override fun onTick(millisUntilFinished: Long) {
                    if (millisUntilFinished <= 3 * 60 * 1000 && !warningShown) {
                        warningShown = true
                        binding.tvStatus.text = getString(R.string.ending_text)
                    }
                }

                override fun onFinish() {
                    binding.tvStatus.text = getString(R.string.concluded_text)
                    binding.tvEndTime.text = DateFormatter.formatDateShort(System.currentTimeMillis())
                }
            }.start()
        } else {
            binding.tvStatus.text = getString(R.string.concluded_text)
        }
    }


    private fun viewPhoto() {
        parking.photoUri?.let { uriString ->
            val uri = uriString.toUri()
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "image/*")
                flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
            }
            startActivity(intent)
        } ?: Toast.makeText(requireContext(), "Nenhuma foto disponível", Toast.LENGTH_SHORT).show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        if (::countDownTimer.isInitialized) countDownTimer.cancel()
        _binding = null
    }

    companion object {
        fun newInstance(): ParkingDetailViewFragmentHistory = ParkingDetailViewFragmentHistory()
    }
}
