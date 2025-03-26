package pt.estga.spotme.ui.parking;

import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.room.Room;

import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import pt.estga.spotme.R;
import pt.estga.spotme.adapters.ParkingListAdapter;
import pt.estga.spotme.database.AppDatabase;
import pt.estga.spotme.database.ParkingDao;
import pt.estga.spotme.entities.Parking;
import pt.estga.spotme.utils.UserSession;

import java.util.List;
import java.util.concurrent.Executors;

public class ParkingListViewFragment extends Fragment {

    private ParkingListViewViewModel mViewModel;
    private RecyclerView recyclerView;
    private ParkingListAdapter adapter;
    private static final int LIMIT = 8;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_parking_list_view, container, false);
        recyclerView = root.findViewById(R.id.recyclerViewParkings);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        mViewModel = new ViewModelProvider(this).get(ParkingListViewViewModel.class);

        long userId = UserSession.getInstance(requireContext()).getUserId();

        if (mViewModel.getParkings().isEmpty()) {
            loadParkingList(userId, mViewModel.getCurrentOffset(), LIMIT);
        } else {
            setupRecyclerView(mViewModel.getParkings());
        }

        root.findViewById(R.id.buttonSeeMore).setOnClickListener(v -> {
            mViewModel.setCurrentOffset(mViewModel.getCurrentOffset() + LIMIT);
            loadParkingList(userId, mViewModel.getCurrentOffset(), LIMIT);
        });

        return root;
    }

    private void loadParkingList(long userId, int offset, int limit) {
        Executors.newSingleThreadExecutor().execute(() -> {

            if (userId == -1) {
                requireActivity().runOnUiThread(() ->
                        Toast.makeText(requireContext(), "User not logged in", Toast.LENGTH_SHORT).show());
                return;
            }

            AppDatabase db = Room.databaseBuilder(requireContext(), AppDatabase.class, "spotme_database").build();
            ParkingDao parkingDao = db.parkingDao();

            List<Parking> newParkings = parkingDao.getParkingsByUserIdWithLimit(userId, offset, limit);

            requireActivity().runOnUiThread(() -> {
                if (adapter == null) {
                    mViewModel.setParkings(newParkings);
                    setupRecyclerView(newParkings);
                } else {
                    mViewModel.getParkings().addAll(newParkings);
                    adapter.notifyDataSetChanged();
                }
            });
        });
    }

    private void setupRecyclerView(List<Parking> parkings) {
        adapter = new ParkingListAdapter(parkings, view -> {
            int position = recyclerView.getChildLayoutPosition(view);
            Parking parking = parkings.get(position);
            NavController navController = Navigation.findNavController(requireActivity(), R.id.nav_host_fragment_content_main);
            if (navController.getCurrentDestination() != null
                    && navController.getCurrentDestination().getId() != R.id.parkingDetailViewFragmentHistory) {
                Bundle bundle = new Bundle();
                bundle.putSerializable("parking", parking);
                navController.navigate(R.id.parkingDetailViewFragmentHistory, bundle);
            }
        });
        recyclerView.setAdapter(adapter);
    }
}