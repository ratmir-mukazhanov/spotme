package pt.estga.spotme.adapters;

import android.annotation.SuppressLint;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import pt.estga.spotme.R;
import pt.estga.spotme.entities.Parking;

import java.util.List;

public class ParkingListAdapter extends RecyclerView.Adapter<ParkingListAdapter.ParkingViewHolder> {

    private List<Parking> parkingList;
    private View.OnClickListener onItemClickListener;

    public ParkingListAdapter(List<Parking> parkingList, View.OnClickListener onItemClickListener) {
        this.parkingList = parkingList;
        this.onItemClickListener = onItemClickListener;
    }

    @NonNull
    @Override
    public ParkingViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.parking_list_item_layout, parent, false);
        return new ParkingViewHolder(view, onItemClickListener);
    }

    @Override
    public void onBindViewHolder(@NonNull ParkingViewHolder holder, int position) {
        Parking parking = parkingList.get(position);
        holder.titleTextView.setText(parking.getTitle());
        holder.descriptionTextView.setText(parking.getDescription());
    }

    @Override
    public int getItemCount() {
        return parkingList.size();
    }

    public static class ParkingViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
        TextView titleTextView, descriptionTextView;
        View.OnClickListener onItemClickListener;

        public ParkingViewHolder(@NonNull View itemView, View.OnClickListener onItemClickListener) {
            super(itemView);
            titleTextView = itemView.findViewById(R.id.textViewTitle);
            descriptionTextView = itemView.findViewById(R.id.textViewDescription);
            this.onItemClickListener = onItemClickListener;
            itemView.setOnClickListener(this);
        }

        @Override
        public void onClick(View v) {
            onItemClickListener.onClick(v);
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    public void setParkingList(List<Parking> parkingList) {
        this.parkingList = parkingList;
        notifyDataSetChanged();
    }

    public Parking getParkingAt(int position) {
        return parkingList.get(position);
    }

    public void removeItem(int position) {
        parkingList.remove(position);
        notifyItemRemoved(position);
    }
}
