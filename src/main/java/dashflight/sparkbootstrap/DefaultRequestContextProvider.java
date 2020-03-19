package dashflight.sparkbootstrap;

import com.auth0.jwt.interfaces.DecodedJWT;
import dashflight.jwt.JwtVerifier;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import net.dashflight.data.postgres.PostgresFactory;
import org.postgresql.util.PGobject;

public class DefaultRequestContextProvider implements SparkRequestContextGenerator {

    private static JwtVerifier jwtUtil = new JwtVerifier();

    @Override
    public Object createContext(String token, String tokenFgp) {
        String userId = null;
        Organization organization = null;
        Location homeLocation = null;
        List<Location> locations = new ArrayList<>();


        try {
            DecodedJWT result = jwtUtil.decodeJwtToken(token, tokenFgp);
            if (result != null) {
                userId = result.getClaim("user_id").asString();
            }

            try (Connection con = PostgresFactory.withDefaults().getConnection()) {
                String SQL = "select organizations.id as org_id, organizations.name as org_name, locations.id as home_location_id, locations.name as home_location_name from accounts.users "
                        + "inner join accounts.organizations on users.organization_id = organizations.id "
                        + "left join accounts.locations on users.home_location_id = locations.id "
                        + "where users.id = ? limit 1";

                PreparedStatement stmt = con.prepareStatement(SQL);

                PGobject uid = new PGobject();
                uid.setType("uuid");
                uid.setValue(userId);

                stmt.setObject(1, uid);

                ResultSet res = stmt.executeQuery();

                if (res.next()) {
                    organization = new Organization(
                            res.getInt("org_id"),
                            res.getString("org_name")
                    );
                    homeLocation = Location.withFields(
                            res.getInt("home_location_id"),
                            res.getString("home_location_name")
                    );
                }
            } catch (Exception e) {
                e.printStackTrace();
            }

            try (Connection con = PostgresFactory.withDefaults().getConnection()) {
                String SQL = "select locations.id, locations.name from accounts.user_locations "
                        + "inner join accounts.locations on user_locations.location_id = locations.id "
                        + "where user_id = ?";

                PreparedStatement stmt = con.prepareStatement(SQL);

                PGobject uid = new PGobject();
                uid.setType("uuid");
                uid.setValue(userId);

                stmt.setObject(1, uid);

                ResultSet res = stmt.executeQuery();
                while (res.next()) {
                    locations.add(new Location(res.getInt("id"), res.getString("name")));
                }

            } catch (Exception e) {
                e.printStackTrace();
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return new RequestContext(userId != null ? UUID.fromString(userId) : null, organization, homeLocation, locations);
    }
}
