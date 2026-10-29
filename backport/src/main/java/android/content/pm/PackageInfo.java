package android.content.pm;

import java.lang.reflect.Field;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public final class PackageInfo {
    private PackageInfo() {}

    private static final ConcurrentMap<String, Integer> MAJOR = new ConcurrentHashMap<>();

    private static Object readField(Object target, String name) {
        if (target == null) return null;
        try {
            Field field = target.getClass().getField(name);
            return field.get(target);
        } catch (ReflectiveOperationException ignored) {
            return null;
        }
    }

    private static void writeIntField(Object target, String name, int value) {
        if (target == null) return;
        try {
            Field field = target.getClass().getField(name);
            field.setInt(target, value);
        } catch (ReflectiveOperationException ignored) {
        }
    }

    private static String key(Object info) {
        Object name = readField(info, "packageName");
        return name instanceof String ? (String) name : "@" + System.identityHashCode(info);
    }

    private static int readInt(Object info, String name, int fallback) {
        Object value = readField(info, name);
        return value instanceof Integer ? (Integer) value : fallback;
    }

    public static long getLongVersionCode(Object info) {
        if (info == null) throw new NullPointerException("info");
        int major = getVersionCodeMajor(info);
        int version = readInt(info, "versionCode", 0);
        return ((long) major << 32) | (((long) version) & 0xffffffffL);
    }

    public static void setLongVersionCode(Object info, long value) {
        if (info == null) throw new NullPointerException("info");
        writeIntField(info, "versionCode", (int) value);
        setVersionCodeMajor(info, (int) (value >>> 32));
    }

    public static int getVersionCodeMajor(Object info) {
        if (info == null) throw new NullPointerException("info");
        String key = key(info);
        return MAJOR.getOrDefault(key, readInt(info, "versionCodeMajor", 0));
    }

    public static void setVersionCodeMajor(Object info, int value) {
        if (info == null) throw new NullPointerException("info");
        MAJOR.put(key(info), value);
        writeIntField(info, "versionCodeMajor", value);
    }

    public static int getVersionCodeMajor(long value) {
        return (int) (value >>> 32);
    }

    public static int getVersionCode(long value) {
        return (int) value;
    }

    public static SigningInfo getSigningInfo(Object info) {
        if (info == null) return null;
        Object value = readField(info, "signatures");
        if (!(value instanceof Signature[])) return null;
        Signature[] signatures = (Signature[]) value;
        if (signatures.length == 0) return null;
        return new SigningInfo(signatures, signatures);
    }    
}
