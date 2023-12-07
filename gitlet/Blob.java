package gitlet;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;

/** An object representing the contents of a specific version of a file.
 *  @author Daniel Chung
 */
public class Blob implements Serializable {

    /** Creates a new blob object, recording information
     * about its sha1 ID and byte contents.
     * @param file the file whose contents this Blob will store.
     */
    public Blob(File file) {
        byte[] fileContents = Utils.readContents(file);
        _contents = fileContents;
        _sha1 = Utils.sha1(fileContents);
        _name = _sha1.substring(0, 6);
    }

    /** Retrieve my contents from within a file.
     * @param file the file inside which this file is stored.
     * @return the blob contained within the given file. */
    Blob fromFile(File file) {
        return Utils.readObject(file, Blob.class);
    }

    /** Save this blob by serializing it into a file. */
    void saveBlob() {
        File thisBlob = new File(".gitlet/blobs/" + _name);
        Utils.join(Main.BLOBS_FOLDER, ".gitlet/blobs/" + _name);
        try {
            thisBlob.createNewFile();
        } catch (IOException ex) {
            System.out.println("Path leading to file is"
                   + " incomplete or malformed.");
        }
        Utils.writeContents(thisBlob, this);
    }

    /** Return my SHA1 ID. */
    public String sha1() {
        return _sha1;
    }

    /** Return my name. */
    public String name() {
        return _name;
    }

    /** Return my byte contents. */
    public byte[] contents() {
        return _contents;
    }

    /** The string representation of this blob's SHA1 ID. */
    private String _sha1;

    /** The name of this blob, simply the first six characters of
     * its SHA1 ID. */
    private String _name;

    /** The byte contents of the file this blob represents. */
    private byte[] _contents;

}
