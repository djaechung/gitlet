# Gitlet Design Document

**Name**: Daniel Chung

## Classes and Data Structures

###Director
This class takes commands from Main and activates code in downstream 
classes to fulfill the commands it recognizes.
####Fields:
`Committee committee` The directory has access to this committee, 
a class that allows the director to indirectly interact with the commits
saved in the .gitlet directory.

`boolean gitletPresent` Whether a .gitlet directory has been initiated.


###Reporter
This class searches for data stored in Committee and formats it to
be sent back to Main for printing.
####Fields:
`Committee committee` The reporter has access to this committee, which is a 
class that allows the reporter to indirectly interact with the commits
saved in the .gitlet directory.

`List<String> modifiedFiles` A list containing files known to the committee 
that exist in the current head but are of a different version in the CWD or
have been staged for addition.

`List<String> untrackedFiles` A list containing files known to the committee 
that exist in the CWD but are not tracked by any commits.

`SimpleDateFormat GITLET_FORMAT` Used for formatting commit timestamps.

###Committee
This class is privy to information about all commits within the .gitlet
directory and is therefore utilized for branch and checkout operations.
####Fields:
1. `Commit head` The current commit recognized within the .gitlet 
directory.
2. `Commit currBranch` The current branch. This is essentially a pointer
to the head commit of the current branch.
3. `String currBranchName` The name of the current branch.
4. `HashMap<String, Commit> branches` Maps branch names, whether current
or not, to their corresponding head commits. Useful for reference and
searching operations.
5. `HashMap<String, Commit> allCommits` Maps every commit in the 
.gitlet directory by its message attribute, whether in the current 
branch or not. Useful for reference and searching operations.
6. `HashMap<String, Commit> allIDs` Maps every commit in the 
.gitlet directory by its SHA1 ID, whether in the current 
branch or not. Useful for reference and searching operations.


###Commit
This class stores information about a commit, including metadata and blobs.
####Fields:
1. `String sha1-ID` The unique SHA-1 ID for this commit, as determined by 
running the SHA-1 function on its contents.
2. `String Message` The message given to the commit upon its creation.
3. `Commit parent` The commit that precedes this commit. Can be shared by
up to two commits.
4. `Commit mergeParent` The other parent of this commit if this commit was
formed by the mergin of two branches.
5. `Date timeStamp` Includes the date and time that the commit was 
created.
6. `File[] trackedFiles` Contains all files that are being tracked by this
commit only.
7. `Blob[] blobs` The blobs being tracked by this commit.
8. `String[] _mergeIDs` The abbreviated SHA1 IDs of the heads of the two
branches that merged to create the commit.


###Blob
This class stores information about a blob, including metadata and file
contents.
####Fields:
1. `String sha1-ID` The unique SHA-1 ID for this blob, as determined by 
running the SHA-1 function on its contents.
2. `Byte[] contents` The series of bytes that represents the contents of 
the file version encapsulated by the blob.

## Algorithms

###Director Class
`Director()` Initializes a new director object.

`process(string[] args)` Takes the command input from Main and checks to
see if it matches any recognized commands. If  a command is recognized,
it is executed, sending the data flow down to the Committee. If the 
command is not recognized, the director will print an error message.

`init()` Creates the .gitlet directory and creates four new directories
inside of that: the stageAddition directory, the stageRemoval directory,
the commits directory, and the blobs directory. This command also
initializes the one and only Committee object, establishing the initial
commit and master branch.

`add(String fileName)` Searches the working directory for the file
with fileName. If no file is found an error will be thrown. If a file 
is found, the Director will send a copy of it to the stageAdd folder
within the.gitler folder.

`commit(String message)` Initializes a commit by calling Committee's 
makeCommit method. For every file in the stageAddition folder, a blob
will be initialized to capture that version of the file and added to
a list of blobs, and the file itself will be added to a list of files.
These lists will be fed into the initialization of a new commit which
will point to said blobs and "track" said files. The Committee class
will integrate the new commit into its records on its own. The Director,
meanwhile, will remove any files in the stageAddition folder.

`remove(String fileName)` Relays the remove command to Committee, which
will check if the file is currently being tracked. If it is tracked
and Director also sees if the file is also in the stageAddition folder.
If neither are true, it will throw an exception. If, however, the file is
found in the staging area, the Director will remove it from that folder, 
and if the file is found to be tracked, it will be added to the stageRemove
folder so that it will not be tracked when the next commit is made.

`checkoutFile(String fileName)` Relays the fileName to Committee, where 
Committee will search for it in the head commit. If it doesn't exist there,
an error will be printed. If it does exist there, the file will be added
to the working directory by Director. If the different version of the file
already exists there, it will be removed.

`checkoutID(String commitID, String fileName)` Relays the fileName to 
Committee, where Committee will search for it in the commit with ID commitID. 
If no such commit exists, or if the file cannot be found in that commit, 
an error will be printed. If the commit does exist and contains the file 
fileName, the file will be added to the working directory by Director. If
the different version if the file already exists there, it will be removed.

`checkoutBranch(String branchName)` Relays the branchName to Committee, where
Committee will search for it in its branches variable. If no such branch
exists, an error will be printed. If the branch does exist but is Committee's
currbranch, a note will be printed. If the branch does exist and is a 
different branch than Committee's currBranch, first check if any files from
its head commit match files in the working directory. If such matches exist, 
send a message to alert the user to avoid unintentional overwrites. Once this 
check is complete, all files from its head commit will be put into the 
working directory by Director. In addition, Committee will recognize 
branchName as its currBranch by calling updateBranch.

`branch(String branchName)` Relays the branch command to Committee, which will
initiate a new branch whose head is the current head. If there exists a
preexisting branch in Committee branches variable, an error message will be 
printed. If branchName is unique and ready to be added, Committee will 
finish initializing the branch and call its updateBranch 
method to make branchName the current branch. 

`removeBranch(String branchName)` Relays the rm-branch command to 
Committee, which will search for branchName in its branches variable. If no
such branch exists, an error message will be printed. If such a branch does
exist but it is the same branch as currBranch, an error message will be 
printed. Otherwise, the branch is removed by removing it from the branches 
variable in Committee, which deletes the pointer to the branch but not the
commits within the branch itself.

`reset(String commitID)` Sends the commitID to Committee so Committee can
search for it in its commits variable. If the commit with that ID cannot be
found, an error message is printed. If the commit with that ID does exist,
its files are searched and compared to files in the working directory by 
Directory. If matches are found (that is, if there is a risk of overwriting
files in the working directory), a warning message is printed. If these 
conditions are satisfied, all files from that commit are copied into the
working directory by Director and any preexisting files that match them are
removed (overwritten). The stageAddition folder is also cleared by Committee.

`merge(String branchName)` Sends the merge command to Committee, which will
iterate through the current branch and branchName and compare files before
combining metadata to form a new commit which will become the new head of
master, which acts as the head for both branches to eliminate the pointer 
for the old branch that was merged.


###Reporter Class
`Reporter()` Creates a new reporter object.

`process(string[] args)` Takes the command input from Main and checks to
see if it matches any recognized commands. If  a command is recognized,
it is executed, sending the data flow down to the Committee. If the 
command is not recognized, the director will print an error message.

`log()` Relays the log command to Committee, which will use its
method writeLog() to produce a stream of print lines based on the commit
data that the .gitlet directory has access to. This information will be 
formatted and relayed to Main to be printed for the user. 

`globalLog()` Relays the global-log command to Committee, which will return
the desired data. This data will be formatted and sent to Main to be printed.

`find(String commitMessage)` Relays the find command to Committee,
which will use its own find(String commitMessage) method to return a list of
all commit ids whose commits have that same commitMessage. This data will be
formatted and sent to Main to be printed.

`status()` Launches a series of inquiries to Committee to acquire a list of
branches, staged files, and removed files, which it then represents as a list
of strings before formatting it and sending it Main to be printed.


###Committee Class
`Committee()` Creates a new committee object. This command also calls the
committee's makeInitCommit and makeBranch methods to create the inital
commit and set the master branch to that as its head. It will also
call the Committee's updateHead method to point the head to that
initial commit.

`writeLog()` Assembles a list of arrays containing information on every
commit in the given branch in chronological order.

`writeGlobalLog()` Does the same task as writeLog() but with every commit
in no particular order.

`findByMessage(String commitMessage)` Searches the map of commit messages and 
returns a list of commit IDs whose commits have the same message as
commitMessage. It will return an empty list if it finds no such commits.

`findFile(String fileName)` Searches for the given file in the current head
commit and returns it if it exists.

`hasFile(String fileName)` Returns whether a file with the given name exists
and it tracked by the current head commit.

`findFileVersion(String fileName, String sha1)` Returns a byte array
representing the contents of a specific version of a file stored in a
particular commit, if it exists.

`findFileVersions(String sha1)` Does the same task as findFileVersion() but
returns a mapping of all filenames in the given commit to byte arrays
representing their versions within that commit.

`checkout(String branch)` Returns whether the branch is ok to check out.
That is, checks if the branch exists or if it is the current branch.

`headBytesOfbranch(String branch)` Essentially calls findFileVersions on
the head commit of the given branch.

`trackedFiles()` Returns a list of filenames for all files tracked by the
current head commit.

`trackedFilesOfBr(String branch)` Performs the task of trackedFiles() on the 
head of the given branch.

`trackedFilesOfCom(String sha1)` Performs the task of trackedFiles() on the 
commit with the given SHA1 ID.

`findSplitPoint(String currBranch, String givenBranch` A graph traversal
algorithm that finds the common parent commit between the heads of the
two given branches that is the least number of pointers away from the 
current head. It does so by first finding the total path of the given
branch and then performing breadth first traversal of the current branch
until it hits a commit in the total path of the given branch, which is
necessarily the closest splitpoint to the current branch. 

`totalPathOf(Commit head, List<Commit> path` Helper method for findSplitPoint
that finds the total path from the given commit to the inital commit,
including branching from criss-cross merges and merge parents.

`seekFrom(Commit head, List<Commit> path)` Helper method for findSplitPoint
that uses a breadth-first graph traversal to find the closest splitpoint
to the current head.

`updateHead(Commit newHead)` Changes the head instance variable such that it
points to the commit newHead. This method will call the updateBranch method as
well if the branch of newHead is not the same as the branch of the old head.

`updateBranch(Commit[] newBranch)` Calls the updateHead instance variable such 
that it points to the latest commit in branch newBranch. Then it will also 
update the currBranch instance variable to reflect this change. 

`makeCommit(HashMap<String, File> trackedFiles, HashMap<String,
Blob> blobs, String message, List<String> toRemove)` Initializes a new commit 
and performs external tasks like assigning the new commit's parent to the 
previous head commit as well as updating the head. Takes care to keep files
from the parent commit in the new commit's tracked files and to remove files
tracked by the commit if they are listed as being staged for removal.

`makeMergeCommit(HashMap<String, File> trackedFiles, HashMap<String,
Blob> blobs, String message, List<String> toRemove, String currID, 
String givenID)` Initializes a new commit using the special mergeCommit()
constructor and performs external tasks like assigning the new commit's parent
and merge parent and merge parent IDs as well as updating the head. Otherwise 
performs the same tasks as the makeCommit() command.

`makeInitCommit()` Initializes a new commit using the special initCommit
constructor within the commit class.

`removeBranch(String branch)` Removes the branch from the HashMap of branches.

`makeBranch(String name)` Adds the new branch name as a key to the map of
branches, having the value be the current head commit. Also updates
currBranch to be branch name.

`fromFile(File file)` Reads a committee from file and deserializes it to
return the committee object.

`save()` Serializes the committee into a new file.


###Commit Class
`Commit(HashMap<String, File> files, HashMap<String, Blob> blobs,
String message)` Creates a new commit by generating a SHA-1 ID based 
on its metadata and blob contents. File and blob contents will be filled 
based on which ones were fed into the constructor. Metadata such as time 
and date of initialization and the message with which the commit was 
initialized will also be stored as instance variables.

`Commit(HashMap<String, File> files, HashMap<String, Blob> blobs,
String message, String currBranchID, String givenBranchID)` Creates a new
commit the same way the normal constructor does, but also fills in
additional instance variables like merge parent and merge parent IDs.

`Commit()` Special constructor for an initial commit with "initial commit"
message and unix epoch timestamp.

`bytesFromBlob(String fileName)` Returns a byte array representing a blob
for the file with the given filename, which is a distinct version of the
file stored within the commit. 

`fromFile(File file)` Reads a commit from file and deserializes it to
return the commit object.

`saveBlob()` Serializes the commit into a new file.


###Blob Class
`Blob(File file)` Creates a new blob by generating a SHA-1 ID based on the
contents of file. The file itself is also saved as an instance variable within
the new blob.

`fromFile(File file)` Reads a blob from file and deserializes it to
return the blob object.

`saveBlob()` Serializes the blob into a new file.


## Persistence
1. The init method of the Director class will create a .gitlet folder inside
the working directory, which will serve as a space in which the files tracked
by Gitlet can persist. Inside .gitlet will be a stageAddition folder, which
will allow files staged for addition to persist, a stageRemoval folder, which
will allow file staged for removal to persist, a commits folder, which will
allow commits serialized in file form to persist, and a blobs folder, which 
will allow blobs serialized in file form to persist. A files folder will store
versions of files currently being tracked by heads of all branches, and a temp
folder will be used to perform file operations without overwriting files
elsewhere. The committee will be saved as its own file inside the commits folder.

2. As stated previously, Committees, Commits, and Blobs can be written to files
and read from files. This behavior will be made possible by having these
classes implement the Serializable interface, and each of these three classes
will have a method to serialize their contents into bytes which will be stored
in files with appropriate names.
`Committee` will have the special file name "Committee",
`Commits` Will have a file name that matches their message, and
`Blobs` will have a file name that matches the name of the file whose contents
they contain along with a number which will be incremented with every modified
version of the file to keep track of versions. Collectively, this allows the
program to find these files efficiently before de-serializing them for work.


