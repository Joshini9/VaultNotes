package com.vaultnotes;
import javax.crypto.SecretKey;
import java.io.IOException;
import java.security.GeneralSecurityException;

/**
 * Represents a note entry in the vault, extending VaultItem.
 * Stores the encrypted text of the note.
 */
public class NoteEntry extends VaultItem {
    private static final long serialVersionUID = 1L;

    private String encryptedNoteText; // Base64 encoded IV + ciphertext + tag

    public NoteEntry(String title, String userId) {
        super(title, userId);
    }

    /**
     * Sets the plain-text note, encrypting it before storing.
     *
     * @param plainNote     The plain-text note to set.
     * @param encryptionKey The secret key for encryption.
     * @throws GeneralSecurityException If encryption fails.
     */
    public void setNoteText(String plainNote, SecretKey encryptionKey)
            throws GeneralSecurityException {
        this.encryptedNoteText = SecurityManager.encryptAES(plainNote, encryptionKey);
    }

    /**
     * Retrieves the decrypted note text.
     *
     * @param encryptionKey The secret key for decryption.
     * @return The decrypted plain-text note.
     * @throws GeneralSecurityException If decryption fails.
     */
    public String getDecryptedNoteText(SecretKey encryptionKey)
            throws GeneralSecurityException {
        return SecurityManager.decryptAES(encryptedNoteText, encryptionKey);
    }

    @Override
    public String displayDetails() {
        // Note: For displayDetails, we might not want to show the raw decrypted text directly
        // Instead, show other metadata. The getDecryptedNoteText is for explicit viewing.
        return "Title: " + title + "\n" +
               "Created Date: " + createdDate.toString();
    }

    @Override
    public String getSummary() {
        return "Note: " + title;
    }
}
