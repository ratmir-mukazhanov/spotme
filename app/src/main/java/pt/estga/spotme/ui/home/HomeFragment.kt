package pt.estga.spotme.ui.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import pt.estga.spotme.ui.BaseFragment
import pt.estga.spotme.databinding.FragmentHomeBinding
import pt.estga.spotme.navigation.HomeNavigationManager

class HomeFragment : BaseFragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        setupNavigationListeners()
        return binding.root
    }

    private fun setupNavigationListeners() {
        val navController = findNavController()

        val setLocationClickListener = View.OnClickListener {
            navController.navigate(pt.estga.spotme.R.id.parkingFormFragment)
        }

        binding.setLocationCard.setOnClickListener(setLocationClickListener)
        binding.setMyCarImage.setOnClickListener(setLocationClickListener)
        binding.setLocationButton.setOnClickListener(setLocationClickListener)

        val findCarClickListener = View.OnClickListener {
            val userId = userViewModel.userId.value
            if (userId != null) {
                HomeNavigationManager.navigateToLastParking(
                    context = requireContext(),
                    navController = navController,
                    userId = userId,
                    lifecycleScope = viewLifecycleOwner.lifecycleScope
                ) {
                    Toast.makeText(requireContext(), "No parking records found", Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(requireContext(), "Utilizador não encontrado.", Toast.LENGTH_SHORT).show()
            }
        }

        binding.findLocationCard.setOnClickListener(findCarClickListener)
        binding.pickMyCarImage.setOnClickListener(findCarClickListener)
        binding.findMyCarButton.setOnClickListener(findCarClickListener)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}