package oldlib.dalvik.system;

import android.os.MemoryFile;
import oldlib.android.os.SharedMemory;

import java.io.File;
import java.io.FileDescriptor;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public final class InMemoryDexClassLoader extends ClassLoader {
    private final Object[] dexFiles;
    private final SharedMemory[] sharedMemories;
    private final ByteBuffer[] mappings;

    public InMemoryDexClassLoader(ByteBuffer buffer, ClassLoader parent) throws IOException {
        this(new ByteBuffer[]{buffer}, parent);
    }

    public InMemoryDexClassLoader(ByteBuffer[] buffers, ClassLoader parent) throws IOException {
        super(parent);
        if (buffers == null || buffers.length == 0) {
            throw new IllegalArgumentException("buffers must not be empty");
        }
        List<SharedMemory> memories = new ArrayList<>();
        List<ByteBuffer> maps = new ArrayList<>();
        List<Object> files = new ArrayList<>();
        try {
            Class<?> dexClass = Class.forName("dalvik.system.DexFile");
            Constructor<?> dexConstructor = dexClass.getConstructor(String.class);
            for (int i = 0; i < buffers.length; i++) {
                ByteBuffer source = buffers[i];
                if (source == null) {
                    throw new NullPointerException("buffers[" + i + "]");
                }
                ByteBuffer view = source.duplicate();
                byte[] bytes = new byte[view.remaining()];
                view.get(bytes);
                if (bytes.length == 0) {
                    throw new IOException("Empty DEX buffer at index " + i);
                }
                SharedMemory memory = SharedMemory.create("InMemoryDexClassLoader-" + i, bytes.length);
                memories.add(memory);
                ByteBuffer mapping = memory.mapReadWrite();
                maps.add(mapping);
                if (mapping.capacity() < bytes.length) {
                    throw new IOException("Shared mapping is too small");
                }
                mapping.position(0);
                mapping.put(bytes);
                mapping.position(0);
                mapping.limit(bytes.length);
                FileDescriptor fd = memory.getFileDescriptor();
                int rawFd = getRawFd(fd);
                if (rawFd < 0) {
                    throw new IOException("Invalid shared-memory FD");
                }
                String path = "/proc/self/fd/" + rawFd;
                Object dexFile = dexConstructor.newInstance(path);
                files.add(dexFile);
            }
            this.sharedMemories = memories.toArray(new SharedMemory[0]);
            this.mappings = maps.toArray(new ByteBuffer[0]);
            this.dexFiles = files.toArray(new Object[0]);
            appendDexElements(this.dexFiles, parent);
        } catch (Throwable error) {
            closeDexFiles(files);
            unmapAll(maps);
            closeMemories(memories);
            if (error instanceof IOException) {
                throw (IOException) error;
            }
            if (error instanceof RuntimeException) {
                throw (RuntimeException) error;
            }
            throw new IOException("Unable to load in-memory DEX", error);
        }
    }

    public InMemoryDexClassLoader(Object dexFile, ClassLoader parent) {
        super(parent);
        if (dexFile == null) {
            throw new NullPointerException("dexFile");
        }
        this.dexFiles = new Object[]{dexFile};
        this.sharedMemories = new SharedMemory[0];
        this.mappings = new ByteBuffer[0];
        appendDexElements(this.dexFiles, parent);
    }

    private static int getRawFd(FileDescriptor fd) throws ReflectiveOperationException {
        Field field = FileDescriptor.class.getDeclaredField("descriptor");
        field.setAccessible(true);
        return field.getInt(fd);
    }

    private static void appendDexElements(Object[] files, ClassLoader parent) {
        if (parent == null) {
            throw new IllegalArgumentException("A non-null BaseDexClassLoader parent is required");
        }
        try {
            Class<?> dexClass = Class.forName("dalvik.system.DexFile");
            Class<?> baseClass = Class.forName("dalvik.system.BaseDexClassLoader");
            if (!baseClass.isInstance(parent)) {
                throw new IllegalArgumentException("Parent must be a BaseDexClassLoader");
            }

            Field pathListField = baseClass.getDeclaredField("pathList");
            pathListField.setAccessible(true);
            Object pathList = pathListField.get(parent);
            Class<?> pathListClass = Class.forName("dalvik.system.DexPathList");
            Field elementsField = pathListClass.getDeclaredField("dexElements");
            elementsField.setAccessible(true);
            Object[] oldElements = (Object[]) elementsField.get(pathList);
            Class<?> elementClass = Class.forName("dalvik.system.DexPathList$Element");
            Constructor<?> ctor = elementClass.getDeclaredConstructor(File.class, boolean.class, File.class, dexClass);
            ctor.setAccessible(true);
            Object[] added = new Object[files.length];
            for (int i = 0; i < files.length; i++) {
                added[i] = ctor.newInstance(null, false, null, files[i]);
            }
            Object[] merged = Arrays.copyOf(oldElements, oldElements.length + added.length);
            System.arraycopy(added, 0, merged, oldElements.length, added.length);
            elementsField.set(pathList, merged);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Unable to append DexPathList elements", e);
        }
    }

    private static void closeDexFiles(List<Object> files) {
        for (Object file : files) {
            try {
                Method close = file.getClass().getMethod("close");
                close.invoke(file);
            } catch (Throwable ignored) {
            }
        }
    }

    private static void unmapAll(List<ByteBuffer> maps) {
        for (ByteBuffer map : maps) {
            try {
                SharedMemory.unmap(map);
            } catch (Throwable ignored) {
            }
        }
    }

    private static void closeMemories(List<SharedMemory> memories) {
        for (SharedMemory memory : memories) {
            try {
                memory.close();
            } catch (Throwable ignored) {
            }
        }
    }
}
