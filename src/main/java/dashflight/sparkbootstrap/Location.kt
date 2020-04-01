package dashflight.sparkbootstrap

class Location(val id: Int, val name: String) {

    companion object {
        fun withFields(id: Int?, name: String?): Location? {
            return if (id == null || name == null) {
                null
            } else Location(id, name)
        }
    }

}