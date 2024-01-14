Usage:

`java -jar /path/to/generated/maven/artifact /path/to/patch/files /path to /source/directories`

We are not responsible, if this Project breaks something.
This is very much a hacked together project, made to help with simplification of Ketting Patch files.
If you have a use for this, go ahead use or fork it (just make sure to respect the license).

This project is specialized for java patch files.
- It will apply any import changes. No deleted or added import Lines should survive, after using this.
- It will delete any added Whitespace lines, if the previous and next line has not been changed.
- It will revert any deleted whitespace lines under the same conditions
- It will also recalculate the hunk sizes. It won't check the line numbers in the hunk head.

Known Bugs:
- static imports are handled incorrectly. This has not been worth any investigation/fixing, because it always affected a small subset of patches (usally single digit) in our repository.
