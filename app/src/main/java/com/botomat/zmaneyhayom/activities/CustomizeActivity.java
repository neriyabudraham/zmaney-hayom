package com.botomat.zmaneyhayom.activities;

import android.content.SharedPreferences;
import android.database.Cursor;
import android.media.MediaPlayer;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.SeekBar;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.preference.PreferenceManager;

import com.botomat.zmaneyhayom.R;
import com.botomat.zmaneyhayom.utils.ThemeHelper;
import com.google.android.material.appbar.MaterialToolbar;

import java.util.ArrayList;
import java.util.List;

public class CustomizeActivity extends AppCompatActivity {

    private SharedPreferences prefs;
    private TextView fontPreview;
    private TextView currentRingtoneText;
    private MediaPlayer previewPlayer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        ThemeHelper.applyTheme(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_customize);

        prefs = PreferenceManager.getDefaultSharedPreferences(this);

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { finish(); }
        });

        fontPreview = findViewById(R.id.font_size_preview);
        currentRingtoneText = findViewById(R.id.txt_current_ringtone);

        loadCurrentValues();
        setupListeners();
    }

    private void loadCurrentValues() {
        int fontScale = prefs.getInt("font_scale", 2);
        SeekBar fontSeekbar = findViewById(R.id.seekbar_font_size);
        fontSeekbar.setProgress(fontScale);
        updateFontPreview(fontScale);

        updateRingtoneLabel();
    }

    private void updateRingtoneLabel() {
        String uri = prefs.getString("custom_ringtone", null);
        if (uri == null) {
            currentRingtoneText.setText(getString(R.string.default_ringtone));
        } else {
            try {
                android.media.Ringtone r = RingtoneManager.getRingtone(this, Uri.parse(uri));
                if (r != null) {
                    currentRingtoneText.setText(r.getTitle(this));
                } else {
                    currentRingtoneText.setText(getString(R.string.custom_ringtone));
                }
            } catch (Exception e) {
                currentRingtoneText.setText(getString(R.string.custom_ringtone));
            }
        }
    }

    private void setupListeners() {
        findViewById(R.id.row_ringtone).setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { showRingtonePicker(); }
        });

        SeekBar fontSeekbar = findViewById(R.id.seekbar_font_size);
        fontSeekbar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                updateFontPreview(progress);
                if (fromUser) prefs.edit().putInt("font_scale", progress).apply();
            }
            @Override public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override public void onStopTrackingTouch(SeekBar seekBar) {}
        });
    }

    private void showRingtonePicker() {
        RingtoneManager manager = new RingtoneManager(this);
        manager.setType(RingtoneManager.TYPE_ALARM | RingtoneManager.TYPE_RINGTONE
                | RingtoneManager.TYPE_NOTIFICATION);
        Cursor cursor = manager.getCursor();

        final List<String> names = new ArrayList<>();
        final List<Uri> uris = new ArrayList<>();
        names.add(getString(R.string.default_ringtone));
        uris.add(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM));

        while (cursor.moveToNext()) {
            String title = cursor.getString(RingtoneManager.TITLE_COLUMN_INDEX);
            Uri uri = manager.getRingtoneUri(cursor.getPosition());
            names.add(title);
            uris.add(uri);
        }

        new AlertDialog.Builder(this)
                .setTitle(R.string.choose_ringtone)
                .setItems(names.toArray(new String[0]), new android.content.DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(android.content.DialogInterface dialog, int which) {
                        final Uri selectedUri = uris.get(which);
                        if (which == 0) {
                            prefs.edit().remove("custom_ringtone").apply();
                        } else {
                            prefs.edit().putString("custom_ringtone", selectedUri.toString()).apply();
                        }
                        updateRingtoneLabel();

                        // Preview
                        try {
                            if (previewPlayer != null) previewPlayer.release();
                            previewPlayer = new MediaPlayer();
                            previewPlayer.setDataSource(CustomizeActivity.this, selectedUri);
                            previewPlayer.prepare();
                            previewPlayer.start();
                            new Handler().postDelayed(new Runnable() {
                                @Override public void run() {
                                    try {
                                        if (previewPlayer != null) {
                                            if (previewPlayer.isPlaying()) previewPlayer.stop();
                                            previewPlayer.release();
                                            previewPlayer = null;
                                        }
                                    } catch (Exception ignored) {}
                                }
                            }, 3000);
                        } catch (Exception ignored) {}
                    }
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    private void updateFontPreview(int scale) {
        float[] sizes = {11f, 13f, 15f, 17f, 20f};
        fontPreview.setTextSize(sizes[Math.min(scale, sizes.length - 1)]);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (previewPlayer != null) {
            try { previewPlayer.release(); } catch (Exception ignored) {}
            previewPlayer = null;
        }
    }
}
