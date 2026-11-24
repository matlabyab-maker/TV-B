# Easy Git

An Android GitHub repository manager built with Kotlin and JGit.

## Features in this starter
- Clone a GitHub repository over HTTPS
- Select an Android folder through Storage Access Framework
- Copy a selected folder into the local repository while preserving subfolders
- Commit and push changes
- Pull remote changes
- Show working-tree status and recent commit history
- Export the working tree as a ZIP (excluding `.git`)

## Build
- Android Studio / Gradle
- JDK 17
- Android SDK 35
- Minimum Android 8.0 (API 26)

Open the repository in Android Studio or push it to GitHub and run the included workflow.

## GitHub authentication
Create a fine-grained Personal Access Token with access to the target repository and the required Contents read/write permission. Paste it into the token field. The app uses HTTPS and does not store the token after the process ends.

## Notes / limitations
- This is the first functional foundation, not a full replacement for every MGit feature.
- Clone uses HTTPS. SSH keys, OAuth device login, branch switching, conflict resolution UI, selective staging, and background transfer recovery are not included yet.
- Choose an empty destination folder for Clone.
- Import copies folder contents into the repository root; it does not delete existing repository files.
- Do not import a `.git` folder.
- GitHub file-size limits and repository policies still apply.


## Folder layout
Android source and manifest are kept directly in `app/src/main` to keep the project folder depth shallow (maximum three nested directories from the project root for app source files).
