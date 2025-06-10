package com.vaultnotes;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.*;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec; // For reconstructing SecretKey from bytes

/**
 * Main GUI class for the Password Encrypted Notes App.
 * Manages different panels for login, vault, adding entries, and resetting passwords.
 */
public class NotesAppGUI extends JFrame {

    // --- GUI Components ---
    private CardLayout cardLayout;
    private JPanel mainPanel; // Panel to hold all other panels
    private JPanel loginPanel;
    private JPanel registerPanel;
    private JPanel vaultPanel;
    private JPanel addEntryPanel;
    private JPanel resetPasswordPanel;
    private JPanel messageBoxPanel; // Custom message box

    // Login Panel Components
    private JTextField loginUsernameField;
    private JPasswordField loginPasswordField;

    // Register Panel Components
    private JTextField regUsernameField;
    private JPasswordField regPasswordField;
    private JPasswordField regConfirmPasswordField;

    // Vault Panel Components
    private JLabel welcomeLabel;
    private DefaultListModel<VaultItem> vaultListModel;
    private JList<VaultItem> vaultList;
    private JTextArea displayDetailsArea; // To show full details of selected item

    // Add Entry Panel Components
    private JComboBox<String> entryTypeComboBox;
    private JPanel entrySpecificPanel; // Panel to swap based on entry type
    private JTextField entryTitleField;

    // Password Entry Fields
    private JTextField passwordSiteField;
    private JTextField passwordUserField;
    private JPasswordField passwordPassField;

    // Note Entry Fields
    private JTextArea noteTextField;

    // Reset Password Panel Components
    private JPasswordField currentPasswordField;
    private JPasswordField newPasswordField;
    private JPasswordField confirmNewPasswordField;

    // Message Box Components
    private JLabel messageBoxLabel;
    private JButton messageBoxOkButton;

    // --- Application State ---
    private Map<String, User> users; // Stores User objects keyed by username
    private Map<String, Vault> vaults; // Stores Vault objects keyed by userId
    private User currentUser; // Currently logged-in user
    private SecretKey currentEncryptionKey; // Encryption key for the current user's vault

    // --- File Paths for Persistence ---
    private static final String USERS_FILE = "users.dat";
    private static final String VAULTS_FILE = "vaults.dat";

    // --- Colors for Vibrant GUI ---
    private static final Color PRIMARY_BG = new Color(30, 30, 30); // Darker Grey
    private static final Color SECONDARY_BG = new Color(45, 45, 45); // Medium Grey
    private static final Color ACCENT_COLOR = new Color(0, 150, 136); // Teal
    private static final Color ACCENT_LIGHT = new Color(77, 208, 225); // Light Cyan
    private static final Color TEXT_COLOR = new Color(230, 230, 230); // Light Grey
    private static final Color BUTTON_COLOR = new Color(255, 102, 0); // Orange
    private static final Color BUTTON_HOVER = new Color(255, 140, 0); // Lighter Orange
    private static final Color BORDER_COLOR = new Color(60, 60, 60); // Even darker grey

    /**
     * Constructor for the NotesAppGUI.
     * Initializes the frame, loads data, and sets up all panels.
     */
    public NotesAppGUI() {
        setTitle("Password Encrypted Notes App");
        setSize(800, 600);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null); // Center the window

        loadData(); // Load users and vaults from files

        cardLayout = new CardLayout();
        mainPanel = new JPanel(cardLayout);
        mainPanel.setBackground(PRIMARY_BG);

        // Initialize and add all panels
        createMessageBoxPanel();
        createLoginPanel();
        createRegisterPanel();
        createVaultPanel();
        createAddEntryPanel();
        createResetPasswordPanel();

        mainPanel.add(loginPanel, "Login");
        mainPanel.add(registerPanel, "Register");
        mainPanel.add(vaultPanel, "Vault");
        mainPanel.add(addEntryPanel, "AddEntry");
        mainPanel.add(resetPasswordPanel, "ResetPassword");
        mainPanel.add(messageBoxPanel, "MessageBox"); // Add message box, initially hidden

        add(mainPanel);

        // Show login panel initially
        cardLayout.show(mainPanel, "Login");
        setVisible(true);
    }

    /**
     * Creates a custom message box panel.
     */
    private void createMessageBoxPanel() {
        messageBoxPanel = new JPanel(new BorderLayout(10, 10));
        messageBoxPanel.setBackground(SECONDARY_BG);
        messageBoxPanel.setBorder(BorderFactory.createLineBorder(ACCENT_COLOR, 3));
        messageBoxPanel.setPreferredSize(new Dimension(300, 150)); // Fixed size for modal

        messageBoxLabel = new JLabel("Message", SwingConstants.CENTER);
        messageBoxLabel.setForeground(TEXT_COLOR);
        messageBoxLabel.setFont(new Font("Inter", Font.BOLD, 16));
        messageBoxPanel.add(messageBoxLabel, BorderLayout.CENTER);

        messageBoxOkButton = createStyledButton("OK");
        messageBoxOkButton.setForeground(Color.BLACK);
        messageBoxOkButton.addActionListener(e -> cardLayout.show(mainPanel, "Login")); // Go back to login or previous state
        JPanel buttonPanel = new JPanel();
        buttonPanel.setBackground(SECONDARY_BG);
        buttonPanel.add(messageBoxOkButton);
        messageBoxPanel.add(buttonPanel, BorderLayout.SOUTH);

        // Hide by default, will be shown via cardLayout
        messageBoxPanel.setVisible(false);
    }

    /**
     * Displays a custom message box.
     *
     * @param message The message to display.
     * @param returnCardName The name of the card to return to after "OK" is pressed.
     */
    private void showMessageBox(String message, String returnCardName) {
        messageBoxLabel.setText("<html><center>" + message + "</center></html>"); // Allow HTML for wrapping
        messageBoxOkButton.removeActionListener(messageBoxOkButton.getActionListeners()[0]); // Remove old listener
        messageBoxOkButton.addActionListener(e -> cardLayout.show(mainPanel, returnCardName));
        cardLayout.show(mainPanel, "MessageBox");
    }

    /**
     * Creates and styles the login panel.
     */
    private void createLoginPanel() {
        loginPanel = new JPanel(new GridBagLayout());
        loginPanel.setBackground(PRIMARY_BG);
        loginPanel.setBorder(new EmptyBorder(50, 50, 50, 50));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 10, 10, 10);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        JLabel titleLabel = new JLabel("Login", SwingConstants.CENTER);
        titleLabel.setFont(new Font("Inter", Font.BOLD, 36));
        titleLabel.setForeground(ACCENT_LIGHT);
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 2;
        loginPanel.add(titleLabel, gbc);

        gbc.gridwidth = 1; // Reset gridwidth

        JLabel userLabel = new JLabel("Username:");
        userLabel.setForeground(TEXT_COLOR);
        userLabel.setFont(new Font("Inter", Font.PLAIN, 16));
        gbc.gridx = 0;
        gbc.gridy = 1;
        loginPanel.add(userLabel, gbc);

        loginUsernameField = createStyledTextField();
        gbc.gridx = 1;
        gbc.gridy = 1;
        loginPanel.add(loginUsernameField, gbc);

        JLabel passLabel = new JLabel("Password:");
        passLabel.setForeground(TEXT_COLOR);
        passLabel.setFont(new Font("Inter", Font.PLAIN, 16));
        gbc.gridx = 0;
        gbc.gridy = 2;
        loginPanel.add(passLabel, gbc);

        loginPasswordField = createStyledPasswordField();
        gbc.gridx = 1;
        gbc.gridy = 2;
        loginPanel.add(loginPasswordField, gbc);

        JButton loginButton = createStyledButton("Login");
        loginButton.setForeground(Color.BLACK);
        gbc.gridx = 0;
        gbc.gridy = 3;
        gbc.gridwidth = 2;
        loginPanel.add(loginButton, gbc);

        JButton registerButton = createStyledButton("New User? Register");
        // Custom colors for register button
        registerButton.setBackground(ACCENT_COLOR);
        registerButton.setForeground(Color.BLACK); // Ensure text color is white
        registerButton.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                registerButton.setBackground(ACCENT_LIGHT);
            }
            public void mouseExited(java.awt.event.MouseEvent evt) {
                registerButton.setBackground(ACCENT_COLOR);
            }
        });
        gbc.gridy = 4;
        loginPanel.add(registerButton, gbc);

        loginButton.addActionListener(e -> handleLogin());
        registerButton.addActionListener(e -> cardLayout.show(mainPanel, "Register"));
    }

    /**
     * Creates and styles the registration panel.
     */
    private void createRegisterPanel() {
        registerPanel = new JPanel(new GridBagLayout());
        registerPanel.setBackground(PRIMARY_BG);
        registerPanel.setBorder(new EmptyBorder(50, 50, 50, 50));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 10, 10, 10);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        JLabel titleLabel = new JLabel("Register New User", SwingConstants.CENTER);
        titleLabel.setFont(new Font("Inter", Font.BOLD, 32));
        titleLabel.setForeground(ACCENT_LIGHT);
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 2;
        registerPanel.add(titleLabel, gbc);

        gbc.gridwidth = 1;

        JLabel userLabel = new JLabel("Username:");
        userLabel.setForeground(TEXT_COLOR);
        userLabel.setFont(new Font("Inter", Font.PLAIN, 16));
        gbc.gridx = 0;
        gbc.gridy = 1;
        registerPanel.add(userLabel, gbc);

        regUsernameField = createStyledTextField();
        gbc.gridx = 1;
        gbc.gridy = 1;
        registerPanel.add(regUsernameField, gbc);

        JLabel passLabel = new JLabel("Master Password:");
        passLabel.setForeground(TEXT_COLOR);
        passLabel.setFont(new Font("Inter", Font.PLAIN, 16));
        gbc.gridx = 0;
        gbc.gridy = 2;
        registerPanel.add(passLabel, gbc);

        regPasswordField = createStyledPasswordField();
        gbc.gridx = 1;
        gbc.gridy = 2;
        registerPanel.add(regPasswordField, gbc);

        JLabel confirmPassLabel = new JLabel("Confirm Password:");
        confirmPassLabel.setForeground(TEXT_COLOR);
        confirmPassLabel.setFont(new Font("Inter", Font.PLAIN, 16));
        gbc.gridx = 0;
        gbc.gridy = 3;
        registerPanel.add(confirmPassLabel, gbc);

        regConfirmPasswordField = createStyledPasswordField();
        gbc.gridx = 1;
        gbc.gridy = 3;
        registerPanel.add(regConfirmPasswordField, gbc);

        JButton registerButton = createStyledButton("Register");
        gbc.gridx = 0;
        gbc.gridy = 4;
        gbc.gridwidth = 2;
        registerButton.setForeground(Color.BLACK);
        registerPanel.add(registerButton, gbc);

        JButton backButton = createStyledButton("Back to Login");
        // Custom colors for back button
        backButton.setBackground(SECONDARY_BG);
        backButton.setForeground(Color.BLACK); // Ensure text color is light grey
        backButton.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                backButton.setBackground(BORDER_COLOR);
            }
            public void mouseExited(java.awt.event.MouseEvent evt) {
                backButton.setBackground(SECONDARY_BG);
            }
        });
        gbc.gridy = 5;
        registerPanel.add(backButton, gbc);

        registerButton.addActionListener(e -> handleRegister());
        backButton.addActionListener(e -> cardLayout.show(mainPanel, "Login"));
    }

    /**
     * Creates and styles the main vault panel.
     */
    private void createVaultPanel() {
        vaultPanel = new JPanel(new BorderLayout(15, 15));
        vaultPanel.setBackground(PRIMARY_BG);
        vaultPanel.setBorder(new EmptyBorder(15, 15, 15, 15));

        // Top Panel: Welcome and Controls
        JPanel topPanel = new JPanel(new BorderLayout());
        topPanel.setBackground(SECONDARY_BG);
        topPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        // Using UIManager property for rounded rect for panels.
        topPanel.putClientProperty("JComponent.roundRect", true);

        welcomeLabel = new JLabel("Welcome, User!", SwingConstants.LEFT);
        welcomeLabel.setFont(new Font("Inter", Font.BOLD, 24));
        welcomeLabel.setForeground(ACCENT_LIGHT);
        topPanel.add(welcomeLabel, BorderLayout.WEST);

        JPanel buttonContainer = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        buttonContainer.setBackground(SECONDARY_BG);
        JButton addEntryButton = createStyledButton("Add New Entry");
        addEntryButton.setForeground(Color.BLACK); // Explicitly set foreground
        JButton resetPassButton = createStyledButton("Reset Master Password");
        resetPassButton.setForeground(Color.BLACK); // Explicitly set foreground
        JButton logoutButton = createStyledButton("Logout");
        logoutButton.setForeground(Color.BLACK); // Explicitly set foreground

        buttonContainer.add(addEntryButton);
        buttonContainer.add(resetPassButton);
        buttonContainer.add(logoutButton);
        topPanel.add(buttonContainer, BorderLayout.EAST);
        vaultPanel.add(topPanel, BorderLayout.NORTH);

        // Center Panel: List of Vault Items
        vaultListModel = new DefaultListModel<>();
        vaultList = new JList<>(vaultListModel);
        vaultList.setFont(new Font("Inter", Font.PLAIN, 16));
        vaultList.setBackground(SECONDARY_BG);
        vaultList.setForeground(Color.WHITE);
        vaultList.setSelectionBackground(ACCENT_COLOR);
        vaultList.setSelectionForeground(Color.WHITE);
        vaultList.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER_COLOR, 1),
                BorderFactory.createEmptyBorder(5, 5, 5, 5)
        ));
        vaultList.putClientProperty("JComponent.roundRect", true);

        vaultList.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                VaultItem selectedItem = vaultList.getSelectedValue();
                if (selectedItem != null) {
                    if (selectedItem instanceof PasswordEntry) {
                        try {
                            PasswordEntry pe = (PasswordEntry) selectedItem;
                            String decryptedPass = pe.getDecryptedPassword(currentEncryptionKey);
                            displayDetailsArea.setText(pe.displayDetails() + "\nPassword: " + decryptedPass);
                        } catch (GeneralSecurityException ex) {
                            displayDetailsArea.setText("Error decrypting password.");
                            System.err.println("Decryption error: " + ex.getMessage());
                        }
                    } else if (selectedItem instanceof NoteEntry) {
                        try {
                            NoteEntry ne = (NoteEntry) selectedItem;
                            String decryptedNote = ne.getDecryptedNoteText(currentEncryptionKey);
                            displayDetailsArea.setText(ne.displayDetails() + "\nNote: " + decryptedNote);
                        } catch (GeneralSecurityException ex) {
                            displayDetailsArea.setText("Error decrypting note.");
                            System.err.println("Decryption error: " + ex.getMessage());
                        }
                    }
                }
            }
        });

        JScrollPane listScrollPane = new JScrollPane(vaultList);
        listScrollPane.setBorder(BorderFactory.createLineBorder(BORDER_COLOR, 1));
        listScrollPane.getViewport().setBackground(SECONDARY_BG); // Ensure viewport also matches
        vaultPanel.add(listScrollPane, BorderLayout.CENTER);

        // Right Panel: Details and Actions
        JPanel rightPanel = new JPanel(new BorderLayout(10, 10));
        rightPanel.setBackground(SECONDARY_BG);
        rightPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        rightPanel.putClientProperty("JComponent.roundRect", true);

        displayDetailsArea = new JTextArea();
        displayDetailsArea.setEditable(false);
        displayDetailsArea.setLineWrap(true);
        displayDetailsArea.setWrapStyleWord(true);
        displayDetailsArea.setFont(new Font("Inter", Font.PLAIN, 14));
        displayDetailsArea.setBackground(PRIMARY_BG);
        displayDetailsArea.setForeground(Color.WHITE);
        displayDetailsArea.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER_COLOR, 1),
                BorderFactory.createEmptyBorder(10, 10, 10, 10)
        ));
        JScrollPane detailsScrollPane = new JScrollPane(displayDetailsArea);
        detailsScrollPane.getViewport().setBackground(PRIMARY_BG);
        detailsScrollPane.setBorder(null); // No extra border from scroll pane
        rightPanel.add(detailsScrollPane, BorderLayout.CENTER);

        JPanel itemActionPanel = new JPanel(new GridLayout(1, 2, 10, 0));
        itemActionPanel.setBackground(SECONDARY_BG);
        JButton deleteButton = createStyledButton("Delete Selected");
        deleteButton.setForeground(Color.BLACK); // Explicitly set foreground
        JButton copyButton = createStyledButton("Copy Content"); // For password/note text
        copyButton.setForeground(Color.BLACK); // Explicitly set foreground
        itemActionPanel.add(copyButton);
        itemActionPanel.add(deleteButton);
        rightPanel.add(itemActionPanel, BorderLayout.SOUTH);

        vaultPanel.add(rightPanel, BorderLayout.EAST);

        // Action Listeners
        addEntryButton.addActionListener(e -> {
            entryTitleField.setText(""); // Clear fields when navigating
            passwordSiteField.setText("");
            passwordUserField.setText("");
            passwordPassField.setText("");
            noteTextField.setText("");
            entryTypeComboBox.setSelectedIndex(0); // Reset to default
            cardLayout.show(mainPanel, "AddEntry");
        });
        resetPassButton.addActionListener(e -> {
            currentPasswordField.setText("");
            newPasswordField.setText("");
            confirmNewPasswordField.setText("");
            cardLayout.show(mainPanel, "ResetPassword");
        });
        logoutButton.addActionListener(e -> handleLogout());
        deleteButton.addActionListener(e -> handleDeleteEntry());
        copyButton.addActionListener(e -> handleCopyContent());
    }

    /**
     * Creates and styles the add entry panel.
     */
    private void createAddEntryPanel() {
        addEntryPanel = new JPanel(new BorderLayout(15, 15));
        addEntryPanel.setBackground(PRIMARY_BG);
        addEntryPanel.setBorder(new EmptyBorder(15, 15, 15, 15));

        JLabel titleLabel = new JLabel("Add New Vault Entry", SwingConstants.CENTER);
        titleLabel.setFont(new Font("Inter", Font.BOLD, 32));
        titleLabel.setForeground(ACCENT_LIGHT);
        addEntryPanel.add(titleLabel, BorderLayout.NORTH);

        JPanel formPanel = new JPanel(new GridBagLayout());
        formPanel.setBackground(SECONDARY_BG);
        formPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER_COLOR, 1),
                BorderFactory.createEmptyBorder(20, 20, 20, 20)
        ));
        formPanel.putClientProperty("JComponent.roundRect", true);

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 10, 10, 10);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.anchor = GridBagConstraints.WEST;

        // Entry Type
        JLabel typeLabel = new JLabel("Entry Type:");
        typeLabel.setForeground(Color.WHITE);
        typeLabel.setFont(new Font("Inter", Font.PLAIN, 16));
        gbc.gridx = 0;
        gbc.gridy = 0;
        formPanel.add(typeLabel, gbc);

        String[] entryTypes = {"Password", "Note"};
        entryTypeComboBox = new JComboBox<>(entryTypes);
        entryTypeComboBox.setFont(new Font("Inter", Font.PLAIN, 16));
        entryTypeComboBox.setBackground(PRIMARY_BG);
        entryTypeComboBox.setForeground(Color.BLACK);
        entryTypeComboBox.setRenderer(new DefaultListCellRenderer() { // Custom renderer for combo box items
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                setBackground(isSelected ? ACCENT_COLOR : PRIMARY_BG);
                setForeground(isSelected ? Color.WHITE : TEXT_COLOR);
                setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
                return this;
            }
        });
        entryTypeComboBox.addActionListener(e -> updateEntrySpecificPanel());
        gbc.gridx = 1;
        gbc.gridy = 0;
        gbc.weightx = 1.0;
        formPanel.add(entryTypeComboBox, gbc);

        // Common Title
        JLabel titleFieldLabel = new JLabel("Title:");
        titleFieldLabel.setForeground(TEXT_COLOR);
        titleFieldLabel.setFont(new Font("Inter", Font.PLAIN, 16));
        gbc.gridx = 0;
        gbc.gridy = 1;
        formPanel.add(titleFieldLabel, gbc);

        entryTitleField = createStyledTextField();
        gbc.gridx = 1;
        gbc.gridy = 1;
        formPanel.add(entryTitleField, gbc);

        // Entry Specific Panel (holds password or note fields)
        entrySpecificPanel = new JPanel(new CardLayout());
        entrySpecificPanel.setOpaque(false); // Make it transparent to show parent's background

        // Password Entry fields
        JPanel passwordFieldsPanel = new JPanel(new GridBagLayout());
        passwordFieldsPanel.setOpaque(false);
        GridBagConstraints pGbc = new GridBagConstraints();
        pGbc.insets = new Insets(5, 5, 5, 5);
        pGbc.fill = GridBagConstraints.HORIZONTAL;
        pGbc.anchor = GridBagConstraints.WEST;

        pGbc.gridx = 0; pGbc.gridy = 0; passwordFieldsPanel.add(createStyledLabel("Site Name:"), pGbc);
        pGbc.gridx = 1; pGbc.weightx = 1.0; passwordSiteField = createStyledTextField(); passwordFieldsPanel.add(passwordSiteField, pGbc);

        pGbc.gridx = 0; pGbc.gridy = 1; pGbc.weightx = 0; passwordFieldsPanel.add(createStyledLabel("Username:"), pGbc);
        pGbc.gridx = 1; pGbc.weightx = 1.0; passwordUserField = createStyledTextField(); passwordFieldsPanel.add(passwordUserField, pGbc);

        pGbc.gridx = 0; pGbc.gridy = 2; pGbc.weightx = 0; passwordFieldsPanel.add(createStyledLabel("Password:"), pGbc);
        pGbc.gridx = 1; pGbc.weightx = 1.0; passwordPassField = createStyledPasswordField(); passwordFieldsPanel.add(passwordPassField, pGbc);

        entrySpecificPanel.add(passwordFieldsPanel, "Password");

        // Note Entry fields
        JPanel noteFieldsPanel = new JPanel(new GridBagLayout());
        noteFieldsPanel.setOpaque(false);
        GridBagConstraints nGbc = new GridBagConstraints();
        nGbc.insets = new Insets(5, 5, 5, 5);
        nGbc.fill = GridBagConstraints.BOTH;
        nGbc.anchor = GridBagConstraints.WEST;

        nGbc.gridx = 0; nGbc.gridy = 0; noteFieldsPanel.add(createStyledLabel("Note Text:"), nGbc);
        nGbc.gridx = 1; nGbc.weightx = 1.0; nGbc.weighty = 1.0;
        noteTextField = new JTextArea(8, 30);
        noteTextField.setLineWrap(true);
        noteTextField.setWrapStyleWord(true);
        noteTextField.setFont(new Font("Inter", Font.PLAIN, 14));
        noteTextField.setBackground(PRIMARY_BG);
        noteTextField.setForeground(TEXT_COLOR);
        noteTextField.setCaretColor(ACCENT_LIGHT);
        noteTextField.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER_COLOR, 1),
                BorderFactory.createEmptyBorder(8, 8, 8, 8)
        ));
        JScrollPane noteScrollPane = new JScrollPane(noteTextField);
        noteScrollPane.getViewport().setBackground(PRIMARY_BG);
        noteScrollPane.setBorder(null);
        noteFieldsPanel.add(noteScrollPane, nGbc);

        entrySpecificPanel.add(noteFieldsPanel, "Note");

        gbc.gridx = 0;
        gbc.gridy = 2;
        gbc.gridwidth = 2;
        gbc.weighty = 1.0; // Allow it to expand vertically
        gbc.fill = GridBagConstraints.BOTH;
        formPanel.add(entrySpecificPanel, gbc);

        addEntryPanel.add(formPanel, BorderLayout.CENTER);

        JPanel bottomButtons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        bottomButtons.setBackground(PRIMARY_BG);
        JButton saveEntryButton = createStyledButton("Save Entry");
        saveEntryButton.setForeground(Color.BLACK); // Explicitly set foreground
        JButton backFromAddButton = createStyledButton("Back to Vault");
        backFromAddButton.setForeground(Color.BLACK); // Explicitly set foreground
        bottomButtons.add(saveEntryButton);
        bottomButtons.add(backFromAddButton);
        addEntryPanel.add(bottomButtons, BorderLayout.SOUTH);

        saveEntryButton.addActionListener(e -> handleAddEntry());
        backFromAddButton.addActionListener(e -> cardLayout.show(mainPanel, "Vault"));

        updateEntrySpecificPanel(); // Set initial view
    }

    /**
     * Updates the panel within addEntryPanel based on the selected entry type.
     */
    private void updateEntrySpecificPanel() {
        String selectedType = (String) entryTypeComboBox.getSelectedItem();
        CardLayout cl = (CardLayout) (entrySpecificPanel.getLayout());
        cl.show(entrySpecificPanel, selectedType);
    }

    /**
     * Creates and styles the reset password panel.
     */
    private void createResetPasswordPanel() {
        resetPasswordPanel = new JPanel(new GridBagLayout());
        resetPasswordPanel.setBackground(PRIMARY_BG);
        resetPasswordPanel.setBorder(new EmptyBorder(50, 50, 50, 50));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 10, 10, 10);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        JLabel titleLabel = new JLabel("Reset Master Password", SwingConstants.CENTER);
        titleLabel.setFont(new Font("Inter", Font.BOLD, 32));
        titleLabel.setForeground(ACCENT_LIGHT);
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 2;
        resetPasswordPanel.add(titleLabel, gbc);

        gbc.gridwidth = 1;

        JLabel currentPassLabel = createStyledLabel("Current Password:");
        gbc.gridx = 0; gbc.gridy = 1; resetPasswordPanel.add(currentPassLabel, gbc);
        currentPasswordField = createStyledPasswordField();
        gbc.gridx = 1; gbc.gridy = 1; resetPasswordPanel.add(currentPasswordField, gbc);

        JLabel newPassLabel = createStyledLabel("New Password:");
        gbc.gridx = 0; gbc.gridy = 2; resetPasswordPanel.add(newPassLabel, gbc);
        newPasswordField = createStyledPasswordField();
        gbc.gridx = 1; gbc.gridy = 2; resetPasswordPanel.add(newPasswordField, gbc);

        JLabel confirmNewPassLabel = createStyledLabel("Confirm New Password:");
        gbc.gridx = 0; gbc.gridy = 3; resetPasswordPanel.add(confirmNewPassLabel, gbc);
        confirmNewPasswordField = createStyledPasswordField();
        gbc.gridx = 1; gbc.gridy = 3; resetPasswordPanel.add(confirmNewPasswordField, gbc);

        JButton resetPwdButton = createStyledButton("Reset Password");
        resetPwdButton.setForeground(Color.BLACK); // Explicitly set foreground
        gbc.gridx = 0; gbc.gridy = 4; gbc.gridwidth = 2; resetPasswordPanel.add(resetPwdButton, gbc);

        JButton backButton = createStyledButton("Back to Vault");
        // Custom colors for back button
        backButton.setBackground(SECONDARY_BG);
        backButton.setForeground(Color.BLACK); // Ensure text color is light grey
        backButton.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                backButton.setBackground(BORDER_COLOR);
            }
            public void mouseExited(java.awt.event.MouseEvent evt) {
                backButton.setBackground(SECONDARY_BG);
            }
        });
        gbc.gridy = 5; resetPasswordPanel.add(backButton, gbc);

        resetPwdButton.addActionListener(e -> handleResetPassword());
        backButton.addActionListener(e -> cardLayout.show(mainPanel, "Vault"));
    }

    /**
     * Handles user login attempt.
     */
    private void handleLogin() {
        String username = loginUsernameField.getText();
        String password = new String(loginPasswordField.getPassword());

        if (username.isEmpty() || password.isEmpty()) {
            showMessageBox("Username and password cannot be empty.", "Login");
            return;
        }

        User user = users.get(username);
        if (user == null) {
            showMessageBox("User not found. Please register.", "Login");
            return;
        }

        try {
            if (user.login(username, password)) {
                currentUser = user;
                // Re-derive the encryption key for the current session
                Vault userVault = vaults.get(currentUser.getUserId());
                if (userVault != null) {
                    userVault.rederiveEncryptionKey(password); // Use plain password to re-derive key
                    currentEncryptionKey = userVault.getEncryptionKey();
                } else {
                    // This should ideally not happen if a vault is created at registration
                    // but handling it just in case.
                    showMessageBox("Error: User vault not found.", "Login");
                    currentUser = null;
                    currentEncryptionKey = null;
                    return;
                }

                populateVaultList();
                welcomeLabel.setText("Welcome, " + currentUser.getUsername() + "!");
                cardLayout.show(mainPanel, "Vault");
                loginPasswordField.setText(""); // Clear password field
            } else {
                showMessageBox("Invalid username or password.", "Login");
            }
        } catch (GeneralSecurityException ex) {
            System.err.println("Login security error: " + ex.getMessage());
            showMessageBox("A security error occurred during login.", "Login");
        }
    }

    /**
     * Handles new user registration.
     */
    private void handleRegister() {
        String username = regUsernameField.getText();
        String password = new String(regPasswordField.getPassword());
        String confirmPassword = new String(regConfirmPasswordField.getPassword());

        if (username.isEmpty() || password.isEmpty() || confirmPassword.isEmpty()) {
            showMessageBox("All fields must be filled for registration.", "Register");
            return;
        }

        if (users.containsKey(username)) {
            showMessageBox("Username already exists. Please choose another.", "Register");
            return;
        }

        if (!password.equals(confirmPassword)) {
            showMessageBox("Passwords do not match.", "Register");
            return;
        }

        try {
            User newUser = new User(username, password);
            users.put(username, newUser);

            // Create a new vault for the registered user
            Vault newVault = new Vault(newUser.getUserId(), password); // Use plain password for key derivation
            vaults.put(newUser.getUserId(), newVault);

            saveData(); // Save new user and their vault
            showMessageBox("Registration successful! Please login.", "Login");

            // Clear registration fields
            regUsernameField.setText("");
            regPasswordField.setText("");
            regConfirmPasswordField.setText("");
        } catch (GeneralSecurityException ex) {
            System.err.println("Registration security error: " + ex.getMessage());
            showMessageBox("A security error occurred during registration.", "Register");
        }
    }

    /**
     * Handles adding a new vault entry (Password or Note).
     */
    private void handleAddEntry() {
        if (currentUser == null) {
            showMessageBox("Please log in first.", "Login");
            return;
        }

        String title = entryTitleField.getText();
        if (title.isEmpty()) {
            showMessageBox("Title cannot be empty.", "AddEntry");
            return;
        }

        VaultItem newItem = null;
        String selectedType = (String) entryTypeComboBox.getSelectedItem();

        try {
            if ("Password".equals(selectedType)) {
                String siteName = passwordSiteField.getText();
                String userName = passwordUserField.getText();
                String plainPassword = new String(passwordPassField.getPassword());

                if (siteName.isEmpty() || userName.isEmpty() || plainPassword.isEmpty()) {
                    showMessageBox("All password entry fields must be filled.", "AddEntry");
                    return;
                }

                PasswordEntry passwordEntry = new PasswordEntry(title, currentUser.getUserId(), siteName, userName);
                passwordEntry.setPassword(plainPassword, currentEncryptionKey);
                newItem = passwordEntry;
            } else if ("Note".equals(selectedType)) {
                String plainNote = noteTextField.getText();
                if (plainNote.isEmpty()) {
                    showMessageBox("Note text cannot be empty.", "AddEntry");
                    return;
                }
                NoteEntry noteEntry = new NoteEntry(title, currentUser.getUserId());
                noteEntry.setNoteText(plainNote, currentEncryptionKey);
                newItem = noteEntry;
            }

            if (newItem != null) {
                Vault userVault = vaults.get(currentUser.getUserId());
                if (userVault != null) {
                    userVault.addEntry(newItem);
                    saveData();
                    populateVaultList();
                    showMessageBox("Entry added successfully!", "Vault");

                    // Clear fields
                    entryTitleField.setText("");
                    passwordSiteField.setText("");
                    passwordUserField.setText("");
                    passwordPassField.setText("");
                    noteTextField.setText("");
                } else {
                    showMessageBox("Error: User vault not found.", "Vault");
                }
            }
        } catch (GeneralSecurityException ex) {
            System.err.println("Encryption error during add entry: " + ex.getMessage());
            showMessageBox("Failed to encrypt entry. Security error.", "AddEntry");
        }
    }

    /**
     * Handles deleting a selected vault entry.
     */
    private void handleDeleteEntry() {
        int selectedIndex = vaultList.getSelectedIndex();
        if (selectedIndex == -1) {
            showMessageBox("Please select an entry to delete.", "Vault");
            return;
        }

        VaultItem selectedItem = vaultListModel.getElementAt(selectedIndex);
        Vault userVault = vaults.get(currentUser.getUserId());

        if (userVault != null && userVault.deleteEntry(selectedItem)) {
            saveData();
            populateVaultList(); // Refresh list
            displayDetailsArea.setText(""); // Clear details area
            showMessageBox("Entry deleted successfully.", "Vault");
        } else {
            showMessageBox("Failed to delete entry.", "Vault");
        }
    }

    /**
     * Handles copying the decrypted content of a selected entry to clipboard.
     */
    private void handleCopyContent() {
        VaultItem selectedItem = vaultList.getSelectedValue();
        if (selectedItem == null) {
            showMessageBox("Please select an entry to copy.", "Vault");
            return;
        }

        String contentToCopy = null;
        try {
            if (selectedItem instanceof PasswordEntry) {
                PasswordEntry pe = (PasswordEntry) selectedItem;
                contentToCopy = pe.getDecryptedPassword(currentEncryptionKey);
            } else if (selectedItem instanceof NoteEntry) {
                NoteEntry ne = (NoteEntry) selectedItem;
                contentToCopy = ne.getDecryptedNoteText(currentEncryptionKey);
            }

            if (contentToCopy != null) {
                Toolkit.getDefaultToolkit().getSystemClipboard().setContents(
                        new java.awt.datatransfer.StringSelection(contentToCopy), null);
                showMessageBox("Content copied to clipboard!", "Vault");
            }
        } catch (GeneralSecurityException ex) {
            System.err.println("Decryption error during copy: " + ex.getMessage());
            showMessageBox("Failed to decrypt content for copying.", "Vault");
        }
    }


    /**
     * Handles resetting the user's master password.
     */
    private void handleResetPassword() {
        if (currentUser == null) {
            showMessageBox("Please log in first.", "Login");
            return;
        }

        String currentPass = new String(currentPasswordField.getPassword());
        String newPass = new String(newPasswordField.getPassword());
        String confirmNewPass = new String(confirmNewPasswordField.getPassword());

        if (currentPass.isEmpty() || newPass.isEmpty() || confirmNewPass.isEmpty()) {
            showMessageBox("All password fields must be filled.", "ResetPassword");
            return;
        }

        if (!newPass.equals(confirmNewPass)) {
            showMessageBox("New passwords do not match.", "ResetPassword");
            return;
        }

        try {
            if (currentUser.resetPassword(currentPass, newPass)) {
                // Update the vault's encryption key based on the new password
                Vault userVault = vaults.get(currentUser.getUserId());
                if (userVault != null) {
                    userVault.rederiveEncryptionKey(newPass); // Re-derive with new password
                    currentEncryptionKey = userVault.getEncryptionKey(); // Update the session key
                } else {
                    showMessageBox("Error: User vault not found for key update.", "ResetPassword");
                    return;
                }
                saveData(); // Save updated user and vault
                showMessageBox("Password reset successfully!", "Vault");

                // Clear fields
                currentPasswordField.setText("");
                newPasswordField.setText("");
                confirmNewPasswordField.setText("");
            } else {
                showMessageBox("Incorrect current password.", "ResetPassword");
            }
        } catch (GeneralSecurityException ex) {
            System.err.println("Password reset security error: " + ex.getMessage());
            showMessageBox("A security error occurred during password reset.", "ResetPassword");
        }
    }

    /**
     * Handles user logout.
     */
    private void handleLogout() {
        if (currentUser != null) {
            currentUser.logout();
            currentUser = null;
            currentEncryptionKey = null; // Clear the key for security
            vaultListModel.clear(); // Clear the displayed vault items
            displayDetailsArea.setText(""); // Clear details area
            cardLayout.show(mainPanel, "Login");
            loginUsernameField.setText(""); // Clear username field
            loginPasswordField.setText(""); // Clear password field
            showMessageBox("You have been logged out.", "Login");
        }
    }

    /**
     * Populates the vault list model with the current user's vault items.
     */
    private void populateVaultList() {
        vaultListModel.clear();
        if (currentUser != null) {
            Vault userVault = vaults.get(currentUser.getUserId());
            if (userVault != null) {
                // Sort items by title for better readability
                userVault.getVaultItems().stream()
                        .sorted((item1, item2) -> item1.getTitle().compareToIgnoreCase(item2.getTitle()))
                        .forEach(vaultListModel::addElement);
            }
        }
    }

    /**
     * Loads user and vault data from files.
     */
    @SuppressWarnings("unchecked") // Suppress warning for unchecked cast
    private void loadData() {
        users = new HashMap<>();
        vaults = new HashMap<>();

        // Load Users
        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(USERS_FILE))) {
            users = (HashMap<String, User>) ois.readObject();
            System.out.println("Users loaded: " + users.size());
        } catch (FileNotFoundException e) {
            System.out.println("No users file found. Starting with empty user database.");
        } catch (IOException | ClassNotFoundException e) {
            System.err.println("Error loading users: " + e.getMessage());
        }

        // Load Vaults
        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(VAULTS_FILE))) {
            vaults = (HashMap<String, Vault>) ois.readObject();
            System.out.println("Vaults loaded: " + vaults.size());

            // During load, SecretKey is transient, so we only load key bytes and salt
            // The actual SecretKey will be re-derived on successful login.
        } catch (FileNotFoundException e) {
            System.out.println("No vaults file found. Starting with empty vault database.");
        } catch (IOException | ClassNotFoundException e) {
            System.err.println("Error loading vaults: " + e.getMessage());
        }
    }

    /**
     * Saves user and vault data to files.
     */
    private void saveData() {
        // Save Users
        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(USERS_FILE))) {
            oos.writeObject(users);
        } catch (IOException e) {
            System.err.println("Error saving users: " + e.getMessage());
        }

        // Save Vaults
        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(VAULTS_FILE))) {
            oos.writeObject(vaults);
        } catch (IOException e) {
            System.err.println("Error saving vaults: " + e.getMessage());
        }
    }

    /**
     * Helper method to create a styled JTextField.
     */
    private JTextField createStyledTextField() {
        JTextField field = new JTextField(20);
        field.setFont(new Font("Inter", Font.PLAIN, 16));
        field.setBackground(SECONDARY_BG);
        field.setForeground(Color.WHITE);
        field.setCaretColor(ACCENT_LIGHT); // Cursor color
        field.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER_COLOR, 1),
                BorderFactory.createEmptyBorder(8, 8, 8, 8) // Padding
        ));
        // Client property for rounded corners (works with FlatLaf or custom UI delegates)
        field.putClientProperty("JComponent.roundRect", true);
        return field;
    }

    /**
     * Helper method to create a styled JPasswordField.
     */
    private JPasswordField createStyledPasswordField() {
        JPasswordField field = new JPasswordField(20);
        field.setFont(new Font("Inter", Font.PLAIN, 16));
        field.setBackground(SECONDARY_BG);
        field.setForeground(Color.WHITE);
        field.setCaretColor(ACCENT_LIGHT);
        field.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER_COLOR, 1),
                BorderFactory.createEmptyBorder(8, 8, 8, 8)
        ));
        field.putClientProperty("JComponent.roundRect", true);
        return field;
    }

    /**
     * Helper method to create a styled JButton with rounded corners and hover effects.
     */
    private JButton createStyledButton(String text) {
        JButton button = new JButton(text);
        button.setFont(new Font("Inter", Font.BOLD, 16));
        button.setBackground(BUTTON_COLOR); // Default button color
        button.setForeground(Color.WHITE); // Ensure default text color is white
        button.setFocusPainted(false); // Remove focus border
        button.setBorder(BorderFactory.createEmptyBorder(10, 20, 10, 20)); // Padding
        button.setCursor(new Cursor(Cursor.HAND_CURSOR)); // Hand cursor on hover

        // Enable rounded corners (requires a custom UI delegate or FlatLaf)
        button.putClientProperty("JComponent.roundRect", true);
        button.putClientProperty("JButton.buttonType", "roundRect"); // Specific to some LAFs

        // Add hover effect
        button.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                button.setBackground(BUTTON_HOVER); // Lighter shade on hover
            }

            @Override
            public void mouseExited(java.awt.event.MouseEvent evt) {
                button.setBackground(BUTTON_COLOR); // Revert to original on exit
            }
        });
        return button;
    }

    /**
     * Helper method to create a styled JLabel for form fields.
     */
    private JLabel createStyledLabel(String text) {
        JLabel label = new JLabel(text);
        label.setForeground(Color.WHITE);
        label.setFont(new Font("Inter", Font.PLAIN, 16));
        return label;
    }

    public static void main(String[] args) {
        // Set an anti-aliased rendering hint for better font appearance
        System.setProperty("awt.useSystemAAFontSettings", "on");
        System.setProperty("swing.aatext", "true");

        // Set look and feel (Optional, but can improve consistency)
        // Using a modern Look and Feel like FlatLaf is highly recommended for better styling
        // and to make 'JComponent.roundRect' property work effectively.
        try {
            // If FlatLaf is available in your classpath:
            // UIManager.setLookAndFeel("com.formdev.flatlaf.FlatDarkLaf");
            // Otherwise, use system default for basic appearance:
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            System.err.println("Could not set Look and Feel: " + e.getMessage());
        }

        SwingUtilities.invokeLater(NotesAppGUI::new);
    }
}