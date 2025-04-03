package pt.estga.spotme.ui.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.NavController
import androidx.navigation.Navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import com.google.android.material.button.MaterialButton
import pt.estga.spotme.R
import pt.estga.spotme.database.AppDatabase.Companion.getInstance
import pt.estga.spotme.databinding.FragmentHomeBinding
import pt.estga.spotme.utils.UserSession
import java.util.concurrent.Executors

class HomeFragment : Fragment() {
    private var binding: FragmentHomeBinding? = null
    private val appBarConfiguration: AppBarConfiguration? = null

    private var userSession: UserSession? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val homeViewModel = ViewModelProvider(this)[HomeViewModel::class.java]
        userSession = UserSession.getInstance(requireContext())

        binding = FragmentHomeBinding.inflate(inflater, container, false)
        val root: View = binding!!.root

        val navController = findNavController(
            (activity as AppCompatActivity?)!!,
            R.id.nav_host_fragment_content_main
        )

        // Setup click listeners for all interactive elements
        setupNavigationListeners(root, navController)

        return root
    }

    private fun setupNavigationListeners(root: View, navController: NavController) {
        // Set Location card and its elements
        val setLocationCard = root.findViewById<CardView>(R.id.setLocationCard)
        val setMyCarImage = root.findViewById<View>(R.id.setMyCarImage)
        val setLocationButton = root.findViewById<MaterialButton>(R.id.setLocationButton)

        // Add click listeners to all set location elements
        val setLocationClickListener = View.OnClickListener {
            navController.navigate(R.id.parkingFormFragment)
        }

        setLocationCard.setOnClickListener(setLocationClickListener)
        setMyCarImage.setOnClickListener(setLocationClickListener)
        setLocationButton.setOnClickListener(setLocationClickListener)

        // Find My Car card and its elements
        val findLocationCard = root.findViewById<CardView>(R.id.findLocationCard)
        val pickMyCarImage = root.findViewById<View>(R.id.pickMyCarImage)
        val findMyCarButton = root.findViewById<MaterialButton>(R.id.findMyCarButton)

        // Create a single click listener for all "Find My Car" elements
        val findCarClickListener = View.OnClickListener {
            navigateToLastParking(navController)
        }

        findLocationCard.setOnClickListener(findCarClickListener)
        pickMyCarImage.setOnClickListener(findCarClickListener)
        findMyCarButton.setOnClickListener(findCarClickListener)
    }

    private fun navigateToLastParking(navController: NavController) {
        val db = getInstance(requireContext())
        Executors.newSingleThreadExecutor().execute {
            val lastParking = db.parkingDao().getLastParkingByUserId(userSession!!.userId)
            requireActivity().runOnUiThread {
                if (lastParking != null) {
                    val bundle = Bundle()
                    bundle.putSerializable("parking", lastParking)
                    navController.navigate(R.id.parkingDetailViewFragment, bundle)
                } else {
                    Toast.makeText(
                        requireContext(),
                        "No parking records found",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding = null
    }
}