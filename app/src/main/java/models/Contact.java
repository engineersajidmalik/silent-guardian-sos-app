// Contact.java - SIMPLIFIED VERSION
package models;

public class Contact {
    private long id;
    private String name;
    private String phone;
    private String relationship;
    private String photoPath;

    // Constructors
    public Contact() {
        this.id = System.currentTimeMillis();
    }

    public Contact(String name, String phone) {
        this.id = System.currentTimeMillis();
        this.name = name;
        this.phone = phone;
        this.relationship = "Family";
    }

    // Getters and Setters
    public long getId() { return id; }
    public void setId(long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public String getRelationship() { return relationship; }
    public void setRelationship(String relationship) { this.relationship = relationship; }

    public String getPhotoPath() { return photoPath; }
    public void setPhotoPath(String photoPath) { this.photoPath = photoPath; }

    // Helper method to convert to "Name|Phone" format
    public String toContactData() {
        return name + "|" + phone;
    }

    // Helper method to parse from "Name|Phone" format
    public static Contact fromContactData(String contactData) {
        String[] parts = contactData.split("\\|");
        if (parts.length == 2) {
            return new Contact(parts[0], parts[1]);
        }
        return null;
    }
}