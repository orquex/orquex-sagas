package co.orquex.sagas.domain.version;

public final class OrquexSagasVersion {

    private static final int MAJOR = 0;
    private static final int MINOR = 0;
    private static final int PATCH = 1;

    public static final long SERIAL_VERSION = getVersion().hashCode();

    public static String getVersion() {
        return MAJOR + "." + MINOR + "." + PATCH;
    }
}
