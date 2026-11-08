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
    private static long sArtMethodsAddrOffset;
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
                /*
                 * methods 对应的数据结构
                 * template<typename T> class LengthPrefixedArray {
                 *     uint32_t size_;
                 *     uint8_t data_[0];
                 * };
                 *
                 * data_ 就是ArtMethod数组，怎么得到这个地址呢
                 * 按照内存排序，将指针从 LengthPrefixedArray 首地址加上 sizeof(size_) 就得到 data_ 的数组地址了
                 *
                 * maAddr 是模拟的第一个方法的地址
                 * methods 是模拟的 LengthPrefixedArray 的地址
                 *
                 * 为什么要多减1个sArtMethodSize，模拟的类还有个默认构造函数，也是ArtMethod，所以要减掉
                 * maAddr - methods - sArtMethodSize;
                 * 通过如上方法就得到 sizeof(size_) 的大小了，这里这么算是为了兼容 32/64 不需要指定值，按照系统的内存排序得到偏移大小
                 */
                sArtMethodsAddrOffset = maAddr - methods - sArtMethodSize;

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
    private static Method sSetHiddenApiExemptions;
    private static String[] sExemptions;

    static synchronized boolean bypass(int sdkInt, String... methodNames) {
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

            if (sSetHiddenApiExemptions == null) {
                sSetHiddenApiExemptions = findMethod(vm.getClass(), "setHiddenApiExemptions");
            }
            String[] exemptions = methodNames == null ? new String[]{""} : methodNames.clone();
            if (!applyExemptions(exemptions)) {
                return false;
            }
            sExemptions = exemptions;
        }
        return true;
    }

    // 其他代码可能覆盖进程的豁免列表；仅恢复调用方明确设置过的范围。
    static synchronized boolean restore() {
        return sExemptions != null && applyExemptions(sExemptions);
    }

    private static boolean applyExemptions(String[] exemptions) {
        if (vmRuntime == null || sSetHiddenApiExemptions == null) {
            return false;
        }
        try {
            sSetHiddenApiExemptions.invoke(vmRuntime, (Object) exemptions);
            return true;
        } catch (Throwable t) {
            t.printStackTrace();
            return false;
        }
    }

    private static Object vmRuntime;

    private static Object getVMRuntime() {
        if (vmRuntime != null) {
            return vmRuntime;
        }
        try {
            Class classVMRuntime = Class.forName("dalvik.system.VMRuntime");
            Method method = findMethod(classVMRuntime, "getRuntime");
            if (method != null) {
                vmRuntime = method.invoke(null);
            }
        } catch (Throwable e) {
            e.printStackTrace();
        }

        return vmRuntime;
    }

    private static Method findMethod(Class clazz, String name) {
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
                    long artMethodAddress = clsMethodsOffset + sArtMethodsAddrOffset + i * sArtMethodSize;
                    unsafe.putLong(mh, sArtFieldOrMethodOffset, artMethodAddress);
                    unsafe.putObject(mh, sInfoOffset, null);

                    Object methodHandleInfo = MethodHandles.lookup().revealDirect(mh);
                    Object member = unsafe.getObject(methodHandleInfo, sMemberOffset);
                    if (Method.class.isInstance(member)) {
                        Method method = (Method) member;
                        method.setAccessible(true);
                        if (method.getName().equals(name)) {
                            return method;
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
