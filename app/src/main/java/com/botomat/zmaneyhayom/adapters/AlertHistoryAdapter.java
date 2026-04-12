package com.botomat.zmaneyhayom.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.botomat.zmaneyhayom.R;
import com.botomat.zmaneyhayom.models.AlertHistoryItem;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class AlertHistoryAdapter extends RecyclerView.Adapter<AlertHistoryAdapter.HistoryViewHolder> {

    private List<AlertHistoryItem> items = new ArrayList<>();
    private final SimpleDateFormat dateTimeFormat =
            new SimpleDateFormat("dd/MM HH:mm", Locale.getDefault());

    public void setItems(List<AlertHistoryItem> items) {
        this.items = items;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public HistoryViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_alert_history, parent, false);
        return new HistoryViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull HistoryViewHolder holder, int position) {
        holder.bind(items.get(position));
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    class HistoryViewHolder extends RecyclerView.ViewHolder {
        private final TextView zmanName;
        private final TextView details;
        private final TextView time;

        HistoryViewHolder(@NonNull View itemView) {
            super(itemView);
            zmanName = itemView.findViewById(R.id.history_zman_name);
            details = itemView.findViewById(R.id.history_details);
            time = itemView.findViewById(R.id.history_time);
        }

        void bind(AlertHistoryItem item) {
            zmanName.setText(item.getZmanType().getHebrewName());
            details.setText(item.getDisplayDetails());
            time.setText(dateTimeFormat.format(item.getAlertTime()));
        }
    }
}
