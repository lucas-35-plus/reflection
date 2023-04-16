package ruyi.reflection;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

import sun.misc.Unsafe;

/**
 * MethodHandles Api Add In Android 26!
 */
class ByPasser {

    // Class.methods
    // 所有方法的起始地址，注意这个地址存放的值代表的是从这个地址开始会有N个方法
    private static long sMethodsOffset;

    // MethodHandle.artFieldOrMethod native address
    private static long sArtFieldOrMethodOffset;

    // methods地址开始，需要再偏移一段地址大小才能得到第一个method的地址
    // methodsOffst + methodsAddrOffset = 第一个ArtMethod的地址
    private static long sMethodsAddrOffset;
    // ArtMethod 结构体大小
    private static long sArtMethodSize;

    // MethodHandleImpl.info native address
    private static long sInfoOffset;
    // HandleInfo.member native address
    private static long sMemberOffset;

    private static void init(int sdkInt) {
        if (sMethodsOffset > 0) {
            return;
        }

        try {
            Unsafe unsafe = getUnsafe();
            if (unsafe != null) {
                sMethodsOffset = unsafe.objectFieldOffset(
                        NeverUse.getClass(sdkInt).getDeclaredField("methods"));
                sArtFieldOrMethodOffset = unsafe.objectFieldOffset(
                        NeverUse.getMethodHandle(sdkInt).getDeclaredField("artFieldOrMethod"));

                MethodHandle ma = MethodHandles.lookup().unreflect(
                        NeverUse.getCaller().getDeclaredMethod("a"));
                MethodHandle mb = MethodHandles.lookup().unreflect(
                        NeverUse.getCaller().getDeclaredMethod("b"));
                long maAddr = unsafe.getLong(ma, sArtFieldOrMethodOffset);
                long mbAddr = unsafe.getLong(mb, sArtFieldOrMethodOffset);
                long methods = unsafe.getLong(NeverUse.getCaller(), sMethodsOffset);
                sArtMethodSize = mbAddr - maAddr;
                // 为什么多减1个？因为还有个默认的无参构造函数，他肯定是排在a方法前面的，所有要再减1个
                sMethodsAddrOffset = maAddr - methods - sArtMethodSize;

                sInfoOffset = unsafe.objectFieldOffset(
                        NeverUse.getMethodHandleImpl(sdkInt).getDeclaredField("info"));
                sMemberOffset = unsafe.objectFieldOffset(
                        NeverUse.getHandleInfo(sdkInt).getDeclaredField("member"));
            }
        } catch (Throwable t) {
            t.printStackTrace();
        }
    }

    private static Unsafe unsafe;

    private static Unsafe getUnsafe() {
        try {
            Field f = Unsafe.class.getDeclaredField("theUnsafe");
            f.setAccessible(true);
            unsafe = (Unsafe) f.get(null);
        } catch (Throwable t) {
            t.printStackTrace();
        }

        return unsafe;
    }

    //*************************************************************************
    // VMRuntime setHiddenApiExemptions ByPass All Hidden Api
    //*************************************************************************
    static boolean bypass(int sdkInt, String... methodNames) {
        // >= Android P Need to ByPass HiddenApi
        // < Android P No HiddenApi Limit
        // MethodHandles add in android 26
        if (sdkInt >= 28) {
            ByPasser.init(sdkInt);
            // >= Android P
            Object vm = getVMRuntime();
            if (vm == null) {
                return false;
            }

            methodNames = methodNames == null ? new String[]{""} : methodNames;
            invoke(vm.getClass(), vm, "setHiddenApiExemptions", (Object) methodNames);
        }
        return true;
    }

    private static Object vmRuntime;

    private static Object getVMRuntime() {
        if (vmRuntime != null) {
            return vmRuntime;
        }
        try {
            Class classVMRuntime = Class.forName("dalvik.system.VMRuntime");
            vmRuntime = invoke(classVMRuntime, null, "getRuntime");
        } catch (Throwable e) {
            e.printStackTrace();
        }

        return vmRuntime;
    }

    private static Object invoke(Class clazz, Object target, String name, Object... args) {
        Unsafe unsafe = ByPasser.getUnsafe();
        if (unsafe == null || sMethodsOffset <= 0) {
            return null;
        }

        // mh real type is MethodHandleImpl
        // mh is a fake object to set artMethodNativePointer to reveal real MethodHandleInfo
        try {
            MethodHandle mh = MethodHandles.lookup().unreflect(
                    NeverUse.getCaller().getDeclaredMethod("a"));

            long clsMethodsOffset = unsafe.getLong(clazz, sMethodsOffset);
            int methodsCount = unsafe.getInt(clsMethodsOffset);
            for (int i = 0; i < methodsCount; i++) {
                try {
                    long artMethodAddress = clsMethodsOffset + sMethodsAddrOffset + i * sArtMethodSize;
                    unsafe.putLong(mh, sArtFieldOrMethodOffset, artMethodAddress);
                    unsafe.putObject(mh, sInfoOffset, null);

                    Object methodHandleInfo = MethodHandles.lookup().revealDirect(mh);
                    Object member = unsafe.getObject(methodHandleInfo, sMemberOffset);
                    if (Method.class.isInstance(member)) {
                        Method method = (Method) member;
                        method.setAccessible(true);
                        if (method.getName().equals(name)) {
                            return method.invoke(target, args);
                        }
                    }
                } catch (Throwable t) {
                }
            }
        } catch (Throwable t) {
            t.printStackTrace();
        }

        return null;
    }
}
