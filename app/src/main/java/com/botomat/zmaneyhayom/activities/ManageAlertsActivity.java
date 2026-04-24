package com.botomat.zmaneyhayom.activities;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.CheckBox;
import android.widget.EditText;
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
import com.google.android.material.button.MaterialButton;
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
        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { finish(); }
        });

        emptyText = findViewById(R.id.empty_text);

        RecyclerView recyclerView = findViewById(R.id.alerts_list);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new AlertsAdapter();
        adapter.setListener(this);
        recyclerView.setAdapter(adapter);

        FloatingActionButton fab = findViewById(R.id.fab_add);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { showAlertDialog(null); }
        });

        loadAlerts();
    }

    private void loadAlerts() {
        List<AlertRule> rules = db.getAllAlertRules();
        adapter.setAlerts(rules);
        emptyText.setVisibility(rules.isEmpty() ? View.VISIBLE : View.GONE);
    }

    private void showAlertDialog(final AlertRule existingRule) {
        final boolean isEdit = existingRule != null;
        final View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_add_alert, null);

        final TextView txtZman = dialogView.findViewById(R.id.txt_zman);
        final TextView txtOffsetType = dialogView.findViewById(R.id.txt_offset_type);
        final EditText offsetValue = dialogView.findViewById(R.id.edit_offset_value);
        final CheckBox soundCheck = dialogView.findViewById(R.id.check_sound);
        final CheckBox vibrateCheck = dialogView.findViewById(R.id.check_vibrate);

        final CheckBox[] dayBoxes = new CheckBox[]{
                (CheckBox) dialogView.findViewById(R.id.day_sun),
                (CheckBox) dialogView.findViewById(R.id.day_mon),
                (CheckBox) dialogView.findViewById(R.id.day_tue),
                (CheckBox) dialogView.findViewById(R.id.day_wed),
                (CheckBox) dialogView.findViewById(R.id.day_thu),
                (CheckBox) dialogView.findViewById(R.id.day_fri),
                (CheckBox) dialogView.findViewById(R.id.day_sat)
        };

        final String[] zmanNames = getResources().getStringArray(R.array.zman_entries);
        final String[] zmanValues = getResources().getStringArray(R.array.zman_values);
        final String[] offsetTypes = getResources().getStringArray(R.array.offset_type_entries);
        final String[] offsetTypeValues = getResources().getStringArray(R.array.offset_type_values);

        // Track current selections
        final int[] selectedZmanIdx = {0};
        final int[] selectedOffsetIdx = {0};

        if (isEdit) {
            for (int i = 0; i < zmanValues.length; i++) {
                if (zmanValues[i].equals(existingRule.getZmanType().name())) {
                    selectedZmanIdx[0] = i;
                    break;
                }
            }
            for (int i = 0; i < offsetTypeValues.length; i++) {
                if (offsetTypeValues[i].equals(existingRule.getOffsetType().getValue())) {
                    selectedOffsetIdx[0] = i;
                    break;
                }
            }
            offsetValue.setText(String.valueOf(existingRule.getOffsetMinutes()));
            soundCheck.setChecked(existingRule.isSoundEnabled());
            vibrateCheck.setChecked(existingRule.isVibrateEnabled());
            int mask = existingRule.getDaysMask();
            for (int i = 0; i < 7; i++) {
                dayBoxes[i].setChecked((mask & (1 << i)) != 0);
            }
        }

        txtZman.setText(zmanNames[selectedZmanIdx[0]]);
        txtOffsetType.setText(offsetTypes[selectedOffsetIdx[0]]);

        // Click on Zman row -> show selection dialog
        dialogView.findViewById(R.id.row_zman).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new AlertDialog.Builder(ManageAlertsActivity.this)
                        .setTitle("בחר זמן")
                        .setSingleChoiceItems(zmanNames, selectedZmanIdx[0],
                                new android.content.DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(android.content.DialogInterface d, int which) {
                                selectedZmanIdx[0] = which;
                                txtZman.setText(zmanNames[which]);
                                d.dismiss();
                            }
                        })
                        .show();
            }
        });

        // Click on offset type row -> show selection dialog
        dialogView.findViewById(R.id.row_offset_type).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new AlertDialog.Builder(ManageAlertsActivity.this)
                        .setTitle("סוג היסט")
                        .setSingleChoiceItems(offsetTypes, selectedOffsetIdx[0],
                                new android.content.DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(android.content.DialogInterface d, int which) {
                                selectedOffsetIdx[0] = which;
                                txtOffsetType.setText(offsetTypes[which]);
                                d.dismiss();
                            }
                        })
                        .show();
            }
        });

        // Preset buttons for days
        dialogView.findViewById(R.id.preset_all_days).setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                for (CheckBox cb : dayBoxes) cb.setChecked(true);
            }
        });
        dialogView.findViewById(R.id.preset_weekdays).setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                dayBoxes[0].setChecked(true); dayBoxes[1].setChecked(true);
                dayBoxes[2].setChecked(true); dayBoxes[3].setChecked(true);
                dayBoxes[4].setChecked(true);
                dayBoxes[5].setChecked(false); dayBoxes[6].setChecked(false);
            }
        });
        dialogView.findViewById(R.id.preset_sun_fri).setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                for (int i = 0; i < 6; i++) dayBoxes[i].setChecked(true);
                dayBoxes[6].setChecked(false);
            }
        });

        new AlertDialog.Builder(this)
                .setTitle(isEdit ? R.string.edit_alert : R.string.add_alert)
                .setView(dialogView)
                .setPositiveButton(R.string.save, new android.content.DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(android.content.DialogInterface dialog, int which) {
                        AlertRule rule = isEdit ? existingRule : new AlertRule();
                        rule.setZmanType(ZmanType.fromString(zmanValues[selectedZmanIdx[0]]));
                        rule.setOffsetType(OffsetType.fromString(offsetTypeValues[selectedOffsetIdx[0]]));

                        String offsetStr = offsetValue.getText().toString().trim();
                        rule.setOffsetMinutes(offsetStr.isEmpty() ? 0 : Integer.parseInt(offsetStr));
                        rule.setSoundEnabled(soundCheck.isChecked());
                        rule.setVibrateEnabled(vibrateCheck.isChecked());
                        rule.setEnabled(true);

                        int mask = 0;
                        for (int i = 0; i < 7; i++) {
                            if (dayBoxes[i].isChecked()) mask |= (1 << i);
                        }
                        if (mask == 0) mask = AlertRule.DAYS_ALL;
                        rule.setDaysMask(mask);

                        if (isEdit) {
                            db.updateAlertRule(rule);
                            AlarmScheduler.cancelAlarm(ManageAlertsActivity.this, rule.getId());
                        } else {
                            long id = db.insertAlertRule(rule);
                            rule.setId(id);
                        }

                        AlarmScheduler.scheduleAlarm(ManageAlertsActivity.this, rule,
                                new ZmanimCalculator(ManageAlertsActivity.this));
                        loadAlerts();
                    }
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
    public void onLongClick(final AlertRule rule, int position) {
        String[] options = {getString(R.string.edit), getString(R.string.delete)};
        new AlertDialog.Builder(this)
                .setTitle(rule.getZmanType().getHebrewName())
                .setItems(options, new android.content.DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(android.content.DialogInterface dialog, int which) {
                        if (which == 0) {
                            showAlertDialog(rule);
                        } else {
                            new AlertDialog.Builder(ManageAlertsActivity.this)
                                    .setTitle(R.string.confirm_delete)
                                    .setMessage(rule.getDisplayText())
                                    .setPositiveButton(R.string.delete,
                                            new android.content.DialogInterface.OnClickListener() {
                                                @Override
                                                public void onClick(android.content.DialogInterface d, int w) {
                                                    AlarmScheduler.cancelAlarm(
                                                            ManageAlertsActivity.this, rule.getId());
                                                    db.deleteAlertRule(rule.getId());
                                                    loadAlerts();
                                                }
                                            })
                                    .setNegativeButton(R.string.cancel, null)
                                    .show();
                        }
                    }
                })
                .show();
    }
}
