package com.botomat.zmaneyhayom.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.SwitchCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.botomat.zmaneyhayom.R;
import com.botomat.zmaneyhayom.models.AlertRule;

import java.util.ArrayList;
import java.util.List;

public class AlertsAdapter extends RecyclerView.Adapter<AlertsAdapter.AlertViewHolder> {

    public interface OnAlertActionListener {
        void onToggle(AlertRule rule, boolean enabled);
        void onLongClick(AlertRule rule, int position);
    }

    private List<AlertRule> alerts = new ArrayList<>();
    private OnAlertActionListener listener;

    public void setAlerts(List<AlertRule> alerts) {
        this.alerts = alerts;
        notifyDataSetChanged();
    }

    public void setListener(OnAlertActionListener listener) {
        this.listener = listener;
    }

    @NonNull
    @Override
    public AlertViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_alert, parent, false);
        return new AlertViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull AlertViewHolder holder, int position) {
        holder.bind(alerts.get(position), position);
    }

    @Override
    public int getItemCount() {
        return alerts.size();
    }

    class AlertViewHolder extends RecyclerView.ViewHolder {
        private final TextView zmanName;
        private final TextView offsetText;
        private final TextView nextTime;
        private final SwitchCompat toggle;

        AlertViewHolder(@NonNull View itemView) {
            super(itemView);
            zmanName = itemView.findViewById(R.id.alert_zman_name);
            offsetText = itemView.findViewById(R.id.alert_offset_text);
            nextTime = itemView.findViewById(R.id.alert_next_time);
            toggle = itemView.findViewById(R.id.alert_switch);
        }

        void bind(AlertRule rule, int position) {
            zmanName.setText(rule.getZmanType().getHebrewName());
            offsetText.setText(rule.getDisplayText());
            toggle.setChecked(rule.isEnabled());

            toggle.setOnCheckedChangeListener((buttonView, isChecked) -> {
                if (listener != null) {
                    listener.onToggle(rule, isChecked);
                }
            });

            itemView.setOnLongClickListener(v -> {
                if (listener != null) {
                    listener.onLongClick(rule, position);
                }
                return true;
            });
        }
    }
}
