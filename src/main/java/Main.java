import dashflight.sparkbootstrap.RuntimeEnvironment;
import dashflight.sparkbootstrap.SparkInitializer;

public class Main {

    public static void main(String[] args) {
        SparkInitializer.setGraphQLEndpoint("/test");
        SparkInitializer.setEnvironment(RuntimeEnvironment.DEVELOPMENT);
        SparkInitializer.setPort(8080);

        SparkInitializer.startServer();
    }
}
