package info.noverguo.gpshack.service;

import android.content.Context;
import android.content.SharedPreferences;

import info.noverguo.gpshack.IGpsOffsetService;

/**
 * Created by noverguo on 2016/6/8.
 */
public class GpsOffsetService extends IGpsOffsetService.Stub {
    private static final String TAG = GpsOffsetService.class.getSimpleName();
    private static final String PREF_NAME = GpsOffsetService.class.getSimpleName();
    private static final String KEY_LATITUDE = "LATITUDE";
    private static final String KEY_LONGITUDE = "LONGITUDE";
    private static final String KEY_LATITUDE_OFFSET = "LATITUDE_OFFSET";
    private static final String KEY_LONGITUDE_OFFSET = "LONGITUDE_OFFSET";
    private static GpsOffsetService sInst;
    SharedPreferences sharedPreferences;
    private double latitude;
    private double longitude;
    private double latitudeOffset;
    private double longitudeOffset;
    public static GpsOffsetService get(Context context) {
        if (sInst == null) {
            synchronized (GpsOffsetService.class) {
                if (sInst == null) {
                    sInst = new GpsOffsetService(context);
                }
            }
        }
        return sInst;
    }
    private GpsOffsetService(Context context) {
        sharedPreferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        latitudeOffset = Double.valueOf(sharedPreferences.getString(KEY_LATITUDE_OFFSET, "0"));
        longitudeOffset = Double.valueOf(sharedPreferences.getString(KEY_LONGITUDE_OFFSET, "0"));
        latitude = Double.valueOf(sharedPreferences.getString(KEY_LATITUDE, "0"));
        longitude = Double.valueOf(sharedPreferences.getString(KEY_LONGITUDE, "0"));
    }

    public void setLatitude(double latitude) {
        this.latitude = latitude;
        sharedPreferences.edit().putString(KEY_LATITUDE, latitude + "").apply();
    }

    public void setLongitude(double longitude) {
        this.longitude = longitude;
        sharedPreferences.edit().putString(KEY_LONGITUDE, longitude + "").apply();
    }

    public void setLatitudeOffset(double latitudeOffset) {
        this.latitudeOffset = latitudeOffset;
        sharedPreferences.edit().putString(KEY_LATITUDE_OFFSET, latitudeOffset + "").apply();
    }

    public void setLongitudeOffset(double longitudeOffset) {
        this.longitudeOffset = longitudeOffset;
        sharedPreferences.edit().putString(KEY_LONGITUDE_OFFSET, longitudeOffset + "").apply();
    }

    @Override
    public double getLatitudeOffset() {
        return latitudeOffset;
    }

    @Override
    public double getLongitudeOffset() {
        return longitudeOffset;
    }

    @Override
    public double getLatitude() {
        return latitude;
    }

    @Override
    public double getLongitude() {
        return longitude;
    }
}
