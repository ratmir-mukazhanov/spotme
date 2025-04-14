package pt.estga.spotme.ui.parking

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.os.CountDownTimer
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
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
            ParkingDetailHelper.bindDetails(binding, parking)
            startTimer(parking)
        } ?: Toast.makeText(requireContext(), "Erro ao carregar o estacionamento", Toast.LENGTH_SHORT).show()

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
        binding.btnEditNotes.setOnClickListener { editNotes() }
        binding.btnDelete.setOnClickListener { deleteParking() }
    }

    private fun editNotes() {
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_edit_notes, null)
        val etEditNotes = dialogView.findViewById<android.widget.EditText>(R.id.et_edit_notes)
        val btnSaveNotes = dialogView.findViewById<android.widget.Button>(R.id.btn_save_notes)

        etEditNotes.setText(parking.description)

        val alertDialog = android.app.AlertDialog.Builder(requireContext())
            .setView(dialogView)
            .create()

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
        Executors.newSingleThreadExecutor().execute {
            parkingDao.delete(parking)
            requireActivity().runOnUiThread {
                Toast.makeText(requireContext(), "Estacionamento eliminado com sucesso", Toast.LENGTH_SHORT).show()
                val navController = findNavController(requireActivity(), R.id.nav_host_fragment_content_main)
                navController.navigate(R.id.nav_parking_history)
            }
        }
    }

    @SuppressLint("SetTextI18n")
    private fun startTimer(parking: Parking) {
        val currentTime = System.currentTimeMillis()
        val timeLeft = (parking.startTime + parking.allowedTime) - currentTime

        if (timeLeft > 0) {
            binding.tvParkingTime.text = """
                Hora de Início: ${DateFormatter.formatDate(parking.startTime)}
                Hora de Fim: Ainda em andamento
            """.trimIndent()

            countDownTimer = object : CountDownTimer(timeLeft, 1000) {
                override fun onTick(millisUntilFinished: Long) {
                    val seconds = millisUntilFinished / 1000
                    val minutes = seconds / 60
                    val remainingSeconds = seconds % 60
                    binding.tvTimer.text = String.format(Locale.getDefault(), "%02d:%02d", minutes, remainingSeconds)

                    if (millisUntilFinished <= 3 * 60 * 1000) {
                        binding.tvTimerWarning.text = "Atenção! Está quase a terminar!"
                        ParkingNotificationHelper.send(
                            requireContext(),
                            "O tempo do teu estacionamento está quase a terminar!"
                        )
                    } else {
                        binding.tvTimerWarning.text = ""
                    }
                }

                override fun onFinish() {
                    binding.tvTimer.text = "00:00"
                    binding.tvTimerWarning.text = "Tempo esgotado!"
                    binding.tvParkingTime.text = """
                        Hora de Início: ${DateFormatter.formatDate(parking.startTime)}
                        Hora de Fim: ${DateFormatter.formatDate(parking.endTime)}
                    """.trimIndent()
                }
            }.start()
        } else {
            binding.tvTimer.text = "00:00"
            binding.tvTimerWarning.text = "Tempo esgotado!"
            binding.tvParkingTime.text = """
                Hora de Início: ${DateFormatter.formatDate(parking.startTime)}
                Hora de Fim: ${DateFormatter.formatDate(parking.endTime)}
            """.trimIndent()
        }
    }

    private fun viewPhoto() {
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