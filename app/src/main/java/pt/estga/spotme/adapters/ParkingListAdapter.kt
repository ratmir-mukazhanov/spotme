package pt.estga.spotme.adapters

import android.annotation.SuppressLint
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
    private val onItemClick: (Parking) -> Unit  // Modificado para receber o Parking diretamente
) : RecyclerView.Adapter<ParkingListAdapter.ParkingViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ParkingViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_parking, parent, false)
        return ParkingViewHolder(view) { position ->
            // Passamos o Parking correto diretamente usando a posição
            if (position in parkingList.indices) {
                onItemClick(parkingList[position])
            }
        }
    }

    override fun onBindViewHolder(holder: ParkingViewHolder, position: Int) {
        val parking = parkingList[position]
        val context = holder.itemView.context

        holder.titleTextView.text = parking.title

        val dateFormat = SimpleDateFormat("dd/MM/yyyy - HH:mm", Locale.getDefault())
        val date = Date(parking.startTime)
        holder.dateTextView.text = dateFormat.format(date)

        if (parking.allowedTime > 0) {
            val durationMinutes = parking.allowedTime / 60000
            val hours = durationMinutes / 60
            val minutes = durationMinutes % 60

            holder.durationTextView.text = if (hours > 0) {
                context.getString(R.string.duration_format_hours, hours, minutes)
            } else {
                context.getString(R.string.duration_format_minutes, minutes)
            }
        } else {
            holder.durationTextView.text = context.getString(R.string.duration_undefined)
        }

        val now = System.currentTimeMillis()
        if (parking.allowedTime == 0L) {
            holder.statusChip.text = context.getString(R.string.status_no_timer)
            holder.statusChip.setChipBackgroundColorResource(R.color.chip_neutral)
        } else if (parking.startTime + parking.allowedTime > now) {
            holder.statusChip.text = context.getString(R.string.status_active)
            holder.statusChip.setChipBackgroundColorResource(R.color.chip_active)
        } else {
            holder.statusChip.text = context.getString(R.string.status_completed)
            holder.statusChip.setChipBackgroundColorResource(R.color.chip_inactive)
        }

        holder.parkingImageView.setImageResource(R.drawable.ic_parking_placeholder)
    }

    override fun getItemCount(): Int = parkingList.size

    class ParkingViewHolder(
        itemView: View,
        private val onClick: (Int) -> Unit
    ) : RecyclerView.ViewHolder(itemView) {

        val titleTextView: TextView = itemView.findViewById(R.id.textViewTitle)
        val dateTextView: TextView = itemView.findViewById(R.id.textViewDate)
        val durationTextView: TextView = itemView.findViewById(R.id.textViewDuration)
        val statusChip: Chip = itemView.findViewById(R.id.chipStatus)
        val parkingImageView: ImageView = itemView.findViewById(R.id.imageViewParking)

        init {
            itemView.setOnClickListener {
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    onClick(position)
                }
            }
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

    // Propriedade para expor a lista atual
    val currentList: List<Parking>
        get() = parkingList
}