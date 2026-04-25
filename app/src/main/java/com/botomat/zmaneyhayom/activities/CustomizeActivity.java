package com.botomat.zmaneyhayom.activities;

import android.content.Intent;
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

    private static final int PICK_AUDIO_FILE = 5001;

    private void showRingtonePicker() {
        final String[] options = {"צלצולי המכשיר", "בחר קובץ אודיו מהמכשיר", "צלצול ברירת מחדל"};
        new AlertDialog.Builder(this)
                .setTitle(R.string.choose_ringtone)
                .setItems(options, new android.content.DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(android.content.DialogInterface dialog, int which) {
                        if (which == 0) {
                            showSystemRingtones();
                        } else if (which == 1) {
                            pickAudioFile();
                        } else {
                            prefs.edit().remove("custom_ringtone").apply();
                            updateRingtoneLabel();
                        }
                    }
                })
                .show();
    }

    private void pickAudioFile() {
        try {
            Intent intent;
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.KITKAT) {
                // Use ACTION_OPEN_DOCUMENT for persistable URI permissions
                intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
                intent.addCategory(Intent.CATEGORY_OPENABLE);
                intent.setType("audio/*");
                intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.KITKAT) {
                    intent.addFlags(Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);
                }
            } else {
                intent = new Intent(Intent.ACTION_GET_CONTENT);
                intent.addCategory(Intent.CATEGORY_OPENABLE);
                intent.setType("audio/*");
            }
            startActivityForResult(Intent.createChooser(intent, "בחר קובץ אודיו"), PICK_AUDIO_FILE);
        } catch (Exception e) {
            android.widget.Toast.makeText(this, "לא ניתן לפתוח בורר קבצים",
                    android.widget.Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_AUDIO_FILE && resultCode == RESULT_OK && data != null) {
            Uri uri = data.getData();
            if (uri != null) {
                // Try to persist URI permission (works only with ACTION_OPEN_DOCUMENT)
                try {
                    getContentResolver().takePersistableUriPermission(
                            uri, Intent.FLAG_GRANT_READ_URI_PERMISSION);
                } catch (Exception ignored) {}

                // Copy file to internal storage so alarm can play it later
                String localPath = copyAudioToInternalStorage(uri);
                if (localPath != null) {
                    prefs.edit().putString("custom_ringtone", "file://" + localPath).apply();
                    updateRingtoneLabel();
                    previewUri(Uri.parse("file://" + localPath));
                } else {
                    // Fallback - save the URI as-is
                    prefs.edit().putString("custom_ringtone", uri.toString()).apply();
                    updateRingtoneLabel();
                    previewUri(uri);
                }
            }
        }
    }

    private String copyAudioToInternalStorage(Uri uri) {
        try {
            java.io.File outFile = new java.io.File(getFilesDir(), "custom_alarm.audio");
            if (outFile.exists()) outFile.delete();

            java.io.InputStream in = getContentResolver().openInputStream(uri);
            if (in == null) return null;

            java.io.FileOutputStream out = new java.io.FileOutputStream(outFile);
            byte[] buf = new byte[4096];
            int n;
            while ((n = in.read(buf)) > 0) {
                out.write(buf, 0, n);
            }
            in.close();
            out.close();
            return outFile.getAbsolutePath();
        } catch (Exception e) {
            android.util.Log.e("CustomizeActivity", "Failed to copy audio", e);
            return null;
        }
    }

    private void showSystemRingtones() {
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
                        previewUri(selectedUri);
                    }
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    private void previewUri(final Uri uri) {
        try {
            if (previewPlayer != null) previewPlayer.release();
            previewPlayer = new MediaPlayer();
            previewPlayer.setDataSource(CustomizeActivity.this, uri);
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
