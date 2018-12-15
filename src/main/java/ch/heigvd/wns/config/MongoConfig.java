package ch.heigvd.wns.config;

import com.mongodb.MongoClient;
import com.mongodb.MongoClientURI;
import io.github.cdimascio.dotenv.Dotenv;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.config.AbstractMongoConfiguration;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;

@Configuration
@EnableMongoRepositories("ch.heigvd.wns.repository.mongo")
public class MongoConfig extends AbstractMongoConfiguration {

    @Bean
    public Dotenv dotenv() {
        return Dotenv
                .configure()
                .ignoreIfMissing()
                .load();
    }

    @Override
    public MongoClient mongoClient() {
        String URI = "";
        if (dotenv().get("MONGO_URI").isEmpty())
            URI = "mongodb://" + dotenv().get("MONGO_USER") + ":" +
                dotenv().get("MONGO_PASS") + "@" + dotenv().get("MONGO_HOST") + ":" + dotenv().get("MONGO_PORT") + "/" + dotenv().get("DB_NAME");
        else
            URI = dotenv().get("MONGO_URI");
        MongoClientURI mongoClientURI = new MongoClientURI(URI);
        return new MongoClient(mongoClientURI);
    }

    @Override
    protected String getDatabaseName() {
        return dotenv().get("DB_NAME");
    }

}
