package pt.estga.spotme.ui.settings;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import pt.estga.spotme.R;
import pt.estga.spotme.databinding.FragmentSlideshowBinding;
import pt.estga.spotme.utils.UserSession;

public class SettingsFragment extends Fragment {

    private UserSession userSession;
    private FragmentSlideshowBinding binding;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {

        View root = inflater.inflate(R.layout.settings_main, container, false);
        userSession = UserSession.getInstance(requireContext());
        return root;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}