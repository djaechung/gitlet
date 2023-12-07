package gitlet;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import static gitlet.Main.GITLET_FOLDER;
import static gitlet.Main.FILES_FOLDER;
import static gitlet.Main.TEMP_FOLDER;
import static gitlet.Main.STAGEADD_FOLDER;
import static gitlet.Main.STAGEREM_FOLDER;
import static gitlet.Main.COMMITS_FOLDER;
import static gitlet.Main.BLOBS_FOLDER;
import static gitlet.Main.CWD_PATH;
import static gitlet.Main.CWD;
import static gitlet.Utils.plainFilenamesIn;
import static gitlet.Utils.reactivate;
import static gitlet.Utils.moveFile;
import static gitlet.Utils.clearStage;
import static gitlet.Utils.clearStageOf;
import static gitlet.Utils.restrictedDelete;
import static gitlet.Utils.readContents;
import static gitlet.Utils.surgicalDelete;
import static gitlet.Utils.join;
import static gitlet.Utils.writeContents;
import static gitlet.Utils.readContentsAsString;

/** Controller class with authority over the .gitlet and working
 * directories.
 * @author Daniel Chung
 */
public class Director {

    /** The Director has authority to orchestrate operations
     * within the .gitlet directory and working directory including
     * the movement of files and control of the Committee class, which
     * has commit methods. */
    Director() {
        _gitletPresent = false;
    }

    /** Takes in a line of input, identifies which command to
     * execute, and executes the command if it exists, potentially
     * changing the state of files in the .gitlet or working directory.
     * @param args the input specifying a gitlet command and qualifier.
     */
    public void process(String[] args) {
        String command = args[0];
        if (!command.equals("init")) {
            _committee = Committee.fromFile(
                    new File(".gitlet/commits/committee"));
        }
        switch (command) {
        case "init":
            preInit(args);
            break;
        case "add":
            preAdd(args);
            break;
        case "commit":
            preCommit(args);
            break;
        case "rm":
            preRemove(args);
            break;
        case "checkout":
            preCheckout(args);
            break;
        case "branch":
            preBranch(args);
            break;
        case "rm-branch":
            preRemoveBranch(args);
            break;
        case "reset":
            preReset(args);
            break;
        case "merge":
            preMerge(args);
            break;
        default:
            System.out.println("No command with that name exists.");
            break;
        }
    }

    /** Sanitize input for init command.
     * @param args the command to be evaluated. */
    private void preInit(String...args) {
        if (args.length == 1) {
            init();
        } else {
            System.out.println("Incorrect operands.");
        }
    }

    /** Sanitize input for add command.
     * @param args the command to be evaluated. */
    private void preAdd(String...args) {
        if (args.length == 2) {
            add(args[1]);
        } else if (args.length == 1) {
            System.out.println("Specify a file to add.");
        } else {
            System.out.println("Incorrect operands.");
        }
    }

    /** Sanitize input for commit command.
     * @param args the command to be evaluated.*/
    private void preCommit(String...args) {
        if (args.length == 2 && !args[1].equals("")) {
            commit(args[1]);
        } else if (args[1].equals("")) {
            System.out.println("Please enter a commit message.");
        } else {
            System.out.println("Incorrect operands.");
        }
    }

    /** Sanitize input for remove command.
     * @param args the command to be evaluated. */
    private void preRemove(String...args) {
        if (args.length == 2) {
            remove(args[1]);
        } else {
            System.out.println("Incorrect operands.");
        }
    }

    /** Sanitize input for checkout command.
     * @param args the command to be evaluated. */
    private void preCheckout(String...args) {
        if (args.length == 3 && args[1].equals("--")) {
            checkoutName(args[2]);
        } else if (args.length == 4 && args[2].equals("--")) {
            checkoutID(args[1], args[3]);
        } else if (args.length == 2) {
            checkoutBranch(args[1]);
        } else {
            System.out.println("Incorrect operands.");
        }
    }

    /** Sanitize input for branch command.
     * @param args the command to be evaluated. */
    private void preBranch(String...args) {
        if (args.length == 2) {
            branch(args[1]);
        } else {
            System.out.println("Incorrect operands");
        }
    }

    /** Sanitize input for rm-branch command.
     * @param args the command to be evaluated. */
    private void preRemoveBranch(String...args) {
        if (args.length == 2) {
            removeBranch(args[1]);
        } else {
            System.out.println("Incorrect operands");
        }
    }

    /** Sanitize input for reset command.
     * @param args the command to be evaluated. */
    private void preReset(String...args) {
        if (args.length == 2) {
            reset(args[1]);
        } else {
            System.out.println("Incorrect operands");
        }
    }

    /** Sanitize input for merge command.
     * @param args the command to be evaluated. */
    private void preMerge(String...args) {
        if (args.length == 2) {
            merge(args[1]);
        } else {
            System.out.println("Incorrect operands");
        }
    }

    /**
     * Does required filesystem operations to allow for persistence.
     * Creates any necessary folders or files in this structure:
     *
     * .gitet/ -- top level folder for all persistent data
     *    - files/    -- folder containing all of the persistent data
     *                   for added and committed files.
     *    - stageAdd/ -- folder containing all of the persistent data
     *                   for files staged for addition.
     *    - stageRem/ -- folder containing all of the persistent data
     *                   for files staged for removal.
     *    - commits/  -- folder containing all of the persistent data
     *                   for commits.
     *    - blobs/    -- folder containing all of the persistent data
     *                   for blobs.
     */
    private void init() {
        if (_gitletPresent) {
            throw new GitletException("A Gitlet version-control system "
                  +  "already exists in the current directory.");
        }

        GITLET_FOLDER.mkdir();

        Utils.join(GITLET_FOLDER, ".gitlet/files/");
        FILES_FOLDER.mkdir();

        Utils.join(GITLET_FOLDER, ".gitlet/temp/");
        TEMP_FOLDER.mkdir();

        Utils.join(GITLET_FOLDER, ".gitlet/stageAdd/");
        STAGEADD_FOLDER.mkdir();

        Utils.join(GITLET_FOLDER, ".gitlet/stageRem/");
        STAGEREM_FOLDER.mkdir();

        Utils.join(GITLET_FOLDER, ".gitlet/commits/");
        COMMITS_FOLDER.mkdir();

        Utils.join(GITLET_FOLDER, ".gitlet/blobs/");
        BLOBS_FOLDER.mkdir();

        _committee = new Committee();
        _committee.save();
        _gitletPresent = true;
    }

    /** Copies a file with name fileName from the working directory
     * to the stage-add directory, which stages it for addition.
     * @param fileName the name of the file to be moved. */
    private void add(String fileName) {
        File cwdFile = new File(CWD_PATH + fileName);
        File addFile = new File(".gitlet/stageAdd/" + fileName);
        File remFile = new File(".gitlet/stageRem/" + fileName);
        if (!cwdFile.exists() && !addFile.exists() && !remFile.exists()) {
            System.out.println("File does not exist.");
            return;
        }

        List<String> toRemove = plainFilenamesIn(".gitlet/stageRem/");
        if (toRemove.contains(fileName)) {
            File currFile = new File(".gitlet/files/" + fileName);
            byte[] currBytes = readContents(currFile);
            byte[] remBytes = readContents(remFile);

            if (!Arrays.equals(remBytes, currBytes)) {
                moveFile(".gitlet/stageRem/",
                        ".gitlet/stageAdd/", STAGEADD_FOLDER, fileName);
            }
            surgicalDelete(".gitlet/stageRem/", fileName);
        }

        byte[] newBytes = readContents(cwdFile);
        if (_committee.hasFile(fileName)) {
            File currFile = new File(".gitlet/files/" + fileName);
            byte[] currBytes = readContents(currFile);
            if (Arrays.equals(newBytes, currBytes)) {
                return;
            }
        }

        moveFile(CWD_PATH, ".gitlet/stageAdd/", STAGEADD_FOLDER, fileName);
    }

    /** Creates a new commit, which tracks any files previously in the staging
     * area and removes all files from the staging area following
     * initialization. Data about the new commit is recorded in the
     * committee'shashmaps.
     * @param message the string message to be associated with this commit. */
    private void commit(String message) {

        List<String> trackableNames = plainFilenamesIn(".gitlet/stageAdd/");
        List<String> toRemove = plainFilenamesIn(".gitlet/stageRem/");

        if ((trackableNames == null || trackableNames.size() == 0)
                && (toRemove == null || toRemove.size() == 0)) {
            System.out.println("No changes added to the commit.");
            return;
        }

        HashMap<String, File> commitFiles = new HashMap<String, File>();
        HashMap<String, Blob> commitBlobs = new HashMap<String, Blob>();
        for (String name: trackableNames) {
            File asFile = new File(name);
            Blob asBlob = new Blob(asFile);
            commitFiles.put(name, asFile);
            commitBlobs.put(name, asBlob);
            moveFile(".gitlet/stageAdd/",
                    ".gitlet/files/", FILES_FOLDER, name);
        }

        _committee.makeCommit(commitFiles, commitBlobs, message, toRemove);
        _committee.save();
        clearStage();
    }

    /** Creates a new merge commit, which tracks any files previously in the
     * staging area and removes all files from the staging area following
     * initialization. Data about the new commit is recorded in the committee's
     * hashmaps. Also, poly whorf schism.
     * @param message the string message to be associated with this commit.
     * @param currID the ID of the head of the current branch,
     * @param givenID the ID of the head of the branch which merged into the
     *                current branch to create this commit. */
    private void commit(String message, String currID, String givenID) {
        List<String> trackableNames = plainFilenamesIn(".gitlet/stageAdd/");
        List<String> toRemove = plainFilenamesIn(".gitlet/stageRem/");

        HashMap<String, File> commitFiles = new HashMap<String, File>();
        HashMap<String, Blob> commitBlobs = new HashMap<String, Blob>();
        for (String name: trackableNames) {
            File asFile = new File(name);
            Blob asBlob = new Blob(asFile);
            commitFiles.put(name, asFile);
            commitBlobs.put(name, asBlob);
            moveFile(".gitlet/stageAdd/",
                    ".gitlet/files/", FILES_FOLDER, name);
        }
        _committee.makeMergeCommit(commitFiles, commitBlobs, message,
                toRemove, currID, givenID);
        _committee.save();
        clearStage();
    }

    /** Unstage the file if it is currently staged for addition. If the
     * file is tracked in the current commit, stage it for removal and
     * remove the file from the working directory if the user has not
     * already done so (do not remove it unless it is tracked in the
     * current commit).
     * @param fileName the name of the file to remove. */
    private void remove(String fileName) {
        List<String> stagedFiles = plainFilenamesIn(".gitlet/stageAdd/");
        if (stagedFiles.contains(fileName)) {
            clearStageOf(fileName);
        }

        List<String> trackedFiles = _committee.trackedFiles();
        if (trackedFiles.contains(fileName)) {

            moveFile(".gitlet/files/",
                    ".gitlet/stageRem/", STAGEREM_FOLDER, fileName);

            List<String> workingFiles = plainFilenamesIn(CWD_PATH);
            if (workingFiles.contains(fileName)) {
                File deleteMe = new File(CWD_PATH + fileName);
                restrictedDelete(deleteMe);
            }
        }

        if (!stagedFiles.contains(fileName)
                && !trackedFiles.contains(fileName)) {
            System.out.println("No reason to remove the file.");
        }

    }

    /** Takes the version of the file as it exists in the head commit,
     * front of the current branch, and puts it in the working directory,
     * overwriting the version of the file that's already there if there
     * is one. The new version of the file is not staged.
     * @param fileName the name of the file to be checked out. */
    private void checkoutName(String fileName) {
        _committee.findFile(fileName);
        moveFile(".gitlet/files", CWD_PATH, CWD, fileName);
    }

    /** Takes the version of the file as it exists in the commit with
     * the given id, and puts it in the working directory, overwriting
     * the version of the file that's already there if there is one.
     * The new version of the file is not staged.
     * @param commitID the ID of the commit whose file version is sought.
     * @param fileName the name of the file whose version is in the commit. */
    private void checkoutID(String commitID, String fileName) {
        byte[] versionBytes = _committee.findFileVersion(fileName, commitID);
        if (versionBytes == null) {
            return;
        }
        reactivate(versionBytes, fileName);
    }

    /** Takes all files in the commit at the head of the given branch,
     * and puts them in the working directory, overwriting the versions
     * of the files that are already there if they exist. Also, at the
     * end of this command, the given branch will now be considered the
     * current branch (HEAD). Any files that are tracked in the current
     * branch but are not present in the checked-out branch are deleted.
     * The staging area is cleared, unless the checked-out branch is the
     * current branch
     * @param branch the name of the branch to checked out. */
    private void checkoutBranch(String branch) {
        if (!_committee.checkout(branch)) {
            return;
        }
        List<String> workingFiles = plainFilenamesIn(CWD_PATH);
        List<String> currFiles = _committee.trackedFiles();
        List<String> givenFiles = _committee.trackedFilesOfBr(branch);

        String givenID = _committee.headOf(branch).sha1();

        if (!checkCheckout(givenID)) {
            return;
        }
        for (String file : currFiles) {
            if (!givenFiles.contains(file)) {
                File deleteMe = new File(CWD_PATH + file);
                restrictedDelete(deleteMe);
            }
        }
        HashMap<String, byte[]> headBytes =
                _committee.headBytesOfBranch(branch);
        for (String fileName: headBytes.keySet()) {
            reactivate(headBytes.get(fileName), fileName);
        }
        _committee.updateBranch(branch);
        _committee.save();
        clearStage();
    }

    /** Creates a new branch which points to the current head commit.
     * @param branch the name of the branch to be created. */
    private void branch(String branch) {
        _committee.makeBranch(branch);
        _committee.save();
    }

    /** Removes the branch pointer but not the commits it points to.
     * @param branch the name of the branch to be removed. */
    private void removeBranch(String branch) {
        _committee.removeBranch(branch);
        _committee.save();
    }

    /** Checks out all the files tracked by the given commit. Removes
     * tracked files that are not present in that commit. Also moves the
     * current branch's head to that commit node. The staging area is
     * cleared. The command is essentially checkout of an arbitrary
     * commit that also changes the current branch head.
     * @param commitID the ID the commit to reset as the head.  */
    private void reset(String commitID) {
        List<String> workingFiles = plainFilenamesIn(CWD_PATH);
        List<String> files = _committee.trackedFilesOfCom(commitID);
        if (files == null) {
            return;
        }
        if (!checkCheckout(commitID)) {
            return;
        }
        for (String file: files) {
            checkoutID(commitID, file);
        }
        for (String file: workingFiles) {
            if (!files.contains(file)) {
                restrictedDelete(CWD_PATH + file);
            }
        }
        _committee.updateHead(commitID);
        _committee.save();
        clearStage();
    }

    /** Essentially creates a new commit in the current branch which
     * contains the merged contents of both the current branch and the
     * given branch. If a conflict between the contents exists, a merge
     * conflict message will be displayed. Ties between "criss-cross"
     * merges which have more than one merge parent distance will be
     * broken by distance, else arbitrarily.
     * @param branch the given branch we wish to merge into the current
     *               branch. */
    private void merge(String branch) {
        List<String> stageAddFiles = plainFilenamesIn(".gitlet/stageAdd/");
        List<String> stageRemFiles = plainFilenamesIn(".gitlet/stageRem/");
        if (stageAddFiles.size() > 0 || stageRemFiles.size() > 0) {
            System.out.println("You have uncommitted changes.");
            return;
        }
        if (!_committee.branches().contains(branch)) {
            System.out.println("A branch with that name does not exist.");
            return;
        }
        if (branch.equals(_committee.currBranchName())) {
            System.out.println("Cannot merge a branch with itself.");
            return;
        }

        List<String> workingFiles = plainFilenamesIn(CWD_PATH);
        List<String> currFiles = _committee.trackedFiles();
        List<String> givenFiles = _committee.trackedFilesOfBr(branch);
        String givenID = _committee.headOf(branch).sha1();
        if (!checkCheckout(givenID)) {
            return;
        }
        Commit splitPoint = _committee.findSplitPoint(
                _committee.currBranchName(), branch);
        if (splitPoint.equals(_committee.headOf(branch))) {
            System.out.println("Given branch is an ancestor "
                    + "of the current branch.");
            return;
        }
        if (splitPoint.equals(_committee.head())) {
            System.out.println("Current branch fast-forwarded.");
            checkoutBranch(branch);
            _committee.save();
            return;
        }
        HashMap<String, File> splitFiles = splitPoint.files();
        HashMap<String, String> allFiles = new HashMap<String, String>();
        for (String file: currFiles) {
            allFiles.put(file, "dummy string");
        }
        for (String file: givenFiles) {
            allFiles.put(file, "dummy string");
        }

        boolean mergeConflict = false;
        for (String file: allFiles.keySet()) {
            if (!mergeHandle(splitFiles, currFiles, givenFiles,
                    splitPoint, branch, file)) {
                mergeConflict = true;
            }
        }
        String currID = _committee.head().sha1();
        if (mergeConflict) {
            System.out.println("Encountered a merge conflict.");
        }
        commit("Merged " + branch + " into "
                + _committee.currBranchName() + ".", currID, givenID);
        _committee.save();
    }

    /** Handles merge conflicts by writing a conflict message into the
     * file which has been modified differently between two branches since
     * their last splitPoint.
     * @param file the name of the file which has caused the merge conflict.
     * @param currVersion the byte array of the version of file in one branch.
     * @param givenVersion the bytes of the version of file in the other. */
    private void handleConflict(String file, byte[] currVersion,
                                byte[] givenVersion) {
        String mergeMessage = "<<<<<<< HEAD\n";

        File tempFile = new File(".gitlet/temp/" + file);
        join(TEMP_FOLDER, ".gitlet/temp/" + file);
        try {
            tempFile.createNewFile();
        } catch (IOException ex) {
            System.out.println("Path leading to file "
                    + "is imcomplete or malformed.");
        }
        if (currVersion != null) {
            writeContents(tempFile, currVersion);
            mergeMessage += readContentsAsString(tempFile);
        } else {
            mergeMessage += "";
        }
        mergeMessage += "=======\n";
        if (givenVersion != null) {
            writeContents(tempFile, givenVersion);
            mergeMessage += readContentsAsString(tempFile);
        } else {
            mergeMessage += "";
        }
        mergeMessage += ">>>>>>>\n";

        writeContents(tempFile, mergeMessage);
        moveFile(".gitlet/temp/", CWD_PATH, CWD, file);
        add(file);
    }

    /** Check if any working files could be overwritten by checking out
     * the given commit. Return true if there are no such problems, and
     * return false if there are.
     * @param commitID the ID of the commit which will be screened to ensure
     *                 that checking it out overwrites no untracked files.
     * @return whether checkout out the commit with this ID will in fact
     * overwrite any untracked files. */
    private boolean checkCheckout(String commitID) {
        List<String> workingFiles = plainFilenamesIn(CWD_PATH);
        List<String> currFiles = _committee.trackedFiles();
        List<String> givenFiles = _committee.trackedFilesOfCom(commitID);
        for (String file : givenFiles) {
            if (workingFiles.contains(file) && currFiles.contains(file)) {
                byte[] workingVersion = readContents(
                        new File(CWD_PATH + file));
                byte[] currVersion = _committee.headBytesOfBranch(
                        _committee.currBranchName()).get(file);
                if (!Arrays.equals(currVersion, workingVersion)) {
                    System.out.println("There is an untracked file in the way;"
                            + " delete it, or add and commit it first.");
                    return false;
                }
            } else if (workingFiles.contains(file)
                    && givenFiles.contains(file)) {
                byte[] workingVersion = readContents(
                        new File(CWD_PATH + file));
                byte[] givenVersion = _committee.findFileVersion(
                        file, commitID);
                if (!Arrays.equals(givenVersion, workingVersion)) {
                    System.out.println("There is an untracked file in the way;"
                            + " delete it, or add and commit it first.");
                    return false;
                }
            }
        }
        return true;
    }

    /** Helper function for the merge method. Evaluates a file in the context
     * of the two branches being merged and of their splitpoint commit,
     * performing the necessary additions, removals, or conflict handling
     * depending on what merge case the file falls under.
     * @param splitFiles the files tracked by the splitpoint commit.
     * @param currFiles the files tracked by the head of the current branch.
     * @param givenFiles the files tracked by the head of the given branch.
     * @param splitPoint the commit representation of the splitpoint.
     * @param branch the name of the given branch.
     * @param file the name of the file to be evaluated.
     * @return whether the file can be merged without conflict. */
    private boolean mergeHandle(HashMap<String, File> splitFiles, List<String>
            currFiles, List<String> givenFiles, Commit splitPoint,
                                String branch, String file) {
        boolean splitHasFile = splitFiles.containsKey(file);
        boolean currHasFile = currFiles.contains(file);
        boolean givenHasFile = givenFiles.contains(file);
        byte[] splitVersion = new byte[]{};
        byte[] currVersion = new byte[]{};
        byte[] givenVersion = new byte[]{};
        boolean currIsModified = false;
        boolean givenIsModified = false;
        boolean currEqGiven = false;
        if (splitHasFile) {
            splitVersion = _committee.findFileVersion(file, splitPoint.sha1());
        }
        if (currHasFile) {
            currVersion = _committee.headBytesOfBranch(
                    _committee.currBranchName()).get(file);
            currIsModified = !Arrays.equals(splitVersion, currVersion);
        }
        if (givenHasFile) {
            givenVersion = _committee.headBytesOfBranch(branch).get(file);
            givenIsModified = !Arrays.equals(splitVersion, givenVersion);
        }
        if (currHasFile && givenHasFile) {
            currEqGiven = Arrays.equals(currVersion, givenVersion);
        }
        if (!splitFiles.containsKey(file)) {
            if (currHasFile && !givenHasFile) {
                return true;
            } else if (!currHasFile && givenHasFile) {
                moveFile(".gitlet/files", CWD_PATH, CWD, file);
                add(file);
                return true;
            } else if (!currEqGiven) {
                handleConflict(file, currVersion, givenVersion);
                return false;
            }
        } else {
            boolean case6 = currIsModified && givenIsModified && !currEqGiven;
            boolean case7 =  currIsModified && currHasFile && !givenHasFile;
            boolean case8 = givenIsModified && !currHasFile && givenHasFile;
            if (case6 || case7 || case8) {
                handleConflict(file, currVersion, givenVersion);
                return false;
            } else if (currHasFile && !givenHasFile && !currIsModified) {
                remove(file);
                return true;
            } else if (!currHasFile && givenHasFile && !givenIsModified) {
                return true;
            } else if (!currIsModified && givenIsModified) {
                moveFile(".gitlet/files", CWD_PATH, CWD, file);
                add(file);
                return true;
            } else if (currIsModified && !givenIsModified) {
                return true;
            } else if (currIsModified && givenIsModified && currEqGiven) {
                return true;
            }
        }
        return true;
    }

    /** Return the Committee I oversee. */
    public Committee committee() {
        return _committee;
    }

    /** The Committee that this Director oversees and has access to. */
    private Committee _committee;

    /** Whether or not a .gitlet directory has already been initialized
     * under this Director's watch. */
    private boolean _gitletPresent;

}
