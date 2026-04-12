package com.botomat.zmaneyhayom.adapters;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.botomat.zmaneyhayom.R;
import com.botomat.zmaneyhayom.models.ZmanItem;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class ZmanimAdapter extends RecyclerView.Adapter<ZmanimAdapter.ZmanViewHolder> {

    private List<ZmanItem> zmanim = new ArrayList<>();
    private final SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm", Locale.getDefault());

    public void setZmanim(List<ZmanItem> zmanim) {
        this.zmanim = zmanim;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ZmanViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_zman, parent, false);
        return new ZmanViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ZmanViewHolder holder, int position) {
        ZmanItem item = zmanim.get(position);
        holder.bind(item);
    }

    @Override
    public int getItemCount() {
        return zmanim.size();
    }

    class ZmanViewHolder extends RecyclerView.ViewHolder {
        private final TextView nameView;
        private final TextView timeView;
        private final View alertIndicator;

        ZmanViewHolder(@NonNull View itemView) {
            super(itemView);
            nameView = itemView.findViewById(R.id.zman_name);
            timeView = itemView.findViewById(R.id.zman_time);
            alertIndicator = itemView.findViewById(R.id.alert_indicator);
        }

        void bind(ZmanItem item) {
            nameView.setText(item.getName());

            if (item.getTime() != null) {
                timeView.setText(timeFormat.format(item.getTime()));
            } else {
                timeView.setText("--:--");
            }

            // Color coding
            if (item.isPassed()) {
                nameView.setTextColor(Color.parseColor("#9E9E9E"));
                timeView.setTextColor(Color.parseColor("#9E9E9E"));
            } else if (item.isNext()) {
                nameView.setTextColor(Color.parseColor("#FF6F00"));
                timeView.setTextColor(Color.parseColor("#FF6F00"));
                nameView.setTextSize(14);
                timeView.setTextSize(15);
            } else {
                nameView.setTextColor(Color.parseColor("#212121"));
                timeView.setTextColor(Color.parseColor("#1565C0"));
            }

            // Alert indicator
            alertIndicator.setVisibility(item.hasAlert() ? View.VISIBLE : View.INVISIBLE);
        }
    }
}
