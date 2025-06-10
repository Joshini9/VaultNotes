package com.vaultnotes;
import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.Arrays;
import java.security.GeneralSecurityException;

/**
 * Utility class for handling security-related operations such as
 * encryption, decryption, password hashing, and verification.
 */
public class SecurityManager {

    // Algorithm constants
    private static final String ALGORITHM_AES = "AES";
    private static final String ALGORITHM_AES_GCM_NOPADDING = "AES/GCM/NoPadding";
    private static final String ALGORITHM_PBKDF2 = "PBKDF2WithHmacSHA256";
    private static final int KEY_LENGTH_AES = 256; // bits
    private static final int IV_LENGTH_GCM = 12; // bytes
    private static final int TAG_LENGTH_GCM = 16; // bytes (128 bits)
    private static final int ITERATIONS_PBKDF2 = 65536;
    private static final int KEY_LENGTH_PBKDF2 = 256; // bits for derived key

    private static final SecureRandom secureRandom = new SecureRandom();

    /**
     * Derives a SecretKey from a password and salt using PBKDF2.
     * This key can then be used for AES encryption.
     *
     * @param password The plain-text password.
     * @param salt     The salt used for key derivation.
     * @return A SecretKey suitable for AES.
     * @throws NoSuchAlgorithmException If PBKDF2 algorithm is not found.
     * @throws InvalidKeySpecException  If the key specification is invalid.
     */
    public static SecretKey deriveKey(String password, byte[] salt)
            throws NoSuchAlgorithmException, InvalidKeySpecException {
        PBEKeySpec spec = new PBEKeySpec(password.toCharArray(), salt, ITERATIONS_PBKDF2, KEY_LENGTH_PBKDF2);
        SecretKeyFactory factory = SecretKeyFactory.getInstance(ALGORITHM_PBKDF2);
        byte[] keyBytes = factory.generateSecret(spec).getEncoded();
        return new SecretKeySpec(keyBytes, ALGORITHM_AES);
    }

    /**
     * Generates a random salt for key derivation or password hashing.
     *
     * @return A byte array representing the salt.
     */
    public static byte[] generateSalt() {
        byte[] salt = new byte[16]; // 16 bytes for salt
        secureRandom.nextBytes(salt);
        return salt;
    }

    /**
     * Encrypts plain text using AES/GCM.
     *
     * @param plainText     The text to encrypt.
     * @param encryptionKey The secret key used for encryption.
     * @return The Base64 encoded string containing IV and ciphertext+tag.
     * @throws GeneralSecurityException If an encryption error occurs.
     */
    public static String encryptAES(String plainText, SecretKey encryptionKey)
            throws GeneralSecurityException {
        Cipher cipher = Cipher.getInstance(ALGORITHM_AES_GCM_NOPADDING);

        byte[] iv = new byte[IV_LENGTH_GCM];
        secureRandom.nextBytes(iv); // Generate a random IV for each encryption

        GCMParameterSpec gcmParameterSpec = new GCMParameterSpec(TAG_LENGTH_GCM * 8, iv);
        cipher.init(Cipher.ENCRYPT_MODE, encryptionKey, gcmParameterSpec);

        byte[] cipherText = cipher.doFinal(plainText.getBytes(java.nio.charset.StandardCharsets.UTF_8));

        // Concatenate IV and ciphertext+tag for storage
        byte[] encryptedData = new byte[IV_LENGTH_GCM + cipherText.length];
        System.arraycopy(iv, 0, encryptedData, 0, IV_LENGTH_GCM);
        System.arraycopy(cipherText, 0, encryptedData, IV_LENGTH_GCM, cipherText.length);

        return Base64.getEncoder().encodeToString(encryptedData);
    }

    /**
     * Decrypts AES/GCM encrypted text.
     *
     * @param encryptedText The Base64 encoded string containing IV and ciphertext+tag.
     * @param encryptionKey The secret key used for decryption.
     * @return The decrypted plain text.
     * @throws GeneralSecurityException If a decryption error occurs.
     */
    public static String decryptAES(String encryptedText, SecretKey encryptionKey)
            throws GeneralSecurityException {
        byte[] decodedData = Base64.getDecoder().decode(encryptedText);

        byte[] iv = new byte[IV_LENGTH_GCM];
        System.arraycopy(decodedData, 0, iv, 0, IV_LENGTH_GCM);

        byte[] cipherText = new byte[decodedData.length - IV_LENGTH_GCM];
        System.arraycopy(decodedData, IV_LENGTH_GCM, cipherText, 0, decodedData.length - IV_LENGTH_GCM);

        Cipher cipher = Cipher.getInstance(ALGORITHM_AES_GCM_NOPADDING);
        GCMParameterSpec gcmParameterSpec = new GCMParameterSpec(TAG_LENGTH_GCM * 8, iv);
        cipher.init(Cipher.DECRYPT_MODE, encryptionKey, gcmParameterSpec);

        byte[] plainText = cipher.doFinal(cipherText);
        return new String(plainText, java.nio.charset.StandardCharsets.UTF_8);
    }

    /**
     * Generates a strong random password.
     *
     * @return A strong random password string.
     */
    public static String generateStrongPassword() {
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789!@#$%^&*()-_=+";
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 16; i++) { // 16 characters long
            sb.append(chars.charAt(secureRandom.nextInt(chars.length())));
        }
        return sb.toString();
    }

    /**
     * Hashes a plain-text password using PBKDF2WithHmacSHA256.
     * The salt is prepended to the hashed password for storage.
     *
     * @param plainPassword The plain-text password to hash.
     * @return A Base64 encoded string of salt + hashed password.
     * @throws NoSuchAlgorithmException If the hashing algorithm is not found.
     * @throws InvalidKeySpecException  If the key specification is invalid.
     */
    public static String hashPassword(String plainPassword)
            throws NoSuchAlgorithmException, InvalidKeySpecException {
        byte[] salt = generateSalt(); // Generate a new salt for each hash
        PBEKeySpec spec = new PBEKeySpec(plainPassword.toCharArray(), salt, ITERATIONS_PBKDF2, KEY_LENGTH_PBKDF2);
        SecretKeyFactory factory = SecretKeyFactory.getInstance(ALGORITHM_PBKDF2);
        byte[] hashedPassword = factory.generateSecret(spec).getEncoded();

        // Concatenate salt and hashed password for storage
        byte[] combined = new byte[salt.length + hashedPassword.length];
        System.arraycopy(salt, 0, combined, 0, salt.length);
        System.arraycopy(hashedPassword, 0, combined, salt.length, hashedPassword.length);

        return Base64.getEncoder().encodeToString(combined);
    }

    /**
     * Verifies a plain-text password against a hashed password.
     *
     * @param plainPassword The plain-text password to verify.
     * @param hashedPasswordWithSalt The Base64 encoded string of salt + hashed password.
     * @return True if the password matches, false otherwise.
     * @throws NoSuchAlgorithmException If the hashing algorithm is not found.
     * @throws InvalidKeySpecException  If the key specification is invalid.
     */
    public static boolean verifyPassword(String plainPassword, String hashedPasswordWithSalt)
            throws NoSuchAlgorithmException, InvalidKeySpecException {
        byte[] decodedCombined = Base64.getDecoder().decode(hashedPasswordWithSalt);

        byte[] salt = new byte[16]; // Assuming 16 bytes for salt, same as generateSalt()
        System.arraycopy(decodedCombined, 0, salt, 0, salt.length);

        byte[] storedHashedPassword = new byte[decodedCombined.length - salt.length];
        System.arraycopy(decodedCombined, salt.length, storedHashedPassword, 0, storedHashedPassword.length);

        PBEKeySpec spec = new PBEKeySpec(plainPassword.toCharArray(), salt, ITERATIONS_PBKDF2, KEY_LENGTH_PBKDF2);
        SecretKeyFactory factory = SecretKeyFactory.getInstance(ALGORITHM_PBKDF2);
        byte[] newHashedPassword = factory.generateSecret(spec).getEncoded();

        return Arrays.equals(newHashedPassword, storedHashedPassword);
    }
}
