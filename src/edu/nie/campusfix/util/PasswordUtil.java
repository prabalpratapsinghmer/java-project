package edu.nie.campusfix.util;

import edu.nie.campusfix.exception.CampusFixException;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.util.Arrays;
import java.util.HexFormat;

/**
 * Enterprise-grade cryptographic password hashing using PBKDF2WithHmacSHA256.
 * Generates unique 16-byte random salts per password, executes 65,536 iterations,
 * and performs constant-time equality checks to mitigate timing attacks.
 */
public final class PasswordUtil {
    private static final String ALGORITHM = "PBKDF2WithHmacSHA256";
    private static final int ITERATIONS = 65_536;
    private static final int KEY_LENGTH_BITS = 256;
    private static final int SALT_BYTES = 16;
    private static final SecureRandom RANDOM = new SecureRandom();

    private PasswordUtil() {
        // Utility class
    }

    /**
     * Hashes a password using PBKDF2WithHmacSHA256 with a freshly generated salt.
     * Wipes password array from memory after hashing.
     *
     * @param password Plaintext password characters
     * @return Formatted token string {@code <saltHex>:<hashHex>}
     */
    public static String hashPassword(char[] password) {
        if (password == null || password.length == 0) {
            throw new IllegalArgumentException("Password cannot be null or empty");
        }

        byte[] salt = new byte[SALT_BYTES];
        RANDOM.nextBytes(salt);

        try {
            byte[] hash = pbkdf2(password, salt, ITERATIONS, KEY_LENGTH_BITS);
            String saltHex = HexFormat.of().formatHex(salt);
            String hashHex = HexFormat.of().formatHex(hash);
            return saltHex + ":" + hashHex;
        } finally {
            Arrays.fill(password, '\0');
        }
    }

    /**
     * Verifies a candidate password against a stored {@code <saltHex>:<hashHex>} token.
     * Wipes password array from memory after verification.
     *
     * @param password Candidate password characters
     * @param storedToken Formatted token string
     * @return {@code true} if password matches, {@code false} otherwise
     */
    public static boolean verifyPassword(char[] password, String storedToken) {
        if (password == null || storedToken == null || !storedToken.contains(":")) {
            return false;
        }

        String[] parts = storedToken.split(":");
        if (parts.length != 2) {
            return false;
        }

        try {
            byte[] salt = HexFormat.of().parseHex(parts[0]);
            byte[] expectedHash = HexFormat.of().parseHex(parts[1]);
            byte[] actualHash = pbkdf2(password, salt, ITERATIONS, KEY_LENGTH_BITS);

            return MessageDigest.isEqual(expectedHash, actualHash);
        } catch (IllegalArgumentException e) {
            return false;
        } finally {
            Arrays.fill(password, '\0');
        }
    }

    private static byte[] pbkdf2(char[] password, byte[] salt, int iterations, int keyLengthBits) {
        try {
            PBEKeySpec spec = new PBEKeySpec(password, salt, iterations, keyLengthBits);
            SecretKeyFactory factory = SecretKeyFactory.getInstance(ALGORITHM);
            return factory.generateSecret(spec).getEncoded();
        } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
            throw new CampusFixException("Cryptographic algorithm unavailable: " + ALGORITHM, e);
        }
    }
}
