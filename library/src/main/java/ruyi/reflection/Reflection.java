package ruyi.reflection;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

public class Reflection {

    //********************************************************************
    // ByPass Hidden Api
    //********************************************************************
    public static void bypassAll(int sdkInt) {
        bypass(sdkInt, "");
    }

    public static void bypass(int sdkInt, String... methodNames) {
        ByPasser.bypass(sdkInt, methodNames);
    }

    //********************************************************************
    // Reflect Get Class's Field
    //********************************************************************
    public static <T> T get(Object obj, String name) {
        if (obj == null) {
            return null;
        }
        return get(obj.getClass(), obj, name);
    }

    public static <T> T getStatic(Class clazz, String name) {
        return get(clazz, null, name);
    }

    private static <T> T get(Class clazz, Object obj, String name) {
        if (clazz == null || name == null) {
            return null;
        }

        while (clazz != null) {
            for (Field tmp : clazz.getDeclaredFields()) {
                if (tmp.getName().equals(name)) {
                    try {
                        tmp.setAccessible(true);
                        if (Modifier.isStatic(tmp.getModifiers())) {
                            return (T) tmp.get(clazz);
                        } else {
                            return (T) tmp.get(obj);
                        }
                    } catch (Throwable t) {
                        t.printStackTrace();
                    }
                    return null;
                }
            }
            clazz = clazz.getSuperclass();
        }

        return null;
    }

    //********************************************************************
    // Reflect Set Class's Field
    //********************************************************************
    public static void set(Object obj, String name, Object value) {
        if (obj == null) {
            return;
        }
        set(obj.getClass(), obj, name, value);
    }

    public static void setStatic(Class clazz, String name, Object value) {
        set(clazz, null, name, value);
    }

    private static void set(Class clazz, Object obj, String name, Object value) {
        if (clazz == null || name == null) {
            return;
        }

        while (clazz != null) {
            for (Field tmp : clazz.getDeclaredFields()) {
                if (tmp.getName().equals(name)) {
                    try {
                        tmp.setAccessible(true);
                        if (Modifier.isStatic(tmp.getModifiers())) {
                            tmp.set(clazz, value);
                        } else {
                            tmp.set(obj, value);
                        }
                    } catch (Throwable t) {
                        t.printStackTrace();
                    }
                    return;
                }
            }
            clazz = clazz.getSuperclass();
        }

    }

    //********************************************************************
    // Reflect Invoke Class's Method
    //********************************************************************
    public static <T> T invoke(Object obj, String name, Object... args) {
        if (obj == null) {
            return null;
        }

        return invoke(obj.getClass(), obj, name, args);
    }

    public static <T> T invokeStatic(Class clazz, String name, Object... args) {
        return invoke(clazz, null, name, args);
    }

    private static <T> T invoke(Class clazz, Object obj, String name, Object... args) {
        if (clazz == null || name == null) {
            return null;
        }

        int argsL = args == null ? 0 : args.length;
        while (clazz != null) {
            for (Method tmp : clazz.getDeclaredMethods()) {
                if (tmp.getName().equals(name) && tmp.getParameterTypes().length == argsL) {
                    try {
                        tmp.setAccessible(true);
                        if (Modifier.isStatic(tmp.getModifiers())) {
                            return (T) tmp.invoke(clazz, args);
                        } else {
                            return (T) tmp.invoke(obj, args);
                        }
                    } catch (Throwable t) {
                        t.printStackTrace();
                    }

                    return null;
                }
            }
            clazz = clazz.getSuperclass();
        }

        return null;
    }
}