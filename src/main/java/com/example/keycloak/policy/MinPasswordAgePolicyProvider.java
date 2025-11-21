package com.example.keycloak.policy;

import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.credential.PasswordCredentialModel;
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
        // Heuristic: Check if the request URI starts with "/admin/".
        if (session.getContext() != null && session.getContext().getUri() != null) {
            String path = session.getContext().getUri().getPath();
            if (path != null && path.contains("/admin/")) {
                return null; // Bypass for admin operations
            }
        }

        // 2. Temporary Password / Required Action Bypass
        // If the user is required to update their password (e.g. temporary password set by admin),
        // we must allow them to change it.
        if (user.getRequiredActionsStream().anyMatch(action -> "UPDATE_PASSWORD".equals(action))) {
             return null;
        }

        // 3. Get Configuration (Seconds)
        Integer minAgeSeconds = session.getContext().getRealm().getPasswordPolicy().getPolicyConfig(MinPasswordAgePolicyProviderFactory.ID);
        if (minAgeSeconds == null || minAgeSeconds <= 0) {
            return null; // Policy disabled or invalid config
        }

        // 4. Check existing password age
        return user.credentialManager().getStoredCredentialsByTypeStream(PasswordCredentialModel.TYPE)
                .max(Comparator.comparingLong(org.keycloak.credential.CredentialModel::getCreatedDate))
                .map(credential -> {
                    long created = credential.getCreatedDate();
                    long now = Instant.now().toEpochMilli();
                    long ageMillis = now - created;
                    long minAgeMillis = TimeUnit.SECONDS.toMillis(minAgeSeconds);

                    if (ageMillis < minAgeMillis) {
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
                        return new PolicyError("Password cannot be changed yet. Please wait " + timeWait + ".", "minPasswordAgeNotReached", timeWait);
                    }
                    return null;
                })
                .orElse(null);
    }

    @Override
    public PolicyError validate(String user, String password) {
        return null;
    }

    @Override
    public Object parseConfig(String value) {
        if (value == null || value.trim().isEmpty()) {
            return 0;
        }
        // Format: <number> or <number>:<unit>
        // Units: s (seconds), m (minutes), h (hours), d (days)
        String[] parts = value.split(":");
        int number;
        try {
            number = Integer.parseInt(parts[0]);
        } catch (NumberFormatException e) {
            return 0;
        }

        if (parts.length == 1) {
            return number; // Default to seconds
        }

        String unit = parts[1].toLowerCase();
        switch (unit) {
            case "d": return number * 86400;
            case "h": return number * 3600;
            case "m": return number * 60;
            case "s": return number;
            default: return number; // Default to seconds
        }
    }

    @Override
    public void close() {
    }
}
