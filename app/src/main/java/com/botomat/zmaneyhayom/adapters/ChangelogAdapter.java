package com.botomat.zmaneyhayom.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.botomat.zmaneyhayom.R;
import com.botomat.zmaneyhayom.utils.UpdateChecker;

import java.util.ArrayList;
import java.util.List;

public class ChangelogAdapter extends RecyclerView.Adapter<ChangelogAdapter.VH> {

    private List<UpdateChecker.VersionInfo> items = new ArrayList<>();
    private int currentVersionCode = 0;

    public void setData(List<UpdateChecker.VersionInfo> list, int currentVersionCode) {
        this.items = list != null ? list : new ArrayList<UpdateChecker.VersionInfo>();
        this.currentVersionCode = currentVersionCode;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_changelog, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int position) {
        UpdateChecker.VersionInfo vi = items.get(position);
        h.versionLabel.setText("גרסה " + vi.versionName);
        h.versionDate.setText(vi.date);
        h.currentMarker.setVisibility(vi.versionCode == currentVersionCode ? View.VISIBLE : View.GONE);

        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < vi.changes.size(); i++) {
            if (i > 0) sb.append('\n');
            sb.append("• ").append(vi.changes.get(i));
        }
        h.changesText.setText(sb.toString());
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class VH extends RecyclerView.ViewHolder {
        TextView versionLabel, versionDate, changesText, currentMarker;

        VH(@NonNull View itemView) {
            super(itemView);
            versionLabel = itemView.findViewById(R.id.version_label);
            versionDate = itemView.findViewById(R.id.version_date);
            changesText = itemView.findViewById(R.id.changes_text);
            currentMarker = itemView.findViewById(R.id.current_marker);
        }
    }
}
