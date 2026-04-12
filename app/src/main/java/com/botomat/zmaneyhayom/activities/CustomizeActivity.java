package com.botomat.zmaneyhayom.activities;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.preference.PreferenceManager;

import com.botomat.zmaneyhayom.R;
import com.botomat.zmaneyhayom.utils.ThemeHelper;
import com.google.android.material.appbar.MaterialToolbar;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;

public class CustomizeActivity extends AppCompatActivity {

    private static final int PICK_BG_IMAGE = 1001;
    private static final int PICK_ICON_IMAGE = 1002;
    private SharedPreferences prefs;

    private View previewPrimary, previewAccent, previewBg;
    private TextView txtPrimary, txtAccent, txtBg;
    private ImageView bgPreview;
    private TextView fontPreview;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        ThemeHelper.applyTheme(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_customize);

        prefs = PreferenceManager.getDefaultSharedPreferences(this);

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());

        previewPrimary = findViewById(R.id.preview_primary_color);
        previewAccent = findViewById(R.id.preview_accent_color);
        previewBg = findViewById(R.id.preview_bg_color);
        txtPrimary = findViewById(R.id.txt_primary_color);
        txtAccent = findViewById(R.id.txt_accent_color);
        txtBg = findViewById(R.id.txt_bg_color);
        bgPreview = findViewById(R.id.bg_preview);
        fontPreview = findViewById(R.id.font_size_preview);

        loadCurrentValues();
        setupListeners();
    }

    private void loadCurrentValues() {
        String primaryColor = prefs.getString("custom_primary_color", "#1565C0");
        String accentColor = prefs.getString("custom_accent_color", "#FF6F00");
        String bgColor = prefs.getString("custom_bg_color", "#FAFAFA");
        int fontScale = prefs.getInt("font_scale", 2);

        setColorPreview(previewPrimary, txtPrimary, primaryColor);
        setColorPreview(previewAccent, txtAccent, accentColor);
        setColorPreview(previewBg, txtBg, bgColor);

        SeekBar fontSeekbar = findViewById(R.id.seekbar_font_size);
        fontSeekbar.setProgress(fontScale);
        updateFontPreview(fontScale);

        // Load BG image preview
        File bgFile = new File(getFilesDir(), "custom_bg.jpg");
        if (bgFile.exists()) {
            bgPreview.setVisibility(View.VISIBLE);
            bgPreview.setImageBitmap(BitmapFactory.decodeFile(bgFile.getAbsolutePath()));
        }
    }

    private void setupListeners() {
        // Background image
        findViewById(R.id.btn_pick_bg).setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
            intent.setType("image/*");
            startActivityForResult(Intent.createChooser(intent, getString(R.string.choose_image)), PICK_BG_IMAGE);
        });

        findViewById(R.id.btn_clear_bg).setOnClickListener(v -> {
            File bgFile = new File(getFilesDir(), "custom_bg.jpg");
            if (bgFile.exists()) bgFile.delete();
            prefs.edit().remove("has_custom_bg").apply();
            bgPreview.setVisibility(View.GONE);
        });

        // Colors
        findViewById(R.id.row_primary_color).setOnClickListener(v ->
                showColorPicker("custom_primary_color", "#1565C0", previewPrimary, txtPrimary));

        findViewById(R.id.row_accent_color).setOnClickListener(v ->
                showColorPicker("custom_accent_color", "#FF6F00", previewAccent, txtAccent));

        findViewById(R.id.row_bg_color).setOnClickListener(v ->
                showColorPicker("custom_bg_color", "#FAFAFA", previewBg, txtBg));

        // Reset colors
        findViewById(R.id.btn_reset_colors).setOnClickListener(v -> {
            prefs.edit()
                    .remove("custom_primary_color")
                    .remove("custom_accent_color")
                    .remove("custom_bg_color")
                    .apply();
            loadCurrentValues();
        });

        // Font size
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

        // App icon
        findViewById(R.id.btn_pick_icon).setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
            intent.setType("image/*");
            startActivityForResult(Intent.createChooser(intent, getString(R.string.choose_icon)), PICK_ICON_IMAGE);
        });
    }

    private void showColorPicker(String prefKey, String defaultColor, View preview, TextView hexText) {
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_color_picker, null);

        View colorPreview = dialogView.findViewById(R.id.color_preview);
        SeekBar seekR = dialogView.findViewById(R.id.seekbar_red);
        SeekBar seekG = dialogView.findViewById(R.id.seekbar_green);
        SeekBar seekB = dialogView.findViewById(R.id.seekbar_blue);
        TextView txtR = dialogView.findViewById(R.id.txt_red);
        TextView txtG = dialogView.findViewById(R.id.txt_green);
        TextView txtB = dialogView.findViewById(R.id.txt_blue);
        EditText editHex = dialogView.findViewById(R.id.edit_hex);

        String currentColor = prefs.getString(prefKey, defaultColor);
        int color = Color.parseColor(currentColor);
        seekR.setProgress(Color.red(color));
        seekG.setProgress(Color.green(color));
        seekB.setProgress(Color.blue(color));
        colorPreview.setBackgroundColor(color);
        editHex.setText(currentColor);

        // Add preset colors
        int[] presets = {
            0xFF1565C0, 0xFF0D47A1, 0xFF2196F3, 0xFF00BCD4,
            0xFF4CAF50, 0xFF8BC34A, 0xFFFF6F00, 0xFFFF5722,
            0xFFF44336, 0xFFE91E63, 0xFF9C27B0, 0xFF673AB7,
            0xFF212121, 0xFF795548, 0xFF607D8B, 0xFFFFFFFF
        };
        android.widget.LinearLayout presetsContainer = dialogView.findViewById(R.id.preset_colors);
        for (int preset : presets) {
            View swatch = new View(this);
            android.widget.LinearLayout.LayoutParams lp = new android.widget.LinearLayout.LayoutParams(36, 36);
            lp.setMargins(4, 0, 4, 0);
            swatch.setLayoutParams(lp);
            swatch.setBackgroundColor(preset);
            swatch.setFocusable(true);
            swatch.setOnClickListener(sv -> {
                seekR.setProgress(Color.red(preset));
                seekG.setProgress(Color.green(preset));
                seekB.setProgress(Color.blue(preset));
            });
            presetsContainer.addView(swatch);
        }

        SeekBar.OnSeekBarChangeListener listener = new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                int c = Color.rgb(seekR.getProgress(), seekG.getProgress(), seekB.getProgress());
                colorPreview.setBackgroundColor(c);
                txtR.setText(String.valueOf(seekR.getProgress()));
                txtG.setText(String.valueOf(seekG.getProgress()));
                txtB.setText(String.valueOf(seekB.getProgress()));
                editHex.setText(String.format("#%06X", 0xFFFFFF & c));
            }
            @Override public void onStartTrackingTouch(SeekBar s) {}
            @Override public void onStopTrackingTouch(SeekBar s) {}
        };
        seekR.setOnSeekBarChangeListener(listener);
        seekG.setOnSeekBarChangeListener(listener);
        seekB.setOnSeekBarChangeListener(listener);

        new AlertDialog.Builder(this)
                .setTitle(R.string.pick_color)
                .setView(dialogView)
                .setPositiveButton(R.string.save, (dialog, which) -> {
                    String hex = editHex.getText().toString().trim();
                    if (!hex.startsWith("#")) hex = "#" + hex;
                    try {
                        Color.parseColor(hex);
                        prefs.edit().putString(prefKey, hex).apply();
                        setColorPreview(preview, hexText, hex);
                    } catch (Exception e) {
                        // Invalid color, ignore
                    }
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    private void setColorPreview(View preview, TextView hexText, String colorHex) {
        try {
            GradientDrawable shape = new GradientDrawable();
            shape.setShape(GradientDrawable.RECTANGLE);
            shape.setCornerRadius(4);
            shape.setColor(Color.parseColor(colorHex));
            shape.setStroke(1, Color.GRAY);
            preview.setBackground(shape);
            hexText.setText(colorHex);
        } catch (Exception ignored) {}
    }

    private void updateFontPreview(int scale) {
        float[] sizes = {11f, 13f, 15f, 17f, 20f};
        fontPreview.setTextSize(sizes[Math.min(scale, sizes.length - 1)]);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode != Activity.RESULT_OK || data == null) return;

        Uri uri = data.getData();
        if (uri == null) return;

        try {
            InputStream is = getContentResolver().openInputStream(uri);
            Bitmap bitmap = BitmapFactory.decodeStream(is);
            is.close();

            if (requestCode == PICK_BG_IMAGE) {
                File file = new File(getFilesDir(), "custom_bg.jpg");
                FileOutputStream fos = new FileOutputStream(file);
                bitmap.compress(Bitmap.CompressFormat.JPEG, 85, fos);
                fos.close();
                prefs.edit().putBoolean("has_custom_bg", true).apply();
                bgPreview.setVisibility(View.VISIBLE);
                bgPreview.setImageBitmap(bitmap);
            } else if (requestCode == PICK_ICON_IMAGE) {
                File file = new File(getFilesDir(), "custom_icon.png");
                FileOutputStream fos = new FileOutputStream(file);
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, fos);
                fos.close();
                prefs.edit().putBoolean("has_custom_icon", true).apply();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
