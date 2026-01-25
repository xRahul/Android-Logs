# LLM Memory

## Project Context
*   **Type:** Native Android Application (Java).
*   **Build System:** Gradle (Wrapper available).
*   **JDK Version:** 17.
*   **CI/CD:** GitHub Actions.

## Key Commands
*   **Lint:** `./gradlew lint`
*   **Test:** `./gradlew test`
*   **Debug Build:** `./gradlew assembleDebug`
*   **Release Build:** `./gradlew assembleRelease`

## Architecture Decisions
*   **Secrets:** Managed via GitHub Secrets, keystore as Base64.
*   **Signing:** Configured in `app/build.gradle` to read from Environment Variables for release builds.
*   **Versioning:** Automated semantic versioning based on Git Tags.

## Project Structure
*   `app/`: Main application module.
*   `docs/`: Documentation (PRD, TD).
*   `.github/workflows/`: CI/CD definitions.
