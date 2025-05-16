package pt.estga.spotme.ui.settings

import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatDelegate
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
import pt.estga.spotme.utils.UserPreferences
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

        setupLocationToggle()
        setupLangDropdown()
        setupDarkModeToggle()
        setupNotificationsToggle()
        setupRateAppButton()
        setupDeleteHistoryButton()

        return binding.root
    }

    private fun setupLangDropdown() {
        val prefs = requireContext().getSharedPreferences("Settings", Context.MODE_PRIVATE)
        val savedLang = prefs.getString("My_Lang", "pt") ?: "pt"

        // Criar a lista de idiomas disponíveis
        val languages = listOf("Português", "English")

        // Criar um ArrayAdapter para o Spinner
        val adapter = android.widget.ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, languages)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerLanguage.adapter = adapter

        // Definir o idioma selecionado conforme salvo no SharedPreferences
        val selectedIndex = if (savedLang == "pt") 0 else 1
        binding.spinnerLanguage.setSelection(selectedIndex)

        // Listener para mudanças na seleção do Spinner
        binding.spinnerLanguage.onItemSelectedListener = object : android.widget.AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: android.widget.AdapterView<*>, view: View?, position: Int, id: Long) {
                val selectedLanguageCode = if (position == 0) "pt" else "en"

                // Só muda se for diferente do idioma salvo
                if (savedLang != selectedLanguageCode) {
                    setLocale(selectedLanguageCode)
                }
            }

            override fun onNothingSelected(parent: android.widget.AdapterView<*>) {
                // Não faz nada
            }
        }
    }

    private fun setLocale(langCode: String) {
        val locale = java.util.Locale(langCode)
        java.util.Locale.setDefault(locale)

        val config = resources.configuration
        config.setLocale(locale)

        resources.updateConfiguration(config, resources.displayMetrics)

        // Salvar a preferência
        val prefs = requireContext().getSharedPreferences("Settings", Context.MODE_PRIVATE)
        prefs.edit().putString("My_Lang", langCode).apply()

        // Recarregar a activity para aplicar a mudança
        requireActivity().recreate()
    }

    private fun setupRateAppButton() {
        binding.layoutRateApp.setOnClickListener {
            checkPreviousRating()
        }
    }

    private fun setupDeleteHistoryButton() {
        binding.layoutDeleteHistory.setOnClickListener {
            showDeleteHistoryConfirmationDialog()
        }
    }

    private fun setupDarkModeToggle() {
        val prefs = requireContext().getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
        val isDarkMode = prefs.getBoolean("dark_mode", false)

        // Inicializa o switch com o valor salvo
        binding.switchDarkMode.isChecked = isDarkMode

        // Quando o usuário alternar o switch, atualiza a preferência e o tema
        binding.switchDarkMode.setOnCheckedChangeListener { _, isChecked ->
            prefs.edit().putBoolean("dark_mode", isChecked).apply()

            if (isChecked) {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
            } else {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
            }

            // Opcional: para aplicar o tema imediatamente, reinicia a activity principal (ou o fragmento)
            // Se quiser, pode tentar fazer só requireActivity().recreate()
            requireActivity().recreate()
        }
    }

    private fun setupNotificationsToggle() {
        val userPreferences = UserPreferences.getInstance(requireContext())

        // Definir o estado inicial do switch com base nas preferências
        binding.switchNotifications.isChecked = userPreferences.areNotificationsEnabled()

        // Configurar o listener para mudanças no switch
        binding.switchNotifications.setOnCheckedChangeListener { _, isChecked ->
            userPreferences.setNotificationsEnabled(isChecked)

            if (isChecked) {
                Toast.makeText(
                    requireContext(),
                    "Notificações ativadas",
                    Toast.LENGTH_SHORT
                ).show()
            } else {
                Toast.makeText(
                    requireContext(),
                    "Notificações desativadas",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }

        // Fazer o layout completo ser clicável também
        binding.layoutNotifications.setOnClickListener {
            binding.switchNotifications.isChecked = !binding.switchNotifications.isChecked
        }
    }

    private fun setupLocationToggle() {
        val userPreferences = UserPreferences.getInstance(requireContext())

        // Definir o estado inicial do switch com base nas preferências
        binding.switchLocation.isChecked = userPreferences.isLocationEnabled()

        // Configurar o listener para mudanças no switch
        binding.switchLocation.setOnCheckedChangeListener { _, isChecked ->
            userPreferences.setLocationEnabled(isChecked)

            if (isChecked) {
                // Apenas armazena a preferência, não precisa de permissões extras
                Toast.makeText(
                    requireContext(),
                    "Acesso à localização permitido no aplicativo",
                    Toast.LENGTH_SHORT
                ).show()
            } else {
                // Quando desativado, revoga as permissões de localização concedidas ao aplicativo
                try {
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                        val uri = android.net.Uri.fromParts("package", requireActivity().packageName, null)
                        intent.setData(uri)

                        AlertDialog.Builder(requireContext())
                            .setTitle("Revogar permissões de localização")
                            .setMessage("Para maior segurança, recomendamos revogar as permissões de localização nas configurações do aplicativo. Deseja fazer isso agora?")
                            .setPositiveButton("Ir para configurações") { _, _ ->
                                startActivity(intent)
                            }
                            .setNegativeButton("Não") { _, _ ->
                                Toast.makeText(requireContext(),
                                    "O acesso à localização foi restrito apenas dentro do aplicativo",
                                    Toast.LENGTH_LONG).show()
                            }
                            .show()
                    } else {
                        Toast.makeText(
                            requireContext(),
                            "O acesso à localização foi restrito dentro do aplicativo",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                } catch (e: Exception) {
                    Toast.makeText(
                        requireContext(),
                        "Não foi possível abrir as configurações de permissão",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }

        // Fazer o layout completo ser clicável também
        binding.layoutLocationServices.setOnClickListener {
            binding.switchLocation.isChecked = !binding.switchLocation.isChecked
        }
    }

    private fun showDeleteHistoryConfirmationDialog() {
        AlertDialog.Builder(requireContext())
            .setTitle("Apagar Histórico")
            .setMessage("Tem certeza que deseja apagar todo o seu histórico de estacionamentos? Esta ação não pode ser desfeita.")
            .setPositiveButton("Apagar") { _, _ ->
                deleteUserParkingHistory()
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun deleteUserParkingHistory() {
        val userId = userSession?.userId ?: return

        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val database = AppDatabase.getInstance(requireContext())
                val deletedCount = withContext(Dispatchers.IO) {
                    database.parkingDao().deleteParkingsByUserId(userId)
                }

                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        requireContext(),
                        "Histórico apagado com sucesso! ${deletedCount} registros removidos.",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        requireContext(),
                        "Erro ao apagar histórico: ${e.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
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