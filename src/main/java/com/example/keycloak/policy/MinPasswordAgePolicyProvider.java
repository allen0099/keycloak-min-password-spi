package com.example.keycloak.policy;

import org.jboss.logging.Logger;
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

    private static final Logger logger = Logger.getLogger(MinPasswordAgePolicyProvider.class);
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
        if (user.getRequiredActionsStream().anyMatch(action -> "UPDATE_PASSWORD".equals(action))) {
             return null;
        }

        // 3. Get Configuration (Seconds as Long)
        // Note: getPolicyConfig returns the object returned by parseConfig
        Object config = session.getContext().getRealm().getPasswordPolicy().getPolicyConfig(MinPasswordAgePolicyProviderFactory.ID);
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
                switch (unit) {
                    case "d": multiplier = 86400; break;
                    case "h": multiplier = 3600; break;
                    case "m": multiplier = 60; break;
                    case "s": multiplier = 1; break;
                    default: 
                        logger.warnv("Invalid unit ''{0}'' in Minimum Password Age policy. Defaulting to seconds.", unit);
                        multiplier = 1; 
                }
            } else {
                 logger.warnv("Invalid format ''{0}'' in Minimum Password Age policy. Expected 'value:unit'. Defaulting to seconds.", value);
            }
        }

        try {
            long number = Long.parseLong(numberPart);
            if (number < 0) {
                logger.warnv("Negative value ''{0}'' in Minimum Password Age policy. Treating as 0 (disabled).", number);
                return 0L;
            }
            return number * multiplier;
        } catch (NumberFormatException e) {
            logger.warnv("Invalid number ''{0}'' in Minimum Password Age policy. Treating as 0 (disabled).", numberPart);
            return 0L;
        }
    }

    @Override
    public void close() {
    }
}
