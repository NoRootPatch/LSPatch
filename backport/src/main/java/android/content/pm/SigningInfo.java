package android.content.pm;

import java.lang.reflect.Field;

public final class SigningInfo {
    private final Signature[] apkContentsSigners;
    private final Signature[] signingCertificateHistory;

    public SigningInfo(Signature[] apkContentsSigners, Signature[] signingCertificateHistory) {
        this.apkContentsSigners = copy(apkContentsSigners);
        this.signingCertificateHistory = copy(signingCertificateHistory);
    }

    public SigningInfo(Object originalPackageInfo) {
        this(readSignatures(originalPackageInfo), readSignatures(originalPackageInfo));
    }

    private static Signature[] readSignatures(Object info) {
        if (info == null) return new Signature[0];
        try {
            Field field = info.getClass().getField("signatures");
            Object value = field.get(info);
            return value instanceof Signature[] ? (Signature[]) value : new Signature[0];
        } catch (ReflectiveOperationException ignored) {
            return new Signature[0];
        }
    }

    private static Signature[] copy(Signature[] values) {
        return values == null ? new Signature[0] : values.clone();
    }

    public Signature[] getApkContentsSigners() {
        return apkContentsSigners.clone();
    }

    public Signature[] getSigningCertificateHistory() {
        return signingCertificateHistory.clone();
    }

    public boolean hasMultipleSigners() {
        return apkContentsSigners.length > 1;
    }

    public boolean hasPastSigningCertificates() {
        return signingCertificateHistory.length > apkContentsSigners.length;
    }
}
