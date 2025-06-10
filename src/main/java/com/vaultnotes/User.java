package com.vaultnotes;
import java.io.Serializable;
import java.security.GeneralSecurityException;
import java.util.UUID; // For unique user ID

/**
 * Represents a user of the notes application.
 * Handles user authentication and master password management.
 */
public class User implements Serializable {
    private static final long serialVersionUID = 1L;

    private String userId;
    private String username;
    // Stores salt + hashed password (Base64 encoded)
    private String hashedMasterPassword;

    public User(String username, String masterPassword)
            throws GeneralSecurityException {
        this.userId = UUID.randomUUID().toString(); // Generate a unique ID for the user
        this.username = username;
        this.hashedMasterPassword = SecurityManager.hashPassword(masterPassword);
    }

    public String getUserId() {
        return userId;
    }

    public String getUsername() {
        return username;
    }

    public String getHashedMasterPassword() {
        return hashedMasterPassword;
    }

    /**
     * Attempts to log in the user with the given username and password.
     *
     * @param username The username provided by the user.
     * @param password The plain-text password provided by the user.
     * @return True if login is successful, false otherwise.
     * @throws GeneralSecurityException If a security error occurs during verification.
     */
    public boolean login(String username, String password)
            throws GeneralSecurityException {
        // Check if username matches
        if (!this.username.equals(username)) {
            return false;
        }
        // Verify password
        return SecurityManager.verifyPassword(password, this.hashedMasterPassword);
    }

    /**
     * Simulates a logout operation (primarily for state management in GUI).
     */
    public void logout() {
        // In a real application, this would clear session data, encryption keys etc.
        System.out.println(username + " logged out.");
    }

    /**
     * Resets the user's master password.
     *
     * @param currentPassword The user's current plain-text password.
     * @param newPassword     The new plain-text password.
     * @return True if the password was successfully reset, false if current password is incorrect.
     * @throws GeneralSecurityException If a security error occurs during password hashing/verification.
     */
    public boolean resetPassword(String currentPassword, String newPassword)
            throws GeneralSecurityException {
        if (SecurityManager.verifyPassword(currentPassword, this.hashedMasterPassword)) {
            this.hashedMasterPassword = SecurityManager.hashPassword(newPassword);
            return true;
        }
        return false; // Current password incorrect
    }
}
