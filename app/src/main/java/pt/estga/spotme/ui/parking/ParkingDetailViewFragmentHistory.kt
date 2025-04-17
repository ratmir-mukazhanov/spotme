package pt.estga.spotme.ui.parking

import android.annotation.SuppressLint
import android.os.Bundle
import android.os.CountDownTimer
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.navigation.Navigation.findNavController
import pt.estga.spotme.R
import pt.estga.spotme.database.AppDatabase
import pt.estga.spotme.database.ParkingDao
import pt.estga.spotme.databinding.FragmentParkingDetailViewHistoryBinding
import pt.estga.spotme.entities.Parking
import pt.estga.spotme.ui.BaseFragment
import pt.estga.spotme.utils.ClipboardUtils
import pt.estga.spotme.utils.DateFormatter
import pt.estga.spotme.utils.ParkingDetailHelper
import pt.estga.spotme.utils.ParkingNotificationHelper
import java.util.*
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
        binding.tvTitle.text = parking.title ?: "Meu Estacionamento"

        // Configurar data do estacionamento
        binding.tvParkingDate.text = DateFormatter.formatDate(parking.startTime)

        // Configurar coordenadas
        binding.tvCoordinates.text = "Latitude: ${parking.latitude}\nLongitude: ${parking.longitude}"

        // Configurar notas
        binding.etNotes.setText(parking.description)

        // Configurar horário de início
        binding.tvStartTime.text = DateFormatter.formatTime(parking.startTime)

        // Configurar horário de fim
        val currentTime = System.currentTimeMillis()
        if (parking.endTime > 0) {
            // Se o estacionamento já tem hora de fim definida
            binding.tvEndTime.text = DateFormatter.formatTime(parking.endTime)
        } else if (currentTime > (parking.startTime + parking.allowedTime)) {
            // Se o tempo permitido já passou, mas endTime não foi registrado
            binding.tvEndTime.text = DateFormatter.formatTime(parking.startTime + parking.allowedTime)
        } else {
            // Estacionamento ainda está em andamento
            binding.tvEndTime.text = "Em andamento"
        }

        // Configurar duração
        val durationMinutes = (parking.allowedTime / (1000 * 60)).toInt()
        binding.chipDuration.text = "Duração: $durationMinutes min"

        // Status do estacionamento
        if (currentTime > (parking.startTime + parking.allowedTime)) {
            binding.tvStatus.text = "Concluído"
        } else {
            binding.tvStatus.text = "Em andamento"
            startTimer(parking)
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
        android.app.AlertDialog.Builder(requireContext())
            .setTitle("Eliminar estacionamento")
            .setMessage("Tem certeza que deseja eliminar este estacionamento?")
            .setPositiveButton("Sim") { _, _ ->
                Executors.newSingleThreadExecutor().execute {
                    parkingDao.delete(parking)
                    requireActivity().runOnUiThread {
                        Toast.makeText(requireContext(), "Estacionamento eliminado com sucesso", Toast.LENGTH_SHORT).show()
                        val navController = findNavController(requireActivity(), R.id.nav_host_fragment_content_main)
                        navController.navigate(R.id.nav_parking_history)
                    }
                }
            }
            .setNegativeButton("Cancelar", null)
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
                    val seconds = millisUntilFinished / 1000
                    val minutes = seconds / 60
                    val remainingSeconds = seconds % 60

                    // Atualizar o estado conforme o tempo restante
                    if (millisUntilFinished <= 3 * 60 * 1000 && !warningShown) {
                        warningShown = true
                        binding.tvStatus.text = "Quase terminando!"
                        ParkingNotificationHelper.send(
                            requireContext(),
                            "O tempo do teu estacionamento está quase a terminar!"
                        )
                    }
                }

                override fun onFinish() {
                    binding.tvStatus.text = "Concluído"
                    binding.tvEndTime.text = DateFormatter.formatDateShort(System.currentTimeMillis())
                }
            }.start()
        } else {
            binding.tvStatus.text = "Concluído"
        }
    }

    private fun viewPhoto() {
        Toast.makeText(requireContext(), "Funcionalidade de foto ainda não implementada", Toast.LENGTH_SHORT).show()
        // TODO: Implementar visualização de foto
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
