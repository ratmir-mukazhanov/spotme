package pt.estga.spotme.ui.parking

import android.annotation.SuppressLint
import android.app.AlertDialog
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
import android.widget.*
import androidx.core.app.NotificationCompat
import androidx.fragment.app.Fragment
import androidx.navigation.Navigation.findNavController
import pt.estga.spotme.MainActivity
import pt.estga.spotme.R
import pt.estga.spotme.database.AppDatabase
import pt.estga.spotme.database.ParkingDao
import pt.estga.spotme.entities.Parking
import java.text.SimpleDateFormat
import java.util.*

class ParkingDetailViewFragmentHistory : Fragment() {

    private lateinit var tvParkingLocation: TextView
    private lateinit var tvParkingTime: TextView
    private lateinit var tvCoordinates: TextView
    private lateinit var tvTimer: TextView
    private lateinit var tvTimerWarning: TextView
    private lateinit var etNotes: EditText
    private lateinit var btnCopyCoordinates: ImageButton
    private lateinit var btnEdit: ImageButton
    private lateinit var btnDelete: ImageButton
    private lateinit var btnShare: Button
    private lateinit var btnViewPhoto: Button
    private lateinit var btnRoute: Button

    private lateinit var parking: Parking
    private lateinit var countDownTimer: CountDownTimer
    private lateinit var parkingDao: ParkingDao

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val root = inflater.inflate(R.layout.fragment_parking_detail_view_history, container, false)

        val db = AppDatabase.getInstance(requireContext())
        parkingDao = db.parkingDao()

        // Inicializar componentes visuais
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
        btnEdit = root.findViewById(R.id.btn_edit_notes)
        btnDelete = root.findViewById(R.id.btn_delete)

        // Listeners
        btnCopyCoordinates.setOnClickListener { copyCoordinatesToClipboard() }
        btnShare.setOnClickListener { shareParkingDetails() }
        btnViewPhoto.setOnClickListener { viewPhoto() }
        btnRoute.setOnClickListener { openInMaps(parking.latitude, parking.longitude) }
        btnEdit.setOnClickListener { editNotes() }
        btnDelete.setOnClickListener { deleteParking() }

        // Obter o objeto Parking passado como argumento
        val arg = arguments?.getSerializable("parking")
        if (arg is Parking) {
            parking = arg
            preencherDetalhes(parking)
            startTimer(parking)
        } else {
            Toast.makeText(requireContext(), "Erro ao carregar o estacionamento", Toast.LENGTH_SHORT).show()
        }

        return root
    }

    private fun deleteParking() {
        Log.d("DELETE_PARKING", "Tentando eliminar estacionamento com ID: ${parking.id}")

        Thread {
            parkingDao.delete(parking)
            requireActivity().runOnUiThread {
                Toast.makeText(requireContext(), "Estacionamento eliminado com sucesso", Toast.LENGTH_SHORT).show()
                val navController = findNavController(requireActivity(), R.id.nav_host_fragment_content_main)
                navController.navigate(R.id.nav_parking_history)
            }
        }.start()
    }

    private fun editNotes() {
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_edit_notes, null)
        val etEditNotes = dialogView.findViewById<EditText>(R.id.et_edit_notes)
        val btnSaveNotes = dialogView.findViewById<Button>(R.id.btn_save_notes)

        etEditNotes.setText(parking.description)

        val alertDialog = AlertDialog.Builder(requireContext())
            .setView(dialogView)
            .create()

        alertDialog.show()

        btnSaveNotes.setOnClickListener {
            val newNotes = etEditNotes.text.toString()
            parking.description = newNotes
            Thread {
                parkingDao.update(parking)
                requireActivity().runOnUiThread {
                    etNotes.setText(newNotes)
                    Toast.makeText(requireContext(), "Notas atualizadas com sucesso", Toast.LENGTH_SHORT).show()
                    alertDialog.dismiss()
                }
            }.start()
        }
    }

    private fun preencherDetalhes(parking: Parking) {
        parking.updateEndTime()
        tvParkingLocation.text = parking.title
        tvCoordinates.text = "${parking.latitude}, ${parking.longitude}"
        etNotes.setText(parking.description)
    }

    private fun formatDate(time: Long): String {
        val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
        return if (time == 0L) "Ainda em andamento" else sdf.format(Date(time))
    }

    private fun copyCoordinatesToClipboard() {
        val clipboard = requireContext().getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
        val clip = android.content.ClipData.newPlainText("Coordenadas", "${parking.latitude}, ${parking.longitude}")
        clipboard.setPrimaryClip(clip)
        Toast.makeText(requireContext(), "Coordenadas copiadas para a área de transferência", Toast.LENGTH_SHORT).show()
    }

    private fun shareParkingDetails() {
        val shareText = """
            Localização: ${parking.title}
            Coordenadas: ${parking.latitude}, ${parking.longitude}
        """.trimIndent()

        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, shareText)
        }

        startActivity(Intent.createChooser(shareIntent, "Partilhar detalhes do estacionamento"))
    }

    private fun viewPhoto() {
        // Implementar quando houver suporte a fotos
    }

    @SuppressLint("QueryPermissionsNeeded")
    private fun openInMaps(latitude: Double, longitude: Double) {
        val gmmIntentUri = Uri.parse("geo:$latitude,$longitude?q=$latitude,$longitude")
        val mapIntent = Intent(Intent.ACTION_VIEW, gmmIntentUri)
        mapIntent.setPackage(null)
        startActivity(mapIntent)
    }

    @SuppressLint("SetTextI18n")
    private fun startTimer(parking: Parking) {
        val currentTime = System.currentTimeMillis()
        val timeLeft = (parking.startTime + parking.allowedTime) - currentTime

        if (timeLeft > 0) {
            tvParkingTime.text = """
                Hora de Início: ${formatDate(parking.startTime)}
                Hora de Fim: Ainda em andamento
            """.trimIndent()

            countDownTimer = object : CountDownTimer(timeLeft, 1000) {
                override fun onTick(millisUntilFinished: Long) {
                    val seconds = millisUntilFinished / 1000
                    val minutes = seconds / 60
                    val remainingSeconds = seconds % 60
                    tvTimer.text = String.format(Locale.getDefault(), "%02d:%02d", minutes, remainingSeconds)

                    if (millisUntilFinished <= 3 * 60 * 1000) {
                        tvTimerWarning.text = "Atenção! Está quase a terminar!"
                        sendNotification()
                    } else {
                        tvTimerWarning.text = ""
                    }
                }

                override fun onFinish() {
                    tvTimer.text = "00:00"
                    tvTimerWarning.text = "Tempo esgotado!"
                    tvParkingTime.text = """
                        Hora de Início: ${formatDate(parking.startTime)}
                        Hora de Fim: ${formatDate(parking.endTime)}
                    """.trimIndent()
                }
            }.start()
        } else {
            tvTimer.text = "00:00"
            tvTimerWarning.text = "Tempo esgotado!"
            tvParkingTime.text = """
                Hora de Início: ${formatDate(parking.startTime)}
                Hora de Fim: ${formatDate(parking.endTime)}
            """.trimIndent()
        }
    }

    private fun sendNotification() {
        val notificationManager = requireContext().getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val intent = Intent(requireContext(), MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }

        val pendingIntent = PendingIntent.getActivity(
            requireContext(), 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(requireContext(), MainActivity.CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("Parking Timer")
            .setContentText("Atenção! O tempo do teu estacionamento está quase a terminar!")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)

        notificationManager.notify(1, builder.build())
    }

    override fun onDestroyView() {
        super.onDestroyView()
        if (::countDownTimer.isInitialized) {
            countDownTimer.cancel()
        }
    }

    companion object {
        fun newInstance(): ParkingDetailViewFragmentHistory {
            return ParkingDetailViewFragmentHistory()
        }
    }
}
