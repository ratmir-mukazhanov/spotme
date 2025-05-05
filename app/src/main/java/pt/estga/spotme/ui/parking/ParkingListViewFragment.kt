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
import com.google.android.material.tabs.TabLayout
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
        private const val LIMIT = 5
        private const val STATE_SELECTED_TAB = "selected_tab"
    }

    // Metodo para salvar o estado do fragment
    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        // Salvar a posição da tab selecionada
        outState.putInt(STATE_SELECTED_TAB, binding.tabLayoutTimeFilter.selectedTabPosition)
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

        loadParkingStatistics(userId)

        if (viewModel.parkings.isEmpty()) {
            loadParkingList(userId, viewModel.currentOffset, LIMIT)
        } else {
            setupRecyclerView(viewModel.parkings)
        }

        binding.buttonSeeMore.setOnClickListener {
            viewModel.currentOffset += LIMIT
            loadParkingList(userId, viewModel.currentOffset, LIMIT)
        }

        binding.tabLayoutTimeFilter.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                when (tab?.position) {
                    0 -> applyFilter(TimeFilter.LAST_WEEK)
                    1 -> applyFilter(TimeFilter.LAST_MONTH)
                }
                // Salvar a posição da tab no ViewModel para persistência
                viewModel.selectedTabPosition = tab?.position ?: 0
            }

            override fun onTabUnselected(tab: TabLayout.Tab?) {}
            override fun onTabReselected(tab: TabLayout.Tab?) {}
        })

        // Restaurar a tab selecionada do savedInstanceState ou do ViewModel
        val selectedTab = savedInstanceState?.getInt(STATE_SELECTED_TAB) ?: viewModel.selectedTabPosition
        binding.tabLayoutTimeFilter.getTabAt(selectedTab)?.select()
    }

    override fun onResume() {
        super.onResume()

        val userId = UserSession.getInstance(requireContext()).userId

        // Recarrega os dados quando retornar ao fragment
        Executors.newSingleThreadExecutor().execute {
            val db = AppDatabase.getInstance(requireContext())

            // Recarrega a lista completa de estacionamentos
            val freshParkings = db.parkingDao().getParkingsByUserIdWithLimit(userId, 0, viewModel.currentOffset + LIMIT)
            val totalCount = db.parkingDao().getParkingCountByUserId(userId)

            // Verifica se há mais estacionamentos para carregar
            val hasMoreItems = freshParkings.size < totalCount

            requireActivity().runOnUiThread {
                if (!isAdded || _binding == null) return@runOnUiThread

                // Atualiza os dados no viewModel
                viewModel.parkings.clear()
                viewModel.parkings.addAll(freshParkings)

                // Reaplica o filtro atual baseado no valor armazenado no ViewModel
                val selectedTabPosition = viewModel.selectedTabPosition
                val selectedFilter = when (selectedTabPosition) {
                    0 -> TimeFilter.LAST_WEEK
                    1 -> TimeFilter.LAST_MONTH
                    else -> TimeFilter.LAST_WEEK // Filtro padrão
                }

                // Aplica o filtro (que também atualizará as estatísticas)
                applyFilter(selectedFilter)

                // Garante que a tab correta esteja selecionada visualmente
                binding.tabLayoutTimeFilter.getTabAt(selectedTabPosition)?.select()

                // Atualiza visibilidade do botão "Ver Mais"
                binding.buttonSeeMore.visibility = if (hasMoreItems) View.VISIBLE else View.GONE
            }
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

            // Obtém o total de estacionamentos para este usuário
            val totalCount = db.parkingDao().getParkingCountByUserId(userId)

            // Verifica se já carregamos todos os estacionamentos
            val hasMoreItems = (offset + newParkings.size) < totalCount

            requireActivity().runOnUiThread {
                if (!isAdded || _binding == null) return@runOnUiThread

                viewModel.parkings.addAll(newParkings)

                if (!::adapter.isInitialized) {
                    setupRecyclerView(viewModel.parkings)
                } else {
                    adapter.notifyDataSetChanged()
                }

                // Atualiza a visibilidade do botão Ver Mais
                binding.buttonSeeMore.visibility = if (hasMoreItems) View.VISIBLE else View.GONE

                // Mostra mensagem quando lista estiver vazia
                binding.textViewEmptyList.visibility =
                    if (viewModel.parkings.isEmpty()) View.VISIBLE else View.GONE

                // Aplica o filtro atual
                val selectedTab = binding.tabLayoutTimeFilter.selectedTabPosition
                when (selectedTab) {
                    0 -> applyFilter(TimeFilter.LAST_WEEK)
                    1 -> applyFilter(TimeFilter.LAST_MONTH)
                }
            }
        }
    }

    private fun loadParkingStatistics(userId: Long, timeFilter: TimeFilter = TimeFilter.LAST_WEEK) {
        Executors.newSingleThreadExecutor().execute {
            if (userId == -1L) return@execute

            val db = AppDatabase.getInstance(requireContext())

            // Calcular o limite de tempo com base no filtro
            val now = System.currentTimeMillis()
            val millisInDay = 24 * 60 * 60 * 1000L
            val threshold = when (timeFilter) {
                TimeFilter.LAST_WEEK -> now - (7 * millisInDay)
                TimeFilter.LAST_MONTH -> now - (30 * millisInDay)
            }

            // Obter estacionamentos filtrados por data
            val filteredParkings = db.parkingDao().getParkingsByUserIdAfterTimestamp(userId, threshold)
            val totalParkings = filteredParkings.size

            // Calcular o tempo médio de estacionamento para os itens filtrados
            val averageParkingTime = if (filteredParkings.isEmpty()) {
                0L
            } else {
                filteredParkings.sumOf { parking ->
                    parking.allowedTime
                } / filteredParkings.size
            }

            requireActivity().runOnUiThread {
                if (!isAdded || _binding == null) return@runOnUiThread

                viewModel.totalParkings = totalParkings
                viewModel.averageParkingTime = averageParkingTime

                binding.tvTotalParkings.text = totalParkings.toString()

                val avgMinutes = (averageParkingTime / 60000).toInt()
                val hours = avgMinutes / 60
                val minutes = avgMinutes % 60

                binding.tvAvgTime.text = if(hours > 0) {
                    "${hours}h ${minutes}m"
                } else {
                    "${minutes}m"
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

    private enum class TimeFilter {
        LAST_WEEK, LAST_MONTH
    }

    @SuppressLint("NotifyDataSetChanged")
    private fun applyFilter(filter: TimeFilter) {
        val now = System.currentTimeMillis()
        val millisInDay = 24 * 60 * 60 * 1000L
        val threshold = when (filter) {
            TimeFilter.LAST_WEEK -> now - (7 * millisInDay)
            TimeFilter.LAST_MONTH -> now - (30 * millisInDay)
        }

        val filtered = viewModel.parkings.filter { parking ->
            parking.startTime >= threshold
        }

        if (!::adapter.isInitialized) {
            setupRecyclerView(filtered)
        } else {
            adapter.updateData(filtered)
        }

        // Atualiza visibilidade do texto de lista vazia
        binding.textViewEmptyList.visibility = if (filtered.isEmpty()) View.VISIBLE else View.GONE

        // Mantém a verificação para o botão Ver Mais após filtrar
        val userId = UserSession.getInstance(requireContext()).userId

        // Atualiza as estatísticas com base no filtro aplicado
        loadParkingStatistics(userId, filter)

        Executors.newSingleThreadExecutor().execute {
            val db = AppDatabase.getInstance(requireContext())
            val totalCount = db.parkingDao().getParkingCountByUserId(userId)
            val hasMoreToLoad = viewModel.parkings.size < totalCount

            requireActivity().runOnUiThread {
                if (!isAdded || _binding == null) return@runOnUiThread
                binding.buttonSeeMore.visibility = if (hasMoreToLoad) View.VISIBLE else View.GONE
            }
        }
    }
}
