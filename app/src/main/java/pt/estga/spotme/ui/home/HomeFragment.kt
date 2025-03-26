package pt.estga.spotme.ui.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.NavController
import androidx.navigation.Navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
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
        setupSetMyCarImageClickListener(root, navController)
        setupFindMyCarImageClickListener(root, navController)

        return root
    }

    private fun setupSetMyCarImageClickListener(root: View, navController: NavController) {
        val setMyCarImage = root.findViewById<ImageView>(R.id.setMyCarImage)
        setMyCarImage?.setOnClickListener { navController.navigate(R.id.parkingFormFragment) }
    }

    private fun setupFindMyCarImageClickListener(root: View, navController: NavController) {
        val findMyCarImage = root.findViewById<ImageView>(R.id.pickMyCarImage)
        findMyCarImage?.setOnClickListener {
            val db = getInstance(requireContext())
            Executors.newSingleThreadExecutor().execute {
                val lastParking =
                    db.parkingDao().getLastParkingByUserId(userSession!!.userId)
                if (lastParking != null) {
                    val bundle = Bundle()
                    bundle.putSerializable("parking", lastParking)
                    requireActivity().runOnUiThread {
                        navController.navigate(
                            R.id.parkingDetailViewFragment,
                            bundle
                        )
                    }
                } else {
                    requireActivity().runOnUiThread {
                        Toast.makeText(
                            requireContext(),
                            "No parking records found",
                            Toast.LENGTH_SHORT
                        )
                            .show()
                    }
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding = null
    }
}