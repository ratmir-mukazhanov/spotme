package pt.estga.spotme.ui.parking

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.Navigation.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.room.Room
import pt.estga.spotme.R
import pt.estga.spotme.adapters.ParkingListAdapter
import pt.estga.spotme.database.AppDatabase
import pt.estga.spotme.entities.Parking
import pt.estga.spotme.utils.UserSession
import java.util.concurrent.Executors

class ParkingListViewFragment : Fragment() {

    private lateinit var viewModel: ParkingListViewViewModel
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: ParkingListAdapter

    companion object {
        private const val LIMIT = 8
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val root = inflater.inflate(R.layout.fragment_parking_list_view, container, false)

        recyclerView = root.findViewById(R.id.recyclerViewParkings)
        recyclerView.layoutManager = LinearLayoutManager(context)

        viewModel = ViewModelProvider(this)[ParkingListViewViewModel::class.java]

        val userId = UserSession.getInstance(requireContext()).userId

        if (viewModel.parkings.isEmpty()) {
            loadParkingList(userId, viewModel.currentOffset, LIMIT)
        } else {
            setupRecyclerView(viewModel.parkings)
        }

        root.findViewById<View>(R.id.buttonSeeMore).setOnClickListener {
            viewModel.currentOffset += LIMIT
            loadParkingList(userId, viewModel.currentOffset, LIMIT)
        }

        return root
    }

    @SuppressLint("NotifyDataSetChanged")
    private fun loadParkingList(userId: Long, offset: Int, limit: Int) {
        Executors.newSingleThreadExecutor().execute {
            if (userId == -1L) {
                requireActivity().runOnUiThread {
                    Toast.makeText(
                        requireContext(),
                        "Utilizador não autenticado",
                        Toast.LENGTH_SHORT
                    ).show()
                }
                return@execute
            }

            val db = Room.databaseBuilder(
                requireContext(),
                AppDatabase::class.java, "spotme_database"
            ).build()

            val parkingDao = db.parkingDao()
            val newParkings = parkingDao.getParkingsByUserIdWithLimit(userId, offset, limit)

            requireActivity().runOnUiThread {
                viewModel.parkings.addAll(newParkings)
                if (!::adapter.isInitialized) {
                    setupRecyclerView(viewModel.parkings)
                } else {
                    adapter.notifyDataSetChanged()
                }
            }
        }
    }

    private fun setupRecyclerView(parkings: List<Parking>) {
        adapter = ParkingListAdapter(parkings) { view ->
            val position = recyclerView.getChildAdapterPosition(view)
            val selectedParking = parkings[position]

            val navController = findNavController(requireActivity(), R.id.nav_host_fragment_content_main)
            if (navController.currentDestination?.id != R.id.parkingDetailViewFragmentHistory) {
                val bundle = Bundle().apply {
                    putSerializable("parking", selectedParking)
                }
                navController.navigate(R.id.parkingDetailViewFragmentHistory, bundle)
            }
        }
        recyclerView.adapter = adapter
    }
}
