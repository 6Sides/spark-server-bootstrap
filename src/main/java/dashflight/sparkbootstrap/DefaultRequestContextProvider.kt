package dashflight.sparkbootstrap

import com.google.inject.Inject
import net.dashflight.data.jwt.verify.JwtVerificationResponse
import net.dashflight.data.jwt.verify.JwtVerifier
import net.dashflight.data.postgres.PostgresClient
import org.postgresql.util.PGobject
import java.util.*

class DefaultRequestContextProvider @Inject constructor(
        private val postgresClient: PostgresClient,
        private val jwtVerifier: JwtVerifier
) : SparkRequestContextProvider {

    override fun createContext(token: String?, tokenFgp: String?): RequestContext? {
        if (token == null || tokenFgp == null) {
            return null
        }

        var userId: String? = null

        when (val response = jwtVerifier.verifyToken(token, tokenFgp)) {
            is JwtVerificationResponse.Success -> {
                userId = response.result.getClaim("user_id").asString()
            }
            is JwtVerificationResponse.Error -> println(response.message)
        }

        return userId?.let { id ->
            val locations = getLocations(id)
            val (organization, homeLocation) = getOrgAndHomeLocation(id)

            RequestContext(UUID.fromString(id), organization, homeLocation, locations)
        }
    }

    private fun getLocations(userId: String): List<Location> {
        postgresClient.connection.use { conn ->
            val SQL = ("select locations.id, locations.name from accounts.user_locations "
                    + "inner join accounts.locations on user_locations.location_id = locations.id "
                    + "where user_id = ?")

            val stmt = conn.prepareStatement(SQL)
            val uid = PGobject()
            uid.type = "uuid"
            uid.value = userId
            stmt.setObject(1, uid)
            val res = stmt.executeQuery()

            return res.use {
                generateSequence {
                    if (res.next()) Location(res.getInt("id"), res.getString("name")) else null
                }.toList()
            }
        }
    }

    private fun getOrgAndHomeLocation(userId: String): Pair<Organization, Location> {
        postgresClient.connection.use { conn ->
            val SQL = ("select organizations.id as org_id, organizations.name as org_name, locations.id as home_location_id, locations.name as home_location_name from accounts.users "
                    + "inner join accounts.organizations on users.organization_id = organizations.id "
                    + "left join accounts.locations on users.home_location_id = locations.id "
                    + "where users.id = ? limit 1")

            val stmt = conn.prepareStatement(SQL)
            val uid = PGobject()
            uid.type = "uuid"
            uid.value = userId
            stmt.setObject(1, uid)
            val res = stmt.executeQuery()

            res.next()

            return Pair(
                Organization(
                        res.getInt("org_id"),
                        res.getString("org_name")
                ),
                Location(
                        res.getInt("home_location_id"),
                        res.getString("home_location_name")
                )
            )
        }
    }
}