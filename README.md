`mvn-repo-cleaner` - Maven Repository Cleaner is a utility to clean up the local 
`.m2` directory on a developer box.

As newer versions of dependent libraries appear, the old ones become obsolete 
and never get cleaned up. 
This program assists to free up some disk space by removing older versions of 
libraries downloaded to the local Maven repository.

This can be used just by extracting `mvn-repo-cleaner.tar.gz` / `mvn-repo-cleaner.zip` 
and executing the `execute.sh` / `execute.bat` as per the environment 
to clean up all non-latest versions of the libraries in the `<user_dir>/.m2/repository` directory.

If the local repository is in a different location, use the `--path` flag to specify its location.

To attempt a `dry-run`, just execute the `simulate.sh` / `simulate.bat` 
to see the files that would be removed without actually deleting the files.
This output is also generated in the `mvn-repo-cleaner.log`.

To run with more advanced options, run the `execute` script with the flags below.

`Usage: java -jar mvn-repo-cleaner.jar [options]`

  **Options:**
  
    --accessedBefore, -ab
      Delete all libraries (even if latest version) last accessed on or before 
      this date (MM-DD-YYYY).
      Default: 0
    --accessedAfter, -aa
      Delete all libraries (even if latest version) last accessed on or after 
      this date (MM-DD-YYYY).
    --deleteAllSnapshots, -dsn
      Delete all snapshots irrespective of being latest.
      Default: false
    --deleteJavadoc, -djd
      Delete javadocs for all libraries.
      Default: false
    --deleteSource, -dsr
      Delete sources for all libraries.
      Default: false
    --downloadedBefore, -db
      Delete all libraries (even if latest version) downloaded on or before 
      this date (MM-DD-YYYY).
      Default: 0
    --downloadedAfter, -da
      Delete all libraries (even if latest version) downloaded on or after 
      this date (MM-DD-YYYY).
    --dryrun, -dr
      Do not delete files, just simulate and print result.
      Default: false
    --forceArtifacts, -fa
      Comma separated list of groupId:artifactId combination to be deleted.
    --forceGroups, -fg
      Comma separated list of groupIds (full or part) to be deleted.
    --ignoreArtifacts, -ia
      Comma separated list of groupId:artifactId combination to be ignored.
    --ignoreGroups, -ig
      Comma separated list of groupIds (full or part) to be ignored.
    --path, -p
      Path to local Maven repository if not in <user_dir>/.m2/repository.
    --retainOld, -ro
      Retain the artifacts even if old versions. Only process the configured inputs.
      Default: false
      

Feel free to raise any issues or recommend any changes or improvements.
