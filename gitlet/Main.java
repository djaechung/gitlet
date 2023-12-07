package gitlet;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;

/** Driver class for Gitlet, the tiny stupid (awesome!) version-control system.
 *  @author Daniel Chung
 */
public class Main {

    /** Constructor for main object.
     * Not of much use for this program. */
    Main() {
    }

    /** Usage: java gitlet.Main ARGS, where ARGS contains
     *  <COMMAND> <OPERAND> .... */
    public static void main(String... args) {
        if (args == null || args.length == 0) {
            System.out.println("Please enter a command.");
            System.exit(0);
            return;
        } else if (args.length < 1 || args.length > 4) {
            System.out.println("Incorrect operands.");
            System.exit(0);
            return;
        }
        String command = args[0];
        if (!_gitletPresent && !command.equals("init")) {
            System.out.println("Not in an initialized Gitlet directory.");
            System.exit(0);
            return;
        }
        if (DIRECTIVES.contains(command)) {
            if (command.equals("init") && !_gitletPresent) {
                _gitletPresent = true;
            } else if (command.equals("init") && _gitletPresent) {
                System.out.println("A Gitlet version-control"
                       + " system already exists in the current directory.");
                return;
            }
            _director.process(args);
        } else if (REPORTIVES.contains(command)) {
            _reporter.process(args);
        } else {
            System.out.println("No command with that name exists.");
        }
        System.exit(0);
    }


    /** All recognized commands that the director is responsible
     * for handling. */
    static final ArrayList<String> DIRECTIVES = new ArrayList<String>(
            Arrays.asList("init", "add", "commit", "rm", "checkout",
                    "branch", "rm-branch", "reset", "merge"));

    /** All recognized commands that the reporter is responsible
     * for handling. */
    static final ArrayList<String> REPORTIVES = new ArrayList<String>(
            Arrays.asList("log", "global-log", "find", "status"));


    /** Main metadata folder. */
    static final File GITLET_FOLDER = new File(".gitlet/");

    /** Contains all files that have been staged, added and committed. */
    static final File FILES_FOLDER = new File(".gitlet/files/");

    /** Works as a temporary area to initialize different versions of files
     * than what exists in other folders. */
    static final File TEMP_FOLDER = new File(".gitlet/temp/");

    /** Staging area addition folder. */
    static final File STAGEADD_FOLDER = new File(".gitlet/stageAdd/");

    /** Staging area removal folder. */
    static final File STAGEREM_FOLDER = new File(".gitlet/stageRem/");

    /** Commits folder. */
    static final File COMMITS_FOLDER = new File(".gitlet/commits/");

    /** Blobs folder. */
    static final File BLOBS_FOLDER = new File(".gitlet/blobs/");

    /** The Director object that Main oversees. */
    private static Director _director = new Director();

    /** The Reporter object that Main oversees. */
    private static Reporter _reporter = new Reporter();

    /** Whether or not a .gitlet directory has already been initialized
     * under this Main class. */
    private static boolean _gitletPresent = GITLET_FOLDER.exists();

    /** The current working directory (CWD) path as a string repr. */
    static final String CWD_PATH = System.getProperty("user.dir") + "/";

    /** The current working directory (CWD) as a folder. */
    static final File CWD = new File(System.getProperty("user.dir") + "/");

}
