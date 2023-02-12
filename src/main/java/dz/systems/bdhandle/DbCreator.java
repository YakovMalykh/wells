package dz.systems.bdhandle;

import org.sqlite.JDBC;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

public class DbCreator {
    private static final String DB_ADDRESS = "jdbc:sqlite:./test.db";
    private final Connection connection;
    private static DbCreator instance = null;

    public static synchronized DbCreator getInstance() {
        if (instance == null) {
            instance = new DbCreator();
        }
        return instance;
    }

    private DbCreator() {
        try {
            DriverManager.registerDriver(new JDBC());
            this.connection = DriverManager.getConnection(DB_ADDRESS);
            createTables();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }

    }
    private void createTables() {
        try (Statement statement = connection.createStatement()) {
            statement.executeUpdate("CREATE TABLE IF NOT EXISTS well(" +
                    "id  INTEGER PRIMARY KEY AUTOINCREMENT," +
                    "name VARCHAR(32) UNIQUE NOT NULL )");

            statement.executeUpdate("CREATE TABLE IF NOT EXISTS equipment" +
                    "( id  INTEGER PRIMARY KEY AUTOINCREMENT," +
                    "name VARCHAR(32) UNIQUE NOT NULL," +
                    "well_id INTEGER," +
                    " FOREIGN KEY(well_id) REFERENCES well(id))");

        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
    }

    public Connection getConnection() {
        return connection;
    }











}
