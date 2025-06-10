package com.vaultnotes;
import java.io.Serializable;
import java.util.Date;

/**
 * Abstract base class for items stored in the vault.
 * Implements Serializable to allow saving/loading to file.
 */
public abstract class VaultItem implements Serializable {
    private static final long serialVersionUID = 1L; // For serialization

    protected String title;
    protected Date createdDate;
    protected String userId; // Link to the user who owns this item

    public VaultItem(String title, String userId) {
        this.title = title;
        this.createdDate = new Date(); // Set current date upon creation
        this.userId = userId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public Date getCreatedDate() {
        return createdDate;
    }

    public String getUserId() {
        return userId;
    }

    /**
     * Abstract method to display details of the vault item.
     * Subclasses will implement this to show their specific information.
     */
    public abstract String displayDetails();

    /**
     * Abstract method to get a displayable summary for list views.
     * @return A short string summarizing the item.
     */
    public abstract String getSummary();
}

