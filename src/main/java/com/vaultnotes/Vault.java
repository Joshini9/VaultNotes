package com.vaultnotes;
import javax.crypto.SecretKey;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.security.GeneralSecurityException;

/**
 * Represents a user's vault containing various VaultItems.
 * Each vault is associated with a specific encryption key derived from the user's master password.
 */
public class Vault implements Serializable {
    private static final long serialVersionUID = 1L;

    // We store the encoded key bytes and salt, as SecretKey itself is not directly serializable.
    private byte[] encryptionKeyBytes;
    private byte[] keySalt; // Salt used to derive the encryption key

    private String userId; // The ID of the user this vault belongs to
    private List<VaultItem> vaultItems;

    // Transient field to hold the actual SecretKey during runtime, not serialized
    private transient SecretKey currentEncryptionKey;

    /**
     * Constructor for a new Vault.
     * Derives the encryption key from the user's master password using a new salt.
     *
     * @param userId         The ID of the user.
     * @param masterPassword The user's master password (plain-text).
     * @throws GeneralSecurityException If key derivation fails.
     */
    public Vault(String userId, String masterPassword)
            throws GeneralSecurityException {
        this.userId = userId;
        this.vaultItems = new ArrayList<>();
        this.keySalt = SecurityManager.generateSalt(); // Generate a unique salt for the key
        this.currentEncryptionKey = SecurityManager.deriveKey(masterPassword, keySalt);
        this.encryptionKeyBytes = currentEncryptionKey.getEncoded();
    }

    /**
     * Reconstructs the SecretKey from stored bytes and salt after deserialization
     * or on initial load/login.
     *
     * @param masterPassword The plain-text master password to re-derive the key.
     * @throws GeneralSecurityException If key derivation fails.
     */
    public void rederiveEncryptionKey(String masterPassword) throws GeneralSecurityException {
        if (keySalt == null || masterPassword == null) {
            throw new IllegalStateException("Cannot re-derive key without salt or master password.");
        }
        this.currentEncryptionKey = SecurityManager.deriveKey(masterPassword, keySalt);
    }

    /**
     * Gets the runtime SecretKey. This must be called only after key is rederived or set.
     *
     * @return The SecretKey.
     */
    public SecretKey getEncryptionKey() {
        if (currentEncryptionKey == null) {
            throw new IllegalStateException("Encryption key not derived/set for current session.");
        }
        return currentEncryptionKey;
    }

    public String getUserId() {
        return userId;
    }

    public List<VaultItem> getVaultItems() {
        return vaultItems;
    }

    /**
     * Adds a new VaultItem to the vault.
     *
     * @param item The VaultItem to add.
     */
    public void addEntry(VaultItem item) {
        if (!item.getUserId().equals(this.userId)) {
            throw new IllegalArgumentException("VaultItem does not belong to this user's vault.");
        }
        this.vaultItems.add(item);
    }

    /**
     * Deletes a VaultItem from the vault.
     *
     * @param item The VaultItem to delete.
     * @return True if the item was found and deleted, false otherwise.
     */
    public boolean deleteEntry(VaultItem item) {
        return this.vaultItems.remove(item);
    }

    /**
     * Searches for VaultItems containing a keyword in their title or summary.
     *
     * @param keyword The keyword to search for.
     * @return A list of matching VaultItems.
     */
    public List<VaultItem> searchEntry(String keyword) {
        String lowerCaseKeyword = keyword.toLowerCase();
        return vaultItems.stream()
                .filter(item -> item.getTitle().toLowerCase().contains(lowerCaseKeyword) ||
                                item.getSummary().toLowerCase().contains(lowerCaseKeyword))
                .collect(Collectors.toList());
    }

    /**
     * Get the byte array of the encryption key (for serialization).
     * @return byte[] of encryption key.
     */
    public byte[] getEncryptionKeyBytes() {
        return encryptionKeyBytes;
    }

    /**
     * Get the salt used to derive the encryption key (for serialization).
     * @return byte[] of key salt.
     */
    public byte[] getKeySalt() {
        return keySalt;
    }

    /**
     * Set the encryption key bytes (used during deserialization).
     * @param encryptionKeyBytes byte[] of encryption key.
     */
    public void setEncryptionKeyBytes(byte[] encryptionKeyBytes) {
        this.encryptionKeyBytes = encryptionKeyBytes;
    }

    /**
     * Set the key salt (used during deserialization).
     * @param keySalt byte[] of key salt.
     */
    public void setKeySalt(byte[] keySalt) {
        this.keySalt = keySalt;
    }
}
