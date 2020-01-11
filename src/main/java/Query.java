import graphql.schema.DataFetcher;
import schemabuilder.annotations.GraphQLDataFetcher;
import schemabuilder.annotations.GraphQLTypeConfiguration;

@GraphQLTypeConfiguration("Query")
public class Query {

    @GraphQLDataFetcher
    public DataFetcher test() {
        return env -> "hello, world!";
    }
}
