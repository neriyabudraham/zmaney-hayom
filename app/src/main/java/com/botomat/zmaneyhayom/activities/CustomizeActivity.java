package com.botomat.zmaneyhayom.activities;

import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.SeekBar;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.preference.PreferenceManager;

import com.botomat.zmaneyhayom.R;
import com.botomat.zmaneyhayom.utils.ThemeHelper;
import com.google.android.material.appbar.MaterialToolbar;

public class CustomizeActivity extends AppCompatActivity {

    private SharedPreferences prefs;
    private View previewPrimary, previewAccent, previewBg;
    private TextView txtPrimary, txtAccent, txtBg;
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
        fontPreview = findViewById(R.id.font_size_preview);

        loadCurrentValues();
        setupListeners();
    }

    private void loadCurrentValues() {
        String primaryColor = prefs.getString("custom_primary_color", "#2563EB");
        String accentColor = prefs.getString("custom_accent_color", "#6366F1");
        String bgColor = prefs.getString("custom_bg_color", "#F0F4F8");
        int fontScale = prefs.getInt("font_scale", 2);

        setColorPreview(previewPrimary, txtPrimary, primaryColor);
        setColorPreview(previewAccent, txtAccent, accentColor);
        setColorPreview(previewBg, txtBg, bgColor);

        SeekBar fontSeekbar = findViewById(R.id.seekbar_font_size);
        fontSeekbar.setProgress(fontScale);
        updateFontPreview(fontScale);
    }

    private void setupListeners() {
        findViewById(R.id.row_primary_color).setOnClickListener(v ->
                showColorPicker("custom_primary_color", "#2563EB", previewPrimary, txtPrimary));

        findViewById(R.id.row_accent_color).setOnClickListener(v ->
                showColorPicker("custom_accent_color", "#6366F1", previewAccent, txtAccent));

        findViewById(R.id.row_bg_color).setOnClickListener(v ->
                showColorPicker("custom_bg_color", "#F0F4F8", previewBg, txtBg));

        findViewById(R.id.btn_reset_colors).setOnClickListener(v -> {
            prefs.edit()
                    .remove("custom_primary_color")
                    .remove("custom_accent_color")
                    .remove("custom_bg_color")
                    .apply();
            loadCurrentValues();
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

        int[] presets = {
            0xFF2563EB, 0xFF1D4ED8, 0xFF3B82F6, 0xFF06B6D4,
            0xFF059669, 0xFF84CC16, 0xFFF59E0B, 0xFFEF4444,
            0xFFEC4899, 0xFF8B5CF6, 0xFF6366F1, 0xFF0F172A,
            0xFF475569, 0xFF94A3B8, 0xFFF0F4F8, 0xFFFFFFFF
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
                    } catch (Exception ignored) {}
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    private void setColorPreview(View preview, TextView hexText, String colorHex) {
        try {
            GradientDrawable shape = new GradientDrawable();
            shape.setShape(GradientDrawable.RECTANGLE);
            shape.setCornerRadius(8);
            shape.setColor(Color.parseColor(colorHex));
            shape.setStroke(2, Color.parseColor("#E2E8F0"));
            preview.setBackground(shape);
            hexText.setText(colorHex);
        } catch (Exception ignored) {}
    }

    private void updateFontPreview(int scale) {
        float[] sizes = {11f, 13f, 15f, 17f, 20f};
        fontPreview.setTextSize(sizes[Math.min(scale, sizes.length - 1)]);
    }
}
