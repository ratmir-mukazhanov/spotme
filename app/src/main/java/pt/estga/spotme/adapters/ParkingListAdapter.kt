package pt.estga.spotme.adapters

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import pt.estga.spotme.R
import pt.estga.spotme.adapters.ParkingListAdapter.ParkingViewHolder
import pt.estga.spotme.entities.Parking

class ParkingListAdapter(
    private var parkingList: List<Parking?>,
    private val onItemClickListener: View.OnClickListener
) :
    RecyclerView.Adapter<ParkingViewHolder>() {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ParkingViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.parking_list_item_layout, parent, false)
        return ParkingViewHolder(view, onItemClickListener)
    }

    override fun onBindViewHolder(holder: ParkingViewHolder, position: Int) {
        val parking = parkingList[position]
        holder.titleTextView.text = parking!!.title
        holder.descriptionTextView.text = parking.description
    }

    override fun getItemCount(): Int {
        return parkingList.size
    }

    class ParkingViewHolder(itemView: View, var onItemClickListener: View.OnClickListener) :
        RecyclerView.ViewHolder(itemView), View.OnClickListener {
        var titleTextView: TextView =
            itemView.findViewById(R.id.textViewTitle)
        var descriptionTextView: TextView =
            itemView.findViewById(R.id.textViewDescription)

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
        return parkingList[position]!!
    }

    fun removeItem(position: Int) {
        parkingList.toMutableList().removeAt(position)
        notifyItemRemoved(position)
    }
}
