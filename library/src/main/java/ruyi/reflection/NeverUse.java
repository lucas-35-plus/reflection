package ruyi.reflection;

import java.lang.reflect.Member;

/**
 * compat for android9-android13
 * Maybe update for android newer version
 */
class NeverUse {

    // same as aosp source-code
    private static class MethodHandle {
        private Object type;
        private Object nominalType;
        private MethodHandle cachedSpreadInvoker;
        protected int handleKind;
        protected long artFieldOrMethod;
    }

    // same as aosp source-code
    private static class MethodHandleImpl extends MethodHandle {
        private MethodHandleInfo info;
    }

    // same as aosp source-code
    private interface MethodHandleInfo {

    }

    // same as aosp source-code
    private static class HandleInfo implements MethodHandleInfo {
        private Member member;
        private java.lang.invoke.MethodHandle handle;
    }

    // same as aosp source-code
    private static class Class {
        private transient ClassLoader classLoader;
        private transient Class componentType;
        private transient Object dexCache;
        private transient Object extData;
        private transient Object[] ifTable;
        private transient String name;
        private transient Class superClass;
        private transient Object vtable;
        private transient long iFields;
        private transient long methods;
    }

    private static class Caller {
        public static void a() {

        }

        public static void b() {

        }
    }

    static java.lang.Class getClass(int sdkInt) {
        return Class.class;
    }

    static java.lang.Class getMethodHandle(int sdkInt) {
        return MethodHandle.class;
    }

    static java.lang.Class getMethodHandleImpl(int sdkInt) {
        return MethodHandleImpl.class;
    }

    static java.lang.Class getHandleInfo(int sdkInt) {
        return HandleInfo.class;
    }

    static java.lang.Class getCaller() {
        return Caller.class;
    }
}
