package gitlet;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Collections;
import java.util.HashMap;
import java.util.Arrays;

import static gitlet.Main.COMMITS_FOLDER;
import static gitlet.Main.CWD_PATH;
import static gitlet.Utils.plainFilenamesIn;
import static gitlet.Utils.readContents;

/** Reporter class with authorization to query information from
 * the Committee class and the ability to format it into printable
 * messages. Handles all printing commands.
 * @author Daniel Chung
 */
public class Reporter {

    /** Create a new Reporter object. */
    Reporter() {
        File committeeFile = new File(COMMITS_FOLDER + "/" + "committee");
        if (!committeeFile.exists()) {
            return;
        }
        _committee = Utils.readObject(committeeFile, Committee.class);
    }

    /** Takes in a line of input, identifies which command to
     * execute, and executes the command if it exists, WITHOUT
     * changing the state of files in the .gitlet or working directory.
     * @param args the input specifying a gitlet command and qualifier.
     */
    public void process(String[] args) {
        String command = args[0];
        File committeeFile = new File(COMMITS_FOLDER + "/" + "committee");
        if (!committeeFile.exists()) {
            System.out.println("Not in an initialized Gitlet directory.");
        }
        _committee = Utils.readObject(committeeFile, Committee.class);
        switch (command) {
        case "log":
            preLog(args);
            break;
        case "global-log":
            preGlobalLog(args);
            break;
        case "find":
            preFind(args);
            break;
        case "status":
            preStatus(args);
            break;
        default:
            System.out.println("No command with that name exists.");
            break;
        }
    }

    /** Sanitize input for log command.
     * @param args the command to be evaluated. */
    private void preLog(String...args) {
        if (args.length == 1) {
            log(false);
        } else {
            System.out.println("Incorrect operands");
        }
    }

    /** Sanitize input for global-log command.
     * @param args the command to be evaluated. */
    private void preGlobalLog(String...args) {
        if (args.length == 1) {
            log(true);
        } else {
            System.out.println("Incorrect operands");
        }
    }

    /** Sanitize input for preFind command.
     * @param args the command to be evaluated. */
    private void preFind(String...args) {
        if (args.length == 2) {
            find(args[1]);
        } else {
            System.out.println("Incorrect operands");
        }
    }

    /** Sanitize input for preStatus command.
     * @param args the command to be evaluated. */
    private void preStatus(String...args) {
        if (args.length == 1) {
            status();
        } else {
            System.out.println("Incorrect operands");
        }
    }

    /** Outputs a log of all commits in the current branch, starting
     * from the head commit and ending at the initial commit. OR outputs
     * a log of all commits every created in this repository. Logged
     * information includes SHA1 ID, timestamp, and commit message.
     * @param global whether the log should be global or per branch. */
    private void log(boolean global) {
        ArrayList<String[]> log = new ArrayList<String[]>();
        if (global) {
            log = _committee.writeGlobalLog();
        } else {
            log = _committee.writeLog();
        }
        for (String[] commit: log) {
            System.out.println("===");
            System.out.println("commit " + commit[0]);
            if (commit[1] != null) {
                System.out.println("Merge: " + commit[1]);
            }
            System.out.println("Date: " + commit[2]);
            System.out.println(commit[3] + "\n");
        }
    }

    /** Prints out the ids of all commits that have the given commit
     * message, one per line. If there are multiple such commits, it
     * prints the ids out on separate lines.
     * @param message the message by which to find commits. */
    private void find(String message) {
        ArrayList<String> results = _committee.findByMessage(message);
        if (results == null) {
            return;
        }
        for (String result: results) {
            System.out.println(result);
        }
    }

    /** Displays what branches currently exist, and marks the current
     * branch with a *. Also displays what files have been staged for
     * addition or removal and which files are modified or untracked. */
    private void status() {

        System.out.println("=== Branches ===");

        List<String> branches = _committee.branches();
        Collections.sort(branches);
        for (String branch: branches) {
            if (branch.equals(_committee.currBranchName())) {
                branch = "*" + branch;
            }
            System.out.println(branch);
        }
        System.out.println();

        System.out.println("=== Staged Files ===");

        List<String> addFiles = plainFilenamesIn(".gitlet/stageAdd/");
        for (String addFile: addFiles) {
            System.out.println(addFile);
        }
        System.out.println();

        System.out.println("=== Removed Files ===");

        List<String> remFiles = plainFilenamesIn(".gitlet/stageRem/");
        for (String remFile: remFiles) {
            System.out.println(remFile);
        }
        System.out.println();

        findModOrUnTracked();

        System.out.println("=== Modifications Not Staged For Commit ===");

        for (String file: _modifiedFiles) {
            System.out.println(file);
        }
        System.out.println();

        System.out.println("=== Untracked Files ===");

        for (String file: _untrackedFiles) {
            System.out.println(file);
        }
        System.out.println();
    }

    /** Record the modified and untracked files that I am aware of
     * so that I can report them in my status. */
    private void findModOrUnTracked() {
        List<String> workingFiles = plainFilenamesIn(CWD_PATH);
        List<String> addFiles = plainFilenamesIn(".gitlet/stageAdd/");
        List<String> remFiles = plainFilenamesIn(".gitlet/stageRem/");
        List<String> currFiles = _committee.trackedFiles();
        HashMap<String, String> allFiles = new HashMap<String, String>();
        for (String file: workingFiles) {
            allFiles.put(file, "dummy string");
        }
        for (String file: addFiles) {
            allFiles.put(file, "dummy string");
        }
        for (String file: remFiles) {
            allFiles.put(file, "dummy string");
        }
        for (String file: currFiles) {
            allFiles.put(file, "dummy string");
        }

        for (String file: allFiles.keySet()) {
            boolean trackedInCurr = false;
            boolean inCWD = false;
            boolean stagedAdd = false;
            boolean stagedRem = false;
            byte[] currVersion = new byte[]{};
            byte[] workingVersion = new byte[]{};
            boolean changedInCWD = false;
            if (_committee.trackedFiles().contains(file)) {
                trackedInCurr = true;
                currVersion = _committee.headBytesOfBranch(
                        _committee.currBranchName()).get(file);
            }
            if (workingFiles.contains(file)) {
                inCWD = true;
                workingVersion = readContents(new File(CWD_PATH + file));
            }
            if (trackedInCurr && inCWD) {
                changedInCWD = !Arrays.equals(currVersion, workingVersion);
            }
            if (addFiles.contains(file)) {
                stagedAdd = true;
            }
            if (remFiles.contains(file)) {
                stagedRem = true;
            }
            boolean case1 = trackedInCurr && changedInCWD && !stagedAdd
                    && !stagedRem;
            boolean case2 = stagedAdd && changedInCWD;
            boolean case3 = stagedAdd && !inCWD;
            boolean case4 = !stagedRem && trackedInCurr && !inCWD;
            boolean case5 = inCWD && !stagedAdd && !stagedRem && !trackedInCurr;
            if (case1 || case2 || case3) {
                _modifiedFiles.add(file + " (modified)");
            } else if (case4) {
                _modifiedFiles.add(file + " (deleted)");
            } else if (case5) {
                _untrackedFiles.add(file);
            }
        }
    }

    /** The modified files not staged for commit that I am aware of. */
    private List<String> _modifiedFiles = new ArrayList<String>();

    /** The untracked files I am aware of. */
    private List<String> _untrackedFiles = new ArrayList<String>();

    /** The Committee I report on. */
    private Committee _committee;

    /** Correct gitlet format for a commit date. */
    static final SimpleDateFormat GITLET_FORMAT =
            new SimpleDateFormat("EEE MMM d HH:mm:ss yyyy Z");

}
