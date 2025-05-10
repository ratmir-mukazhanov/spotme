package pt.estga.spotme.adapters

import android.annotation.SuppressLint
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.chip.Chip
import pt.estga.spotme.R
import pt.estga.spotme.entities.Parking
import java.text.SimpleDateFormat
import java.util.*

class ParkingListAdapter(
    private var parkingList: List<Parking>,
    private val onItemClickListener: View.OnClickListener
) : RecyclerView.Adapter<ParkingListAdapter.ParkingViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ParkingViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_parking, parent, false)
        return ParkingViewHolder(view, onItemClickListener)
    }

    override fun onBindViewHolder(holder: ParkingViewHolder, position: Int) {
        val parking = parkingList[position]

        // Definir título
        holder.titleTextView.text = parking.title

        // Formatar e exibir a data
        val dateFormat = SimpleDateFormat("dd/MM/yyyy - HH:mm", Locale.getDefault())
        val date = Date(parking.startTime)
        holder.dateTextView.text = dateFormat.format(date)

        // Calcular duração em minutos
        val durationMinutes = parking.allowedTime / 60000
        val hours = durationMinutes / 60
        val minutes = durationMinutes % 60

        val durationText = if (hours > 0) {
            "Duração: ${hours}h ${minutes}min"
        } else {
            "Duração: ${minutes}min"
        }
        holder.durationTextView.text = durationText

        // Configurar o status do estacionamento
        val now = System.currentTimeMillis()
        val isActive = parking.startTime + parking.allowedTime > now

        holder.statusChip.text = if (isActive) "Ativo" else "Concluído"
        holder.statusChip.setChipBackgroundColorResource(
            if (isActive) R.color.chip_active else R.color.chip_inactive
        )

        // Configurar a imagem (placeholder por enquanto)
        holder.parkingImageView.setImageResource(R.drawable.ic_parking_placeholder)
    }

    override fun getItemCount(): Int {
        return parkingList.size
    }

    class ParkingViewHolder(itemView: View, private val onItemClickListener: View.OnClickListener) :
        RecyclerView.ViewHolder(itemView), View.OnClickListener {

        val titleTextView: TextView = itemView.findViewById(R.id.textViewTitle)
        val dateTextView: TextView = itemView.findViewById(R.id.textViewDate)
        val durationTextView: TextView = itemView.findViewById(R.id.textViewDuration)
        val statusChip: Chip = itemView.findViewById(R.id.chipStatus)
        val parkingImageView: ImageView = itemView.findViewById(R.id.imageViewParking)

        init {
            itemView.setOnClickListener(this)
        }

        override fun onClick(v: View) {
            onItemClickListener.onClick(v)
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    fun updateData(newList: List<Parking>) {
        parkingList = newList
        notifyDataSetChanged()
    }

    @SuppressLint("NotifyDataSetChanged")
    fun setParkingList(parkingList: MutableList<Parking>) {
        this.parkingList = parkingList
        notifyDataSetChanged()
    }

    fun getParkingAt(position: Int): Parking {
        return parkingList[position]
    }

    fun removeItem(position: Int) {
        (parkingList as MutableList).removeAt(position)
        notifyItemRemoved(position)
    }
}