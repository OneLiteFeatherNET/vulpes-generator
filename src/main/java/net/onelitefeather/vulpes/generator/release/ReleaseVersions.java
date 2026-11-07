package net.onelitefeather.vulpes.generator.release;

import java.math.BigInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * The version rules of content releases: {@code X.Y.Z} is a release, {@code X.Y.Z-SNAPSHOT} a snapshot, and the numeric
 * parts have no leading zeros. Versions are compared by their numbers, so {@code 1.10.0} is newer than {@code 1.9.0}.
 *
 * @author TheMeinerLP
 * @version 1.0.0
 * @since 1.0.0
 */
public final class ReleaseVersions {

    /** The channel of a release version. */
    public static final String RELEASE = "release";
    /** The channel of a snapshot version. */
    public static final String SNAPSHOT = "snapshot";

    private static final Pattern VERSION = Pattern.compile(
            "(0|[1-9][0-9]*)\\.(0|[1-9][0-9]*)\\.(0|[1-9][0-9]*)(-SNAPSHOT)?");

    private ReleaseVersions() {
    }

    /**
     * Tells whether a version is {@code X.Y.Z} or {@code X.Y.Z-SNAPSHOT} with no leading zeros in its numeric parts.
     *
     * @param version the version to check, may be {@code null}
     * @return {@code true} for a valid version
     */
    public static boolean isValid(String version) {
        return version != null && VERSION.matcher(version).matches();
    }

    /**
     * Returns the channel of a valid version.
     *
     * @param version a version that {@link #isValid(String)} accepts
     * @return {@link #SNAPSHOT} for {@code X.Y.Z-SNAPSHOT}, {@link #RELEASE} otherwise
     */
    public static String channel(String version) {
        return version.endsWith("-SNAPSHOT") ? SNAPSHOT : RELEASE;
    }

    /**
     * Compares two valid versions by their numeric parts. A snapshot and a release of the same numbers compare equal.
     *
     * @param left  a valid version
     * @param right a valid version
     * @return a negative number, zero or a positive number when {@code left} is older, equal or newer than {@code right}
     * @throws IllegalArgumentException if a version is not valid
     */
    public static int compare(String left, String right) {
        Matcher a = matcher(left);
        Matcher b = matcher(right);
        for (int group = 1; group <= 3; group++) {
            int result = new BigInteger(a.group(group)).compareTo(new BigInteger(b.group(group)));
            if (result != 0) {
                return result;
            }
        }
        return 0;
    }

    private static Matcher matcher(String version) {
        Matcher matcher = VERSION.matcher(version == null ? "" : version);
        if (!matcher.matches()) {
            throw new IllegalArgumentException("not a content version: " + version);
        }
        return matcher;
    }
}
