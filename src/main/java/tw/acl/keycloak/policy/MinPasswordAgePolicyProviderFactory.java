package tw.acl.keycloak.policy;

import org.keycloak.Config;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.policy.PasswordPolicyProvider;
import org.keycloak.policy.PasswordPolicyProviderFactory;

public class MinPasswordAgePolicyProviderFactory implements PasswordPolicyProviderFactory {

    public static final String ID = "minimumPasswordAge";
    private static final String DEFAULT_CONFIG_VALUE = "0";

    @Override
    public String getId() {
        return ID;
    }

    @Override
    public PasswordPolicyProvider create(KeycloakSession session) {
        return new MinPasswordAgePolicyProvider(session);
    }

    @Override
    public void init(Config.Scope config) {
        // No initialization needed
    }

    @Override
    public void postInit(KeycloakSessionFactory factory) {
        // No post-initialization needed
    }

    @Override
    public void close() {
        // No close logic needed
    }

    @Override
    public String getDisplayName() {
        return "Minimum Password Age";
    }

    @Override
    public String getConfigType() {
        return PasswordPolicyProvider.STRING_CONFIG_TYPE;
    }

    @Override
    public String getDefaultConfigValue() {
        return DEFAULT_CONFIG_VALUE;
    }

    @Override
    public boolean isMultiplSupported() {
        return false;
    }
}
