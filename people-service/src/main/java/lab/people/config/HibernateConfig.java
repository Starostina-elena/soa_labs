package lab.people.config;

import org.hibernate.SessionFactory;
import org.hibernate.cfg.Configuration;

import java.sql.Connection;

public final class HibernateConfig {
    private HibernateConfig() {}

    public static SessionFactory create(Class<?>... entities) {
        var config =
                new Configuration()
                        .setProperty("hibernate.connection.driver_class", "org.postgresql.Driver")
                        .setProperty(
                                "hibernate.connection.url",
                                System.getenv()
                                        .getOrDefault(
                                                "DB_URL",
                                                "jdbc:postgresql://pg:5432/studs?currentSchema=s414257"))
                        .setProperty(
                                "hibernate.connection.username",
                                System.getenv().getOrDefault("DB_USER", "s414257"))
                        .setProperty("hibernate.hbm2ddl.auto", "validate")
                        .setProperty("hibernate.connection.pool_size", "4")
                        .setProperty(
                                "hibernate.connection.isolation",
                                Integer.toString(Connection.TRANSACTION_REPEATABLE_READ));

        for (Class<?> entity : entities) config.addAnnotatedClass(entity);
        return config.buildSessionFactory();
    }
}
