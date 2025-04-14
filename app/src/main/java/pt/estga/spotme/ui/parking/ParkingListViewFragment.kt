package pt.estga.spotme.ui.parking

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import pt.estga.spotme.R
import pt.estga.spotme.adapters.ParkingListAdapter
import pt.estga.spotme.database.AppDatabase
import pt.estga.spotme.databinding.FragmentParkingListViewBinding
import pt.estga.spotme.entities.Parking
import pt.estga.spotme.ui.BaseFragment
import pt.estga.spotme.utils.UserSession
import java.util.concurrent.Executors

class ParkingListViewFragment : BaseFragment() {

    private var _binding: FragmentParkingListViewBinding? = null
    private val binding get() = _binding!!

    private val viewModel: ParkingListViewViewModel by viewModels()
    private lateinit var adapter: ParkingListAdapter

    companion object {
        private const val LIMIT = 8
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentParkingListViewBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.recyclerViewParkings.layoutManager = LinearLayoutManager(requireContext())

        val userId = UserSession.getInstance(requireContext()).userId

        if (viewModel.parkings.isEmpty()) {
            loadParkingList(userId, viewModel.currentOffset, LIMIT)
        } else {
            setupRecyclerView(viewModel.parkings)
        }

        binding.buttonSeeMore.setOnClickListener {
            viewModel.currentOffset += LIMIT
            loadParkingList(userId, viewModel.currentOffset, LIMIT)
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    private fun loadParkingList(userId: Long, offset: Int, limit: Int) {
        Executors.newSingleThreadExecutor().execute {
            if (userId == -1L) {
                requireActivity().runOnUiThread {
                    if (!isAdded || _binding == null) return@runOnUiThread
                    Toast.makeText(
                        requireContext(),
                        "Utilizador não autenticado",
                        Toast.LENGTH_SHORT
                    ).show()
                }
                return@execute
            }

            val db = AppDatabase.getInstance(requireContext())
            val newParkings = db.parkingDao().getParkingsByUserIdWithLimit(userId, offset, limit)

            requireActivity().runOnUiThread {
                if (!isAdded || _binding == null) return@runOnUiThread

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
            val position = binding.recyclerViewParkings.getChildAdapterPosition(view)
            if (position != RecyclerView.NO_POSITION) {
                val selectedParking = parkings[position]

                val bundle = Bundle().apply {
                    putSerializable("parking", selectedParking)
                }
                findNavController().navigate(R.id.parkingDetailViewFragmentHistory, bundle)
            }
        }
        binding.recyclerViewParkings.adapter = adapter
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}