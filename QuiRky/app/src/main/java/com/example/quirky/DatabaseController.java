package com.example.quirky;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.util.Log;


import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.WriteBatch;

import org.osmdroid.util.GeoPoint;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Jonathen Adsit
 * This is a controller class that does all the reading and writing to FireStore
 * Our database has these collections:
 *  - users
 *      Holds a document for each player holding their unique username, contact information, etc.
 *  - QRcodes
 *      Has a document for each QRCode that has been scanned with the app.
 *          Each document contains:
 *              - The score of the QRCode
 *              - Two inner collections:
 *                  - comments:
 *                      Holds a document for each comment the QRCode has.
 *                      The documents has 3 fields: content, user, and time
 *                  - userdata:
 *                      Holds a document for each user that has scanned this code.
 *                      The document contains: the photo the user took, and the location the user found the code.
 *
 */

// Reading & Writing Custom Objects to FireStore taken from:
// https://youtu.be/jJnm3YKfAUI
// Made By:
// https://www.youtube.com/channel/UC_Fh8kvtkVPkeihBs42jGcA
// Published April 15, 2018
public class DatabaseController {
    private final String TAG = "DatabaseController says: ";

    /* ~~~~~~~~~~~~~~~~~~~~~
        TODO: Look into AtomicReference<>, or Callback Interfaces
       ~~~~~~~~~~~~~~~~~~~~~ */
    private final FirebaseFirestore db;
    private final Context ct;

    private CollectionReference collection;
    private final OnCompleteListener writeListener;
    private final OnCompleteListener deleteListener;

    public DatabaseController(FirebaseFirestore db, Context ct) {
        this.db = db;
        this.ct = ct;

        this.writeListener = task -> {
            if(task.isSuccessful())
                Log.d(TAG, "Last write was a success");
            else
                Log.d(TAG, "Last write was a fail, cause: " + task.getException().getMessage()); // TODO: find a way to determine what the last write was. Should be easy.
        };

        this.deleteListener = task -> {
            if(task.isSuccessful())
                Log.d(TAG, "Deletion from database success!");
            else
                Log.d(TAG, "Deletion from database failed!!"); // TODO: find a way to determine what the last write was. Should be easy.
        };
    }

    /**
     * Add a comment to a QRCode in the databse
     * @param c The comment to write
     * @param id The QRCode the comment is written on
     */
    public void addComment(Comment c, String id) {
        collection = db.collection("QRcodes");

        collection = collection.document(id).collection("comments");
        collection.document(c.getId()).set(c).addOnCompleteListener(writeListener);
    }

    /**
     * Remove a Comment from a QRCode
     * @param c The comment to be removed
     * @param id The id of the QRCode the comment is on
     */
    public void removeComment(Comment c, String id) {   // TODO: consider making this method take the comment's id, rather than the object itself.
        collection = db.collection("QRcodes");

        collection = collection.document(id).collection("comments");
        collection.document(c.getId()).delete().addOnCompleteListener(deleteListener);
    }

    /**
     * Write a QRCode to the database. Will also add the QRCode to the account of the app-holder
     * @param qr The QRCode to be written
     */
    public void writeQRCode(QRCode qr) {
        assert(qr != null) : "You can't write a null object to the database!";

        // Get the user from memory
        MemoryController mc = new MemoryController(this.ct);
        Profile p = mc.read();
        String user = mc.readUser();

        // A batch write: a group of write operations to be done at once.
        // Batches are not written immediately. Can add many operations to a batch, then commit all writes at once.
        WriteBatch batch = db.batch();
        DocumentReference doc;

        // Set the document location
        collection = db.collection("QRcodes");
        doc = collection.document(qr.getId());

        // Write the score of the QRCode
        Map<String, Object> data = new HashMap<>();
        data.put("score", qr.getScore());
        batch.set(doc, data);

        // Add the comments to the batch. Skip any null comments.
        collection = doc.collection("comments");
        for(Comment c : qr.getComments()) {
            if(c==null) continue;
            batch.set(collection.document(c.getId()), c);
        }

        /* TODO: Writing these fields should be in a seperate method
        // Add the geolocation & photo to the batch
        collection = doc.collection("userdata");
        doc = collection.document(user);

        data = new HashMap<>();
        data.put("location", null);
        data.put("photo", null);

        batch.set(doc, data);
        */

        // Update the player's profile to include this QRCode as one they have scanned.
        p.addScanned(qr.getId());
        ArrayList<String> scanned = p.getScanned();
        mc.write(p);

        collection = db.collection("users");
        doc = collection.document(user);
        batch.update(doc, "scanned", scanned);


        // Issue the batch write
        batch.commit().addOnCompleteListener(writeListener);
    }

    /**
     * Delete a QRCode completely from the Database. Only the Owner should be able to do this.
     * TODO: Implement a method to remove a QRCode from a players profile
     * @param qr The QRCode to delete
     */
    public void deleteQRCode(QRCode qr) {
        collection = db.collection("QRCodes");
        collection.document(qr.getId()).delete().addOnCompleteListener(deleteListener);
    }

    /**
     * Write a new profile to the Database.
     * @param p The profile to be written
     */
    public void writeProfile(Profile p) {
        collection = db.collection("users");
        collection.document(p.getUname()).set(p).addOnCompleteListener(writeListener);
    }

    /**
     * Delete an account from the Database. Only the Owner should be able to do this.
     * @param p The profile to be deleted
     */
    public void deleteProfile(Profile p) {
        collection = db.collection("users");
        collection.document(p.getUname()).delete().addOnCompleteListener(deleteListener);
    }

    /**
     * Begin reading a profile from the Database.
     * This is an Asynchronous operation, and such this method not return the result. The result can be obtained from getProfile()
     * @param username The username of the profile to be read
     * @return A task representing the read operation. This must be passed to getProfile(task)
     */
    public Task<DocumentSnapshot> readProfile(String username) {
        collection = db.collection("users");
        return collection.document(username).get();
    }

    /**
     * Get the results of the readProfile(). This method should only be called once the Task returned from ReadProfile has completed.
     * @param task The task returned from readProfile(). Calling with any other task will result in errors.
     * @return The profile with the username given to readProfile(). Will return null if the username does not exist.
     */
    public Profile getProfile(Task<DocumentSnapshot> task) {
        DocumentSnapshot doc = task.getResult();
        if(doc.getData() == null) {
            Log.d(TAG, "getProfile() returned a null Profile");
            return null;
        }
        return doc.toObject(Profile.class);
    }

    /**
     * Begin querying the database for profiles with a similar username to the given string.
     * This is an Asynchronous operation, and such this method not return the result. The result can be obtained from getProfile()
     * @param search The string to search for similar usernames with
     * @return A task representing the read operation. This must be passed to getUserSearchQuery(task)
     */
    public Task<QuerySnapshot> startUserSearchQuery(String search) {
        collection = db.collection("users");
        return collection.whereGreaterThanOrEqualTo("uname", search).get();
    }

    /**
     * Get the results of the startUserSearchQuery(); This method should only be called once the Task returned from it has completed.
     * @param task The task returned from startUserSearchQuery(). Calling with any other task will result in errors.
     * @return An ArrayList of profiles with usernames similar to the string given to startUserSearchQuery().
     */
    public ArrayList<Profile> getUserSearchQuery(Task<QuerySnapshot> task) {
        QuerySnapshot result = task.getResult();
        return (ArrayList<Profile>) result.toObjects(Profile.class);
    }

    /**
     * Begin reading a QRCode from the database.
     * This is an Asynchronous operation, and such this method not return the result. The result can be obtained from getProfile()
     * @param id The id of the QRCode to be read
     * @return A task representing the read operation. This must be passed to getQRCode(task)
     */
    public Task<DocumentSnapshot> readQRCode(String id) {
        collection = db.collection("QRcodes");
        return collection.document(id).get();
    }

    /**
     * Get the results of the readQRCode(); This method should only be called once the Task returned from it has completed.
     * @param task The task returned from readQRCode(). Calling with any other task will result in errors.
     * @return The QRCode with the ID that was passed to readQRCode(). Returns null if such a QRCode does not exist in the database
     */
    public QRCode getQRCode(Task<DocumentSnapshot> task) {
        DocumentSnapshot doc = task.getResult();
        try {
            return new QRCode(doc.getId(), (int) doc.get("score"));
        } catch (NullPointerException e) {
            Log.d(TAG, "getQRCode returned a Null object");
            return null;
        }
    }

    /* - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - */
    public Task<QuerySnapshot> readQRCodeUserData(String qrcode) {
        collection = db.collection("QRCodes").document(qrcode).collection("userdata");
        return collection.get();
    }

    public ArrayList<GeoPoint> getQRCodeLocations(Task<QuerySnapshot> task) {
        QuerySnapshot query = task.getResult();
        ArrayList<GeoPoint> locations = new ArrayList<>();
        for(DocumentSnapshot doc : query.getDocuments()) {
            if(doc == null) continue;
            locations.add((GeoPoint) doc.get("location"));      // FIXME: this will 1000% crash, need to think about how to get a GeoPoint object from a lat & long in firestore

        }
        return locations;
    }

    public ArrayList<Drawable> getQRCodePhotos(Task<QuerySnapshot> task) {
        QuerySnapshot query = task.getResult();
        ArrayList<Drawable> photos = new ArrayList<>();
        for(DocumentSnapshot doc : query.getDocuments()) {
            if(doc == null) continue;
            photos.add((Drawable) doc.get("photo"));        // FIXME: this will 1000% crash, need to think about how store Drawables to FireStore

        }
        return photos;
    }

    public ArrayList<String> getQRCodeScanners(Task<QuerySnapshot> task) {
        QuerySnapshot query = task.getResult();
        ArrayList<String> photos = new ArrayList<>();
        for(DocumentSnapshot doc : query.getDocuments()) {
            if(doc == null) continue;
            photos.add(doc.getId());
        }
        return photos;
    }
    /* - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - */

}
