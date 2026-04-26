package com.botomat.zmaneyhayom.activities;

import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.botomat.zmaneyhayom.R;
import com.botomat.zmaneyhayom.adapters.ChangelogAdapter;
import com.botomat.zmaneyhayom.utils.ThemeHelper;
import com.botomat.zmaneyhayom.utils.TrustingHttp;
import com.botomat.zmaneyhayom.utils.UpdateChecker;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;

public class UpdateActivity extends AppCompatActivity {

    private ProgressBar progress;
    private ProgressBar downloadProgress;
    private TextView statusText;
    private TextView versionText;
    private MaterialButton btnUpdate;
    private MaterialButton btnRetry;
    private ChangelogAdapter adapter;

    private String apkUrlToDownload;
    private DownloadTask downloadTask;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        ThemeHelper.applyTheme(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_update);

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { finish(); }
        });

        progress = findViewById(R.id.progress);
        downloadProgress = findViewById(R.id.download_progress);
        statusText = findViewById(R.id.status_text);
        versionText = findViewById(R.id.version_text);
        btnUpdate = findViewById(R.id.btn_update);
        btnRetry = findViewById(R.id.btn_retry);

        RecyclerView list = findViewById(R.id.changelog_list);
        list.setLayoutManager(new LinearLayoutManager(this));
        adapter = new ChangelogAdapter();
        list.setAdapter(adapter);

        btnRetry.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { checkForUpdates(); }
        });

        checkForUpdates();
    }

    private void checkForUpdates() {
        progress.setVisibility(View.VISIBLE);
        statusText.setText("בודק עדכונים…");
        versionText.setText("");
        btnUpdate.setVisibility(View.GONE);
        btnRetry.setVisibility(View.GONE);

        UpdateChecker.check(this, new UpdateChecker.UpdateCallback() {
            @Override
            public void onResult(UpdateChecker.UpdateResult result) {
                progress.setVisibility(View.GONE);

                if (result.errorMessage != null) {
                    statusText.setText("בדיקת עדכון נכשלה");
                    versionText.setText(result.errorMessage);
                    btnRetry.setVisibility(View.VISIBLE);
                    return;
                }

                adapter.setData(result.allVersions, result.currentVersionCode);

                if (result.updateAvailable) {
                    statusText.setText("עדכון חדש זמין!");
                    versionText.setText("גרסה נוכחית: " + result.currentVersionName
                            + "\nגרסה חדשה: " + result.latestVersionName);
                    apkUrlToDownload = result.latestApkUrl;
                    btnUpdate.setVisibility(View.VISIBLE);
                    btnUpdate.setOnClickListener(new View.OnClickListener() {
                        @Override public void onClick(View v) { startDownload(); }
                    });
                } else {
                    statusText.setText("הגרסה שלך מעודכנת");
                    versionText.setText("גרסה " + result.currentVersionName);
                }
            }
        });
    }

    private void startDownload() {
        if (apkUrlToDownload == null) return;

        btnUpdate.setEnabled(false);
        btnUpdate.setText("מוריד…");
        downloadProgress.setVisibility(View.VISIBLE);
        downloadProgress.setProgress(0);
        statusText.setText("מוריד עדכון…");

        downloadTask = new DownloadTask();
        downloadTask.execute(apkUrlToDownload);
    }

    private class DownloadTask extends AsyncTask<String, Integer, File> {
        private String errorMessage;

        @Override
        protected File doInBackground(String... params) {
            HttpURLConnection conn = null;
            InputStream in = null;
            FileOutputStream out = null;
            try {
                String url = params[0];

                // Save to public Downloads dir
                File dir = Environment.getExternalStoragePublicDirectory(
                        Environment.DIRECTORY_DOWNLOADS);
                if (!dir.exists()) dir.mkdirs();
                File outFile = new File(dir, "zmaney-hayom-update.apk");
                if (outFile.exists()) outFile.delete();

                conn = TrustingHttp.open(url);
                conn.setRequestMethod("GET");
                conn.connect();

                int code = conn.getResponseCode();
                if (code != 200) {
                    errorMessage = "HTTP " + code;
                    return null;
                }

                int total = conn.getContentLength();
                in = conn.getInputStream();
                out = new FileOutputStream(outFile);

                byte[] buf = new byte[8192];
                long downloaded = 0;
                int n;
                while ((n = in.read(buf)) > 0) {
                    if (isCancelled()) {
                        outFile.delete();
                        return null;
                    }
                    out.write(buf, 0, n);
                    downloaded += n;
                    if (total > 0) {
                        publishProgress((int) (downloaded * 100 / total));
                    }
                }
                out.flush();
                return outFile;
            } catch (Exception e) {
                android.util.Log.e("UpdateActivity", "Download failed", e);
                errorMessage = e.getClass().getSimpleName() + ": " + e.getMessage();
                return null;
            } finally {
                try { if (out != null) out.close(); } catch (Exception ignored) {}
                try { if (in != null) in.close(); } catch (Exception ignored) {}
                if (conn != null) conn.disconnect();
            }
        }

        @Override
        protected void onProgressUpdate(Integer... values) {
            int pct = values[0];
            downloadProgress.setProgress(pct);
            statusText.setText("מוריד " + pct + "%");
        }

        @Override
        protected void onPostExecute(File apkFile) {
            downloadProgress.setVisibility(View.GONE);
            if (apkFile != null && apkFile.exists()) {
                statusText.setText("מתקין עדכון…");
                launchInstall(apkFile);
            } else {
                String msg = "ההורדה נכשלה" + (errorMessage != null ? ": " + errorMessage : "");
                statusText.setText(msg);
                Toast.makeText(UpdateActivity.this, msg, Toast.LENGTH_LONG).show();
                btnUpdate.setEnabled(true);
                btnUpdate.setText("נסה שוב");
            }
        }
    }

    private void launchInstall(File apkFile) {
        try {
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

            Uri installUri;
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
                installUri = FileProvider.getUriForFile(
                        this, getPackageName() + ".fileprovider", apkFile);
                intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            } else {
                installUri = Uri.fromFile(apkFile);
            }

            intent.setDataAndType(installUri, "application/vnd.android.package-archive");
            startActivity(intent);
            finish();
        } catch (Exception e) {
            android.util.Log.e("UpdateActivity", "Install failed", e);
            String errMsg = "שגיאת התקנה: " + e.getClass().getSimpleName()
                    + " - " + e.getMessage();
            statusText.setText(errMsg);
            Toast.makeText(this, errMsg, Toast.LENGTH_LONG).show();
            btnUpdate.setEnabled(true);
            btnUpdate.setText("נסה שוב");
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (downloadTask != null) {
            downloadTask.cancel(true);
        }
    }
}
