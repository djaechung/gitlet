package gitlet;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import static gitlet.Main.COMMITS_FOLDER;

import java.util.Date;
import java.util.HashMap;

/** An object representing a gitlet commit, which contains
 * pointers to blobs, tracks files, and has its own metadata.
 * @author Daniel Chung
 */
public class Commit implements Serializable {

    /** Constructor for a normal commit.
     * @param files the files to be tracked by this commit.
     * @param blobs the blobs which store commit's versions of these files.
     * @param message the string message associated with this commit. */
    Commit(HashMap<String, File> files, HashMap<String, Blob> blobs,
           String message) {

        _timestamp = new Date(System.currentTimeMillis());

        byte[] totalFileBytes = Utils.serialize(_timestamp);
        for (File file: files.values()) {
            byte[] fileBytes = Utils.readContents(file);
            byte[] temp = new byte[totalFileBytes.length
                    + fileBytes.length];
            System.arraycopy(totalFileBytes, 0,
                    temp, 0, totalFileBytes.length);
            System.arraycopy(fileBytes, 0,
                    temp, totalFileBytes.length, fileBytes.length);
            totalFileBytes = temp;
        }
        _sha1 = Utils.sha1(totalFileBytes);

        _name = _sha1.substring(0, 6);
        _message = message;
        _trackedFiles = files;
        _blobs = blobs;
    }

    /** Constructor for a merge commit.
     * @param files the files to be tracked by this commit.
     * @param blobs the blobs which store commit's versions of these files.
     * @param message the string message associated with this commit.
     * @param currBranchID the SHA1 ID of the head of the current branch.
     * @param givenBranchID the SHA1 ID of the head of the given branch. */
    Commit(HashMap<String, File> files, HashMap<String, Blob> blobs,
           String message, String currBranchID, String givenBranchID) {

        _timestamp = new Date(System.currentTimeMillis());

        byte[] totalFileBytes = Utils.serialize(_timestamp);
        for (File file: files.values()) {
            byte[] fileBytes = Utils.readContents(file);
            byte[] temp = new byte[totalFileBytes.length
                    + fileBytes.length];
            System.arraycopy(totalFileBytes, 0,
                    temp, 0, totalFileBytes.length);
            System.arraycopy(fileBytes, 0,
                    temp, totalFileBytes.length, fileBytes.length);
            totalFileBytes = temp;
        }
        _sha1 = Utils.sha1(totalFileBytes);

        _name = _sha1.substring(0, 6);
        _message = message;
        _trackedFiles = files;
        _blobs = blobs;
        _isMerge = true;
        _mergeIDs[0] = currBranchID; _mergeIDs[1] = givenBranchID;
    }

    /** Special constructor for the initial commit, which has the commit
     * message "initial commit" and timestamp of the unix epoch. */
    Commit() {
        _message = "initial commit";
        _timestamp = new Date(0);
        byte[] timestampBytes = Utils.serialize(_timestamp);
        _sha1 = Utils.sha1(timestampBytes);
        _name = _sha1.substring(0, 6);
    }

    /** Return the file version contained in one of my blobs.
     * @param fileName the name of the file to retrieve contents from. */
    public byte[] bytesFromBlob(String fileName) {
        if (!_blobs.containsKey(fileName)) {
            throw new GitletException("File does not exist in that commit.");
        } else {
            return _blobs.get(fileName).contents();
        }
    }

    /** Retrieve my contents from within a file.
     * @param file the file form which to retrieve my contents.
     * @return the commit stored in the file. */
    Commit fromFile(File file) {
        return Utils.readObject(file, Commit.class);
    }

    /** Save this commit by serializing it into a file. */
    public void save() {
        File thisCommit = new File(".gitlet/commits/" + _name);
        Utils.join(COMMITS_FOLDER, (".gitlet/commits/" + _name));
        try {
            thisCommit.createNewFile();
        } catch (IOException ex) {
            System.out.println("Path leading to file is "
                    + "imcomplete or malformed.");
        }
        Utils.writeObject(thisCommit, this);
    }

    /** Determine my parent.
     * @param parent the commit which precedes me in my branch. */
    public void setParent(Commit parent) {
        _parent = parent;
    }

    /** Determine my merge parent.
     * @param mergeParent the head of the other branch
     *                    which merged to create me. */
    public void setMergeParent(Commit mergeParent) {
        _mergeParent = mergeParent;
    }

    /** Return my SHA1 ID. */
    public String sha1() {
        return _sha1;
    }

    /** Return my message. */
    public String message() {
        return _message;
    }

    /** Return my parent. */
    public Commit parent() {
        return _parent;
    }

    /** Return my merge parent. */
    public Commit mergeParent() {
        return _mergeParent;
    }

    /** Return whether I even have a merge parent. */
    public boolean hasMergeParent() {
        return !(_mergeParent == null);
    }

    /** Return my timestamp. */
    public Date timestamp() {
        return _timestamp;
    }

    /** Return the files I track. */
    public HashMap<String, File> files() {
        return _trackedFiles;
    }

    /** Return the blobs I contain. */
    public HashMap<String, Blob> blobs() {
        return _blobs;
    }

    /** Return the SHA1 IDs of the two heads which merged to create me. */
    public String[] mergeIDs() {
        return _mergeIDs;
    }

    /** Return whether I am a merge commit or not. */
    public boolean isMerge() {
        return _isMerge;
    }

    /** A string representation of this commit's SHA1 ID. */
    private String _sha1;

    /** The first 6 characters of this commit's SHA1 ID. This
     * becomes the commit's "nickname." */
    private String _name;

    /** The message that this commit was initialized with. */
    private String _message;

    /** The commit that precedes this commit. */
    private Commit _parent;

    /** The commit from another branch which was the head then the
     * other branch was merged with this commit's branch. In other
     * words, the merge parent. */
    private Commit _mergeParent;

    /** The date of the initialization of this commit. As a date,
     * it also contains information like day and time. */
    private Date _timestamp;

    /** The files that this commit tracks. That is, the files
     * that are saved with this commit. */
    private HashMap<String, File> _trackedFiles = new HashMap<String, File>();

    /** The blobs that this commit contains. */
    private HashMap<String, Blob> _blobs = new HashMap<String, Blob>();

    /** Whether I am a special merge commit or not. */
    private boolean _isMerge = false;

    /** The SHA1-IDS of the branch heads that merged to create me.
     * ONLY applies if I am a special merge commit. */
    private String[] _mergeIDs = new String[2];

}
