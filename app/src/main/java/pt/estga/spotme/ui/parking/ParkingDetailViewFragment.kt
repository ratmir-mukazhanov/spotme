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

    // Tempo total em milissegundos para cálculo da porcentagem do progresso
    private var totalTimeMillis: Long = 0

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

                // Configurar tempos de início e fim separadamente
                binding.tvStartTime.text = DateFormatter.formatTime(parking.startTime)

                if (parking.endTime > 0) {
                    binding.tvEndTime.text = DateFormatter.formatTime(parking.endTime)
                } else {
                    binding.tvEndTime.text = "Em andamento"
                }

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
        totalTimeMillis = parking.allowedTime

        if (timeLeft > 0) {
            countDownTimer = object : CountDownTimer(timeLeft, 1000) {
                private var notified3min = false
                private var notified2min = false
                private var notified1min = false
                private var warningShown = false

                override fun onTick(millisUntilFinished: Long) {
                    var seconds = millisUntilFinished / 1000
                    val minutes = seconds / 60
                    seconds %= 60

                    binding.tvTimer.text = String.format("%02d:%02d", minutes, seconds)

                    // Atualizar barra de progresso circular
                    val progressPercentage = ((totalTimeMillis - millisUntilFinished) * 100 / totalTimeMillis).toInt()
                    binding.progressTimer.progress = progressPercentage

                    // Mostrar aviso visual se faltarem 3 minutos ou menos
                    if (minutes <= 3 && !warningShown) {
                        warningShown = true
                        binding.tvTimerWarning.text = "⚠️ Atenção! Está quase a terminar!"
                    }

                    // Enviar notificações específicas por minuto
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
                    binding.progressTimer.progress = 100
                    binding.tvEndTime.text = DateFormatter.formatTime(System.currentTimeMillis())

                    // Mudar a cor do indicador para vermelho quando terminar
                    binding.progressTimer.setIndicatorColor(resources.getColor(android.R.color.holo_red_light))
                }
            }.start()
        } else {
            binding.tvTimer.text = "00:00"
            binding.tvTimerWarning.text = "Tempo esgotado!"
            binding.progressTimer.progress = 100
            binding.progressTimer.setIndicatorColor(resources.getColor(android.R.color.holo_red_light))

            if (parking.endTime > 0) {
                binding.tvEndTime.text = DateFormatter.formatTime(parking.endTime)
            } else {
                binding.tvEndTime.text = DateFormatter.formatTime(System.currentTimeMillis())
            }
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