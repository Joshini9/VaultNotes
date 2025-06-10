package com.vaultnotes;
import javax.crypto.SecretKey;
import java.io.IOException;
import java.security.GeneralSecurityException;

/**
 * Represents a password entry in the vault, extending VaultItem.
 * Stores site name, username, and an encrypted password.
 */
public class PasswordEntry extends VaultItem {
    private static final long serialVersionUID = 1L;

    private String siteName;
    private String userName;
    private String encryptedPassword; // Base64 encoded IV + ciphertext + tag

    public PasswordEntry(String title, String userId, String siteName, String userName) {
        super(title, userId);
        this.siteName = siteName;
        this.userName = userName;
    }

    public String getSiteName() {
        return siteName;
    }

    public void setSiteName(String siteName) {
        this.siteName = siteName;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    /**
     * Sets the plain-text password, encrypting it before storing.
     *
     * @param plainPassword The plain-text password to set.
     * @param encryptionKey The secret key for encryption.
     * @throws GeneralSecurityException If encryption fails.
     */
    public void setPassword(String plainPassword, SecretKey encryptionKey)
            throws GeneralSecurityException {
        this.encryptedPassword = SecurityManager.encryptAES(plainPassword, encryptionKey);
    }

    /**
     * Retrieves the decrypted password.
     *
     * @param encryptionKey The secret key for decryption.
     * @return The decrypted plain-text password.
     * @throws GeneralSecurityException If decryption fails.
     */
    public String getDecryptedPassword(SecretKey encryptionKey)
            throws GeneralSecurityException {
        return SecurityManager.decryptAES(encryptedPassword, encryptionKey);
    }

    @Override
    public String displayDetails() {
        return "Title: " + title + "\n" +
               "Site Name: " + siteName + "\n" +
               "Username: " + userName + "\n" +
               "Created Date: " + createdDate.toString();
    }

    @Override
    public String getSummary() {
        return "Password: " + title + " (" + siteName + ")";
    }
}
