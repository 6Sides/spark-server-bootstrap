package dashflight.sparkbootstrap

interface SparkRequestContextProvider {
    fun createContext(token: String, tokenFgp: String): Any?
}