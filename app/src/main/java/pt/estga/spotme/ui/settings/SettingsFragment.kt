package pt.estga.spotme.ui.settings

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import pt.estga.spotme.R
import pt.estga.spotme.databinding.FragmentSlideshowBinding
import pt.estga.spotme.utils.UserSession

class SettingsFragment : Fragment() {
    private var userSession: UserSession? = null
    private var binding: FragmentSlideshowBinding? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        val root = inflater.inflate(R.layout.settings_main, container, false)
        userSession = UserSession.getInstance(requireContext())
        return root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding = null
    }
}