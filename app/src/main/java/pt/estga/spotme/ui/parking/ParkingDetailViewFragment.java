package pt.estga.spotme.ui.parking;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import android.app.NotificationManager;
import android.app.PendingIntent;

import androidx.core.app.NotificationCompat;


import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import pt.estga.spotme.MainActivity;
import pt.estga.spotme.R;
import pt.estga.spotme.entities.Parking;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class ParkingDetailViewFragment extends Fragment {

    private TextView tvParkingLocation, tvParkingTime, tvCoordinates, tvTimer, tvTimerWarning;
    private EditText etNotes;
    private ImageButton btnBack, btnCopyCoordinates;
    private ImageView ivSearch;
    private Button btnShare, btnViewPhoto, btnRoute;

    private Parking parking;
    private CountDownTimer countDownTimer;

    public static ParkingDetailViewFragment newInstance() {
        return new ParkingDetailViewFragment();
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_parking_detail_view, container, false);

        // Initialize views
        btnCopyCoordinates = root.findViewById(R.id.btn_copy_coordinates);
        tvParkingLocation = root.findViewById(R.id.tv_parking_location);
        tvParkingTime = root.findViewById(R.id.tv_parking_time);
        tvCoordinates = root.findViewById(R.id.tv_coordinates);
        tvTimer = root.findViewById(R.id.tv_timer);
        tvTimerWarning = root.findViewById(R.id.tv_timer_warning);
        etNotes = root.findViewById(R.id.et_notes);
        btnShare = root.findViewById(R.id.btn_share);
        btnViewPhoto = root.findViewById(R.id.btn_view_photo);
        btnRoute = root.findViewById(R.id.btn_route);

        // Set up listeners
        btnCopyCoordinates.setOnClickListener(v -> copyCoordinatesToClipboard());
        btnShare.setOnClickListener(v -> shareParkingDetails());
        btnViewPhoto.setOnClickListener(v -> viewPhoto());
        btnRoute.setOnClickListener(v -> openInMaps(requireContext(), parking.getLatitude(), parking.getLongitude()));

        // Get parking data from arguments
        if (getArguments() != null) {
            parking = (Parking) getArguments().getSerializable("parking");
            if (parking != null) {
                preencherDetalhes(parking);
                startTimer(parking);
            }
        }

        return root;
    }
    private void preencherDetalhes(Parking parking) {
        parking.updateEndTime(); // Ensure endTime is updated
        tvParkingLocation.setText(parking.getTitle());
//        tvParkingTime.setText("Hora de Início: " + formatDate(parking.getStartTime()) + "\nHora de Fim: " + formatDate(parking.getEndTime()));
        tvCoordinates.setText(parking.getLatitude() + ", " + parking.getLongitude());
        etNotes.setText(parking.getDescription());
        // Set other details as needed
    }

    private String formatDate(long time) {
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault());
        return time == 0 ? "Ainda em andamento" : sdf.format(new Date(time));
    }

    private void copyCoordinatesToClipboard() {
        // Implement copy to clipboard functionality
        Toast.makeText(requireContext(), "Coordinates copied to clipboard", Toast.LENGTH_SHORT).show();
    }

    private void shareParkingDetails() {
        if (parking != null) {
            String shareText = "Localização: " + parking.getTitle() + "\n" +
                    "Coordenadas: " + parking.getLatitude() + ", " + parking.getLongitude();

            Intent shareIntent = new Intent(Intent.ACTION_SEND);
            shareIntent.setType("text/plain");
            shareIntent.putExtra(Intent.EXTRA_TEXT, shareText);

            startActivity(Intent.createChooser(shareIntent, "Compartilhar detalhes do estacionamento"));
        } else {
            Toast.makeText(requireContext(), "Detalhes do estacionamento não disponíveis", Toast.LENGTH_SHORT).show();
        }
    }

    private void viewPhoto() {
        // Implement view photo functionality
    }

    @SuppressLint("QueryPermissionsNeeded")
    public void openInMaps(Context context, double latitude, double longitude) {
        Uri gmmIntentUri = Uri.parse("geo:" + latitude + "," + longitude + "?q=" + latitude + "," + longitude);
        Intent mapIntent = new Intent(Intent.ACTION_VIEW, gmmIntentUri);
        mapIntent.setPackage(null); // Force the user to choose a maps app
        startActivity(mapIntent);
    }

    private void startTimer(Parking parking) {
        long currentTime = System.currentTimeMillis();
        long startTime = parking.getStartTime();
        long allowedTime = parking.getAllowedTime();
        long timeLeft = (startTime + allowedTime) - currentTime;

        if (timeLeft > 0) {
            tvParkingTime.setText("Hora de Início: " + formatDate(parking.getStartTime()) + "\nHora de Fim: Ainda em andamento");
            countDownTimer = new CountDownTimer(timeLeft, 1000) {
                private boolean notified3min = false;
                private boolean notified2min = false;
                private boolean notified1min = false;

                @Override
                public void onTick(long millisUntilFinished) {
                    long seconds = millisUntilFinished / 1000;
                    long minutes = seconds / 60;
                    seconds = seconds % 60;

                    tvTimer.setText(String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds));

                    Log.d("Timer", "Tempo restante: " + minutes + " min " + seconds + " seg");

                    if (minutes == 3 && !notified3min) {
                        Log.d("Timer", "Enviando notificação de 3 minutos...");
                        sendNotification("Faltam 3 minutos para o estacionamento expirar!");
                        notified3min = true;
                    } else if (minutes == 2 && !notified2min) {
                        Log.d("Timer", "Enviando notificação de 2 minutos...");
                        sendNotification("Faltam 2 minutos para o estacionamento expirar!");
                        notified2min = true;
                    } else if (minutes == 1 && !notified1min) {
                        Log.d("Timer", "Enviando notificação de 1 minuto...");
                        sendNotification("Faltam 1 minuto para o estacionamento expirar!");
                        notified1min = true;
                    }
                }

                @Override
                public void onFinish() {
                    tvTimer.setText("00:00");
                    tvTimerWarning.setText("Tempo esgotado!");
                    tvParkingTime.setText("Hora de Início: " + formatDate(parking.getStartTime()) + "\nHora de Fim: " + formatDate(parking.getEndTime()));
                }
            }.start();
        } else {
            tvTimer.setText("00:00");
            tvTimerWarning.setText("Tempo esgotado!");
            tvParkingTime.setText("Hora de Início: " + formatDate(parking.getStartTime()) + "\nHora de Fim: " + formatDate(parking.getEndTime()));
        }
    }

    private void sendNotification(String message) {
        NotificationManager notificationManager = (NotificationManager) requireContext().getSystemService(Context.NOTIFICATION_SERVICE);

        if (notificationManager == null) {
            Log.e("Notification", "NotificationManager é null, verifique as permissões!");
            return;
        }

        Intent intent = new Intent(requireContext(), MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        PendingIntent pendingIntent = PendingIntent.getActivity(requireContext(), 0, intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(requireContext(), MainActivity.CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle("Parking Timer")
                .setContentText(message)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true);

        notificationManager.notify(1, builder.build());

        Log.d("Notification", "Notificação enviada com sucesso!");
    }


    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (countDownTimer != null) {
            countDownTimer.cancel();
        }
    }
}