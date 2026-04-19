package com.botomat.zmaneyhayom.activities;

import android.app.DownloadManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.net.Uri;
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
import com.botomat.zmaneyhayom.utils.UpdateChecker;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;

import java.io.File;

public class UpdateActivity extends AppCompatActivity {

    private ProgressBar progress;
    private ProgressBar downloadProgress;
    private TextView statusText;
    private TextView versionText;
    private MaterialButton btnUpdate;
    private MaterialButton btnRetry;
    private ChangelogAdapter adapter;

    private String apkUrlToDownload;
    private long downloadId = -1;
    private BroadcastReceiver downloadReceiver;

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
        statusText.setText("מוריד עדכון…");

        try {
            // Delete old APK if exists
            File file = new File(getExternalCacheDir(), "zmaney-hayom-update.apk");
            if (file.exists()) file.delete();

            DownloadManager.Request req = new DownloadManager.Request(Uri.parse(apkUrlToDownload));
            req.setTitle("זמני היום");
            req.setDescription("הורדת עדכון…");
            req.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
            req.setDestinationInExternalFilesDir(this, null, "zmaney-hayom-update.apk");
            req.setMimeType("application/vnd.android.package-archive");

            DownloadManager dm = (DownloadManager) getSystemService(Context.DOWNLOAD_SERVICE);
            downloadId = dm.enqueue(req);

            registerDownloadReceiver();
            pollDownloadProgress();
        } catch (Exception e) {
            statusText.setText("שגיאה בהורדה: " + e.getMessage());
            btnUpdate.setEnabled(true);
            btnUpdate.setText("נסה שוב");
        }
    }

    private void registerDownloadReceiver() {
        downloadReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                long id = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1);
                if (id == downloadId) {
                    onDownloadComplete();
                }
            }
        };
        IntentFilter filter = new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE);
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(downloadReceiver, filter, Context.RECEIVER_EXPORTED);
        } else {
            registerReceiver(downloadReceiver, filter);
        }
    }

    private void pollDownloadProgress() {
        final DownloadManager dm = (DownloadManager) getSystemService(Context.DOWNLOAD_SERVICE);
        final android.os.Handler h = new android.os.Handler();
        h.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (downloadId < 0) return;
                DownloadManager.Query q = new DownloadManager.Query();
                q.setFilterById(downloadId);
                Cursor c = dm.query(q);
                if (c != null && c.moveToFirst()) {
                    int status = c.getInt(c.getColumnIndex(DownloadManager.COLUMN_STATUS));
                    long total = c.getLong(c.getColumnIndex(DownloadManager.COLUMN_TOTAL_SIZE_BYTES));
                    long done = c.getLong(c.getColumnIndex(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR));
                    if (total > 0) {
                        int pct = (int) (done * 100 / total);
                        downloadProgress.setProgress(pct);
                        statusText.setText("מוריד " + pct + "%");
                    }
                    c.close();
                    if (status == DownloadManager.STATUS_SUCCESSFUL
                            || status == DownloadManager.STATUS_FAILED) {
                        return;
                    }
                }
                h.postDelayed(this, 500);
            }
        }, 500);
    }

    private void onDownloadComplete() {
        downloadProgress.setVisibility(View.GONE);
        statusText.setText("מתקין עדכון…");

        DownloadManager dm = (DownloadManager) getSystemService(Context.DOWNLOAD_SERVICE);
        DownloadManager.Query q = new DownloadManager.Query();
        q.setFilterById(downloadId);
        Cursor c = dm.query(q);
        if (c != null && c.moveToFirst()) {
            int status = c.getInt(c.getColumnIndex(DownloadManager.COLUMN_STATUS));
            if (status == DownloadManager.STATUS_SUCCESSFUL) {
                String localUri = c.getString(c.getColumnIndex(DownloadManager.COLUMN_LOCAL_URI));
                c.close();
                launchInstall(Uri.parse(localUri));
            } else {
                c.close();
                Toast.makeText(this, "הורדה נכשלה", Toast.LENGTH_LONG).show();
                btnUpdate.setEnabled(true);
                btnUpdate.setText("נסה שוב");
                statusText.setText("ההורדה נכשלה");
            }
        }
    }

    private void launchInstall(Uri fileUri) {
        try {
            Intent intent = new Intent(Intent.ACTION_VIEW);
            Uri installUri;
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
                File file;
                if ("file".equals(fileUri.getScheme())) {
                    file = new File(fileUri.getPath());
                } else {
                    file = new File(getExternalFilesDir(null), "zmaney-hayom-update.apk");
                }
                installUri = FileProvider.getUriForFile(
                        this, getPackageName() + ".fileprovider", file);
                intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            } else {
                installUri = fileUri;
            }
            intent.setDataAndType(installUri, "application/vnd.android.package-archive");
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            finish();
        } catch (Exception e) {
            Toast.makeText(this, "שגיאת התקנה: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (downloadReceiver != null) {
            try { unregisterReceiver(downloadReceiver); } catch (Exception ignored) {}
        }
    }
}
