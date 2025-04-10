package pt.estga.spotme.ui.account

import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.fragment.app.Fragment
import pt.estga.spotme.R
import pt.estga.spotme.database.AppDatabase
import pt.estga.spotme.database.UserDao
import pt.estga.spotme.utils.UserSession
import java.io.File

class PersonalStatisticsFragment : Fragment() {

    private var profileImage: ImageView? = null
    private var userSession: UserSession? = null
    private var userDAO: UserDao? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Infla o layout para este fragment
        val root = inflater.inflate(R.layout.personal_statistics, container, false)

        // Vincula a ImageView do layout que exibirá a foto de perfil
        profileImage = root.findViewById(R.id.profileImage)

        // Inicializa a sessão e a base de dados
        userSession = UserSession.getInstance(requireContext())
        userDAO = AppDatabase.getInstance(requireContext()).userDao()

        // Carrega a imagem de perfil do usuário
        carregarImagemDePerfil()

        return root
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
                            Log.e("ProfileImage", "Ficheiro não encontrado: $dbImagePath")
                            profileImage!!.setImageResource(R.drawable.ic_default_profile)
                        }
                    } else {
                        Log.e("ProfileImage", "Nenhuma imagem encontrada na base de dados.")
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
}
