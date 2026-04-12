package com.botomat.zmaneyhayom.adapters;

import android.graphics.Color;
import android.util.TypedValue;
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

    public static final int VIEW_LIST = 0;
    public static final int VIEW_CARDS = 1;
    public static final int VIEW_COMPACT = 2;

    private List<ZmanItem> zmanim = new ArrayList<>();
    private final SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm", Locale.getDefault());
    private int viewMode = VIEW_LIST;
    private int fontScale = 2; // 0-4, default 2 (normal)

    public void setZmanim(List<ZmanItem> zmanim) {
        this.zmanim = zmanim;
        notifyDataSetChanged();
    }

    public void setViewMode(int mode) {
        if (this.viewMode != mode) {
            this.viewMode = mode;
            notifyDataSetChanged();
        }
    }

    public void setFontScale(int scale) {
        if (this.fontScale != scale) {
            this.fontScale = scale;
            notifyDataSetChanged();
        }
    }

    private float getScaledSize(float baseSize) {
        float[] multipliers = {0.8f, 0.9f, 1.0f, 1.15f, 1.3f};
        return baseSize * multipliers[Math.min(fontScale, multipliers.length - 1)];
    }

    @Override
    public int getItemViewType(int position) {
        return viewMode;
    }

    @NonNull
    @Override
    public ZmanViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        int layoutRes;
        switch (viewType) {
            case VIEW_CARDS:
                layoutRes = R.layout.item_zman_card;
                break;
            case VIEW_COMPACT:
            case VIEW_LIST:
            default:
                layoutRes = R.layout.item_zman;
                break;
        }
        View view = LayoutInflater.from(parent.getContext()).inflate(layoutRes, parent, false);
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
        private final TextView statusView; // only in card mode

        ZmanViewHolder(@NonNull View itemView) {
            super(itemView);
            nameView = itemView.findViewById(R.id.zman_name);
            timeView = itemView.findViewById(R.id.zman_time);
            alertIndicator = itemView.findViewById(R.id.alert_indicator);
            statusView = itemView.findViewById(R.id.zman_status);
        }

        void bind(ZmanItem item) {
            nameView.setText(item.getName());

            if (item.getTime() != null) {
                timeView.setText(timeFormat.format(item.getTime()));
            } else {
                timeView.setText("--:--");
            }

            // Apply font scaling
            float nameSize = viewMode == VIEW_CARDS ? 15f : (viewMode == VIEW_COMPACT ? 12f : 14f);
            float timeSize = viewMode == VIEW_CARDS ? 18f : (viewMode == VIEW_COMPACT ? 12f : 15f);
            nameView.setTextSize(TypedValue.COMPLEX_UNIT_SP, getScaledSize(nameSize));
            timeView.setTextSize(TypedValue.COMPLEX_UNIT_SP, getScaledSize(timeSize));

            // Compact mode - reduce padding
            if (viewMode == VIEW_COMPACT) {
                int pad = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 3,
                        itemView.getContext().getResources().getDisplayMetrics());
                itemView.setPadding(itemView.getPaddingLeft(), pad, itemView.getPaddingRight(), pad);
            }

            // Color coding
            if (item.isPassed()) {
                nameView.setTextColor(Color.parseColor("#9E9E9E"));
                timeView.setTextColor(Color.parseColor("#9E9E9E"));
                if (viewMode == VIEW_COMPACT) {
                    nameView.setAlpha(0.6f);
                    timeView.setAlpha(0.6f);
                }
            } else if (item.isNext()) {
                nameView.setTextColor(Color.parseColor("#FF6F00"));
                timeView.setTextColor(Color.parseColor("#FF6F00"));
                nameView.setTextSize(TypedValue.COMPLEX_UNIT_SP, getScaledSize(nameSize + 1));
                timeView.setTextSize(TypedValue.COMPLEX_UNIT_SP, getScaledSize(timeSize + 1));
                nameView.setAlpha(1f);
                timeView.setAlpha(1f);
            } else {
                nameView.setTextColor(Color.parseColor("#212121"));
                timeView.setTextColor(Color.parseColor("#1565C0"));
                nameView.setAlpha(1f);
                timeView.setAlpha(1f);
            }

            // Alert indicator
            if (alertIndicator != null) {
                alertIndicator.setVisibility(item.hasAlert() ? View.VISIBLE : View.INVISIBLE);
            }

            // Status text (card mode only)
            if (statusView != null) {
                if (item.isNext()) {
                    statusView.setText("⟵ הזמן הבא");
                    statusView.setVisibility(View.VISIBLE);
                } else if (item.isPassed()) {
                    statusView.setText("עבר");
                    statusView.setVisibility(View.VISIBLE);
                } else {
                    statusView.setVisibility(View.GONE);
                }
            }
        }
    }
}
