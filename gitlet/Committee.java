package gitlet;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.LinkedList;

import static gitlet.Main.COMMITS_FOLDER;
import static gitlet.Reporter.GITLET_FORMAT;

/** The controller class that has access to all commits. Has the
 * authority to add commits and perform branch operations as well
 * as fetch commit-related data.
 * @author Daniel Chung
 */
public class Committee implements Serializable {

    /** A new committee containing a special initial commit and
     * only one (master) branch. */
    Committee() {
        makeInitCommit();
    }

    /** Writes a log containing SHA1 ID, timestamp, merge data, and
     * commit message of the head commit and all of its parents.
     * @return an ArrayList of string arrays, one for each commit,
     * containing (in order) its SHA1 ID, timestamp, merge data, and
     * commit message. */
    ArrayList<String[]> writeLog() {
        ArrayList<String[]> logList = new ArrayList<String[]>();
        Commit currCommit = _head;
        while (currCommit != null) {

            String[] commitData = new String[4];
            commitData[0] = currCommit.sha1();
            if (currCommit.isMerge()) {
                String[] mergeIDs = currCommit.mergeIDs();
                commitData[1] = mergeIDs[0].substring(0, 7)
                        + " " + mergeIDs[1].substring(0, 7);
            }
            commitData[2] = GITLET_FORMAT.format(currCommit.timestamp());
            commitData[3] = currCommit.message();

            logList.add(commitData);

            currCommit = currCommit.parent();
        }
        return logList;
    }

    /** Writes a log containing SHA1 ID, timestamp, merge data, and
     * commit message of ALL COMMITS saved in this gitlet directory.
     * @return an ArrayList of string arrays, one for each commit,
     * containing (in order) its SHA1 ID, timestamp, merge data, and
     * commit message. */
    ArrayList<String[]> writeGlobalLog() {
        ArrayList<String[]> logList = new ArrayList<String[]>();
        for (Commit commit: _allIDs.values()) {

            String[] commitData = new String[4];
            commitData[0] = commit.sha1();
            if (commit.isMerge()) {
                String[] mergeIDs = commit.mergeIDs();
                commitData[1] = mergeIDs[0].substring(0, 7)
                        + " " + mergeIDs[1].substring(0, 7);
            }
            commitData[2] = GITLET_FORMAT.format(commit.timestamp());
            commitData[3] = commit.message();

            logList.add(commitData);
        }
        return logList;
    }

    /** Returns a list of commit IDs whose commits have the
     * given message.
     * @param message the commit message query.
     * @return a list of commit IDs whose commits have message. */
    ArrayList<String> findByMessage(String message) {
        if (!_allCommits.containsKey(message)) {
            System.out.println("Found no commit with that message.");
            return null;
        } else {
            ArrayList<Commit> commitsFound = _allCommits.get(message);
            ArrayList<String> commitsNames = new ArrayList<String>();
            for (Commit commit: commitsFound) {
                commitsNames.add(commit.sha1());
            }
            return commitsNames;
        }
    }

    /** Returns the file with name fileName if it exists and is tracked by
     * the current head commit.
     * @param fileName the name of the file being searched for in current head.
     * @return the file with name fileName, if it exists. */
    File findFile(String fileName) {
        HashMap<String, File> headFiles = _head.files();
        if (!headFiles.containsKey(fileName)) {
            System.out.println("File does not exist in that commit.");
            return null;
        } else {
            return headFiles.get(fileName);
        }
    }

    /** Returns true if a file with name fileName exists and is tracked by
     * the current head commit.
     * @param fileName the name of the file to verify exists or not.
     * @return whether a file with name fileName exists in the current head. */
    boolean hasFile(String fileName) {
        HashMap<String, File> headFiles = _head.files();
        if (!headFiles.containsKey(fileName)) {
            return false;
        }
        return true;
    }

    /** Returns the file with name fileName if it exists and is tracked by
     * the current head commit.
     * @param fileName the name of the file whose version is needed.
     * @param sha1 the ID of the commit whose version of the file is sought.
     * @return a byte array representing the contents of that file version. */
    byte[] findFileVersion(String fileName, String sha1) {
        if (!_allIDs.containsKey(sha1)) {
            for (String id: _allIDs.keySet()) {
                CharSequence abbreviation = sha1;
                if (id.contains(sha1)) {
                    sha1 = id;
                }
            }
            if (!_allIDs.containsKey(sha1)) {
                System.out.println("No commit with that id exists.");
                return null;
            }
        }
        Commit commit = _allIDs.get(sha1);

        if (!commit.blobs().containsKey(fileName)) {
            System.out.println("File does not exist in that commit.");
            return null;
        } else {
            return commit.bytesFromBlob(fileName);
        }
    }

    /** Returns all files that exist and are tracked by the given commit.
     * @param sha1 the ID of the commit whose file versions are sought.
     * @return a mapping of file names to byte arrays representing their
     * contents. Each file listed exists in the commit with this ID. */
    HashMap<String, byte[]> findFileVersions(String sha1) {
        HashMap<String, byte[]> fileBytes = new HashMap<String, byte[]>();
        if (!_allIDs.containsKey(sha1)) {
            System.out.println("No commit with that id exists.");
            return null;
        }
        Commit commit = _allIDs.get(sha1);

        for (String fileName: commit.files().keySet()) {
            if (!commit.blobs().containsKey(fileName)) {
                System.out.println("File does not exist in that commit.");
                return null;
            } else {
                fileBytes.put(fileName, commit.bytesFromBlob(fileName));
            }
        }
        return fileBytes;
    }

    /** Background checks the branch being searched for. Will notify user
     * however if the branch doesn't exist or if branch is the currBranch.
     * Returns true if the branch exists, and false if it doesn't.
     * @param branch the name of the branch to be checked.
     * @return whether the branch can be checked out. */
    boolean checkout(String branch) {
        if (!_branches.containsKey(branch)) {
            System.out.println("No such branch exists.");
            return false;
        } else if (branch.equals(_currBranchName)) {
            System.out.println("No need to checkout the current branch.");
            return false;
        } else {
            return true;
        }
    }

    /** Retrieves a mapping of fileNames to byte arrays from each of the
     * file version stored in the head commit of a given branch.
     * @param branch the name of the branch whose file contents are sought.
     * @return a mapping of file names to byte arrays representing the file
     * contents of the files tracked by the head of the specified branch. */
    HashMap<String, byte[]> headBytesOfBranch(String branch) {
        Commit head = _branches.get(branch);
        return findFileVersions(head.sha1());
    }

    /** Returns a list of string file names for all files tracked by the
     * current head commit.
     * @return a list of file names for files tracked by the current head. */
    ArrayList<String> trackedFiles() {
        ArrayList<String> headFiles = new ArrayList<String>();
        for (String fileName: _head.files().keySet()) {
            headFiles.add(fileName);
        }
        return headFiles;
    }

    /** Returns a list of string file names for all files tracked by
     * the head of the specified branch.
     * @param branch the name of the branch whose tracked files are sought.
     * @return a list of names of the files tracked by the head of the
     * specified branch.*/
    ArrayList<String> trackedFilesOfBr(String branch) {
        ArrayList<String> headFiles = new ArrayList<String>();
        for (String fileName: _branches.get(branch).files().keySet()) {
            headFiles.add(fileName);
        }
        return headFiles;
    }

    /** Returns a list of string file names for all files tracked by the
     * given commit.
     * @param sha1 the ID of the commit whose tracked files are sought.
     * @return a list of names of the files tracked by that commit. */
    ArrayList<String> trackedFilesOfCom(String sha1) {
        ArrayList<String> headFiles = new ArrayList<String>();
        if (!_allIDs.containsKey(sha1)) {
            for (String id: _allIDs.keySet()) {
                CharSequence abbreviation = sha1;
                if (id.contains(sha1)) {
                    sha1 = id;
                }
            }
            if (!_allIDs.containsKey(sha1)) {
                System.out.println("No commit with that id exists.");
                return null;
            }
        }
        Commit commit = _allIDs.get(sha1);
        for (String fileName: commit.files().keySet()) {
            headFiles.add(fileName);
        }
        return headFiles;
    }

    /** Returns the candidate split point closest to the head of the
     * current branch. That is, the split point reachable by the
     * fewest parent pointers.
     * @param currBranch the name of the current branch.
     * @param givenBranch the name of the branch to be merged into currBranch.
     * @return the closest shared ancestral commit between the two branches. */
    Commit findSplitPoint(String currBranch, String givenBranch) {
        List<Commit> givenPath = new ArrayList<Commit>();
        givenPath = totalPathOf(_branches.get(givenBranch), givenPath);

        return seekFrom(_branches.get(currBranch), givenPath);
    }

    /** Helper method for findSplitPoint. Returns a list of commits
     * in the path of a given branch starting from its head, including
     * branching from merge parents.
     * @param head the head of the branch whose path is sought.
     * @param path the list containing all commits along the path so far.
     * @return this list of all commits along the path. */
    private List<Commit> totalPathOf(Commit head, List<Commit> path) {
        if (head == null) {
            return path;
        } else {
            if (!path.contains(head)) {
                path.add(head);
            }
            if (head.hasMergeParent()) {
                path.addAll(totalPathOf(head.mergeParent(), path));
            }
            path.addAll(totalPathOf(head.parent(), path));
        }
        return path;
    }

    /** Helper method for findSplitPoint. Returns the first commit in
     * the path of one branch that also occurs in the path of another.
     * In other words, the closest splitPoint. Returns null if no such
     * splitPoint exists.
     * @param head the head of the current, "seeker" branch in this merge.
     * @param path the total path of the other (given) branch in this merge.
     * @return the closest shared ancestral commit between the two branches. */
    private Commit seekFrom(Commit head, List<Commit> path) {
        LinkedList<Commit> queue = new LinkedList<Commit>();
        while (true) {
            if (head == null) {
                return null;
            }
            if (path.contains(head)) {
                return head;
            } else {
                if (head.hasMergeParent()) {
                    queue.add(head.mergeParent());
                }
                queue.add(head.parent());
                head = queue.pop();
            }
        }
    }

    /** Sets the current head to be commit newHead.
     * @param newHead the commit which will become the new head. */
    void updateHead(Commit newHead) {
        _head = newHead;
        if (_currBranch != newHead) {
            _currBranch = newHead;
            _branches.replace(_currBranchName, newHead);
        }
    }

    /** Sets the current head to be an older commit with the given
     * SHA1 ID, which it assumes exists. Also, poly whorf schism.
     * @param sha1 the ID of the commit which will become the new head. */
    void updateHead(String sha1) {
        Commit newHead = _allIDs.get(sha1);
        updateHead(newHead);
    }

    /** Sets the current branch to point to commit newHead.
     * @param newHead the commit which will become the new head of
     *                the current branch. */
    void updateBranch(Commit newHead) {
        _currBranch = newHead;
        _branches.replace(_currBranchName, newHead);
        if (_head != newHead) {
            _head = newHead;
        }
    }

    /** Sets head to the head commit of the given branch. Also,
     * poly whorf schism.
     * @param branch the name of the branch which will become the current
     *              branch and whose head will become the current head. */
    void updateBranch(String branch) {
        _currBranch = _branches.get(branch);
        if (_head != _currBranch) {
            _head = _currBranch;
        }
        _currBranchName = branch;
    }

    /** Initializes a new commit and stores its data.
     * @param trackedFiles thefiles staged for addition into this commit.
     * @param blobs the blobs which store the versions of said files
     * @param message the string message associated with this commit.
     * @param toRemove the files to remove from this commit's tracking list. */
    void makeCommit(HashMap<String, File> trackedFiles, HashMap<String,
            Blob> blobs, String message, List<String> toRemove) {

        HashMap<String, File> cumulativeFiles = new HashMap<String, File>();
        cumulativeFiles.putAll(_head.files());
        cumulativeFiles.putAll(trackedFiles);

        HashMap<String, Blob> cumulativeBlobs = new HashMap<String, Blob>();
        cumulativeBlobs.putAll(_head.blobs());
        cumulativeBlobs.putAll(blobs);

        for (String removeMe: toRemove) {
            cumulativeFiles.remove(removeMe);
            cumulativeBlobs.remove(removeMe);
        }

        Commit newCommit = new Commit(cumulativeFiles,
                cumulativeBlobs, message);

        ArrayList<Commit> commitContainer = new ArrayList<Commit>();
        if (_allCommits.containsKey(message)) {
            commitContainer = _allCommits.get(message);
        }
        commitContainer.add(newCommit);

        _allCommits.put(message, commitContainer);
        _allIDs.put(newCommit.sha1(), newCommit);
        newCommit.setParent(_head);
        updateHead(newCommit);
        newCommit.save();
    }

    /** Initializes a new special merge commit and stores its data.
     * @param trackedFiles thefiles staged for addition into this commit.
     * @param blobs the blobs which store the versions of said files.
     * @param message the string message associated with this commit.
     * @param toRemove the files to remove from this commit's tracking list.
     * @param currID the ID of the head of the current branch.
     * @param givenID the ID of the head of the branch that merged into the
     *                current branch to create this commit. */
    void makeMergeCommit(HashMap<String, File> trackedFiles, HashMap<String,
            Blob> blobs, String message, List<String> toRemove,
                         String currID, String givenID) {

        HashMap<String, File> cumulativeFiles = new HashMap<String, File>();
        cumulativeFiles.putAll(_head.files());
        cumulativeFiles.putAll(trackedFiles);

        HashMap<String, Blob> cumulativeBlobs = new HashMap<String, Blob>();
        cumulativeBlobs.putAll(_head.blobs());
        cumulativeBlobs.putAll(blobs);

        for (String removeMe: toRemove) {
            cumulativeFiles.remove(removeMe);
            cumulativeBlobs.remove(removeMe);
        }

        Commit newCommit = new Commit(cumulativeFiles, cumulativeBlobs,
                message, currID, givenID);

        ArrayList<Commit> commitContainer = new ArrayList<Commit>();
        if (_allCommits.containsKey(message)) {
            commitContainer = _allCommits.get(message);
        }
        commitContainer.add(newCommit);

        _allCommits.put(message, commitContainer);
        _allIDs.put(newCommit.sha1(), newCommit);
        newCommit.setParent(_head);
        newCommit.setMergeParent(_allIDs.get(givenID));
        updateHead(newCommit);
        newCommit.save();
    }

    /** Initializes the special case initial commit, which has the commit
     * message "initial commit" and a timestamp of the unix epoch. */
    void makeInitCommit() {
        Commit initCommit = new Commit();
        ArrayList<Commit> commitContainer = new ArrayList<Commit>();
        commitContainer.add(initCommit);
        _allCommits.put("initial commit", commitContainer);
        _allIDs.put(initCommit.sha1(), initCommit);
        _branches.put("master", initCommit);
        updateHead(initCommit);
        _currBranchName = "master";
        initCommit.save();

    }

    /** Creates a new branch pointer to the current head.
     * @param branch the name of the branch to be created. */
    void makeBranch(String branch) {
        if (_branches.containsKey(branch)) {
            System.out.println("A branch with that name already exists.");
            return;
        }
        _branches.put(branch, _head);
    }

    /** Removes the branch, but just its pointer, not any of its commits.
     * @param branch the name of the branch to be removed. */
    void removeBranch(String branch) {
        if (!_branches.containsKey(branch)) {
            System.out.println("A branch with that name does not exist.");
            return;
        } else if (branch.equals(_currBranchName)) {
            System.out.println("Cannot remove the current branch.");
            return;
        } else {
            _branches.remove(branch);
        }
    }

    /** Retrieve my contents from within a file.
     * @param file the file from which to retrieve my contents.
     * @return the committee contained inside this file. */
    static Committee fromFile(File file) {
        return Utils.readObject(file, Committee.class);
    }

    /** Save this committee by serializing it into a file. */
    void save() {
        File thisCommittee = new File(".gitlet/commits/"
                + "committee");
        Utils.join(COMMITS_FOLDER, (".gitlet/commits/" + "committee"));
        try {
            thisCommittee.createNewFile();
        } catch (IOException ex) {
            System.out.println("Path leading to file is "
                    + "imcomplete or malformed.");
        }
        Utils.writeObject(thisCommittee, this);
    }

    /** Return a List of my branches as string reprs of their names. */
    List<String> branches() {
        List<String> branches = new ArrayList<String>();
        for (String branch: _branches.keySet()) {
            branches.add(branch);
        }
        return branches;
    }

    /** Return my head commit. */
    Commit head() {
        return _head;
    }

    /** Return the head of one of my branches.
     * @param branch the which points to the commit that is sought. */
    Commit headOf(String branch) {
        return _branches.get(branch);
    }

    /** Return the head of my current branch. */
    Commit branchHead() {
        return _currBranch;
    }

    /** Return the name of my current branch. */
    String currBranchName() {
        return _currBranchName;
    }

    /** The most recent commit of the current branch. */
    private Commit _head;

    /** The commit which the current branch points to. */
    private Commit _currBranch;

    /** The string name of the curren branch. */
    private String _currBranchName;

    /** A mapping of branch names to branch heads. */
    private HashMap<String, Commit> _branches
            = new HashMap<String, Commit>();

    /** A mapping of commit messages to commits. */
    private HashMap<String, ArrayList<Commit>> _allCommits
            = new HashMap<String, ArrayList<Commit>>();

    /** A mapping of commit IDs to commits. */
    private HashMap<String, Commit> _allIDs
            = new HashMap<String, Commit>();

}
