package com.botomat.zmaneyhayom.database;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.botomat.zmaneyhayom.models.AlertHistoryItem;
import com.botomat.zmaneyhayom.models.AlertRule;
import com.botomat.zmaneyhayom.models.OffsetType;
import com.botomat.zmaneyhayom.models.ZmanType;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class DatabaseHelper extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "zmaney_hayom.db";
    private static final int DATABASE_VERSION = 1;

    private static final String TABLE_ALERT_RULES = "alert_rules";
    private static final String TABLE_ALERT_HISTORY = "alert_history";
    private static final String COL_ID = "id";
    private static final String COL_ZMAN_TYPE = "zman_type";
    private static final String COL_OFFSET_TYPE = "offset_type";
    private static final String COL_OFFSET_MINUTES = "offset_minutes";
    private static final String COL_ENABLED = "enabled";
    private static final String COL_SOUND_ENABLED = "sound_enabled";
    private static final String COL_VIBRATE_ENABLED = "vibrate_enabled";
    private static final String COL_ALERT_TIME = "alert_time";
    private static final String COL_ZMAN_TIME = "zman_time";
    private static final String COL_DISMISSED = "dismissed";
    private static final String COL_SNOOZED = "snoozed";

    private static DatabaseHelper instance;

    public static synchronized DatabaseHelper getInstance(Context context) {
        if (instance == null) {
            instance = new DatabaseHelper(context.getApplicationContext());
        }
        return instance;
    }

    private DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE " + TABLE_ALERT_RULES + " (" +
                COL_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                COL_ZMAN_TYPE + " TEXT NOT NULL, " +
                COL_OFFSET_TYPE + " TEXT NOT NULL DEFAULT 'at_time', " +
                COL_OFFSET_MINUTES + " INTEGER NOT NULL DEFAULT 0, " +
                COL_ENABLED + " INTEGER NOT NULL DEFAULT 1, " +
                COL_SOUND_ENABLED + " INTEGER NOT NULL DEFAULT 1, " +
                COL_VIBRATE_ENABLED + " INTEGER NOT NULL DEFAULT 1)");

        db.execSQL("CREATE TABLE " + TABLE_ALERT_HISTORY + " (" +
                COL_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                COL_ZMAN_TYPE + " TEXT NOT NULL, " +
                COL_ALERT_TIME + " INTEGER NOT NULL, " +
                COL_ZMAN_TIME + " INTEGER NOT NULL, " +
                COL_OFFSET_TYPE + " TEXT NOT NULL, " +
                COL_OFFSET_MINUTES + " INTEGER NOT NULL DEFAULT 0, " +
                COL_DISMISSED + " INTEGER NOT NULL DEFAULT 0, " +
                COL_SNOOZED + " INTEGER NOT NULL DEFAULT 0)");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_ALERT_RULES);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_ALERT_HISTORY);
        onCreate(db);
    }

    // ==================== Alert Rules ====================

    public long insertAlertRule(AlertRule rule) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COL_ZMAN_TYPE, rule.getZmanType().name());
        values.put(COL_OFFSET_TYPE, rule.getOffsetType().getValue());
        values.put(COL_OFFSET_MINUTES, rule.getOffsetMinutes());
        values.put(COL_ENABLED, rule.isEnabled() ? 1 : 0);
        values.put(COL_SOUND_ENABLED, rule.isSoundEnabled() ? 1 : 0);
        values.put(COL_VIBRATE_ENABLED, rule.isVibrateEnabled() ? 1 : 0);
        return db.insert(TABLE_ALERT_RULES, null, values);
    }

    public void updateAlertRule(AlertRule rule) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COL_ZMAN_TYPE, rule.getZmanType().name());
        values.put(COL_OFFSET_TYPE, rule.getOffsetType().getValue());
        values.put(COL_OFFSET_MINUTES, rule.getOffsetMinutes());
        values.put(COL_ENABLED, rule.isEnabled() ? 1 : 0);
        values.put(COL_SOUND_ENABLED, rule.isSoundEnabled() ? 1 : 0);
        values.put(COL_VIBRATE_ENABLED, rule.isVibrateEnabled() ? 1 : 0);
        db.update(TABLE_ALERT_RULES, values, COL_ID + " = ?",
                new String[]{String.valueOf(rule.getId())});
    }

    public void deleteAlertRule(long id) {
        getWritableDatabase().delete(TABLE_ALERT_RULES, COL_ID + " = ?",
                new String[]{String.valueOf(id)});
    }

    public List<AlertRule> getAllAlertRules() {
        List<AlertRule> rules = new ArrayList<>();
        Cursor cursor = getReadableDatabase().query(TABLE_ALERT_RULES, null,
                null, null, null, null, COL_ID + " ASC");
        if (cursor.moveToFirst()) {
            do { rules.add(cursorToAlertRule(cursor)); } while (cursor.moveToNext());
        }
        cursor.close();
        return rules;
    }

    public List<AlertRule> getEnabledAlertRules() {
        List<AlertRule> rules = new ArrayList<>();
        Cursor cursor = getReadableDatabase().query(TABLE_ALERT_RULES, null,
                COL_ENABLED + " = 1", null, null, null, null);
        if (cursor.moveToFirst()) {
            do { rules.add(cursorToAlertRule(cursor)); } while (cursor.moveToNext());
        }
        cursor.close();
        return rules;
    }

    public boolean hasAlertForZman(ZmanType zmanType) {
        Cursor cursor = getReadableDatabase().query(TABLE_ALERT_RULES, new String[]{COL_ID},
                COL_ZMAN_TYPE + " = ? AND " + COL_ENABLED + " = 1",
                new String[]{zmanType.name()}, null, null, null);
        boolean has = cursor.getCount() > 0;
        cursor.close();
        return has;
    }

    private AlertRule cursorToAlertRule(Cursor cursor) {
        return new AlertRule(
                cursor.getLong(cursor.getColumnIndexOrThrow(COL_ID)),
                ZmanType.fromString(cursor.getString(cursor.getColumnIndexOrThrow(COL_ZMAN_TYPE))),
                OffsetType.fromString(cursor.getString(cursor.getColumnIndexOrThrow(COL_OFFSET_TYPE))),
                cursor.getInt(cursor.getColumnIndexOrThrow(COL_OFFSET_MINUTES)),
                cursor.getInt(cursor.getColumnIndexOrThrow(COL_ENABLED)) == 1,
                cursor.getInt(cursor.getColumnIndexOrThrow(COL_SOUND_ENABLED)) == 1,
                cursor.getInt(cursor.getColumnIndexOrThrow(COL_VIBRATE_ENABLED)) == 1
        );
    }

    // ==================== Alert History ====================

    public long insertAlertHistory(AlertHistoryItem item) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COL_ZMAN_TYPE, item.getZmanType().name());
        values.put(COL_ALERT_TIME, item.getAlertTime().getTime());
        values.put(COL_ZMAN_TIME, item.getZmanTime().getTime());
        values.put(COL_OFFSET_TYPE, item.getOffsetType().getValue());
        values.put(COL_OFFSET_MINUTES, item.getOffsetMinutes());
        values.put(COL_DISMISSED, item.isDismissed() ? 1 : 0);
        values.put(COL_SNOOZED, item.isSnoozed() ? 1 : 0);
        return db.insert(TABLE_ALERT_HISTORY, null, values);
    }

    public void updateAlertHistoryDismissed(long id) {
        ContentValues values = new ContentValues();
        values.put(COL_DISMISSED, 1);
        getWritableDatabase().update(TABLE_ALERT_HISTORY, values,
                COL_ID + " = ?", new String[]{String.valueOf(id)});
    }

    public List<AlertHistoryItem> getAlertHistory(int limit) {
        List<AlertHistoryItem> items = new ArrayList<>();
        Cursor cursor = getReadableDatabase().query(TABLE_ALERT_HISTORY, null,
                null, null, null, null, COL_ALERT_TIME + " DESC", String.valueOf(limit));
        if (cursor.moveToFirst()) {
            do {
                AlertHistoryItem item = new AlertHistoryItem();
                item.setId(cursor.getLong(cursor.getColumnIndexOrThrow(COL_ID)));
                item.setZmanType(ZmanType.fromString(cursor.getString(cursor.getColumnIndexOrThrow(COL_ZMAN_TYPE))));
                item.setAlertTime(new Date(cursor.getLong(cursor.getColumnIndexOrThrow(COL_ALERT_TIME))));
                item.setZmanTime(new Date(cursor.getLong(cursor.getColumnIndexOrThrow(COL_ZMAN_TIME))));
                item.setOffsetType(OffsetType.fromString(cursor.getString(cursor.getColumnIndexOrThrow(COL_OFFSET_TYPE))));
                item.setOffsetMinutes(cursor.getInt(cursor.getColumnIndexOrThrow(COL_OFFSET_MINUTES)));
                item.setDismissed(cursor.getInt(cursor.getColumnIndexOrThrow(COL_DISMISSED)) == 1);
                item.setSnoozed(cursor.getInt(cursor.getColumnIndexOrThrow(COL_SNOOZED)) == 1);
                items.add(item);
            } while (cursor.moveToNext());
        }
        cursor.close();
        return items;
    }

    public void clearAlertHistory() {
        getWritableDatabase().delete(TABLE_ALERT_HISTORY, null, null);
    }
}
