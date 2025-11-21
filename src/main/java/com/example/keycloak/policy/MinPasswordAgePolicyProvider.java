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
        // Admin Console and Admin REST API usually use this path.
        // Account Console uses "/realms/{realm}/account/..."
        if (session.getContext() != null && session.getContext().getUri() != null) {
            String path = session.getContext().getUri().getPath();
            if (path != null && path.contains("/admin/")) {
                return null; // Bypass for admin operations
            }
        }

        // 2. Get Configuration
        int minAgeDays = session.getContext().getRealm().getPasswordPolicy().getPolicyConfig(MinPasswordAgePolicyProviderFactory.ID);
        if (minAgeDays <= 0) {
            return null; // Policy disabled or invalid config
        }

        // 3. Check existing password age
        // Find the current password credential
        return user.credentialManager().getStoredCredentialsByTypeStream(PasswordCredentialModel.TYPE)
                .max(Comparator.comparingLong(org.keycloak.credential.CredentialModel::getCreatedDate))
                .map(credential -> {
                    long created = credential.getCreatedDate();
                    long now = Instant.now().toEpochMilli();
                    long ageMillis = now - created;
                    long minAgeMillis = TimeUnit.DAYS.toMillis(minAgeDays);

                    if (ageMillis < minAgeMillis) {
                        long daysLeft = TimeUnit.MILLISECONDS.toDays(minAgeMillis - ageMillis);
                        return new PolicyError("Password cannot be changed yet. Please wait " + (daysLeft + 1) + " day(s).", "minPasswordAgeNotReached", daysLeft + 1);
                    }
                    return null;
                })
                .orElse(null); // No existing password, so no restriction
    }

    @Override
    public PolicyError validate(String user, String password) {
        return null;
    }

    @Override
    public Object parseConfig(String value) {
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    @Override
    public void close() {
    }
}
