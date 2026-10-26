package android.content.pm;

public final class SigningInfo {

    private final Signature[] apkContentsSigners;
    private final Signature[] signingCertificateHistory;

    public SigningInfo(Signature[] apkContentsSigners, Signature[] signingCertificateHistory) {
        this.apkContentsSigners = copy(apkContentsSigners);
        this.signingCertificateHistory = copy(signingCertificateHistory);
    }

    public SigningInfo(PackageInfo info) {
        this(info == null ? null : info.signatures, info == null ? null : info.signatures);
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
