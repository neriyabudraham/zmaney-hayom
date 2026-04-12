package com.botomat.zmaneyhayom.utils;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.preference.PreferenceManager;

import com.botomat.zmaneyhayom.models.CityLocation;
import com.botomat.zmaneyhayom.models.ZmanItem;
import com.botomat.zmaneyhayom.models.ZmanType;
import com.kosherjava.zmanim.ComplexZmanimCalendar;
import com.kosherjava.zmanim.util.GeoLocation;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;

public class ZmanimCalculator {

    private final Context context;

    public ZmanimCalculator(Context context) {
        this.context = context;
    }

    public List<ZmanItem> calculateZmanim() {
        return calculateZmanim(Calendar.getInstance());
    }

    public List<ZmanItem> calculateZmanim(Calendar calendar) {
        ComplexZmanimCalendar zc = createZmanimCalendar(calendar);
        List<ZmanItem> zmanim = new ArrayList<>();

        addZman(zmanim, ZmanType.ALOT, zc.getAlos72());
        addZman(zmanim, ZmanType.MISHEYAKIR, zc.getMisheyakir11Point5Degrees());
        addZman(zmanim, ZmanType.HANETZ, zc.getSunrise());
        addZman(zmanim, ZmanType.SOF_SHMA_MGA, zc.getSofZmanShmaMGA());
        addZman(zmanim, ZmanType.SOF_SHMA_GRA, zc.getSofZmanShmaGRA());
        addZman(zmanim, ZmanType.SOF_TFILA_MGA, zc.getSofZmanTfilaMGA());
        addZman(zmanim, ZmanType.SOF_TFILA_GRA, zc.getSofZmanTfilaGRA());
        addZman(zmanim, ZmanType.CHATZOT, zc.getChatzos());
        addZman(zmanim, ZmanType.MINCHA_GEDOLA, zc.getMinchaGedola());
        addZman(zmanim, ZmanType.MINCHA_KETANA, zc.getMinchaKetana());
        addZman(zmanim, ZmanType.PLAG_MINCHA, zc.getPlagHamincha());
        addZman(zmanim, ZmanType.SHKIA, zc.getSunset());
        addZman(zmanim, ZmanType.TZEIT, zc.getTzais());
        addZman(zmanim, ZmanType.TZEIT_RT, zc.getTzais72());
        addZman(zmanim, ZmanType.CHATZOT_LAYLA, getChatzotLayla(zc));

        // Mark passed and next zman
        Date now = new Date();
        boolean foundNext = false;
        for (ZmanItem item : zmanim) {
            if (item.getTime() != null) {
                if (item.getTime().before(now)) {
                    item.setPassed(true);
                } else if (!foundNext) {
                    item.setNext(true);
                    foundNext = true;
                }
            }
        }

        return zmanim;
    }

    public Date getZmanTime(ZmanType type) {
        return getZmanTime(type, Calendar.getInstance());
    }

    public Date getZmanTime(ZmanType type, Calendar calendar) {
        ComplexZmanimCalendar zc = createZmanimCalendar(calendar);
        switch (type) {
            case ALOT: return zc.getAlos72();
            case MISHEYAKIR: return zc.getMisheyakir11Point5Degrees();
            case HANETZ: return zc.getSunrise();
            case SOF_SHMA_MGA: return zc.getSofZmanShmaMGA();
            case SOF_SHMA_GRA: return zc.getSofZmanShmaGRA();
            case SOF_TFILA_MGA: return zc.getSofZmanTfilaMGA();
            case SOF_TFILA_GRA: return zc.getSofZmanTfilaGRA();
            case CHATZOT: return zc.getChatzos();
            case MINCHA_GEDOLA: return zc.getMinchaGedola();
            case MINCHA_KETANA: return zc.getMinchaKetana();
            case PLAG_MINCHA: return zc.getPlagHamincha();
            case SHKIA: return zc.getSunset();
            case TZEIT: return zc.getTzais();
            case TZEIT_RT: return zc.getTzais72();
            case CHATZOT_LAYLA: return getChatzotLayla(zc);
            default: return null;
        }
    }

    private Date getChatzotLayla(ComplexZmanimCalendar zc) {
        Date sunset = zc.getSunset();
        Date nextSunrise = zc.getSunrise();
        if (sunset != null && nextSunrise != null) {
            // Approximate: halfway between sunset and next sunrise
            // For simplicity, add 6 hours to sunset
            Calendar cal = Calendar.getInstance();
            cal.setTime(sunset);
            long diff = 12 * 60 * 60 * 1000; // 12 hours as rough midnight
            // Better: use actual chatzot calculation
            long midpoint = sunset.getTime() + (nextSunrise.getTime() + 24 * 60 * 60 * 1000 - sunset.getTime()) / 2;
            return new Date(midpoint);
        }
        return null;
    }

    private ComplexZmanimCalendar createZmanimCalendar(Calendar calendar) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        String cityKey = prefs.getString("city", "jerusalem");
        TimeZone timeZone = getTimeZone(prefs);

        double latitude, longitude, elevation;

        if ("custom".equals(cityKey)) {
            latitude = Double.parseDouble(prefs.getString("custom_latitude", "31.7683"));
            longitude = Double.parseDouble(prefs.getString("custom_longitude", "35.2137"));
            elevation = Double.parseDouble(prefs.getString("custom_elevation", "800"));
        } else {
            CityLocation city = CityLocation.findByKey(cityKey);
            latitude = city.getLatitude();
            longitude = city.getLongitude();
            elevation = city.getElevation();
        }

        GeoLocation geoLocation = new GeoLocation("", latitude, longitude, elevation, timeZone);
        ComplexZmanimCalendar zc = new ComplexZmanimCalendar(geoLocation);
        zc.getCalendar().set(calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH));
        return zc;
    }

    private TimeZone getTimeZone(SharedPreferences prefs) {
        String dstMode = prefs.getString("dst_mode", "auto");
        TimeZone tz = TimeZone.getTimeZone("Asia/Jerusalem");

        if ("summer".equals(dstMode)) {
            // Force summer time (+3)
            tz = TimeZone.getTimeZone("GMT+3");
        } else if ("winter".equals(dstMode)) {
            // Force winter time (+2)
            tz = TimeZone.getTimeZone("GMT+2");
        }
        // "auto" uses the system timezone which handles DST automatically

        return tz;
    }

    private void addZman(List<ZmanItem> list, ZmanType type, Date time) {
        list.add(new ZmanItem(type, type.getHebrewName(), time));
    }
}
