package oldlib.dalvik.system;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Build;
import dalvik.system.BaseDexClassLoader;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.UUID;

// This is just a temporary version; I'm actually very tired from doing so much research.
public class InMemoryDexClassLoader extends BaseDexClassLoader {

    private static final String TEMP_DEX_PREFIX = ".dex";
    private static volatile boolean sCleanedUp = false;
    
    public InMemoryDexClassLoader(ByteBuffer[] dexBuffers, String librarySearchPath, ClassLoader parent) {
        super(writeBuffersToDexPath(dexBuffers), null, librarySearchPath, parent);
    }

    public InMemoryDexClassLoader(ByteBuffer dexBuffer, ClassLoader parent) {
        this(new ByteBuffer[]{dexBuffer}, null, parent);
    }

    public InMemoryDexClassLoader(ByteBuffer[] dexBuffers, ClassLoader parent) {
        this(dexBuffers, null, parent);
    }

    public InMemoryDexClassLoader(byte[] dexBytes, ClassLoader parent) {
        this(new ByteBuffer[]{ByteBuffer.wrap(dexBytes)}, null, parent);
    }

    public InMemoryDexClassLoader(byte[] dexBytes) {
        this(new ByteBuffer[]{ByteBuffer.wrap(dexBytes)}, null, null);
    }

    public InMemoryDexClassLoader(ByteBuffer dexBuffer) {
        this(new ByteBuffer[]{dexBuffer}, null, null);
    }

    private static String writeBuffersToDexPath(ByteBuffer[] dexBuffers) {
        Context context = getApplicationContext();
        if (context == null) {
            throw new IllegalStateException("The context cannot be automatically found. Please ensure the call is on a valid Android process.");
        }
        ensureCleanupOrphanFiles(context);
        File codeCacheDir = context.getCodeCacheDir();
        StringBuilder dexPathBuilder = new StringBuilder();
        try {
            for (int i = 0; i < dexBuffers.length; i++) {
                ByteBuffer buffer = dexBuffers[i];
                File tempFile = new File(codeCacheDir, TEMP_DEX_PREFIX + UUID.randomUUID().toString().replace("-", "") + ".dex");
                try (FileOutputStream fos = new FileOutputStream(tempFile);
                    FileChannel channel = fos.getChannel()) {
                    ByteBuffer slice = buffer.duplicate();
                    while (slice.hasRemaining()) {
                        channel.write(slice);
                    }
                    channel.force(true);
                }
                tempFile.setReadable(false, false);
                tempFile.setReadable(true, true);
                tempFile.setWritable(false, false);
                tempFile.setWritable(true, true);
                tempFile.setExecutable(false, false);
                tempFile.deleteOnExit();
                if (i > 0) {
                    dexPathBuilder.append(File.pathSeparator);
                }
                dexPathBuilder.append(tempFile.getAbsolutePath());
            }
        } catch (IOException e) {
            throw new RuntimeException("Error during temporary DEX creation", e);
        }
        return dexPathBuilder.toString();
    }

    @SuppressLint("PrivateApi")
    private static Context getApplicationContext() {
        try {
            Class<?> activityThreadClass = Class.forName("android.app.ActivityThread");
            Method method = activityThreadClass.getDeclaredMethod("currentApplication");
            Context app = (Context) method.invoke(null);
            if (app != null) {
                return app.getApplicationContext();
            }
        } catch (Throwable ignored) {
        }
        return null;
    }

    private static void ensureCleanupOrphanFiles(Context context) {
        if (sCleanedUp) return;
        sCleanedUp = true;
        new Thread(() -> {
            try {
                File dir = context.getCodeCacheDir();
                File[] orphanFiles = dir.listFiles((d, name) -> name.startsWith(TEMP_DEX_PREFIX) && name.endsWith(".dex"));
                if (orphanFiles != null) {
                    for (File f : orphanFiles) {                   
                        f.delete();
                    }
                }
            } catch (Throwable ignored) {
            }
        }, "CompatDexCleaner").start();
    }
                                  }
