# Keycloak Minimum Password Age SPI

This custom Keycloak SPI (Service Provider Interface) implements a "Minimum Password Age" password policy. It prevents users from changing their password if their current password was set recently, within a configured time limit.

## Features

-   **Minimum Age Restriction**: Blocks password changes if the current password is too new.
-   **Configurable Units**: Supports Seconds, Minutes, Hours, and Days.
-   **Admin Bypass**: Administrators resetting user passwords via the Admin Console are **not** restricted by this policy.
-   **Temporary Password Bypass**: Users forced to update their password (e.g., first login with a temporary password) are **not** restricted.
-   **Robust Parsing**: Handles whitespace and case-insensitivity. Invalid configurations are safely ignored (policy disabled) with a warning log.

## Compatibility

-   **Keycloak Versions**: Tested on Keycloak 24.0.1 and 26.4.5. Should work with Keycloak 24 - 26.

## Installation

### Option 1: Download (Recommended)

1.  Download the latest JAR file from the [Releases Page](https://github.com/allen0099/keycloak-min-password-spi/releases).
2.  **Deploy to Keycloak**:
    -   Copy the JAR file to the `providers/` directory of your Keycloak installation.
    -   Run the Keycloak build command:
        ```bash
        bin/kc.sh build
        ```
3.  **Restart Keycloak**.

### Option 2: Build from Source

1.  **Build the JAR**:
    ```bash
    mvn clean package
    ```
    This will generate `target/keycloak-min-password-age-spi-1.0.0-SNAPSHOT.jar`.
2.  Follow the "Deploy to Keycloak" steps above.

## Configuration

1.  Log in to the Keycloak Admin Console.
2.  Navigate to **Authentication** -> **Password Policy**.
3.  Click **Add Policy** (or select the policy from the list in newer versions).
4.  Select **Minimum Password Age (Seconds/Time)**.
5.  Enter the time limit. You can use the following formats:
    -   **Seconds**: Just a number (e.g., `60` for 60 seconds).
    -   **Time Units**: Number followed by unit (e.g., `1:d` for 1 day, `30:m` for 30 minutes, `12:h` for 12 hours).
6.  Click **Save**.

### Validation Notes
-   **UI Validation**: The Keycloak Admin UI will validate the format. If you enter an invalid format (e.g., `abc`, `10:years`), the UI will show an error and prevent you from saving the policy.
-   **Negative Numbers**: Negative values are not allowed and will also be blocked by the UI.

## Usage

-   **User**: If a user tries to change their password before the time limit expires, they will see an error message indicating how much time they must wait.
-   **Admin**: Admins can always reset user passwords regardless of this policy.

## Release Process

This project uses GitHub Actions to automatically release artifacts.

1.  Update the version in `pom.xml` (e.g., `1.0.0`).
2.  Commit the change.
3.  Create a tag starting with `v` (e.g., `v1.0.0`).
4.  Push the tag to GitHub.

The workflow will automatically build the JAR and create a GitHub Release with the artifact attached.
