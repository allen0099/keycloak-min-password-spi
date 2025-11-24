package tw.acl.keycloak.policy;

import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.credential.PasswordCredentialModel;
import org.keycloak.policy.PasswordPolicyConfigException;
import org.keycloak.policy.PasswordPolicyProvider;
import org.keycloak.policy.PolicyError;

import java.time.Instant;
import java.util.Comparator;
import java.util.concurrent.TimeUnit;

public class MinPasswordAgePolicyProvider implements PasswordPolicyProvider {
    private final KeycloakSession session;

    public MinPasswordAgePolicyProvider(KeycloakSession session) {
        this.session = session;
    }

    @Override
    public PolicyError validate(RealmModel realm, UserModel user, String password) {
        // 1. Admin Bypass Logic
        if (session.getContext() != null && session.getContext().getUri() != null) {
            String path = session.getContext().getUri().getPath();
            if (path != null && path.contains("/admin/")) {
                return null; // Bypass for admin operations
            }
        }

        // 2. Temporary Password / Required Action Bypass
        if (user.getRequiredActionsStream().anyMatch("UPDATE_PASSWORD"::equals)) {
            return null;
        }

        // 3. Get Configuration (Seconds as Long)
        // Note: getPolicyConfig returns the object returned by parseConfig
        Object config = session.getContext().getRealm().getPasswordPolicy()
                .getPolicyConfig(MinPasswordAgePolicyProviderFactory.ID);
        long minAgeSeconds = 0;
        if (config instanceof Long) {
            minAgeSeconds = (Long) config;
        } else if (config instanceof Integer) {
            minAgeSeconds = ((Integer) config).longValue();
        } else if (config instanceof String) {
            // Fallback if for some reason it wasn't parsed or is raw string
            minAgeSeconds = parseConfig((String) config);
        }

        if (minAgeSeconds <= 0) {
            return null; // Policy disabled or invalid config
        }

        // 4. Check existing password age
        long finalMinAgeSeconds = minAgeSeconds;
        return user.credentialManager().getStoredCredentialsByTypeStream(PasswordCredentialModel.TYPE)
                .max(Comparator.comparingLong(org.keycloak.credential.CredentialModel::getCreatedDate))
                .map(credential -> {
                    long created = credential.getCreatedDate();
                    long now = Instant.now().toEpochMilli();
                    long ageMillis = now - created;
                    long minAgeMillis = TimeUnit.SECONDS.toMillis(finalMinAgeSeconds);

                    if (ageMillis < minAgeMillis) {
                        String timeWait = getTimeString(minAgeMillis, ageMillis);
                        return new PolicyError("Password cannot be changed yet. Please wait " + timeWait + ".",
                                "minPasswordAgeNotReached", timeWait);
                    }
                    return null;
                })
                .orElse(null);
    }

    private static String getTimeString(long minAgeMillis, long ageMillis) {
        long secondsLeft = TimeUnit.MILLISECONDS.toSeconds(minAgeMillis - ageMillis);
        String timeWait;
        if (secondsLeft > 86400) {
            timeWait = (secondsLeft / 86400) + " day(s)";
        } else if (secondsLeft > 3600) {
            timeWait = (secondsLeft / 3600) + " hour(s)";
        } else if (secondsLeft > 60) {
            timeWait = (secondsLeft / 60) + " minute(s)";
        } else {
            timeWait = secondsLeft + " second(s)";
        }
        return timeWait;
    }

    @Override
    public PolicyError validate(String user, String password) {
        return null;
    }

    @Override
    public Long parseConfig(String value) {
        if (value == null || value.trim().isEmpty()) {
            return 0L;
        }

        String normalized = value.trim().toLowerCase();

        // Identify unit
        long multiplier = 1;
        String numberPart = normalized;

        if (normalized.contains(":")) {
            String[] parts = normalized.split(":");
            if (parts.length == 2) {
                numberPart = parts[0].trim();
                String unit = parts[1].trim();
                multiplier = switch (unit) {
                    case "d" -> 86400;
                    case "h" -> 3600;
                    case "m" -> 60;
                    case "s" -> 1;
                    default ->
                        throw new PasswordPolicyConfigException("Invalid unit in Minimum Password Age policy: " + unit);
                };
            } else {
                throw new PasswordPolicyConfigException("Invalid format. Expected 'value:unit'.");
            }
        }

        try {
            long number = Long.parseLong(numberPart);
            if (number < 0) {
                throw new PasswordPolicyConfigException(
                        "Negative value is not allowed in Minimum Password Age policy.");
            }
            return number * multiplier;
        } catch (NumberFormatException e) {
            throw new PasswordPolicyConfigException(
                    "Invalid format. '" + numberPart + "' in Minimum Password Age policy.");
        }
    }

    @Override
    public void close() {
    }
}
