package ruyi.reflection;

import java.lang.reflect.Constructor;
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
        Field field = field(clazz, name);
        if (field != null) {
            try {
                return (T) field.get(obj);
            } catch (Throwable t) {
                t.printStackTrace();
            }
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
        Field field = field(clazz, name);
        if (field != null) {
            try {
                field.set(obj, value);
            } catch (Throwable t) {
                t.printStackTrace();
            }
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
        for (int attempt = 0; attempt < 2; attempt++) {
            boolean found = false;
            Class current = clazz;
            while (current != null) {
                for (Method tmp : current.getDeclaredMethods()) {
                    if (tmp.getName().equals(name)) {
                        Class[] paramTypes = tmp.getParameterTypes();
                        int paramLen = paramTypes == null ? 0 : paramTypes.length;
                        if (paramLen == argsL) {
                            found = true;
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
                        }
                    }
                }
                current = current.getSuperclass();
            }
            // 仅查找失败才恢复豁免重试，不能因业务异常再次执行方法。
            if (found || attempt != 0 || !ByPasser.restore()) {
                break;
            }
        }

        return null;
    }

    //********************************************************************
    // Reflect
    //********************************************************************
    public static <T> T newInstance(Class<T> cls, Object...args) {
        if (cls == null) {
            return null;
        }

        int argsLen = args == null ? 0 : args.length;
        for (int attempt = 0; attempt < 2; attempt++) {
            boolean found = false;
            for (Constructor constructor : cls.getDeclaredConstructors()) {
                Class[] paramTypes = constructor.getParameterTypes();
                int paramLen = paramTypes == null ? 0 : paramTypes.length;
                if (paramLen == argsLen) {
                    found = true;
                    try {
                        return (T) constructor.newInstance(args);
                    } catch (Throwable t) {
                        t.printStackTrace();
                    }
                }
            }
            if (found || attempt != 0 || !ByPasser.restore()) {
                break;
            }
        }

        return null;
    }

    public static Field field(Class cls, String name) {
        if (cls == null || name == null) {
            return null;
        }
        for (int attempt = 0; attempt < 2; attempt++) {
            Class current = cls;
            while (current != null) {
                for (Field tmp : current.getDeclaredFields()) {
                    if (tmp.getName().equals(name)) {
                        try {
                            tmp.setAccessible(true);
                            return tmp;
                        } catch (Throwable t) {
                            t.printStackTrace();
                            return null;
                        }
                    }
                }
                current = current.getSuperclass();
            }
            if (attempt != 0 || !ByPasser.restore()) {
                break;
            }
        }
        return null;
    }

    public static Method method(Class cls, String name, Class... params) {
        if (cls == null || name == null) {
            return null;
        }
        int argsLen = params == null ? 0 : params.length;
        for (int attempt = 0; attempt < 2; attempt++) {
            Class current = cls;
            while (current != null) {
                for (Method tmp : current.getDeclaredMethods()) {
                    if (tmp.getName().equals(name)) {
                        Class[] paramTypes = tmp.getParameterTypes();
                        int paramLen = paramTypes == null ? 0 : paramTypes.length;
                        if (paramLen == argsLen) {
                            try {
                                tmp.setAccessible(true);
                                return tmp;
                            } catch (Throwable t) {
                                t.printStackTrace();
                            }
                            return null;
                        }
                    }
                }
                current = current.getSuperclass();
            }
            if (attempt != 0 || !ByPasser.restore()) {
                break;
            }
        }
        return null;
    }
}
