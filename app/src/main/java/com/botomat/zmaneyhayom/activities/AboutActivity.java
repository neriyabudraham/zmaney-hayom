package com.botomat.zmaneyhayom.activities;

import android.content.Intent;
import android.content.pm.PackageInfo;
import android.net.Uri;
import android.os.Bundle;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.botomat.zmaneyhayom.R;
import com.botomat.zmaneyhayom.utils.ThemeHelper;
import com.google.android.material.appbar.MaterialToolbar;

public class AboutActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        ThemeHelper.applyTheme(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_about);

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());

        // Version
        TextView versionText = findViewById(R.id.about_version);
        try {
            PackageInfo pInfo = getPackageManager().getPackageInfo(getPackageName(), 0);
            versionText.setText(getString(R.string.version) + " " + pInfo.versionName);
        } catch (Exception e) {
            versionText.setText(getString(R.string.version) + " 1.0.0");
        }

        // Email click
        LinearLayout emailRow = findViewById(R.id.email_row);
        emailRow.setOnClickListener(v -> {
            Intent emailIntent = new Intent(Intent.ACTION_SENDTO);
            emailIntent.setData(Uri.parse("mailto:dvir.abu11@gmail.com"));
            emailIntent.putExtra(Intent.EXTRA_SUBJECT, "זמני היום - פנייה");
            startActivity(Intent.createChooser(emailIntent, "שלח אימייל"));
        });

        // Phone click
        LinearLayout phoneRow = findViewById(R.id.phone_row);
        phoneRow.setOnClickListener(v -> {
            Intent phoneIntent = new Intent(Intent.ACTION_DIAL);
            phoneIntent.setData(Uri.parse("tel:0507899701"));
            startActivity(phoneIntent);
        });
    }
}
