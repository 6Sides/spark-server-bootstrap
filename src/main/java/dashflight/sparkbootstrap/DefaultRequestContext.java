package dashflight.sparkbootstrap;

public class DefaultRequestContext implements SparkRequestContextGenerator {

    @Override
    public Object createContext(String token, String tokenFgp) {
        return new RequestContext(token, tokenFgp);
    }
}
