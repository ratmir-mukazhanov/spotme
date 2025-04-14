package pt.estga.spotme.ui.parking

import android.annotation.SuppressLint
import android.os.Bundle
import android.os.CountDownTimer
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import pt.estga.spotme.databinding.FragmentParkingDetailViewBinding
import pt.estga.spotme.entities.Parking
import pt.estga.spotme.ui.BaseFragment
import pt.estga.spotme.utils.ClipboardUtils
import pt.estga.spotme.utils.DateFormatter
import pt.estga.spotme.utils.ParkingDetailHelper
import pt.estga.spotme.utils.ParkingNotificationHelper

class ParkingDetailViewFragment : BaseFragment() {

    private var _binding: FragmentParkingDetailViewBinding? = null
    private val binding get() = _binding!!

    private var countDownTimer: CountDownTimer? = null
    private val viewModel: ParkingDetailViewViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentParkingDetailViewBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        arguments?.getSerializable("parking")?.let {
            viewModel.setParking(it as Parking)
        }

        viewModel.parking.observe(viewLifecycleOwner) { parking ->
            if (parking != null) {
                ParkingDetailHelper.bindDetails(binding, parking)
                startTimer(parking)
            }
        }

        binding.btnCopyCoordinates.setOnClickListener {
            viewModel.parking.value?.let {
                ClipboardUtils.copyText(
                    requireContext(),
                    "Coordenadas",
                    "${it.latitude}, ${it.longitude}"
                )
            }
        }

        binding.btnShare.setOnClickListener {
            viewModel.parking.value?.let {
                ParkingDetailHelper.share(requireContext(), it)
            }
        }

        binding.btnViewPhoto.setOnClickListener { viewPhoto() }
        binding.btnRoute.setOnClickListener {
            viewModel.parking.value?.let {
                ParkingDetailHelper.openInMaps(requireContext(), it.latitude, it.longitude)
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
                private var notified3min = false
                private var notified2min = false
                private var notified1min = false

                override fun onTick(millisUntilFinished: Long) {
                    var seconds = millisUntilFinished / 1000
                    val minutes = seconds / 60
                    seconds %= 60

                    binding.tvTimer.text = String.format("%02d:%02d", minutes, seconds)

                    when {
                        minutes == 3L && !notified3min -> {
                            ParkingNotificationHelper.send(requireContext(), "Faltam 3 minutos para o estacionamento expirar!")
                            notified3min = true
                        }
                        minutes == 2L && !notified2min -> {
                            ParkingNotificationHelper.send(requireContext(), "Faltam 2 minutos para o estacionamento expirar!")
                            notified2min = true
                        }
                        minutes == 1L && !notified1min -> {
                            ParkingNotificationHelper.send(requireContext(), "Faltam 1 minuto para o estacionamento expirar!")
                            notified1min = true
                        }
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
        _binding = null
        countDownTimer?.cancel()
    }

    companion object {
        fun newInstance(): ParkingDetailViewFragment = ParkingDetailViewFragment()
    }
}