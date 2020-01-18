package dashflight.sparkbootstrap;

import core.directives.auth.PermissionCheck;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import org.postgresql.util.PGobject;

public class RequestContext implements PermissionCheck {

    private Connection conn = DatabaseManager.getConnection();
    private String userId;

    public RequestContext(String userId) {
        this.userId = userId;
    }

    private static final String hasRoleSQL =
            "select description from accounts.user_permissions\n"
                    + "inner join accounts.permissions \n"
                    + "on user_permissions.permission_id = permissions.id \n"
                    + "where user_permissions.user_id = ? and\n"
                    + "permissions.prefix = ? and\n"
                    + "permissions.name = ?";

    @Override
    public boolean hasPermission(String permission) {
        if (this.userId.equals("admin")) return true;

        try {
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
}
