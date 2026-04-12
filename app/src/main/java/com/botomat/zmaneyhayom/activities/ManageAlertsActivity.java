package com.botomat.zmaneyhayom.activities;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.botomat.zmaneyhayom.R;
import com.botomat.zmaneyhayom.adapters.AlertsAdapter;
import com.botomat.zmaneyhayom.database.DatabaseHelper;
import com.botomat.zmaneyhayom.models.AlertRule;
import com.botomat.zmaneyhayom.models.OffsetType;
import com.botomat.zmaneyhayom.models.ZmanType;
import com.botomat.zmaneyhayom.utils.AlarmScheduler;
import com.botomat.zmaneyhayom.utils.ThemeHelper;
import com.botomat.zmaneyhayom.utils.ZmanimCalculator;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.List;

public class ManageAlertsActivity extends AppCompatActivity implements AlertsAdapter.OnAlertActionListener {

    private AlertsAdapter adapter;
    private DatabaseHelper db;
    private TextView emptyText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        ThemeHelper.applyTheme(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_manage_alerts);

        db = DatabaseHelper.getInstance(this);

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());

        emptyText = findViewById(R.id.empty_text);

        RecyclerView recyclerView = findViewById(R.id.alerts_list);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new AlertsAdapter();
        adapter.setListener(this);
        recyclerView.setAdapter(adapter);

        FloatingActionButton fab = findViewById(R.id.fab_add);
        fab.setOnClickListener(v -> showAddAlertDialog());

        loadAlerts();
    }

    private void loadAlerts() {
        List<AlertRule> rules = db.getAllAlertRules();
        adapter.setAlerts(rules);
        emptyText.setVisibility(rules.isEmpty() ? View.VISIBLE : View.GONE);
    }

    private void showAddAlertDialog() {
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_add_alert, null);

        Spinner zmanSpinner = dialogView.findViewById(R.id.spinner_zman);
        Spinner offsetSpinner = dialogView.findViewById(R.id.spinner_offset_type);
        EditText offsetValue = dialogView.findViewById(R.id.edit_offset_value);
        CheckBox soundCheck = dialogView.findViewById(R.id.check_sound);
        CheckBox vibrateCheck = dialogView.findViewById(R.id.check_vibrate);

        // Setup zman spinner
        String[] zmanNames = getResources().getStringArray(R.array.zman_entries);
        ArrayAdapter<String> zmanAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, zmanNames);
        zmanAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        zmanSpinner.setAdapter(zmanAdapter);

        // Setup offset type spinner
        String[] offsetTypes = getResources().getStringArray(R.array.offset_type_entries);
        ArrayAdapter<String> offsetAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, offsetTypes);
        offsetAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        offsetSpinner.setAdapter(offsetAdapter);

        new AlertDialog.Builder(this)
                .setTitle(R.string.add_alert)
                .setView(dialogView)
                .setPositiveButton(R.string.save, (dialog, which) -> {
                    String[] zmanValues = getResources().getStringArray(R.array.zman_values);
                    String[] offsetValues = getResources().getStringArray(R.array.offset_type_values);

                    AlertRule rule = new AlertRule();
                    rule.setZmanType(ZmanType.fromString(zmanValues[zmanSpinner.getSelectedItemPosition()]));
                    rule.setOffsetType(OffsetType.fromString(offsetValues[offsetSpinner.getSelectedItemPosition()]));

                    String offsetStr = offsetValue.getText().toString().trim();
                    rule.setOffsetMinutes(offsetStr.isEmpty() ? 0 : Integer.parseInt(offsetStr));
                    rule.setSoundEnabled(soundCheck.isChecked());
                    rule.setVibrateEnabled(vibrateCheck.isChecked());
                    rule.setEnabled(true);

                    long id = db.insertAlertRule(rule);
                    rule.setId(id);

                    // Schedule the new alarm
                    AlarmScheduler.scheduleAlarm(this, rule, new ZmanimCalculator(this));

                    loadAlerts();
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    @Override
    public void onToggle(AlertRule rule, boolean enabled) {
        rule.setEnabled(enabled);
        db.updateAlertRule(rule);

        if (enabled) {
            AlarmScheduler.scheduleAlarm(this, rule, new ZmanimCalculator(this));
        } else {
            AlarmScheduler.cancelAlarm(this, rule.getId());
        }
    }

    @Override
    public void onLongClick(AlertRule rule, int position) {
        new AlertDialog.Builder(this)
                .setTitle(R.string.confirm_delete)
                .setMessage(rule.getDisplayText())
                .setPositiveButton(R.string.delete, (dialog, which) -> {
                    AlarmScheduler.cancelAlarm(this, rule.getId());
                    db.deleteAlertRule(rule.getId());
                    loadAlerts();
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }
}
