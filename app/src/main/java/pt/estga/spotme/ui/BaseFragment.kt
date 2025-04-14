package pt.estga.spotme.ui

import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import pt.estga.spotme.viewmodels.UserViewModel

abstract class BaseFragment : Fragment() {
    protected val userViewModel: UserViewModel by lazy {
        ViewModelProvider(requireActivity())[UserViewModel::class.java]
    }
}

