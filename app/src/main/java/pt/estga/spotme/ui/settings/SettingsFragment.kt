package pt.estga.spotme.ui.settings

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import pt.estga.spotme.R
import pt.estga.spotme.database.AppDatabase
import pt.estga.spotme.databinding.DialogRateAppBinding
import pt.estga.spotme.databinding.FragmentSettingsBinding
import pt.estga.spotme.entities.AppRating
import pt.estga.spotme.utils.UserSession

class SettingsFragment : Fragment() {
    private var userSession: UserSession? = null
    private var _binding: FragmentSettingsBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSettingsBinding.inflate(inflater, container, false)
        userSession = UserSession.getInstance(requireContext())

        setupRateAppButton()

        return binding.root
    }

    private fun setupRateAppButton() {
        binding.layoutRateApp.setOnClickListener {
            checkPreviousRating()
        }
    }

    private fun checkPreviousRating() {
        val userId = userSession?.userId ?: return

        viewLifecycleOwner.lifecycleScope.launch {
            val database = AppDatabase.getInstance(requireContext())
            val previousRating = withContext(Dispatchers.IO) {
                database.appRatingDao().getLatestRatingByUserSync(userId)
            }

            if (previousRating != null) {
                showReplacePreviousRatingDialog(previousRating)
            } else {
                showRateAppDialog()
            }
        }
    }

    private fun showReplacePreviousRatingDialog(previousRating: AppRating) {
        AlertDialog.Builder(requireContext())
            .setTitle("Avaliação Existente")
            .setMessage("Já avaliou o aplicativo com ${previousRating.rating} estrelas. Deseja atualizar sua avaliação?")
            .setPositiveButton("Atualizar") { _, _ ->
                showRateAppDialog(previousRating)
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun showRateAppDialog(previousRating: AppRating? = null) {
        val dialog = Dialog(requireContext(), R.style.Theme_Dialog_Rounded)
        val dialogBinding = DialogRateAppBinding.inflate(layoutInflater)
        dialog.setContentView(dialogBinding.root)


        previousRating?.let {
            dialogBinding.ratingBar.rating = it.rating
            dialogBinding.etFeedback.setText(it.feedback ?: "")

            val message = when {
                it.rating <= 1 -> "Lamentamos que não esteja satisfeito"
                it.rating <= 3 -> "Obrigado pelo feedback"
                else -> "Obrigado pela sua excelente avaliação!"
            }
            dialogBinding.tvRatingMessage.text = message
        }

        dialogBinding.ratingBar.setOnRatingBarChangeListener { _, rating, fromUser ->
            if (fromUser) {
                val message = when {
                    rating <= 1 -> "Lamentamos que não esteja satisfeito"
                    rating <= 3 -> "Obrigado pelo feedback"
                    else -> "Obrigado pela sua excelente avaliação!"
                }
                dialogBinding.tvRatingMessage.text = message
            }
        }

        dialogBinding.btnSubmitRating.setOnClickListener {
            val rating = dialogBinding.ratingBar.rating
            val feedback = dialogBinding.etFeedback.text.toString()

            saveRating(rating, feedback, previousRating?.id)

            dialog.dismiss()
            Toast.makeText(
                requireContext(),
                if (previousRating != null) "Sua avaliação foi atualizada!" else "Obrigado pela sua avaliação!",
                Toast.LENGTH_SHORT
            ).show()
        }

        dialogBinding.btnCancel.setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun saveRating(rating: Float, feedback: String, existingId: Long? = null) {
        val userId = userSession?.userId ?: return

        viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
            val appRating = AppRating(
                rating = rating,
                feedback = if (feedback.isBlank()) null else feedback,
                timestamp = System.currentTimeMillis(),
                userId = userId,
                isSubmittedToServer = false
            )

            if (existingId != null) {
                appRating.id = existingId
            }

            val database = AppDatabase.getInstance(requireContext())
            database.appRatingDao().insert(appRating)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}