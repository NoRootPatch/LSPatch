package android.content.pm;

import java.lang.reflect.Field;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public final class PackageInfo {
    private PackageInfo() {}

    private static final class Bag {
        private static final ConcurrentMap<String, Integer> MAJOR =
                new ConcurrentHashMap<>();

        static int get(String key, int fallback) {
            Integer value = MAJOR.get(key);
            return value != null ? value : fallback;
        }

        static void put(String key, int value) {
            MAJOR.put(key, value);
        }
    }

    private static String key(PackageInfo info) {
        return info.packageName == null ? "@" + System.identityHashCode(info) : info.packageName;
    }

    public static long getLongVersionCode(PackageInfo info) {
        if (info == null) throw new NullPointerException("info");
        int major = getVersionCodeMajor(info);
        return ((long) major << 32) | (((long) info.versionCode) & 0xffffffffL);
    }

    public static void setLongVersionCode(PackageInfo info, long longVersionCode) {
        if (info == null) throw new NullPointerException("info");
        info.versionCode = (int) longVersionCode;
        writeIntField(info, "versionCodeMajor", (int) (longVersionCode >>> 32));
    }

    public static int getVersionCodeMajor(PackageInfo info) {
        if (info == null) throw new NullPointerException("info");
        int stored = Bag.get(key(info), Integer.MIN_VALUE);
        if (stored != Integer.MIN_VALUE) return stored;
    }

    public static int getVersionCodeMajor(long value) {
        return (int) (value >>> 32);
    }

    public static int getVersionCode(long value) {
        return (int) value;
    }

    private static void writeIntField(PackageInfo info, String name, int value) {
        Bag.put(key(info), value);
        try {
            Field field = info.getClass().getField(name);
            field.setInt(info, value);
        } catch (ReflectiveOperationException ignored) {
        }
    }

    private static int readIntField(
            PackageInfo info, String name, int fallback) {
        try {
            return info.getClass().getField(name).getInt(info);
        } catch (ReflectiveOperationException ignored) {
            return fallback;
        }
    }

    public static SigningInfo getSigningInfo(PackageInfo info) {
        if (info == null) {
            return null;
        }
        Signature[] signatures = info.signatures;
        if (signatures == null || signatures.length == 0) {
            return null;
        }
        return new SigningInfo(signatures, signatures);
    }
          }
                                       
