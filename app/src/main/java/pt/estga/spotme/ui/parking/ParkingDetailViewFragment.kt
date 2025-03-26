package pt.estga.spotme.ui.parking

import android.annotation.SuppressLint
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.CountDownTimer
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.core.app.NotificationCompat
import androidx.fragment.app.Fragment
import pt.estga.spotme.MainActivity
import pt.estga.spotme.R
import pt.estga.spotme.entities.Parking
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ParkingDetailViewFragment : Fragment() {
    private lateinit var tvParkingLocation: TextView
    private lateinit var tvParkingTime: TextView
    private lateinit var tvCoordinates: TextView
    private lateinit var tvTimer: TextView
    private lateinit var tvTimerWarning: TextView
    private lateinit var etNotes: EditText
    private val btnBack: ImageButton? = null
    private lateinit var btnCopyCoordinates: ImageButton
    private val ivSearch: ImageView? = null
    private lateinit var btnShare: Button
    private lateinit var btnViewPhoto: Button
    private lateinit var btnRoute: Button

    private var parking: Parking? = null
    private var countDownTimer: CountDownTimer? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val root = inflater.inflate(R.layout.fragment_parking_detail_view, container, false)

        // Initialize views
        btnCopyCoordinates = root.findViewById(R.id.btn_copy_coordinates)
        tvParkingLocation = root.findViewById(R.id.tv_parking_location)
        tvParkingTime = root.findViewById(R.id.tv_parking_time)
        tvCoordinates = root.findViewById(R.id.tv_coordinates)
        tvTimer = root.findViewById(R.id.tv_timer)
        tvTimerWarning = root.findViewById(R.id.tv_timer_warning)
        etNotes = root.findViewById(R.id.et_notes)
        btnShare = root.findViewById(R.id.btn_share)
        btnViewPhoto = root.findViewById(R.id.btn_view_photo)
        btnRoute = root.findViewById(R.id.btn_route)

        // Set up listeners
        btnCopyCoordinates.setOnClickListener { copyCoordinatesToClipboard() }
        btnShare.setOnClickListener { shareParkingDetails() }
        btnViewPhoto.setOnClickListener { viewPhoto() }
        btnRoute.setOnClickListener {
            openInMaps(
                parking!!.latitude,
                parking!!.longitude
            )
        }

        // Get parking data from arguments
        if (arguments != null) {
            parking = requireArguments().getSerializable("parking") as Parking?
            if (parking != null) {
                preencherDetalhes(parking!!)
                startTimer(parking!!)
            }
        }

        return root
    }

    @SuppressLint("SetTextI18n")
    private fun preencherDetalhes(parking: Parking) {
        parking.updateEndTime() // Ensure endTime is updated
        tvParkingLocation.text = parking.title
        //        tvParkingTime.setText("Hora de Início: " + formatDate(parking.getStartTime()) + "\nHora de Fim: " + formatDate(parking.getEndTime()));
        tvCoordinates.text = parking.latitude.toString() + ", " + parking.longitude
        etNotes.setText(parking.description)
        // Set other details as needed
    }

    private fun formatDate(time: Long): String {
        val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
        return if (time == 0L) "Ainda em andamento" else sdf.format(Date(time))
    }

    private fun copyCoordinatesToClipboard() {
        // Implement copy to clipboard functionality
        Toast.makeText(requireContext(), "Coordinates copied to clipboard", Toast.LENGTH_SHORT)
            .show()
    }

    private fun shareParkingDetails() {
        if (parking != null) {
            val shareText = """
                Localização: ${parking!!.title}
                Coordenadas: ${parking!!.latitude}, ${parking!!.longitude}
                """.trimIndent()

            val shareIntent = Intent(Intent.ACTION_SEND)
            shareIntent.setType("text/plain")
            shareIntent.putExtra(Intent.EXTRA_TEXT, shareText)

            startActivity(
                Intent.createChooser(
                    shareIntent,
                    "Compartilhar detalhes do estacionamento"
                )
            )
        } else {
            Toast.makeText(
                requireContext(),
                "Detalhes do estacionamento não disponíveis",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    private fun viewPhoto() {
        // Implement view photo functionality
    }

    @SuppressLint("QueryPermissionsNeeded")
    fun openInMaps(latitude: Double, longitude: Double) {
        val gmmIntentUri = Uri.parse("geo:$latitude,$longitude?q=$latitude,$longitude")
        val mapIntent = Intent(Intent.ACTION_VIEW, gmmIntentUri)
        mapIntent.setPackage(null) // Force the user to choose a maps app
        startActivity(mapIntent)
    }

    @SuppressLint("SetTextI18n")
    private fun startTimer(parking: Parking) {
        val currentTime = System.currentTimeMillis()
        val startTime = parking.startTime
        val allowedTime = parking.allowedTime
        val timeLeft = (startTime + allowedTime) - currentTime

        if (timeLeft > 0) {
            tvParkingTime.text =
                """
                Hora de Início: ${formatDate(parking.startTime)}
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

                    tvTimer.text =
                        String.format(
                            Locale.getDefault(),
                            "%02d:%02d",
                            minutes,
                            seconds
                        )

                    Log.d("Timer", "Tempo restante: $minutes min $seconds seg")

                    if (minutes == 3L && !notified3min) {
                        Log.d("Timer", "Enviando notificação de 3 minutos...")
                        sendNotification("Faltam 3 minutos para o estacionamento expirar!")
                        notified3min = true
                    } else if (minutes == 2L && !notified2min) {
                        Log.d("Timer", "Enviando notificação de 2 minutos...")
                        sendNotification("Faltam 2 minutos para o estacionamento expirar!")
                        notified2min = true
                    } else if (minutes == 1L && !notified1min) {
                        Log.d("Timer", "Enviando notificação de 1 minuto...")
                        sendNotification("Faltam 1 minuto para o estacionamento expirar!")
                        notified1min = true
                    }
                }

                override fun onFinish() {
                    tvTimer.text = "00:00"
                    tvTimerWarning.text = "Tempo esgotado!"
                    tvParkingTime.text =
                        """
                            Hora de Início: ${formatDate(parking.startTime)}
                            Hora de Fim: 
                            """.trimIndent() + formatDate(
                            parking.endTime
                        )
                }
            }.start()
        } else {
            tvTimer.text = "00:00"
            tvTimerWarning.text = "Tempo esgotado!"
            tvParkingTime.text =
                """
                    Hora de Início: ${formatDate(parking.startTime)}
                    Hora de Fim: 
                    """.trimIndent() + formatDate(
                    parking.endTime
                )
        }
    }

    private fun sendNotification(message: String) {
        val notificationManager =
            requireContext().getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val intent = Intent(requireContext(), MainActivity::class.java)
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        val pendingIntent = PendingIntent.getActivity(
            requireContext(),
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(requireContext(), MainActivity.CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("Parking Timer")
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)

        notificationManager.notify(1, builder.build())

        Log.d("Notification", "Notificação enviada com sucesso!")
    }


    override fun onDestroyView() {
        super.onDestroyView()
        if (countDownTimer != null) {
            countDownTimer!!.cancel()
        }
    }

    companion object {
        fun newInstance(): ParkingDetailViewFragment {
            return ParkingDetailViewFragment()
        }
    }
}