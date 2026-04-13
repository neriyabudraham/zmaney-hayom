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

    public interface OnZmanClickListener {
        void onZmanClick(ZmanItem item);
    }

    private List<ZmanItem> zmanim = new ArrayList<>();
    private final SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm", Locale.getDefault());
    private int viewMode = VIEW_CARDS;
    private int fontScale = 2;
    private OnZmanClickListener clickListener;

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

    public void setOnZmanClickListener(OnZmanClickListener listener) {
        this.clickListener = listener;
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
        int layoutRes = viewType == VIEW_CARDS ? R.layout.item_zman_card : R.layout.item_zman;
        View view = LayoutInflater.from(parent.getContext()).inflate(layoutRes, parent, false);
        return new ZmanViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ZmanViewHolder holder, int position) {
        holder.bind(zmanim.get(position));
    }

    @Override
    public int getItemCount() {
        return zmanim.size();
    }

    class ZmanViewHolder extends RecyclerView.ViewHolder {
        private final TextView nameView;
        private final TextView timeView;
        private final View alertIndicator;
        private final TextView statusView;

        ZmanViewHolder(@NonNull View itemView) {
            super(itemView);
            nameView = itemView.findViewById(R.id.zman_name);
            timeView = itemView.findViewById(R.id.zman_time);
            alertIndicator = itemView.findViewById(R.id.alert_indicator);
            statusView = itemView.findViewById(R.id.zman_status);
        }

        void bind(ZmanItem item) {
            nameView.setText(item.getName());
            timeView.setText(item.getTime() != null ? timeFormat.format(item.getTime()) : "--:--");

            float nameSize = viewMode == VIEW_CARDS ? 15f : 14f;
            float timeSize = viewMode == VIEW_CARDS ? 18f : 15f;
            nameView.setTextSize(TypedValue.COMPLEX_UNIT_SP, getScaledSize(nameSize));
            timeView.setTextSize(TypedValue.COMPLEX_UNIT_SP, getScaledSize(timeSize));

            // Aqua Tech colors
            if (item.isPassed()) {
                nameView.setTextColor(Color.parseColor("#9CA3AF"));
                timeView.setTextColor(Color.parseColor("#9CA3AF"));
                nameView.setAlpha(0.7f);
                timeView.setAlpha(0.7f);
                if (viewMode == VIEW_CARDS) {
                    itemView.setBackgroundResource(0);
                }
            } else if (item.isNext()) {
                nameView.setTextColor(Color.parseColor("#0D9488"));
                timeView.setTextColor(Color.parseColor("#0D9488"));
                nameView.setTextSize(TypedValue.COMPLEX_UNIT_SP, getScaledSize(nameSize + 1));
                timeView.setTextSize(TypedValue.COMPLEX_UNIT_SP, getScaledSize(timeSize + 2));
                nameView.setAlpha(1f);
                timeView.setAlpha(1f);
                if (viewMode == VIEW_CARDS) {
                    itemView.setBackgroundResource(R.drawable.card_bg_highlight);
                }
            } else {
                nameView.setTextColor(Color.parseColor("#1F2937"));
                timeView.setTextColor(Color.parseColor("#0D9488"));
                nameView.setAlpha(1f);
                timeView.setAlpha(1f);
                if (viewMode == VIEW_CARDS) {
                    itemView.setBackgroundResource(0);
                }
            }

            // Alert indicator - only show dot, not alarm icon
            if (alertIndicator != null) {
                alertIndicator.setVisibility(item.hasAlert() ? View.VISIBLE : View.INVISIBLE);
            }

            // Status text (card mode)
            if (statusView != null) {
                if (item.isNext()) {
                    statusView.setText("הזמן הבא");
                    statusView.setTextColor(Color.parseColor("#0D9488"));
                    statusView.setVisibility(View.VISIBLE);
                } else if (item.isPassed()) {
                    statusView.setText("עבר");
                    statusView.setVisibility(View.VISIBLE);
                } else {
                    statusView.setVisibility(View.GONE);
                }
            }

            // Click to show countdown
            itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (clickListener != null) {
                        clickListener.onZmanClick(item);
                    }
                }
            });
        }
    }
}
