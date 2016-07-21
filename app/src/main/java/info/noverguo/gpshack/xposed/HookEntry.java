package info.noverguo.gpshack.xposed;

import android.app.Application;
import android.app.PendingIntent;
import android.content.Context;
import android.location.GpsStatus;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.location.LocationProvider;
import android.net.wifi.WifiInfo;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.telephony.CellLocation;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;

import com.google.android.gms.maps.model.BitmapDescriptorFactory;

import java.lang.reflect.Method;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;
import info.noverguo.gpshack.BuildConfig;
import info.noverguo.gpshack.callback.ResultCallback;
import info.noverguo.gpshack.receiver.ResetReceiver;
import info.noverguo.gpshack.service.LocalGpsOffsetService;
import info.noverguo.gpshack.utils.XposedUtils;

/**
 * Created by noverguo on 2016/7/18.
 */
public class HookEntry implements IXposedHookLoadPackage {
    LocalGpsOffsetService localGpsOffsetService;
    private double latitudeOffset;
    private double longitudeOffset;
    private double lastLatitude;
    private double lastLongitude;
    private Location lastLocation;
    private Context context;
    Set<Class> listenerClasses = new HashSet<>();
    Map<LocationListener, Long> listeners = new HashMap<>();
    Map<LocationListener, Long> listenerUpdateTimes = new HashMap<>();
    Set<PendingIntent> pendingIntents  = new HashSet<>();
    String packageName;
    static boolean init = false;
    Handler uiHandler;
    @Override
    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lp) throws Throwable {
        if (lp.appInfo == null) {
            return;
        }
        if (lp.packageName.equals("info.noverguo.gpshack") || !lp.isFirstApplication || init) {
            return;
        }
        init = true;
        packageName = lp.packageName;
        String applicationClass = lp.appInfo.className;
        if (applicationClass == null) {
            applicationClass = Application.class.getName();
        } else {
            try {
                XposedHelpers.findMethodExact(applicationClass, lp.classLoader, "onCreate");
            } catch (NoSuchMethodError e) {
                applicationClass = Application.class.getName();
            }
        }
        if (BuildConfig.DEBUG) XposedBridge.log(packageName + ":  gps hack: " + packageName + ", " + applicationClass);

        XposedUtils.findAndHookMethod(applicationClass, lp.classLoader, "onCreate", new XC_MethodHook() {
            @Override
            public void afterHookedMethod(XC_MethodHook.MethodHookParam param) throws Throwable {
                context = (Context) param.thisObject;
                uiHandler = new Handler(Looper.getMainLooper());
                localGpsOffsetService = LocalGpsOffsetService.get(context);
                reset();
                ResetReceiver.register(context, new ResetReceiver.Callback() {
                    @Override
                    public void onReset() {
                        reset();
                    }
                });
            }
        });
        hookLocationGet();
        hookRequestLocationUpdate();
        hookCell();
        hookGpsStatus();
        hookWifi();
    }

    private void hookLocationGet() {
        XposedUtils.findAndHookMethod(Location.class, "getLatitude", new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                if (localGpsOffsetService == null) {
                    return;
                }
                Double result = (Double) param.getResult();
                if (result == null) {
                    return;
                }
                lastLatitude = result;
                lastLocation = new Location((Location) param.thisObject);
//                if (BuildConfig.DEBUG) XposedBridge.log(packageName + ":  Location.getLatitude: " + result + ", " + latitudeOffset + ", " + (result + latitudeOffset));
                param.setResult(result + latitudeOffset);
            }
        });
        XposedUtils.findAndHookMethod(Location.class, "getLongitude", new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                if (localGpsOffsetService == null) {
                    return;
                }
                Double result = (Double) param.getResult();
                if (result == null) {
                    return;
                }
                lastLongitude = result;
                lastLocation = new Location((Location) param.thisObject);
//                if (BuildConfig.DEBUG) XposedBridge.log(packageName + ":  Location.getLongitude: " + result + ", " + longitudeOffset + ", " + (result + longitudeOffset));
                param.setResult(result + longitudeOffset);
            }
        });

        XposedUtils.findAndHookMethod(Location.class, "setLatitude", double.class, new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                if (localGpsOffsetService == null) {
                    return;
                }
                Double arg0 = (Double) param.args[0];
                if (arg0 == null) {
                    return;
                }
                param.args[0] = arg0 - latitudeOffset;
//                if (BuildConfig.DEBUG) XposedBridge.log(packageName + ":  Location.setLatitude: " + arg0 + ", " + latitudeOffset + ", " + (arg0 - latitudeOffset));
            }
        });
        XposedUtils.findAndHookMethod(Location.class, "setLongitude", double.class, new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                if (localGpsOffsetService == null) {
                    return;
                }
                Double arg0 = (Double) param.args[0];
                if (arg0 == null) {
                    return;
                }
                param.args[0] = arg0 - longitudeOffset;
//                if (BuildConfig.DEBUG) XposedBridge.log(packageName + ":  Location.setLongitude: " + arg0 + ", " + longitudeOffset + ", " + (arg0 - longitudeOffset));
            }
        });
    }

    private void hookRequestLocationUpdate() {
        XposedUtils.findAndHookMethod(LocationManager.class, "getLastKnownLocation", String.class, new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                Object result = param.getResult();
                if (result == null) {
                    if (BuildConfig.DEBUG) XposedBridge.log(packageName + ":  getLastKnownLocation: " + lastLongitude + ", " + lastLatitude);
                    param.setResult(getLastLocation());
                }
            }
        });
        XposedUtils.findAndHookMethod(LocationManager.class, "getLastLocation", new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                Object result = param.getResult();
                if (result == null) {
                    if (BuildConfig.DEBUG) XposedBridge.log(packageName + ":  getLastKnownLocation: " + lastLongitude + ", " + lastLatitude);
                    param.setResult(getLastLocation());
                }
            }
        });
        XposedBridge.hookAllMethods(LocationManager.class, "requestLocationUpdates", new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                LocationListener listener = null;
                Long time = null;
                for (Object arg : param.args) {
                    if (arg != null && arg instanceof LocationListener) {
                        listener = (LocationListener) arg;
                        if (listeners.containsKey(listener)) {
                            return;
                        }
                    }
                    if (arg instanceof PendingIntent) {
                        if (pendingIntents.contains(arg)) {
                            return;
                        }
                        pendingIntents.add((PendingIntent) arg);
                    }
                    if (arg instanceof Long) {
                        time = (Long) arg;
                    }
                }
                if (BuildConfig.DEBUG) XposedBridge.log("requestLocationUpdates: " + param.args[0] + ", " + time + ", " + listener);
                if (listener != null && time != null) {
                    listeners.put(listener, time * 2 + 1000);
                    Class clazz = listener.getClass();
                    if (listenerClasses.contains(clazz)) {
                        listener.onLocationChanged(getLastLocation());
                        return;
                    }
                    listenerClasses.add(clazz);
                    XposedUtils.findAndHookMethod(clazz, "onStatusChanged", String.class, int.class, Bundle.class, new XC_MethodHook() {
                        @Override
                        protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                            Integer status = (Integer) param.args[1];
                            if (BuildConfig.DEBUG) XposedBridge.log("onStatusChanged: " + param.args[0] + ", " + status);
                            if (status == LocationProvider.AVAILABLE) {
                                return;
                            }
                            param.setResult(null);
                        }
                    });
                    XposedUtils.findAndHookMethod(clazz, "onProviderDisabled", String.class, new XC_MethodHook() {
                        @Override
                        protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                            if (BuildConfig.DEBUG) XposedBridge.log("onProviderDisabled: " + param.args[0]);
                            if ("network".equals(param.args[0])) {
                                return;
                            }
                            param.setResult(null);
                        }
                    });
                    XposedUtils.findAndHookMethod(clazz, "onProviderEnabled", String.class, new XC_MethodHook() {
                        @Override
                        protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                            if (BuildConfig.DEBUG) XposedBridge.log("onProviderEnabled: " + param.args[0]);
                        }
                    });
                    XposedUtils.findAndHookMethod(clazz, "onLocationChanged", Location.class, new XC_MethodHook() {
                        LocationListener locationListener;
                        @Override
                        protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                            Location location = (Location) param.args[0];
                            locationListener = (LocationListener) param.thisObject;
                            update(false);
                            if (BuildConfig.DEBUG) XposedBridge.log("onLocationChanged: " + location.getProvider() + ", " + location.getAccuracy() + ", " + location.getAltitude() + ", " + location.getBearing() + ", " + location.getSpeed() + ", " + location.getTime());
                        }

                        Runnable updateRunnable = new Runnable() {
                            @Override
                            public void run() {
                                update(true);
                            }
                        };

                        private void update(boolean run) {
                            if (!listeners.containsKey(locationListener)) {
                                return;
                            }
                            Long updateTime = listeners.get(locationListener);
                            if (updateTime == null) {
                                return;
                            }
                            uiHandler.removeCallbacks(updateRunnable);
                            long currentTime = System.currentTimeMillis();
                            if (run) {
                                if (listenerUpdateTimes.containsKey(locationListener)) {
                                    Long preUpdateTime = listenerUpdateTimes.get(locationListener);
                                    if (preUpdateTime == null || preUpdateTime + updateTime - 100 < currentTime) {
                                        locationListener.onLocationChanged(getLastLocation());
                                    }
                                } else {
                                    locationListener.onLocationChanged(getLastLocation());
                                }
                            }
                            listenerUpdateTimes.put(locationListener, currentTime);
                            uiHandler.postDelayed(updateRunnable, updateTime);
                        }
                    });
                    listener.onLocationChanged(getLastLocation());
                }
            }
        });

        XposedBridge.hookAllMethods(LocationManager.class, "removeUpdates", new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                Object arg0 = param.args[0];
                if (arg0 instanceof LocationListener) {
                    listeners.remove(arg0);
                    listenerUpdateTimes.remove(arg0);
                    return;
                }
                if (arg0 instanceof PendingIntent) {
                    pendingIntents.remove(arg0);
                }
            }
        });
    }

    private void hookCell() {
        XposedUtils.findAndHookMethod(TelephonyManager.class, "getCellLocation", new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                param.setResult(null);
            }
        });
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            XposedUtils.findAndHookMethod(TelephonyManager.class, "getNeighboringCellInfo", new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    param.setResult(Collections.emptyList());
                }
            });
        }
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.JELLY_BEAN) {
            XposedUtils.findAndHookMethod(TelephonyManager.class, "getAllCellInfo", new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    param.setResult(Collections.emptyList());
                }
            });
            XposedBridge.hookAllConstructors(PhoneStateListener.class, new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    XposedUtils.findAndHookMethod(param.thisObject.getClass(), "onCellInfoChanged", List.class, new XC_MethodHook() {
                        @Override
                        protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                            if (param.args[0] != null) {
                                param.args[0] = Collections.emptyList();
                            }
                        }
                    });
                    XposedUtils.findAndHookMethod(param.thisObject.getClass(), "onCellLocationChanged", CellLocation.class, new XC_MethodHook() {
                        @Override
                        protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                            if (param.args[0] != null) {
                                param.args[0] = null;
                            }
                        }
                    });
                }
            });
        }
    }

    Set<Class> gpsStatusClasses = new HashSet<>();
    int firstFixTime = new Random().nextInt(10) + 1;
    private void hookGpsStatus() {
        XposedHelpers.findAndHookMethod(LocationManager.class, "addGpsStatusListener", GpsStatus.Listener.class, new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                GpsStatus.Listener listener = (GpsStatus.Listener) param.args[0];
                if (listener != null) {
                    Class<? extends GpsStatus.Listener> clazz = listener.getClass();
                    if (gpsStatusClasses.contains(clazz)) {
                        return;
                    }
                    gpsStatusClasses.add(clazz);
                    XposedUtils.findAndHookMethod(clazz, "onGpsStatusChanged", int.class, new XC_MethodHook() {
                        boolean firstFix = false;
                        int count = 0;
                        @Override
                        protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                            Integer status = (Integer) param.args[0];
                            if (status == GpsStatus.GPS_EVENT_STARTED) {
                                firstFix = false;
                            }
                            if (!firstFix) {
                                firstFix = status == GpsStatus.GPS_EVENT_FIRST_FIX;
                            }
                            if (status == GpsStatus.GPS_EVENT_STOPPED || status == GpsStatus.GPS_EVENT_SATELLITE_STATUS) {
                                if (!firstFix) {
                                    param.args[0] = GpsStatus.GPS_EVENT_FIRST_FIX;
                                    firstFix = true;
                                } else {
                                    param.args[0] = GpsStatus.GPS_EVENT_SATELLITE_STATUS;
                                }
                            }
                            if ((Integer)param.args[0] == GpsStatus.GPS_EVENT_FIRST_FIX) {
                                firstFixTime = new Random().nextInt(10) + 1;
                            }
                            if ((Integer)param.args[0] == GpsStatus.GPS_EVENT_SATELLITE_STATUS) {
                                count++;
                                if (count > 15) {
                                    firstFix = false;
                                    count = 0;
                                }
                            } else {
                                count = 0;
                            }

                            if (BuildConfig.DEBUG) XposedBridge.log("onGpsStatusChanged: " + status + ", " + param.args[0]);
                        }
                    });
                }
            }
        });
        XposedHelpers.findAndHookMethod(LocationManager.class, "getGpsStatus", GpsStatus.class, new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                Method setStatusMethod = null;
                GpsStatus gpsStatus = (GpsStatus) param.getResult();
                for (Method method : GpsStatus.class.getDeclaredMethods()) {
                    if (method.getName().equals("setStatus") && method.getParameterTypes().length > 6) {
                        setStatusMethod = method;
                        break;
                    }
                }
                Method firstFixMethod = GpsStatus.class.getDeclaredMethod("setTimeToFirstFix", int.class);

                if (setStatusMethod == null || firstFixMethod == null) {
                    return;
                }
                setStatusMethod.setAccessible(true);
                firstFixMethod.setAccessible(true);
                float[] fArr = new float[]{6.0f, 12.0f, 18.0f, 24.0f, BitmapDescriptorFactory.HUE_ORANGE, 36.0f, 42.0f, 48.0f, 54.0f, BitmapDescriptorFactory.HUE_YELLOW, 66.0f, 72.0f};
                float[] fArr2 = new float[]{6.0f, 12.0f, 18.0f, 24.0f, BitmapDescriptorFactory.HUE_ORANGE, 36.0f, 42.0f, 48.0f, 54.0f, BitmapDescriptorFactory.HUE_YELLOW, 66.0f, 72.0f};
                float[] fArr3 = new float[]{BitmapDescriptorFactory.HUE_ORANGE, BitmapDescriptorFactory.HUE_YELLOW, 90.0f, BitmapDescriptorFactory.HUE_GREEN, 150.0f, BitmapDescriptorFactory.HUE_CYAN, BitmapDescriptorFactory.HUE_AZURE, BitmapDescriptorFactory.HUE_BLUE, BitmapDescriptorFactory.HUE_VIOLET, BitmapDescriptorFactory.HUE_MAGENTA, BitmapDescriptorFactory.HUE_ROSE, 360.0f};
                if (setStatusMethod != null) {
                    try {
                        setStatusMethod.invoke(gpsStatus, new Object[]{Integer.valueOf(12), new int[]{1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12}, fArr, fArr2, fArr3, Integer.valueOf(4095), Integer.valueOf(4095), Integer.valueOf(4095)});
                        if (gpsStatus.getTimeToFirstFix() <= 0) {
                            firstFixMethod.invoke(gpsStatus, firstFixTime);
                            firstFixTime += new Random().nextInt(10);
                        }
                        if (BuildConfig.DEBUG) XposedBridge.log("getGpsStatus: " + gpsStatus.getTimeToFirstFix() + ", " + gpsStatus.getMaxSatellites());
                        param.setResult(gpsStatus);
                    } catch (Throwable e) {
                        XposedBridge.log(e);
                    }
                }

            }
        });
    }

    private void hookWifi() {
        XposedHelpers.findAndHookMethod(WifiInfo.class, "getBSSID", new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                if (BuildConfig.DEBUG) XposedBridge.log("getBSSID: " + param.getResult());
                param.setResult("00:00:00:00:00:00");
            }
        });
    }

    private void reset() {
        localGpsOffsetService.getLatitude(new ResultCallback<Double>() {
            @Override
            public void onResult(Double res) {
                lastLatitude = res;
            }
        });
        localGpsOffsetService.getLongitude(new ResultCallback<Double>() {
            @Override
            public void onResult(Double res) {
                lastLongitude = res;
            }
        });
        localGpsOffsetService.getLatitudeOffset(new ResultCallback<Double>() {
            @Override
            public void onResult(final Double latitudeOff) {
                latitudeOffset = latitudeOff;
                localGpsOffsetService.getLongitudeOffset(new ResultCallback<Double>() {
                    @Override
                    public void onResult(Double longitudeOff) {
                        if (BuildConfig.DEBUG) XposedBridge.log(packageName + ":  reset.getLongitudeOffset, getLatitudeOffset: " + longitudeOff + ", " + latitudeOff);
                        longitudeOffset = longitudeOff;
                        updateLocation();
                    }
                });
            }
        });
    }

    private void updateLocation() {
        if (BuildConfig.DEBUG) XposedBridge.log(packageName + ":  updateLocation: " + lastLongitude + ", " + lastLatitude + ", " + longitudeOffset + ", " + latitudeOffset + ", " + listeners.size());
        for (LocationListener locationListener : listeners.keySet()) {
            locationListener.onLocationChanged(getLastLocation());
        }
        for (PendingIntent pendingIntent : pendingIntents) {
            try {
                pendingIntent.send();
            } catch (PendingIntent.CanceledException e) {
                if (BuildConfig.DEBUG) e.printStackTrace();
            }
        }
    }
    Random random = new Random();
    private Location getLastLocation() {
        if (lastLocation == null) {
            lastLocation = new Location(LocationManager.GPS_PROVIDER);
        }
        Location location = new Location(lastLocation);
        location.setLatitude(lastLatitude + latitudeOffset);
        location.setLongitude(lastLongitude + longitudeOffset);
        location.setAccuracy(random.nextFloat() * random.nextInt(50));
        location.setAltitude(random.nextFloat() * random.nextInt(200));
        location.setBearing(random.nextFloat() * random.nextInt(200));
        location.setSpeed(random.nextFloat());
        long curTime = System.currentTimeMillis();
        curTime = (curTime / 1000) * 1000;
        location.setTime(curTime);
        return location;
    }
}
