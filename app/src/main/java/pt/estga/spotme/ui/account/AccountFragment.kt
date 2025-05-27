package pt.estga.spotme.ui.account

import android.annotation.SuppressLint
import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.util.Patterns
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.Navigation
import pt.estga.spotme.MainActivity
import pt.estga.spotme.R
import pt.estga.spotme.database.AppDatabase
import pt.estga.spotme.database.AppDatabase.Companion.getInstance
import pt.estga.spotme.database.UserDao
import pt.estga.spotme.ui.authentication.LoginActivity
import pt.estga.spotme.utils.PasswordUtils
import pt.estga.spotme.utils.UserSession
import pt.estga.spotme.viewmodels.UserViewModel
import java.io.File
import java.io.FileOutputStream

class AccountFragment : Fragment() {
    private var userSession: UserSession? = null
    private var db: AppDatabase? = null
    private var userDAO: UserDao? = null
    private var profileImage: ImageView? = null

    @SuppressLint("QueryPermissionsNeeded")
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val root = inflater.inflate(R.layout.fragment_profile, container, false)

        // Initialize UI components
        profileImage = root.findViewById(R.id.profileImage) // Use the class field
        val tvNomeField = root.findViewById<TextView>(R.id.tvNomeField)
        val tvNome = root.findViewById<TextView>(R.id.tvNome)
        val ivEditNome = root.findViewById<ImageView>(R.id.ivEditNome)
        val tvTelemovel = root.findViewById<TextView>(R.id.tvTelemovel)
        val ivEditTelemovel = root.findViewById<ImageView>(R.id.ivEditTelemovel)
        val tvEmailField = root.findViewById<TextView>(R.id.tvEmailField)
        val tvEmail = root.findViewById<TextView>(R.id.tvEmail)
        val ivEditEmail = root.findViewById<ImageView>(R.id.ivEditEmail)
        val tvChangePassword = root.findViewById<TextView>(R.id.tvChangePassword)
        val cardChangePassword = root.findViewById<LinearLayout>(R.id.cardChangePassword)
        val tvPersonalStats = root.findViewById<TextView>(R.id.tvPersonalStats)
        val cardPersonalStats = root.findViewById<LinearLayout>(R.id.cardPersonalStats)
        val ivEditProfilePhoto = root.findViewById<ImageView>(R.id.ivEditProfilePhoto)
        val tvDeleteAccount = root.findViewById<TextView>(R.id.tvDeleteAccount)
        val cardDeleteAccount = root.findViewById<LinearLayout>(R.id.cardDeleteAccount)

        userSession = UserSession.getInstance(requireContext())
        db = getInstance(requireContext())
        userDAO = db!!.userDao()

        if (userSession!!.isGoogleLogin) {
            root.findViewById<LinearLayout>(R.id.layoutEmail).visibility = View.GONE
            root.findViewById<LinearLayout>(R.id.cardChangePassword).visibility = View.GONE
            ivEditProfilePhoto.visibility = View.GONE
            root.findViewById<LinearLayout>(R.id.cardDeleteAccount).visibility = View.GONE
        }

        // Set initial values for all text fields
        tvNome.text = userSession!!.userName
        tvNomeField.text = userSession!!.userName
        tvEmail.text = userSession!!.userEmail
        tvEmailField.text = userSession!!.userEmail
        tvTelemovel.text = userSession!!.userPhone

        // Load the profile image
        carregarImagemDePerfil()

        // Add any additional setup or listeners here
        ivEditNome.setOnClickListener { v: View? ->
            val inflater1 = LayoutInflater.from(requireContext())
            val dialogView = inflater1.inflate(R.layout.dialog_edit_name, null)

            val dialogBuilder = AlertDialog.Builder(requireContext())
            dialogBuilder.setView(dialogView)

            val etNewName = dialogView.findViewById<EditText>(R.id.etNewName)
            val btnSave = dialogView.findViewById<Button>(R.id.btnSave)

            val buttonClose = dialogView.findViewById<ImageButton>(R.id.btnClose)
            val buttonCancel = dialogView.findViewById<Button>(R.id.btnCancel)

            etNewName.setText(userSession!!.userName)

            val alertDialog = dialogBuilder.create()
            alertDialog.show()

            btnSave.setOnClickListener {
                val newName = etNewName.text.toString().trim()

                // Validação para verificar se o nome está vazio
                if (newName.isEmpty()) {
                    Toast.makeText(
                        requireContext(),
                        getString(R.string.name_empty_error),
                        Toast.LENGTH_SHORT
                    ).show()
                    return@setOnClickListener
                }

                // Validação para verificar se o nome tem pelo menos 2 caracteres
                if (newName.length < 2) {
                    Toast.makeText(
                        requireContext(),
                        getString(R.string.name_too_short),
                        Toast.LENGTH_SHORT
                    ).show()
                    return@setOnClickListener
                }

                if (newName.length > 30) {
                    Toast.makeText(
                        requireContext(),
                        getString(R.string.name_too_long),
                        Toast.LENGTH_SHORT
                    ).show()
                    return@setOnClickListener
                }

                // Validação para verificar se o nome contém apenas letras e espaços
                if (!newName.matches(Regex("^[\\p{L} ]+$"))) {
                    Toast.makeText(
                        requireContext(),
                        getString(R.string.name_invalid_chars),
                        Toast.LENGTH_SHORT
                    ).show()
                    return@setOnClickListener
                }

                // Se passar por todas as validações, atualizamos o nome
                userSession!!.userName = newName
                tvNome.text = newName
                tvNomeField.text = newName

                Thread {
                    userDAO!!.updateNome(newName, userSession!!.userId)
                }.start()

                // Atualiza o ViewModel
                val userViewModel = ViewModelProvider(requireActivity())[UserViewModel::class.java]
                userViewModel.updateUser(
                    newName,
                    userSession!!.userEmail,
                    userSession!!.userProfileImage
                )

                Toast.makeText(
                    requireContext(),
                    getString(R.string.name_updated),
                    Toast.LENGTH_SHORT
                ).show()
                alertDialog.dismiss()
            }

            buttonClose.setOnClickListener { view: View? -> alertDialog.dismiss() }
            buttonCancel.setOnClickListener { view: View? -> alertDialog.dismiss() }
        }

        ivEditEmail.setOnClickListener { v: View? ->
            val inflater2 = LayoutInflater.from(requireContext())
            val dialogView = inflater2.inflate(R.layout.dialog_edit_email, null)

            val dialogBuilder = AlertDialog.Builder(requireContext())
            dialogBuilder.setView(dialogView)

            val etNewEmail = dialogView.findViewById<EditText>(R.id.etNewEmail)
            val btnSave = dialogView.findViewById<Button>(R.id.btnSave)

            val btnCancel = dialogView.findViewById<Button>(R.id.btnCancel)
            val btnClose = dialogView.findViewById<ImageButton>(R.id.btnClose)

            etNewEmail.setText(userSession!!.userEmail)

            val alertDialog = dialogBuilder.create()
            alertDialog.show()

            btnSave.setOnClickListener {
                val newEmail = etNewEmail.text.toString().trim()
                if (newEmail.isNotEmpty() && Patterns.EMAIL_ADDRESS.matcher(newEmail).matches()) {
                    Thread {
                        val existingUser = userDAO!!.findByEmail(newEmail)
                        if (existingUser == null) {
                            userSession!!.userEmail = newEmail
                            userDAO!!.updateEmail(newEmail, userSession!!.userId)

                            requireActivity().runOnUiThread {
                                tvEmail.text = newEmail
                                tvEmailField.text = newEmail
                                Toast.makeText(
                                    requireContext(),
                                    getString(R.string.email_updated_successfully),
                                    Toast.LENGTH_SHORT
                                ).show()

                                // Atualizar ViewModel
                                val userViewModel = ViewModelProvider(requireActivity())[UserViewModel::class.java]
                                userViewModel.updateUser(
                                    userSession!!.userName,
                                    newEmail,
                                    userSession!!.userProfileImage
                                )

                                alertDialog.dismiss()
                            }
                        } else {
                            requireActivity().runOnUiThread {
                                Toast.makeText(
                                    requireContext(),
                                    getString(R.string.email_already_registered),
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        }
                    }.start()
                } else {
                    Toast.makeText(
                        requireContext(),
                        getString(R.string.email_invalid),
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }

            btnCancel.setOnClickListener { view: View? -> alertDialog.dismiss() }
            btnClose.setOnClickListener { view: View? -> alertDialog.dismiss() }
        }

        ivEditTelemovel.setOnClickListener { v: View? ->
            val inflater3 = LayoutInflater.from(requireContext())
            val dialogView = inflater3.inflate(R.layout.dialog_edit_phone, null)

            val dialogBuilder = AlertDialog.Builder(requireContext())
            dialogBuilder.setView(dialogView)

            val etNewPhone = dialogView.findViewById<EditText>(R.id.etNewPhone)
            val btnSave = dialogView.findViewById<Button>(R.id.btnSave)

            val btnCancel = dialogView.findViewById<Button>(R.id.btnCancel)
            val btnClose = dialogView.findViewById<ImageButton>(R.id.btnClose)

            val alertDialog = dialogBuilder.create()
            alertDialog.show()

            etNewPhone.setText(userSession!!.userPhone)

            btnSave.setOnClickListener { view: View? ->
                val newPhone = etNewPhone.text.toString().trim()

                if (newPhone.isEmpty()) {
                    Toast.makeText(
                        requireContext(),
                        getString(R.string.phone_empty_error),
                        Toast.LENGTH_SHORT
                    ).show()
                    return@setOnClickListener
                }

                // Validar formato do número de telefone com regex
                // Aceita formatos como: +351 123456789, 351123456789, 123456789
                if (!newPhone.matches(Regex("^(\\+?\\d{1,3}\\s?)?\\d{9}\$"))) {
                    Toast.makeText(
                        requireContext(),
                        getString(R.string.phone_invalid_format),
                        Toast.LENGTH_SHORT
                    ).show()
                    return@setOnClickListener
                }

                userSession!!.userPhone = newPhone
                tvTelemovel.text = newPhone
                Thread {
                    userDAO!!.updatePhone(newPhone, userSession!!.userId)
                    requireActivity().runOnUiThread {
                        Toast.makeText(
                            requireContext(),
                            getString(R.string.phone_updated),
                            Toast.LENGTH_SHORT
                        ).show()
                        alertDialog.dismiss()
                    }
                }.start()
            }

            btnCancel.setOnClickListener { view: View? -> alertDialog.dismiss() }
            btnClose.setOnClickListener { view: View? -> alertDialog.dismiss() }
        }

        cardChangePassword.setOnClickListener { v: View? ->
            showChangePasswordDialog()
        }

        tvChangePassword.setOnClickListener { v: View? ->
            showChangePasswordDialog()
        }

        ivEditProfilePhoto.setOnClickListener { v: View? ->
            showChangeProfileImageDialog()
        }

        cardDeleteAccount.setOnClickListener {
            showDeleteAccountDialog()
        }

        tvDeleteAccount.setOnClickListener {
            showDeleteAccountDialog()
        }

        cardPersonalStats.setOnClickListener {
            navigateToPersonalStats()
        }

        tvPersonalStats.setOnClickListener {
            navigateToPersonalStats()
        }

        return root
    }

    private fun showChangePasswordDialog() {
        val inflater4 = LayoutInflater.from(requireContext())
        val dialogView = inflater4.inflate(R.layout.dialog_change_password, null)

        val dialogBuilder = AlertDialog.Builder(requireContext())
        dialogBuilder.setView(dialogView)

        val etCurrentPassword = dialogView.findViewById<EditText>(R.id.etCurrentPassword)
        val etNewPassword = dialogView.findViewById<EditText>(R.id.etNewPassword)
        val etConfirmPassword = dialogView.findViewById<EditText>(R.id.etConfirmPassword)
        val btnSave = dialogView.findViewById<Button>(R.id.btnSave)

        val btnClose = dialogView.findViewById<ImageButton>(R.id.btnClose)
        val btnCancel = dialogView.findViewById<Button>(R.id.btnCancel)

        val alertDialog = dialogBuilder.create()
        alertDialog.show()

        btnSave.setOnClickListener { view: View? ->
            val currentPassword = etCurrentPassword.text.toString().trim()
            val newPassword = etNewPassword.text.toString().trim()
            val confirmPassword = etConfirmPassword.text.toString().trim()

            // Verificação de campos vazios
            if (currentPassword.isEmpty() || newPassword.isEmpty() || confirmPassword.isEmpty()) {
                Toast.makeText(
                    requireContext(),
                    getString(R.string.password_empty_error),
                    Toast.LENGTH_SHORT
                ).show()
                return@setOnClickListener
            }

            // Verificação do tamanho mínimo da senha
            if (newPassword.length < 6) {
                Toast.makeText(
                    requireContext(),
                    getString(R.string.password_too_short),
                    Toast.LENGTH_SHORT
                ).show()
                return@setOnClickListener
            }

            // Verificação se a senha contém pelo menos um número
            if (!newPassword.any { it.isDigit() }) {
                Toast.makeText(
                    requireContext(),
                    getString(R.string.password_must_have_number),
                    Toast.LENGTH_SHORT
                ).show()
                return@setOnClickListener
            }

            // Verificação se a senha contém pelo menos uma letra
            if (!newPassword.any { it.isLetter() }) {
                Toast.makeText(
                    requireContext(),
                    getString(R.string.password_must_have_letter),
                    Toast.LENGTH_SHORT
                ).show()
                return@setOnClickListener
            }

            // Verificação se as senhas novas coincidem
            if (newPassword != confirmPassword) {
                Toast.makeText(
                    requireContext(),
                    getString(R.string.password_no_match),
                    Toast.LENGTH_SHORT
                ).show()
                return@setOnClickListener
            }

            // Verificar a password atual na base de dados
            Thread {
                val user = userDAO!!.getById(userSession!!.userId.toInt())
                val isPasswordCorrect = user?.let {
                    PasswordUtils.verifyPassword(currentPassword, it.password)
                } ?: false

                requireActivity().runOnUiThread {
                    if (isPasswordCorrect) {
                        // Se a password atual estiver correta, atualiza para a nova password
                        val hashedPassword = PasswordUtils.hashPassword(newPassword)
                        Thread {
                            userDAO!!.updatePassword(hashedPassword, userSession!!.userId)
                            requireActivity().runOnUiThread {
                                Toast.makeText(
                                    requireContext(),
                                    getString(R.string.password_updated),
                                    Toast.LENGTH_SHORT
                                ).show()
                                alertDialog.dismiss()
                                // ir para a tela de login
                                userSession!!.clearSession()
                                val intent = Intent(
                                    requireContext(),
                                    LoginActivity::class.java
                                )
                                startActivity(intent)
                                requireActivity().finish()
                            }
                        }.start()
                    } else {
                        // password atual incorreta
                        Toast.makeText(
                            requireContext(),
                            getString(R.string.password_incorrect),
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            }.start()
        }

        btnClose.setOnClickListener { view: View? -> alertDialog.dismiss() }
        btnCancel.setOnClickListener { view: View? -> alertDialog.dismiss() }
    }

    private fun showChangeProfileImageDialog() {
        val inflater5 = LayoutInflater.from(requireContext())
        val dialogView = inflater5.inflate(R.layout.dialog_change_profile_image, null)

        val dialogBuilder = AlertDialog.Builder(requireContext())
        dialogBuilder.setView(dialogView)

        val alertDialog = dialogBuilder.create()
        alertDialog.show()

        val layoutCamera = dialogView.findViewById<LinearLayout>(R.id.layoutCamera)
        val layoutGallery = dialogView.findViewById<LinearLayout>(R.id.layoutGallery)
        val layoutRemove = dialogView.findViewById<LinearLayout>(R.id.layoutRemove)
        val btnCancel = dialogView.findViewById<Button>(R.id.btnCancel)

        layoutCamera.setOnClickListener {
            // Handle taking a photo
            val takePictureIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
            if (takePictureIntent.resolveActivity(requireActivity().packageManager) != null) {
                startActivityForResult(
                    takePictureIntent,
                    REQUEST_IMAGE_CAPTURE
                )
            }
            alertDialog.dismiss()
        }

        layoutGallery.setOnClickListener {
            // Handle choosing from gallery
            val pickPhotoIntent = Intent(
                Intent.ACTION_PICK,
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI
            )
            startActivityForResult(
                pickPhotoIntent,
                REQUEST_IMAGE_PICK
            )
            alertDialog.dismiss()
        }

        layoutRemove.setOnClickListener {
            userSession!!.userProfileImage = null
            this.profileImage!!.setImageResource(R.drawable.ic_default_profile)

            Thread {
                userDAO!!.updateProfileImage("", userSession!!.userId)

                requireActivity().runOnUiThread {
                    // Atualizar ViewModel partilhado
                    val userViewModel = ViewModelProvider(requireActivity())[UserViewModel::class.java]
                    userViewModel.updateUser(
                        userSession!!.userName,
                        userSession!!.userEmail,
                        null
                    )

                    Toast.makeText(
                        requireContext(),
                        getString(R.string.profile_image_removed),
                        Toast.LENGTH_SHORT
                    ).show()

                    alertDialog.dismiss()
                }
            }.start()
        }

        btnCancel.setOnClickListener { alertDialog.dismiss() }
    }

    private fun showDeleteAccountDialog() {
        val inflater6 = LayoutInflater.from(requireContext())
        val dialogView1 = inflater6.inflate(R.layout.dialog_delete_account, null)

        val dialogBuilder1 = AlertDialog.Builder(requireContext())
        dialogBuilder1.setView(dialogView1)

        val etPassword = dialogView1.findViewById<EditText>(R.id.etPassword)
        val btnDelete = dialogView1.findViewById<Button>(R.id.btnDelete)

        val btnClose = dialogView1.findViewById<ImageButton>(R.id.btnClose)
        val buttonCancelar = dialogView1.findViewById<Button>(R.id.btnCancel)

        val alertDialog = dialogBuilder1.create()
        alertDialog.show()

        btnDelete.setOnClickListener { view1: View? ->
            val password = etPassword.text.toString().trim { it <= ' ' }
            if (password.isEmpty()) {
                Toast.makeText(
                    requireContext(),
                    getString(R.string.password_empty),
                    Toast.LENGTH_SHORT
                ).show()
            } else {
                // Iniciar uma thread para verificar a senha no banco de dados
                Thread {
                    val user = userDAO!!.getById(userSession!!.userId.toInt())
                    val isPasswordCorrect = user?.let {
                        PasswordUtils.verifyPassword(password, it.password)
                    } ?: false

                    requireActivity().runOnUiThread {
                        if (isPasswordCorrect) {
                            // Senha correta, exclui a conta
                            Thread {
                                userDAO!!.delete(userSession!!.userId)
                                requireActivity().runOnUiThread {
                                    Toast.makeText(
                                        requireContext(),
                                        getString(R.string.account_deleted),
                                        Toast.LENGTH_SHORT
                                    ).show()
                                    alertDialog.dismiss()
                                    // ir para a tela de login
                                    userSession!!.clearSession()
                                    val intent = Intent(
                                        requireContext(),
                                        LoginActivity::class.java
                                    )
                                    startActivity(intent)
                                    requireActivity().finish()
                                }
                            }.start()
                        } else {
                            // Senha incorreta
                            Toast.makeText(
                                requireContext(),
                                getString(R.string.password_incorrect_delete),
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                }.start()
            }
        }

        btnClose.setOnClickListener { view1: View? -> alertDialog.dismiss() }
        buttonCancelar.setOnClickListener { view1: View? -> alertDialog.dismiss() }
    }

    private fun navigateToPersonalStats() {
        val navController = Navigation.findNavController(requireActivity(), R.id.nav_host_fragment_content_main)
        navController.navigate(R.id.personalStatisticsFragment)
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == Activity.RESULT_OK && data != null) {
            if (requestCode == REQUEST_IMAGE_PICK) {
                val selectedImageUri = data.data
                if (selectedImageUri != null) {
                    try {
                        val userId = userSession!!.userId
                        if (userId <= 0) {
                            Log.e("ProfileImage", "ID do utilizador inválido.")
                            return
                        }

                        val imagePath = copyImageToInternalStorage(selectedImageUri, userId)

                        if (imagePath != null) {
                            val uriWithTimestamp = Uri.fromFile(File(imagePath)).buildUpon()
                                .appendQueryParameter("ts", System.currentTimeMillis().toString())
                                .build()
                            profileImage!!.setImageURI(uriWithTimestamp)

                            userSession!!.userProfileImage = imagePath

                            Thread {
                                userDAO!!.updateProfileImage(imagePath, userId)
                                requireActivity().runOnUiThread {
                                    val userViewModel = ViewModelProvider(requireActivity())[UserViewModel::class.java]
                                    userViewModel.updateUser(
                                        userSession!!.userName,
                                        userSession!!.userEmail,
                                        imagePath
                                    )
                                }
                            }.start()
                        } else {
                            Log.e("ProfileImage", "Erro ao copiar imagem para armazenamento interno.")
                        }
                    } catch (e: Exception) {
                        Log.e("ProfileImage", "Erro ao processar a imagem", e)
                    }
                }
            } else if (requestCode == REQUEST_IMAGE_CAPTURE) {
                // Processar a imagem capturada pela câmera
                try {
                    val userId = userSession!!.userId
                    if (userId <= 0) {
                        Log.e("ProfileImage", "ID do utilizador inválido.")
                        return
                    }
                    
                    val imageBitmap = data.extras?.get("data") as android.graphics.Bitmap
                    val cw = ContextWrapper(requireContext())
                    val directory = cw.getDir("profile_images", Context.MODE_PRIVATE)
                    val imageFile = File(directory, "profile_$userId.jpg")
                    
                    val fos = FileOutputStream(imageFile)
                    imageBitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, 100, fos)
                    fos.close()

                    val imagePath = imageFile.absolutePath
                    val uriWithTimestamp = Uri.fromFile(imageFile).buildUpon()
                        .appendQueryParameter("ts", System.currentTimeMillis().toString())
                        .build()
                    profileImage!!.setImageURI(uriWithTimestamp)

                    userSession!!.userProfileImage = imagePath
                    
                    Thread {
                        userDAO!!.updateProfileImage(imagePath, userId)
                        requireActivity().runOnUiThread {
                            val userViewModel = ViewModelProvider(requireActivity())[UserViewModel::class.java]
                            userViewModel.updateUser(
                                userSession!!.userName,
                                userSession!!.userEmail,
                                imagePath
                            )
                        }
                    }.start()
                } catch (e: Exception) {
                    Log.e("ProfileImage", "Erro ao processar a imagem da câmera", e)
                }
            }
        }
    }

    @Deprecated("Deprecated in Java")
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 1) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Log.d("Permissions", "Permissão concedida!")
                carregarImagemDePerfil() // Agora podemos carregar a imagem
            } else {
                Log.e(
                    "Permissions",
                    "Permissão negada! Não será possível carregar imagens do armazenamento."
                )
                Toast.makeText(
                    requireContext(),
                    getString(R.string.permission_needed_storage),
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    // Método corrigido para garantir que cada utilizador tem a sua própria imagem
    private fun copyImageToInternalStorage(imageUri: Uri, userId: Long): String? {
        val cw = ContextWrapper(requireContext())
        val directory = cw.getDir("profile_images", Context.MODE_PRIVATE)
        if (!directory.exists()) {
            directory.mkdirs() // Criar diretório se não existir
        }

        // Criar nome de ficheiro único baseado no ID do utilizador
        val imageFile = File(directory, "profile_$userId.jpg")

        try {
            FileOutputStream(imageFile).use { fos ->
                requireContext().contentResolver.openInputStream(imageUri).use { inputStream ->
                    if (inputStream != null) {
                        val buffer = ByteArray(1024)
                        var bytesRead: Int
                        while ((inputStream.read(buffer).also { bytesRead = it }) != -1) {
                            fos.write(buffer, 0, bytesRead)
                        }
                        fos.flush()
                        Log.d("ProfileImage", "Imagem copiada para: " + imageFile.absolutePath)
                        return imageFile.absolutePath
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("ProfileImage", "Erro ao copiar imagem para armazenamento interno", e)
        }
        return null
    }

    // Método para carregar corretamente a imagem de perfil do utilizador logado
    private fun carregarImagemDePerfil() {
        val userId = userSession!!.userId
        if (userId <= 0) {
            Log.e("ProfileImage", "ID do utilizador inválido.")
            profileImage!!.setImageResource(R.drawable.ic_default_profile)
            return
        }

        val profileImagePath = userSession!!.userProfileImage
        Log.d("ProfileImage", "Caminho guardado na sessão: $profileImagePath")

        if (profileImagePath.isNullOrEmpty()) {
            // Se não houver caminho na sessão, tenta buscar na base de dados
            Thread {
                val dbImagePath = userDAO!!.getProfileImage(userSession!!.userId)
                requireActivity().runOnUiThread {
                    if (!dbImagePath.isNullOrEmpty()) {
                        val imgFile = File(dbImagePath)
                        if (imgFile.exists()) {
                            profileImage!!.setImageURI(Uri.fromFile(imgFile))
                        } else {
                            Log.e(
                                "ProfileImage",
                                "Ficheiro não encontrado: $dbImagePath"
                            )
                            profileImage!!.setImageResource(R.drawable.ic_default_profile)
                        }
                    } else {
                        Log.e(
                            "ProfileImage",
                            "Nenhuma imagem encontrada na base de dados."
                        )
                        profileImage!!.setImageResource(R.drawable.ic_default_profile)
                    }
                }
            }.start()
        } else {
            // Se o caminho está na sessão, carrega diretamente
            val imgFile = File(profileImagePath)
            if (imgFile.exists()) {
                profileImage!!.setImageURI(Uri.fromFile(imgFile))
            } else {
                Log.e("ProfileImage", "Ficheiro não encontrado: $profileImagePath")
                profileImage!!.setImageResource(R.drawable.ic_default_profile)
            }
        }
    }

    companion object {
        private const val REQUEST_IMAGE_CAPTURE = 1
        private const val REQUEST_IMAGE_PICK = 2
    }
}
