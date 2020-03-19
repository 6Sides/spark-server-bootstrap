package dashflight.sparkbootstrap;

import core.directives.auth.PolicyCheck;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.dashflight.data.postgres.PostgresFactory;

/*
TODO: Remove any logic / database queries from class. Should be immutable POJO.
        Will require refactor of GraphQL annotation library auth directive.
 */
public class RequestContext implements PolicyCheck {

    private final UUID userId;
    private final Organization organization;
    private final Location homeLocation;
    private final List<Location> locations;

    private PolicyCache policyCache = new PolicyCache();
    

    public RequestContext(UUID userId, Organization organization, Location homeLocation, List<Location> locations) {
        this.userId = userId;
        this.organization = organization;
        this.homeLocation = homeLocation;
        this.locations = locations;
    }


    private static Map<String, Integer> policyIdMap = new ConcurrentHashMap<>();

    /**
     * Returning null means user has permission.
     * @param policy
     * @return The response body if the user is not authorized, null otherwise.
     */
    @Override
    public Object hasPermission(String policy) {
        if (this.userId == null) {
            return new UnauthenticatedErrorResponse();
        } else if (policy == null) {
            return null;
        }

        policyIdMap.computeIfAbsent(policy, p -> {
            try (Connection conn = PostgresFactory.withDefaults().getConnection()) {
                PreparedStatement stmt = conn.prepareStatement("select id from accounts.policies where prefix = ? and name = ?");

                String[] parts = policy.split(":");
                stmt.setString(1, parts[0]);
                stmt.setString(2, parts[1]);

                ResultSet res = stmt.executeQuery();
                if (res.next()) {
                    return res.getInt("id");
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }

            return null;
        });


        if (policyCache.check(this.userId, policyIdMap.get(policy))) {
            return null;
        }

        return new UnauthorizedErrorResponse();
    }

    public UUID getUserId() {
        return this.userId;
    }

    public Organization getOrganization() {
        return this.organization;
    }

    public Location getHomeLocation() {
        return this.homeLocation;
    }

    public List<Location> getLocations() {
        return Collections.unmodifiableList(this.locations);
    }

    public int[] getLocationIds() {
        int[] result = new int[this.locations.size()];
        for (int i = 0; i < result.length; i++) {
            result[i] = this.locations.get(i).getId();
        }

        return result;
    }

    private static class UnauthenticatedErrorResponse {
        private static String type = "Authentication Error";
        private static String title = "You are not authenticated";
        private static String message = "Either your session has expired or you have authenticated improperly. Try logging in again at https://www.dashflight.net/login";
    }

    private static class UnauthorizedErrorResponse {
        private static String type = "Authorization Error";
        private static String title = "You are not authorized to access this resource";
        private static String message = "Your account does not have access to this resource. Contact your system administrator if you believe this is a mistake.";
    }
}
