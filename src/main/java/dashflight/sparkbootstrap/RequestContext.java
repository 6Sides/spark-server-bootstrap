package dashflight.sparkbootstrap;

import core.directives.auth.PolicyCheck;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

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


    /**
     * Returning null means user has permission.
     * @param permission
     * @return
     */
    @Override
    public Object hasPermission(Integer permission) {
        if (this.userId == null) {
            return new UnauthenticatedErrorResponse();
        } else if (permission == null) {
            return null;
        }


        if (policyCache.check(this.userId, permission)) {
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
