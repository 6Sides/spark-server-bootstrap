package dashflight.sparkbootstrap;

import core.directives.auth.PermissionCheck;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
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
                    + "permissions.prefix = ? and "
                    + "permissions.name = ?";

    @Override
    public boolean hasPermission(String permission) {
        if (this.userId == null) return false;

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

            return stmt.executeQuery().next();
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return false;
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
}
