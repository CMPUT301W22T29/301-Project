package com.example.quirky;

import java.util.ArrayList;

/**
 * @author Jonathen Adsit
 * Model class that stores a user's name, contact information, and id's of the qrcodes they scanned
 */
public class Profile {
    private String uname;
    private String email;
    private String phone;

    // Array list of QRCode id's to track which codes the user has scanned. TODO: determine if the array should hold QRCodes or just their id's.
    private ArrayList<String> scanned;
    public Profile(String uname, String email, String phone, ArrayList<String> scanned) {
        this.uname = uname;
        this.email = email;
        this.phone = phone;
        this.scanned = scanned;
    }

    public Profile(String uname) {
        this.uname = uname;
        this.scanned = new ArrayList<>();
        this.email = "";
        this.phone = "";
    }

    public String getUname() {
        return uname;
    }

    public void setUname(String uname) {
        this.uname = uname;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public ArrayList<String> getScanned() {
        return scanned;
    }

    /**
     * Adds a QRCode to the list of scanned QRCodes
     * @param qrId
     *      - The ID of the QRCode the player has scanned
     */
    public void addScanned(String qrId) {
        scanned.add(qrId);
    }

    /**
     * Removes a QRCode from the list of scanned QRCodes. Does nothing if the id is not in the array.
     * @param qrId
     *      - The ID of the QRCode the player wants to delete
     */
    public void removeScanned(String qrId) {
        if(scanned.contains(qrId))
            scanned.remove(qrId);
    }
}
