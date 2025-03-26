package pt.estga.spotme.ui.parking;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
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
import androidx.navigation.NavController;
import androidx.navigation.Navigation;

import pt.estga.spotme.MainActivity;
import pt.estga.spotme.R;
import pt.estga.spotme.database.AppDatabase;
import pt.estga.spotme.database.ParkingDao;
import pt.estga.spotme.entities.Parking;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class ParkingDetailViewFragmentHistory extends Fragment {

    private TextView tvParkingLocation, tvParkingTime, tvCoordinates, tvTimer, tvTimerWarning;
    private EditText etNotes;
    private ImageButton btnCopyCoordinates, btnEdit, btnDelete;
    private ImageView ivSearch;
    private Button btnShare, btnViewPhoto, btnRoute;

    private Parking parking;
    private CountDownTimer countDownTimer;

    private ParkingDao parkingDao;

    public static ParkingDetailViewFragmentHistory newInstance() {
        return new ParkingDetailViewFragmentHistory();
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_parking_detail_view_history, container, false);

        AppDatabase db = AppDatabase.getInstance(getContext());
        parkingDao = db.parkingDao();

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
        btnEdit = root.findViewById(R.id.btn_edit_notes);
        btnDelete = root.findViewById(R.id.btn_delete);

        // Set up listeners
        btnCopyCoordinates.setOnClickListener(v -> copyCoordinatesToClipboard());
        btnShare.setOnClickListener(v -> shareParkingDetails());
        btnViewPhoto.setOnClickListener(v -> viewPhoto());
        btnRoute.setOnClickListener(v -> openInMaps(requireContext(), parking.getLatitude(), parking.getLongitude()));
        btnEdit.setOnClickListener(v -> editNotes());
        btnDelete.setOnClickListener(v -> deleteParking());


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

    private void deleteParking() {
        if (parking != null) {
            Log.d("DELETE_PARKING", "Tentando excluir o estacionamento com ID: " + parking.getId());

            new Thread(() -> {
                parkingDao.delete(parking);
                requireActivity().runOnUiThread(() -> {
                    Toast.makeText(requireContext(), "Estacionamento eliminado com sucesso", Toast.LENGTH_SHORT).show();
                  //  Intent intent = new Intent(requireContext(), MainActivity.class);
                  //  startActivity(intent);
                    NavController navController = Navigation.findNavController(requireActivity(), R.id.nav_host_fragment_content_main);
                    navController.navigate(R.id.nav_parking_history);
                });
            }).start();
        } else {
            Log.e("DELETE_PARKING", "Nenhum estacionamento selecionado");
            Toast.makeText(requireContext(), "Nenhum estacionamento selecionado", Toast.LENGTH_SHORT).show();
        }
    }

    private void editNotes() {

        LayoutInflater inflater = LayoutInflater.from(requireContext());
        View dialogView = inflater.inflate(R.layout.dialog_edit_notes, null);

        AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(requireContext());
        dialogBuilder.setView(dialogView);

        EditText etEditNotes = dialogView.findViewById(R.id.et_edit_notes);
        Button btnSaveNotes = dialogView.findViewById(R.id.btn_save_notes);

        etEditNotes.setText(parking.getDescription());

        AlertDialog alertDialog = dialogBuilder.create();
        alertDialog.show();

        btnSaveNotes.setOnClickListener(v -> {
            String newNotes = etEditNotes.getText().toString();
            parking.setDescription(newNotes);

            new Thread(() -> {
                parkingDao.update(parking);
                requireActivity().runOnUiThread(() -> {
                    etNotes.setText(newNotes);
                    Toast.makeText(requireContext(), "Notas salvas com sucesso", Toast.LENGTH_SHORT).show();
                    alertDialog.dismiss();
                });
            }).start();
        });
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
                @Override
                public void onTick(long millisUntilFinished) {
                    long seconds = millisUntilFinished / 1000;
                    long minutes = seconds / 60;
                    seconds = seconds % 60;
                    tvTimer.setText(String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds));

                    if (millisUntilFinished <= 3 * 60 * 1000) { // 3 minutes in milliseconds
                        tvTimerWarning.setText("Atenção! Está quase a terminar!");
                        sendNotification();
                    } else {
                        tvTimerWarning.setText("");
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

    private void sendNotification() {
        NotificationManager notificationManager = (NotificationManager) requireContext().getSystemService(Context.NOTIFICATION_SERVICE);

        Intent intent = new Intent(requireContext(), MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        PendingIntent pendingIntent = PendingIntent.getActivity(requireContext(), 0, intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(requireContext(), MainActivity.CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle("Parking Timer")
                .setContentText("Atenção! O tempo do teu estacionamento está quase a terminar!")
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true);

        notificationManager.notify(1, builder.build());
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (countDownTimer != null) {
            countDownTimer.cancel();
        }
    }
}