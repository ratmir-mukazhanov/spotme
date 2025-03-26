package pt.estga.spotme.ui.parking;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.TimePickerDialog;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;

import pt.estga.spotme.R;
import pt.estga.spotme.database.AppDatabase;
import pt.estga.spotme.entities.Parking;
import pt.estga.spotme.utils.UserSession;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnSuccessListener;

import java.util.Calendar;
import java.util.concurrent.Executors;

public class ParkingFormFragment extends Fragment {

    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1001;

    private ParkingFormViewModel mViewModel;
    private EditText titleEditText, latitudeEditText, longitudeEditText, durationEditText, descriptionEditText;
    private TextView textViewStartTime;
    private Button btnGetLocation, btnSelectStartTime, btnSave;

    private FusedLocationProviderClient fusedLocationClient;
    private Calendar startTimeCalendar;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_parking_form, container, false);

        titleEditText = root.findViewById(R.id.editTextTitle);
        latitudeEditText = root.findViewById(R.id.editTextLatitude);
        longitudeEditText = root.findViewById(R.id.editTextLongitude);
        descriptionEditText = root.findViewById(R.id.editTextDescription);
        textViewStartTime = root.findViewById(R.id.textViewStartTime);
        durationEditText = root.findViewById(R.id.editTextDuration);
        btnGetLocation = root.findViewById(R.id.btnGetLocation);
        btnSelectStartTime = root.findViewById(R.id.btnSelectStartTime);
        btnSave = root.findViewById(R.id.btnSave);

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity());

        btnGetLocation.setOnClickListener(v -> requestLocation());
        btnSelectStartTime.setOnClickListener(v -> showTimePicker());

        return root;
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        mViewModel = new ViewModelProvider(this).get(ParkingFormViewModel.class);

        mViewModel.getParking().observe(getViewLifecycleOwner(), parking -> {
            if (parking == null) {
                // Se for null, cria um novo objeto Parking
                parking = new Parking();
                mViewModel.setParking(parking);
            } else {
                // Se existir, preencher os campos do formulário
                titleEditText.setText(parking.getTitle());
                latitudeEditText.setText(String.valueOf(parking.getLatitude()));
                longitudeEditText.setText(String.valueOf(parking.getLongitude()));
                durationEditText.setText(String.valueOf(parking.getAllowedTime()));
                descriptionEditText.setText(parking.getDescription());

                if (parking.getStartTime() != 0) {
                    textViewStartTime.setText("Hora de início: " + parking.getStartTime());
                }
            }
        });

        btnSave.setOnClickListener(v -> saveParking());
    }
    private void saveParking() {
        Parking parking = mViewModel.getParking().getValue();
        if (parking == null) {
            parking = new Parking();
        }

        long userId = UserSession.getInstance(requireContext()).getUserId();

        if (userId == -1) {
            Toast.makeText(requireContext(), "Utilizador não está loggedIn!", Toast.LENGTH_SHORT).show();
            return;
        }

        // Atualizar os valores do estacionamento
        parking.setTitle(titleEditText.getText().toString());
        parking.setLatitude(Double.parseDouble(latitudeEditText.getText().toString()));
        parking.setLongitude(Double.parseDouble(longitudeEditText.getText().toString()));
        parking.setAllowedTime((long) Integer.parseInt(durationEditText.getText().toString()) * 60 * 1000);
        parking.setDescription(descriptionEditText.getText().toString());
        parking.setUserId(userId);

        if (startTimeCalendar != null) {
            long startTimeMillis = startTimeCalendar.getTimeInMillis();
            parking.setStartTime(startTimeMillis);
        }

        AppDatabase db = AppDatabase.getInstance(requireContext());

        Parking finalParking = parking;
        Executors.newSingleThreadExecutor().execute(() -> {
            if (finalParking.getId() == null) {
                db.parkingDao().insert(finalParking);
            } else {
                db.parkingDao().update(finalParking);
            }

            requireActivity().runOnUiThread(() -> {
                Toast.makeText(requireContext(), "Estacionamento salvo!", Toast.LENGTH_SHORT).show();
                // Navegar de volta para a lista de estacionamentos
                NavController navController = Navigation.findNavController(requireActivity(), R.id.nav_host_fragment_content_main);
                navController.navigate(R.id.nav_home);
            });
        });
    }

    private void requestLocation() {
        if (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_PERMISSION_REQUEST_CODE);
            return;
        }
        getLastLocation();
    }

    @SuppressLint("MissingPermission")
    private void getLastLocation() {
        fusedLocationClient.getLastLocation()
                .addOnSuccessListener(requireActivity(), new OnSuccessListener<Location>() {
                    @Override
                    public void onSuccess(Location location) {
                        if (location != null) {
                            double latitude = location.getLatitude();
                            double longitude = location.getLongitude();

                            latitudeEditText.setText(String.valueOf(latitude));
                            longitudeEditText.setText(String.valueOf(longitude));

                            Parking parking = mViewModel.getParking().getValue();
                            if (parking != null) {
                                parking.setLatitude(latitude);
                                parking.setLongitude(longitude);
                                mViewModel.setParking(parking);
                            }
                        } else {
                            Toast.makeText(requireContext(), "Não foi possível obter a localização", Toast.LENGTH_SHORT).show();
                        }
                    }
                });
    }

    private void showTimePicker() {
        Calendar calendar = Calendar.getInstance();
        int hour = calendar.get(Calendar.HOUR_OF_DAY);
        int minute = calendar.get(Calendar.MINUTE);

        TimePickerDialog timePickerDialog = new TimePickerDialog(requireContext(),
                (view, selectedHour, selectedMinute) -> {
                    Calendar selectedTime = Calendar.getInstance();
                    selectedTime.set(Calendar.HOUR_OF_DAY, selectedHour);
                    selectedTime.set(Calendar.MINUTE, selectedMinute);

                    startTimeCalendar = selectedTime;
                    textViewStartTime.setText("Hora de início: " + selectedHour + ":" + String.format("%02d", selectedMinute));
                }, hour, minute, true);
        timePickerDialog.show();
    }
}
