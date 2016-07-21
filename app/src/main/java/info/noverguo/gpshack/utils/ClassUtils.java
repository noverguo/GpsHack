package info.noverguo.gpshack.utils;

/**
 * Created by noverguo on 2016/6/8.
 */
public class ClassUtils {
    public static Class loadClass(String className, ClassLoader classLoader) throws ClassNotFoundException {
        try {
            char start = className.charAt(0);
            if (start == '[') {
                return Class.forName(className);
            }
            if (className.equals("boolean")) {
                return boolean.class;
            }
            if (className.equals("byte")) {
                return byte.class;
            }
            if (className.equals("char")) {
                return char.class;
            }
            if (className.equals("short")) {
                return short.class;
            }
            if (className.equals("int")) {
                return int.class;
            }
            if (className.equals("long")) {
                return long.class;
            }
            if (className.equals("float")) {
                return float.class;
            }
            if (className.equals("double")) {
                return double.class;
            }
            if (className.equals("void")) {
                return void.class;
            }
            return Class.forName(className);
        } catch (ClassNotFoundException e) {
            return classLoader.loadClass(className);
        }
    }

    public static String getClassName(Object obj) {
        if (obj == null) {
            return "";
        }
        return obj.getClass().getName();
    }

    public static String toSimpleName(String className) {
        className = toDescName(className);
        int idx = className.lastIndexOf('.');
        if (idx > 0) {
            className = className.substring(idx + 1);
        }
        return className;
    }

    public static String toDescName(String className) {
        int arrCount = className.lastIndexOf('[') + 1;
        if (arrCount > 0) {
            className = className.substring(arrCount);
        }
        if (className.length() == 1) {
            switch (className.charAt(0)) {
                case 'Z':
                    className = "boolean";
                    break;
                case 'B':
                    className = "byte";
                    break;
                case 'C':
                    className = "char";
                    break;
                case 'S':
                    className = "short";
                    break;
                case 'I':
                    className = "int";
                    break;
                case 'J':
                    className = "long";
                    break;
                case 'F':
                    className = "float";
                    break;
                case 'D':
                    className = "double";
                    break;
            }
        }
        for (int i=0;i<arrCount;++i) {
            className += "[]";
        }
        return className;
    }

    public static Class getDeclaredMethodClass(Class clazz, String methodName, Class... params) {
        do {
            try {
                clazz.getDeclaredMethod(methodName, params);
                return clazz;
            } catch (NoSuchMethodException e) {
                clazz = clazz.getSuperclass();
                if (clazz == null) {
                    return null;
                }
            }
        } while (true);
    }
}