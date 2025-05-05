package pt.estga.spotme.ui.parking

import android.annotation.SuppressLint
import android.os.Bundle
import android.os.CountDownTimer
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import pt.estga.spotme.R
import pt.estga.spotme.databinding.FragmentParkingDetailViewBinding
import pt.estga.spotme.entities.Parking
import pt.estga.spotme.ui.BaseFragment
import pt.estga.spotme.utils.*

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

                binding.tvStartTime.text = DateFormatter.formatTime(parking.startTime)

                if (parking.endTime > 0) {
                    binding.tvEndTime.text = DateFormatter.formatTime(parking.endTime)
                } else {
                    binding.tvEndTime.text = "Em andamento"
                }

                startTimer(parking)
                scheduleNotificationWorkers(parking)
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
                private var warningShown = false

                override fun onTick(millisUntilFinished: Long) {
                    var seconds = millisUntilFinished / 1000
                    val minutes = seconds / 60
                    seconds %= 60

                    binding.tvTimer.text = String.format("%02d:%02d", minutes, seconds)

                    val progressPercentage =
                        ((totalTimeMillis - millisUntilFinished) * 100 / totalTimeMillis).toInt()
                    binding.progressTimer.progress = progressPercentage

                    if (minutes <= 3 && !warningShown) {
                        warningShown = true
                        binding.tvTimerWarning.text = "⚠️ Atenção! Está quase a terminar!"
                    }
                }

                override fun onFinish() {
                    binding.tvTimer.text = "00:00"
                    binding.tvTimerWarning.text = "O tempo acabou!"
                    binding.progressTimer.progress = 100
                    binding.tvEndTime.text = DateFormatter.formatTime(System.currentTimeMillis())
                    binding.progressTimer.setIndicatorColor(resources.getColor(android.R.color.holo_red_light))
                }
            }.start()
        } else {
            binding.tvTimer.text = "00:00"
            binding.tvTimerWarning.text = "O tempo acabou!"
            binding.progressTimer.progress = 100
            binding.progressTimer.setIndicatorColor(resources.getColor(android.R.color.holo_red_light))

            if (parking.endTime > 0) {
                binding.tvEndTime.text = DateFormatter.formatTime(parking.endTime)
            } else {
                binding.tvEndTime.text = DateFormatter.formatTime(System.currentTimeMillis())
            }
        }
    }

    private fun scheduleNotificationWorkers(parking: Parking) {
        val now = System.currentTimeMillis()
        val endTime = parking.startTime + parking.allowedTime

        fun scheduleIfPossible(minBefore: Int) {
            val delayMillis = endTime - now - minBefore * 60 * 1000
            if (delayMillis > 0) {
                ParkingReminderScheduler.scheduleReminder(
                    requireContext(),
                    delayMillis,
                    "Faltam $minBefore minuto(s) para o estacionamento expirar!"
                )
            }
        }

        scheduleIfPossible(3)
        scheduleIfPossible(2)
        scheduleIfPossible(1)
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
