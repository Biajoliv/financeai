package com.financeai.config;

import io.github.cdimascio.dotenv.Dotenv;
import org.springframework.context.annotation.Configuration;

@Configuration
public class DotenvConfig {
    
    static {
        Dotenv dotenv = Dotenv.configure()
                .directory(".")
                .ignoreIfMissing()
                .load();
        
        // Load DATABASE_URL
        String databaseUrl = dotenv.get("DATABASE_URL");
        if (databaseUrl != null) {
            System.setProperty("DATABASE_URL", databaseUrl);
        }
        
        // Load DATABASE_USERNAME
        String databaseUsername = dotenv.get("DATABASE_USERNAME");
        if (databaseUsername != null) {
            System.setProperty("DATABASE_USERNAME", databaseUsername);
        }
        
        // Load DATABASE_PASSWORD
        String databasePassword = dotenv.get("DATABASE_PASSWORD");
        if (databasePassword != null) {
            System.setProperty("DATABASE_PASSWORD", databasePassword);
        }
        
        // Load JWT_SECRET
        String jwtSecret = dotenv.get("JWT_SECRET");
        if (jwtSecret != null) {
            System.setProperty("JWT_SECRET", jwtSecret);
        }
    }
}
