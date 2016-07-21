package info.noverguo.gpshack.utils;

import java.util.Arrays;

import de.robv.android.xposed.XposedHelpers;
import info.noverguo.gpshack.BuildConfig;

/**
 * Created by noverguo on 2016/7/8.
 */
public class XposedUtils {
    public static void findAndHookMethod(Class<?> clazz, String methodName, Object... parameterTypesAndCallback) {
        int size = parameterTypesAndCallback.length - 1;
        Class[] argsClass = new Class[size];
        for (int i=0;i<size;++i) {
            argsClass[i] = (Class) parameterTypesAndCallback[i];
        }
        Class newClazz = ClassUtils.getDeclaredMethodClass(clazz, methodName, argsClass);
        if (newClazz == null) {
            if (BuildConfig.DEBUG) new NullPointerException("XposedUtils.findAndHookMethod error: " + clazz.getName() + methodName + Arrays.toString(argsClass)).printStackTrace();
            return;
        }
        XposedHelpers.findAndHookMethod(newClazz, methodName, parameterTypesAndCallback);
    }

    public static void findAndHookMethod(String className, ClassLoader classLoader, String methodName, Object... parameterTypesAndCallback) throws ClassNotFoundException {
        Class clazz = ClassUtils.loadClass(className, classLoader);
        findAndHookMethod(clazz, methodName, parameterTypesAndCallback);
    }
}
