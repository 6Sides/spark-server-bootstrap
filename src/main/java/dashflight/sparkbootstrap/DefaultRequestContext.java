package dashflight.sparkbootstrap;

public class DefaultRequestContext implements SparkRequestContextGenerator {

    @Override
    public Object createContext(String userId) {
        return new RequestContext(userId);
    }
}
