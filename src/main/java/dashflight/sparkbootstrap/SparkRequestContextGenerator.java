package dashflight.sparkbootstrap;

public interface SparkRequestContextGenerator {

    Object createContext(String token, String tokenFgp);

}