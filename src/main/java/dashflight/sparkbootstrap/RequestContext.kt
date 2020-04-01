package dashflight.sparkbootstrap

import java.util.*

data class RequestContext(val userId: UUID, val organization: Organization, val homeLocation: Location?, private val locations: List<Location>) {

    val locationIds: IntArray
        get() {
            val result = IntArray(locations.size)
            for (i in result.indices) {
                result[i] = locations[i].id
            }
            return result
        }

}