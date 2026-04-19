package com.botomat.zmaneyhayom.utils;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.os.AsyncTask;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public class UpdateChecker {

    private static final String TAG = "UpdateChecker";
    public static final String VERSIONS_URL =
            "https://files.neriyabudraham.co.il/files/zmaney-versions.json";

    public static class VersionInfo {
        public int versionCode;
        public String versionName;
        public String date;
        public String apkUrl;
        public List<String> changes = new ArrayList<>();
    }

    public static class UpdateResult {
        public boolean updateAvailable;
        public int currentVersionCode;
        public String currentVersionName;
        public int latestVersionCode;
        public String latestVersionName;
        public String latestApkUrl;
        public List<VersionInfo> allVersions = new ArrayList<>();
        public String errorMessage;
    }

    public interface UpdateCallback {
        void onResult(UpdateResult result);
    }

    public static void check(final Context context, final UpdateCallback callback) {
        new AsyncTask<Void, Void, UpdateResult>() {
            @Override
            protected UpdateResult doInBackground(Void... voids) {
                UpdateResult result = new UpdateResult();
                try {
                    PackageInfo pi = context.getPackageManager()
                            .getPackageInfo(context.getPackageName(), 0);
                    result.currentVersionCode = pi.versionCode;
                    result.currentVersionName = pi.versionName;
                } catch (Exception e) {
                    result.currentVersionCode = 1;
                    result.currentVersionName = "1.0.0";
                }

                HttpURLConnection conn = null;
                try {
                    URL url = new URL(VERSIONS_URL);
                    conn = (HttpURLConnection) url.openConnection();
                    conn.setConnectTimeout(10000);
                    conn.setReadTimeout(10000);
                    conn.setRequestMethod("GET");
                    int code = conn.getResponseCode();
                    if (code != 200) {
                        result.errorMessage = "HTTP " + code;
                        return result;
                    }

                    BufferedReader reader = new BufferedReader(
                            new InputStreamReader(conn.getInputStream(), "UTF-8"));
                    StringBuilder sb = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) {
                        sb.append(line).append('\n');
                    }
                    reader.close();

                    JSONObject root = new JSONObject(sb.toString());
                    result.latestVersionCode = root.getInt("latest_version_code");
                    result.latestVersionName = root.getString("latest_version_name");
                    result.latestApkUrl = root.getString("latest_apk_url");
                    result.updateAvailable = result.latestVersionCode > result.currentVersionCode;

                    JSONArray versions = root.getJSONArray("versions");
                    for (int i = 0; i < versions.length(); i++) {
                        JSONObject v = versions.getJSONObject(i);
                        VersionInfo vi = new VersionInfo();
                        vi.versionCode = v.getInt("version_code");
                        vi.versionName = v.getString("version_name");
                        vi.date = v.optString("date", "");
                        vi.apkUrl = v.optString("apk_url", "");
                        JSONArray ch = v.optJSONArray("changes");
                        if (ch != null) {
                            for (int j = 0; j < ch.length(); j++) {
                                vi.changes.add(ch.getString(j));
                            }
                        }
                        result.allVersions.add(vi);
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Update check failed", e);
                    result.errorMessage = "שגיאת רשת: " + e.getMessage();
                } finally {
                    if (conn != null) conn.disconnect();
                }
                return result;
            }

            @Override
            protected void onPostExecute(UpdateResult result) {
                if (callback != null) callback.onResult(result);
            }
        }.execute();
    }
}
