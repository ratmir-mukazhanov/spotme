package pt.estga.spotme.ui.account

import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import pt.estga.spotme.R
import pt.estga.spotme.database.AppDatabase
import pt.estga.spotme.database.ParkingDao
import pt.estga.spotme.database.UserDao
import pt.estga.spotme.utils.UserSession
import pt.estga.spotme.viewmodels.UserViewModel
import java.io.File
import java.util.Calendar
import java.util.concurrent.TimeUnit

class PersonalStatisticsFragment : Fragment() {

    private var profileImage: ImageView? = null
    private var userSession: UserSession? = null
    private var userDAO: UserDao? = null
    private var parkingDAO: ParkingDao? = null

    // TextViews para estatísticas
    private var tvTotalAllTime: TextView? = null
    private var tvAvgAllTime: TextView? = null
    private var tvTotalThisWeek: TextView? = null
    private var tvAvgThisWeek: TextView? = null
    private var tvTotalThisMonth: TextView? = null
    private var tvAvgThisMonth: TextView? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Infla o layout para este fragment
        val root = inflater.inflate(R.layout.personal_statistics, container, false)

        // Vincula a ImageView do layout que exibirá a foto de perfil
        profileImage = root.findViewById(R.id.profileImage)

        // Vincula os TextViews de estatísticas
        tvTotalAllTime = root.findViewById(R.id.tvTotalAllTime)
        tvAvgAllTime = root.findViewById(R.id.tvAvgAllTime)
        tvTotalThisWeek = root.findViewById(R.id.tvTotalThisWeek)
        tvAvgThisWeek = root.findViewById(R.id.tvAvgThisWeek)
        tvTotalThisMonth = root.findViewById(R.id.tvTotalThisMonth)
        tvAvgThisMonth = root.findViewById(R.id.tvAvgThisMonth)

        // Inicializa a sessão e a base de dados
        userSession = UserSession.getInstance(requireContext())
        userDAO = AppDatabase.getInstance(requireContext()).userDao()
        parkingDAO = AppDatabase.getInstance(requireContext()).parkingDao()

        // Carrega a imagem de perfil do utilizador
        carregarImagemDePerfil()

        // Carrega as estatísticas do utilizador
        carregarEstatisticas()

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

    // Método para carregar as estatísticas do utilizador
    private fun carregarEstatisticas() {
        val userId = UserSession.getInstance(requireContext()).userId

        if (userId == null || userId <= 0) {
            Log.e("PersonalStats", "ID do utilizador inválido ou nulo.")
            return
        }

        Thread {
            try {
                // Definição consistente dos períodos de tempo
                val now = System.currentTimeMillis()
                val millisInDay = 24 * 60 * 60 * 1000L

                // Início da semana (últimos 7 dias)
                val startOfWeek = now - (7 * millisInDay)

                // Início do mês (últimos 30 dias)
                val startOfMonth = now - (30 * millisInDay)

                // Obter estatísticas totais
                val allTimeTotal = parkingDAO!!.getAllTimeParkingCount(userId)
                val weekTotal = parkingDAO!!.getWeeklyParkingCount(userId, startOfWeek)
                val monthTotal = parkingDAO!!.getMonthlyParkingCount(userId, startOfMonth)

                // Obter médias de tempo (com verificação de nulos)
                val allTimeAvg = parkingDAO!!.getAllTimeParkingTimeAvg(userId) ?: 0L
                val weekAvg = parkingDAO!!.getWeeklyParkingTimeAvg(userId, startOfWeek) ?: 0L
                val monthAvg = parkingDAO!!.getMonthlyParkingTimeAvg(userId, startOfMonth) ?: 0L

                // Registrar valores para debug
                Log.d("StatisticsDebug", "All Time Total: $allTimeTotal, Avg: $allTimeAvg")
                Log.d("StatisticsDebug", "Week Total: $weekTotal, Avg: $weekAvg")
                Log.d("StatisticsDebug", "Month Total: $monthTotal, Avg: $monthAvg")

                // Formatar médias para exibição
                val formattedAllTimeAvg = formatTimeInMinutes(allTimeAvg)
                val formattedWeekAvg = formatTimeInMinutes(weekAvg)
                val formattedMonthAvg = formatTimeInMinutes(monthAvg)

                // Atualizar a UI
                requireActivity().runOnUiThread {
                    tvTotalAllTime?.text = allTimeTotal.toString()
                    tvAvgAllTime?.text = formattedAllTimeAvg

                    tvTotalThisWeek?.text = weekTotal.toString()
                    tvAvgThisWeek?.text = formattedWeekAvg

                    tvTotalThisMonth?.text = monthTotal.toString()
                    tvAvgThisMonth?.text = formattedMonthAvg
                }
            } catch (e: Exception) {
                Log.e("StatisticsError", "Erro ao carregar estatísticas: ${e.message}")
                e.printStackTrace()
            }
        }.start()
    }

    // Formatar tempo em milissegundos para um formato legível (ex: 65min ou 1h 5min)
    private fun formatTimeInMinutes(millis: Long): String {
        val minutes = TimeUnit.MILLISECONDS.toMinutes(millis)
        return if (minutes >= 60) {
            val hours = minutes / 60
            val mins = minutes % 60
            "${hours}h ${mins}min"
        } else {
            "${minutes}min"
        }
    }
}