package com.aravinth.cochat;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import org.w3c.dom.Text;

public class customAdapter extends RecyclerView.Adapter<customAdapter.myViewHolder> {

    String name,address;
    Context context;
    public customAdapter(Context ct,String name,String address)
    {
        context = ct;
        this.name = name;
        this.address = address;
    }
    @NonNull
    @Override
    public myViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater layoutInflater = LayoutInflater.from(context);
        View view = layoutInflater.inflate(R.layout.list_available_device,parent,false);
        return new myViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull myViewHolder holder, int position) {
        holder.name.setText(name);
        holder.address.setText(address);

    }

    @Override
    public int getItemCount() {
        return 3;
    }

    public class myViewHolder extends RecyclerView.ViewHolder {
        TextView name,address;
        public myViewHolder(@NonNull View itemView) {
            super(itemView);
            name = itemView.findViewById(R.id.device_name);
            address = itemView.findViewById(R.id.device_address);
        }
    }
}
