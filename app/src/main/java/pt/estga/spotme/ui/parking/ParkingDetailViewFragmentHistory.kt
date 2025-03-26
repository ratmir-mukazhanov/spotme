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
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.core.app.NotificationCompat
import androidx.fragment.app.Fragment
import androidx.navigation.Navigation.findNavController
import pt.estga.spotme.MainActivity
import pt.estga.spotme.R
import pt.estga.spotme.database.AppDatabase.Companion.getInstance
import pt.estga.spotme.database.ParkingDao
import pt.estga.spotme.entities.Parking
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

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
    private val ivSearch: ImageView = ImageView(context)
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

        val db = getInstance(requireContext())
        parkingDao = db.parkingDao()

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
        btnEdit = root.findViewById(R.id.btn_edit_notes)
        btnDelete = root.findViewById(R.id.btn_delete)

        // Set up listeners
        btnCopyCoordinates.setOnClickListener { copyCoordinatesToClipboard() }
        btnShare.setOnClickListener { shareParkingDetails() }
        btnViewPhoto.setOnClickListener { viewPhoto() }
        btnRoute.setOnClickListener {
            openInMaps(
                parking.latitude,
                parking.longitude
            )
        }
        btnEdit.setOnClickListener { editNotes() }
        btnDelete.setOnClickListener { deleteParking() }


        // Get parking data from arguments
        if (arguments != null) {
            parking = (requireArguments().getSerializable("parking") as Parking?)!!
            preencherDetalhes(parking)
            startTimer(parking)
        }

        return root
    }

    private fun deleteParking() {
        Log.d("DELETE_PARKING", "Tentando excluir o estacionamento com ID: " + parking.id)

        Thread {
            parkingDao.delete(parking)
            requireActivity().runOnUiThread {
                Toast.makeText(
                    requireContext(),
                    "Estacionamento eliminado com sucesso",
                    Toast.LENGTH_SHORT
                ).show()
                //  Intent intent = new Intent(requireContext(), MainActivity.class);
                //  startActivity(intent);
                val navController =
                    findNavController(requireActivity(), R.id.nav_host_fragment_content_main)
                navController.navigate(R.id.nav_parking_history)
            }
        }.start()
    }

    private fun editNotes() {
        val inflater = LayoutInflater.from(requireContext())
        val dialogView = inflater.inflate(R.layout.dialog_edit_notes, null)

        val dialogBuilder = AlertDialog.Builder(requireContext())
        dialogBuilder.setView(dialogView)

        val etEditNotes = dialogView.findViewById<EditText>(R.id.et_edit_notes)
        val btnSaveNotes = dialogView.findViewById<Button>(R.id.btn_save_notes)

        etEditNotes.setText(parking.description)

        val alertDialog = dialogBuilder.create()
        alertDialog.show()

        btnSaveNotes.setOnClickListener { v: View? ->
            val newNotes = etEditNotes.text.toString()
            parking.description = newNotes
            Thread {
                parkingDao.update(parking)
                requireActivity().runOnUiThread {
                    etNotes.setText(newNotes)
                    Toast.makeText(
                        requireContext(),
                        "Notas salvas com sucesso",
                        Toast.LENGTH_SHORT
                    ).show()
                    alertDialog.dismiss()
                }
            }.start()
        }
    }

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
        val shareText = """
            Localização: ${parking.title}
            Coordenadas: ${parking.latitude}, ${parking.longitude}
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

                    if (millisUntilFinished <= 3 * 60 * 1000) { // 3 minutes in milliseconds
                        tvTimerWarning.text = "Atenção! Está quase a terminar!"
                        sendNotification()
                    } else {
                        tvTimerWarning.text = ""
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

    private fun sendNotification() {
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
            .setContentText("Atenção! O tempo do teu estacionamento está quase a terminar!")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)

        notificationManager.notify(1, builder.build())
    }

    override fun onDestroyView() {
        super.onDestroyView()
        countDownTimer.cancel()
    }

    companion object {
        fun newInstance(): ParkingDetailViewFragmentHistory {
            return ParkingDetailViewFragmentHistory()
        }
    }
}