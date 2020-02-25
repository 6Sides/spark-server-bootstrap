package dashflight.sparkbootstrap;

import core.directives.auth.PermissionCheck;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Collections;
import java.util.List;
import net.dashflight.data.postgres.PostgresConnectionPool;
import org.postgresql.util.PGobject;

/*
TODO: Remove any logic / database queries from class. Should be immutable POJO.
        Will require refactor of GraphQL annotation library auth directive.
 */
public class RequestContext implements PermissionCheck {

    private final String userId;
    private final Organization organization;
    private final Location homeLocation;
    private final List<Location> locations;

    public RequestContext(String userId, Organization organization, Location homeLocation, List<Location> locations) {
        this.userId = userId;
        this.organization = organization;
        this.homeLocation = homeLocation;
        this.locations = locations;
    }

    private static final String hasRoleSQL =
            "select description from accounts.user_permissions "
                    + "inner join accounts.permissions "
                    + "on user_permissions.permission_id = permissions.id "
                    + "where user_permissions.user_id = ? and "
                    + "(permissions.prefix = ? and ("
                    + "permissions.name = ? or permissions.name = 'all'))";

    /**
     * Returning null means user has permission.
     * @param permission
     * @return
     */
    @Override
    public Object hasPermission(String permission) {
        if (this.userId == null) {
            return new UnauthenticatedErrorResponse();
        } else if (permission == null) {
            return null;
        }

        try(Connection conn = PostgresConnectionPool.getConnection()) {
            String[] parts = permission.split(":");
            if (parts.length != 2) {
                throw new IllegalArgumentException("The role is malformed. Valid role: `prefix:permission`");
            }

            PreparedStatement stmt = conn.prepareStatement(hasRoleSQL);
            PGobject id = new PGobject();
            id.setType("uuid");
            id.setValue(this.userId);
            stmt.setObject(1, id);
            stmt.setString(2, parts[0]);
            stmt.setString(3, parts[1]);

            if (stmt.executeQuery().next()) {
                return null;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return new UnauthorizedErrorResponse();
    }

    public String getUserId() {
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
