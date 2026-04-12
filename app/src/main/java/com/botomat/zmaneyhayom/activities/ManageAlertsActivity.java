package com.botomat.zmaneyhayom.activities;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
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

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class ManageAlertsActivity extends AppCompatActivity implements AlertsAdapter.OnAlertActionListener {

    private static final int PICK_RINGTONE = 2001;
    private static final int PICK_FILE_RINGTONE = 2002;
    private static final int PERMISSION_RECORD = 100;

    private AlertsAdapter adapter;
    private DatabaseHelper db;
    private TextView emptyText;

    private MediaRecorder recorder;
    private MediaPlayer player;
    private boolean isRecording = false;
    private String recordedFilePath;
    private AlertRule currentEditingRule;

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
        fab.setOnClickListener(v -> showAlertDialog(null));

        loadAlerts();
    }

    private void loadAlerts() {
        List<AlertRule> rules = db.getAllAlertRules();
        adapter.setAlerts(rules);
        emptyText.setVisibility(rules.isEmpty() ? View.VISIBLE : View.GONE);
    }

    private void showAlertDialog(AlertRule existingRule) {
        boolean isEdit = existingRule != null;
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_add_alert, null);

        Spinner zmanSpinner = dialogView.findViewById(R.id.spinner_zman);
        Spinner offsetSpinner = dialogView.findViewById(R.id.spinner_offset_type);
        EditText offsetValue = dialogView.findViewById(R.id.edit_offset_value);
        CheckBox soundCheck = dialogView.findViewById(R.id.check_sound);
        CheckBox vibrateCheck = dialogView.findViewById(R.id.check_vibrate);

        // Setup spinners
        String[] zmanNames = getResources().getStringArray(R.array.zman_entries);
        String[] zmanValues = getResources().getStringArray(R.array.zman_values);
        ArrayAdapter<String> zmanAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, zmanNames);
        zmanAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        zmanSpinner.setAdapter(zmanAdapter);

        String[] offsetTypes = getResources().getStringArray(R.array.offset_type_entries);
        String[] offsetTypeValues = getResources().getStringArray(R.array.offset_type_values);
        ArrayAdapter<String> offsetAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, offsetTypes);
        offsetAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        offsetSpinner.setAdapter(offsetAdapter);

        // Pre-fill for edit
        if (isEdit) {
            for (int i = 0; i < zmanValues.length; i++) {
                if (zmanValues[i].equals(existingRule.getZmanType().name())) {
                    zmanSpinner.setSelection(i);
                    break;
                }
            }
            for (int i = 0; i < offsetTypeValues.length; i++) {
                if (offsetTypeValues[i].equals(existingRule.getOffsetType().getValue())) {
                    offsetSpinner.setSelection(i);
                    break;
                }
            }
            offsetValue.setText(String.valueOf(existingRule.getOffsetMinutes()));
            soundCheck.setChecked(existingRule.isSoundEnabled());
            vibrateCheck.setChecked(existingRule.isVibrateEnabled());
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(this)
                .setTitle(isEdit ? R.string.edit_alert : R.string.add_alert)
                .setView(dialogView)
                .setPositiveButton(R.string.save, (dialog, which) -> {
                    AlertRule rule = isEdit ? existingRule : new AlertRule();
                    rule.setZmanType(ZmanType.fromString(zmanValues[zmanSpinner.getSelectedItemPosition()]));
                    rule.setOffsetType(OffsetType.fromString(offsetTypeValues[offsetSpinner.getSelectedItemPosition()]));

                    String offsetStr = offsetValue.getText().toString().trim();
                    rule.setOffsetMinutes(offsetStr.isEmpty() ? 0 : Integer.parseInt(offsetStr));
                    rule.setSoundEnabled(soundCheck.isChecked());
                    rule.setVibrateEnabled(vibrateCheck.isChecked());
                    rule.setEnabled(true);

                    if (isEdit) {
                        db.updateAlertRule(rule);
                        AlarmScheduler.cancelAlarm(this, rule.getId());
                    } else {
                        long id = db.insertAlertRule(rule);
                        rule.setId(id);
                    }

                    AlarmScheduler.scheduleAlarm(this, rule, new ZmanimCalculator(this));
                    loadAlerts();
                })
                .setNegativeButton(R.string.cancel, null);

        // Add ringtone button for the dialog
        builder.setNeutralButton(R.string.ringtone, (dialog, which) -> {
            currentEditingRule = isEdit ? existingRule : null;
            showRingtoneOptions();
        });

        builder.show();
    }

    private void showRingtoneOptions() {
        String[] options = {
                getString(R.string.device_ringtones),
                getString(R.string.upload_ringtone),
                getString(R.string.record_ringtone)
        };

        new AlertDialog.Builder(this)
                .setTitle(R.string.choose_ringtone)
                .setItems(options, (dialog, which) -> {
                    switch (which) {
                        case 0: // Device ringtones
                            showDeviceRingtones();
                            break;
                        case 1: // Upload file
                            Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                            intent.setType("audio/*");
                            startActivityForResult(Intent.createChooser(intent,
                                    getString(R.string.upload_ringtone)), PICK_FILE_RINGTONE);
                            break;
                        case 2: // Record
                            requestRecordPermissionAndStart();
                            break;
                    }
                })
                .show();
    }

    private void showDeviceRingtones() {
        RingtoneManager manager = new RingtoneManager(this);
        manager.setType(RingtoneManager.TYPE_ALARM | RingtoneManager.TYPE_RINGTONE | RingtoneManager.TYPE_NOTIFICATION);
        Cursor cursor = manager.getCursor();

        List<String> names = new ArrayList<>();
        List<Uri> uris = new ArrayList<>();
        names.add(getString(R.string.default_ringtone));
        uris.add(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM));

        while (cursor.moveToNext()) {
            String title = cursor.getString(RingtoneManager.TITLE_COLUMN_INDEX);
            Uri uri = manager.getRingtoneUri(cursor.getPosition());
            names.add(title);
            uris.add(uri);
        }

        new AlertDialog.Builder(this)
                .setTitle(R.string.device_ringtones)
                .setItems(names.toArray(new String[0]), (dialog, which) -> {
                    Uri selectedUri = uris.get(which);
                    saveRingtoneUri(selectedUri.toString());

                    // Preview
                    try {
                        if (player != null) {
                            player.release();
                        }
                        player = new MediaPlayer();
                        player.setDataSource(this, selectedUri);
                        player.prepare();
                        player.start();
                        // Auto stop after 3 seconds
                        new android.os.Handler().postDelayed(() -> {
                            if (player != null && player.isPlaying()) {
                                player.stop();
                                player.release();
                                player = null;
                            }
                        }, 3000);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                })
                .show();
    }

    private void requestRecordPermissionAndStart() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.RECORD_AUDIO}, PERMISSION_RECORD);
        } else {
            showRecordDialog();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_RECORD && grantResults.length > 0
                && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            showRecordDialog();
        } else {
            Toast.makeText(this, "נדרשת הרשאת הקלטה", Toast.LENGTH_SHORT).show();
        }
    }

    private void showRecordDialog() {
        recordedFilePath = new File(getFilesDir(), "recorded_ringtone.3gp").getAbsolutePath();

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle(R.string.record_ringtone)
                .setMessage(getString(R.string.recording))
                .setPositiveButton(R.string.stop_recording, null)
                .setNegativeButton(R.string.cancel, (d, w) -> stopRecording())
                .create();

        dialog.setOnShowListener(d -> {
            startRecording();
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
                stopRecording();
                saveRingtoneUri(recordedFilePath);
                Toast.makeText(this, "צלצול הוקלט בהצלחה", Toast.LENGTH_SHORT).show();
                dialog.dismiss();
            });
        });

        dialog.show();
    }

    private void startRecording() {
        try {
            recorder = new MediaRecorder();
            recorder.setAudioSource(MediaRecorder.AudioSource.MIC);
            recorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
            recorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);
            recorder.setOutputFile(recordedFilePath);
            recorder.prepare();
            recorder.start();
            isRecording = true;
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "שגיאה בהקלטה", Toast.LENGTH_SHORT).show();
        }
    }

    private void stopRecording() {
        if (isRecording && recorder != null) {
            try {
                recorder.stop();
                recorder.release();
            } catch (Exception ignored) {}
            recorder = null;
            isRecording = false;
        }
    }

    private void saveRingtoneUri(String uriString) {
        androidx.preference.PreferenceManager.getDefaultSharedPreferences(this)
                .edit()
                .putString("custom_ringtone", uriString)
                .apply();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == Activity.RESULT_OK && data != null && data.getData() != null) {
            if (requestCode == PICK_FILE_RINGTONE) {
                saveRingtoneUri(data.getData().toString());
                Toast.makeText(this, "צלצול נבחר בהצלחה", Toast.LENGTH_SHORT).show();
            }
        }
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
        String[] options = {getString(R.string.edit), getString(R.string.delete)};
        new AlertDialog.Builder(this)
                .setTitle(rule.getZmanType().getHebrewName())
                .setItems(options, (dialog, which) -> {
                    if (which == 0) {
                        // Edit
                        showAlertDialog(rule);
                    } else {
                        // Delete
                        new AlertDialog.Builder(this)
                                .setTitle(R.string.confirm_delete)
                                .setMessage(rule.getDisplayText())
                                .setPositiveButton(R.string.delete, (d, w) -> {
                                    AlarmScheduler.cancelAlarm(this, rule.getId());
                                    db.deleteAlertRule(rule.getId());
                                    loadAlerts();
                                })
                                .setNegativeButton(R.string.cancel, null)
                                .show();
                    }
                })
                .show();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopRecording();
        if (player != null) {
            player.release();
            player = null;
        }
    }
}
