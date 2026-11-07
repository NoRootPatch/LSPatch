package j$.lang;

import java.lang.reflect.Method;
import java.lang.reflect.Constructor;
import java.lang.reflect.Member;

public abstract class Executable implements Member {
    public static boolean isSupported() {
        return false; 
    }
}
