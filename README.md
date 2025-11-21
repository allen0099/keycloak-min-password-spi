# Keycloak Minimum Password Age SPI

This custom Keycloak SPI (Service Provider Interface) implements a "Minimum Password Age" password policy. It prevents users from changing their password if their current password was set recently, within a configured time limit.

## Features

-   **Minimum Age Restriction**: Blocks password changes if the current password is too new.
-   **Configurable Units**: Supports Seconds, Minutes, Hours, and Days.
-   **Admin Bypass**: Administrators resetting user passwords via the Admin Console are **not** restricted by this policy.
-   **Temporary Password Bypass**: Users forced to update their password (e.g., first login with a temporary password) are **not** restricted.
-   **Robust Parsing**: Handles whitespace and case-insensitivity. Invalid configurations are safely ignored (policy disabled) with a warning log.

## Installation

1.  **Build the JAR**:
    ```bash
    mvn clean package
    ```
    This will generate `target/keycloak-min-password-age-spi-1.0.0-SNAPSHOT.jar`.

2.  **Deploy to Keycloak**:
    -   Copy the JAR file to the `providers/` directory of your Keycloak installation.
    -   Run the Keycloak build command:
        ```bash
        bin/kc.sh build
        ```

3.  **Restart Keycloak**.

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
-   **Invalid Format**: If you enter an invalid format (e.g., `abc`, `10:years`), the policy will be **disabled** for safety, and a warning will be logged in the Keycloak server logs.
-   **Negative Numbers**: Negative values are treated as `0` (disabled).
-   **UI Validation**: Please note that the Keycloak Admin UI may not prevent you from saving invalid text. Check the server logs if the policy does not seem to apply.

## Usage

-   **User**: If a user tries to change their password before the time limit expires, they will see an error message indicating how much time they must wait.
-   **Admin**: Admins can always reset user passwords regardless of this policy.
