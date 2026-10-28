package com.agristep.config;

import com.agristep.persistence.Database;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.Properties;

/**
 * Which database to use. Read from agristep.properties (next to the program, or the built-in default).
 * Set db.type=h2 for the no-install file database, or db.type=mysql for XAMPP / MySQL.
 */
public record DatabaseConfig(String type, String url, String user, String password) {

    private static final String DEFAULT_MYSQL_URL =
            "jdbc:mysql://localhost:3306/agristep_swing?createDatabaseIfNotExist=true&useUnicode=true&characterEncoding=utf8";

    public static DatabaseConfig from(Properties p) {
        String type = p.getProperty("db.type", "h2").trim().toLowerCase(Locale.ROOT);
        return switch (type) {
            case "h2" -> new DatabaseConfig(type,
                    "jdbc:h2:file:" + p.getProperty("h2.path", "./data/agristep").trim() + ";MODE=MySQL", "sa", "");
            case "mysql" -> new DatabaseConfig(type,
                    p.getProperty("mysql.url", DEFAULT_MYSQL_URL).trim(),
                    p.getProperty("mysql.user", "root").trim(),
                    p.getProperty("mysql.password", ""));
            default -> throw new IllegalArgumentException(
                    "db.type must be h2 or mysql, but it was \"" + type + "\" in agristep.properties");
        };
    }

    /** Loads ./agristep.properties if it exists, otherwise the copy inside the program. */
    public static DatabaseConfig load() throws IOException {
        Properties p = new Properties();
        Path file = Path.of("agristep.properties");
        if (Files.exists(file)) {
            try (Reader in = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
                p.load(in);
            }
        } else {
            try (InputStream in = DatabaseConfig.class.getResourceAsStream("/agristep.properties")) {
                if (in != null) p.load(new InputStreamReader(in, StandardCharsets.UTF_8));
            }
        }
        return from(p);
    }

    public boolean isMySql() { return "mysql".equals(type); }

    /** A short sentence saying where the data is kept, shown on the sign-in screen. */
    public String describe() {
        if (isMySql()) {
            int q = url.indexOf('?');
            return "Your data is saved in MySQL (" + url.substring("jdbc:".length(), q < 0 ? url.length() : q) + ").";
        }
        String path = url.substring("jdbc:h2:file:".length());
        int semi = path.indexOf(';');
        if (semi >= 0) path = path.substring(0, semi);
        return "Your data is saved on this computer in " + Path.of(path).toAbsolutePath().normalize() + ".";
    }

    public Database openDatabase() { return new Database(url, user, password); }
}
