package be.garagepoort.staffplusplus.trello.repository.database.migrations.sqlite;

import be.garagepoort.staffplusplus.trello.repository.database.migrations.Migration;
import be.garagepoort.staffplusplus.trello.repository.database.migrations.SqlMigrations;
import org.bukkit.Bukkit;

import java.sql.*;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

public class SqLiteMigrations implements SqlMigrations {

    private static SqLiteMigrations instance;
    // Define all migrations in this list.
    private static final List<Migration> migrations = Arrays.asList(new V1_CreateReportsTableMigration());

    private SqLiteMigrations() {
    }

    public static SqLiteMigrations getInstance() {
        if (instance == null) {
            instance = new SqLiteMigrations();
        }
        return instance;
    }

    @Override
    public void createMigrationTable() {
        try (Connection connect = SqlLiteConnection.connect();
             Statement stmt = connect.createStatement()) {
            Bukkit.getLogger().info("Creating migration table");

            String sql = "CREATE TABLE IF NOT EXISTS migrations (\n"
                    + "	id integer PRIMARY KEY,\n"
                    + "	version integer NOT NULL\n"
                    + ");";
            stmt.execute(sql);
        } catch (SQLException e) {
            Bukkit.getLogger().severe("Failure creating migration table: " + e.getMessage());
        }

    }

    @Override
    public void runMigrations() {
        try (Connection connect = SqlLiteConnection.connect()) {
            Bukkit.getLogger().info("Starting migrations");
            connect.setAutoCommit(false);
            int maxVersion = getMaxVersion();

            List<Migration> validMigrations = migrations.stream().filter(m -> m.getVersion() > maxVersion)
                    .sorted(Comparator.comparingInt(Migration::getVersion))
                    .collect(Collectors.toList());

            for (Migration migration : validMigrations) {
                try (Statement stmt = connect.createStatement()) {
                    String sql = migration.getStatement();
                    stmt.execute(sql);
                    PreparedStatement migrationStatement = connect.prepareStatement("INSERT INTO migrations (version) VALUES (?);");
                    migrationStatement.setInt(1, migration.getVersion());
                    migrationStatement.execute();

                    connect.commit();
                }
            }
        } catch (SQLException e) {
            Bukkit.getLogger().severe("Failure executing migrations: " + e.getMessage());
        }
    }

    private int getMaxVersion() {
        try (Connection connect = SqlLiteConnection.connect();
             Statement stmt = connect.createStatement()) {
            ResultSet resultSet = stmt.executeQuery("SELECT max(version) as max from migrations");
            int max = resultSet.next() ? resultSet.getInt("max") : 0;
            Bukkit.getLogger().info("Latest migration version = " + max);
            return max;
        } catch (SQLException e) {
            Bukkit.getLogger().severe("Failure retrieving max migration version: " + e.getMessage());
        }
        return 0;
    }
}
