package com.example.smartclassapp;

import android.telecom.Call;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class DeviceListAdapter extends RecyclerView.Adapter<DeviceListAdapter.DeviceViewHolder> {

    List<String> deviceNames;
    private ItemClickListner mitemClickListner;

    public DeviceListAdapter(List<String> deviceNames,ItemClickListner itemClickListner) {
        this.deviceNames = deviceNames;
        this.mitemClickListner=itemClickListner;
    }

    @NonNull
    @Override
    public DeviceViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.list_item_device, parent, false);
        return new DeviceViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull DeviceViewHolder holder, int position) {
        String deviceName = deviceNames.get(position);
        holder.deviceNameTextView.setText(deviceName);

        holder.itemView.setOnClickListener(view -> {
            mitemClickListner.onItemClick(deviceNames.get(position));
        });

    }

    @Override
    public int getItemCount() {
        return deviceNames.size();
    }

    public interface ItemClickListner{
        void onItemClick(String details);
    }

    public static class DeviceViewHolder extends RecyclerView.ViewHolder {
        TextView deviceNameTextView;

        public DeviceViewHolder(View itemView) {
            super(itemView);
            deviceNameTextView = itemView.findViewById(R.id.device_name);
        }
    }
}

