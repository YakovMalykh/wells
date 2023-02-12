package dz.systems;

import de.vandermeer.asciitable.AsciiTable;
import dz.systems.Exceptions.DataBaseSaveException;
import dz.systems.converToXML.WellDTO;
import dz.systems.entities.Equipment;
import dz.systems.entities.Well;
import org.sqlite.JDBC;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class DbHandler {
    private static final String DB_ADDRESS = "jdbc:sqlite:./test.db";
    private final Connection connection;
    private static DbHandler instance = null;

    public static synchronized DbHandler getInstance() {
        if (instance == null) {
            instance = new DbHandler();
        }
        return instance;
    }

    private DbHandler() {
        try {
            DriverManager.registerDriver(new JDBC());
            this.connection = DriverManager.getConnection(DB_ADDRESS);
            createTables();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }

    }

//    public Connection getConnection() {
//        return connection;
//    }


    public void createTables() {
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

    

    private Optional<Well> getWellByName(String wellsName) {
        try (PreparedStatement statement = connection.prepareStatement("SELECT * FROM well WHERE name = ?")) {
            statement.setString(1, wellsName.toUpperCase());
            ResultSet resultSet = statement.executeQuery();

            if (resultSet.getInt("id") != 0) {
                return Optional.of(
                        new Well(resultSet.getInt("id"), resultSet.getString("name"))
                );
            }
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
        return Optional.empty();
    }

    public void addEquipment(int number, String wellsName) {
        Well well = null;
        Optional<Well> wellOptional = getWellByName(wellsName);

        if (wellOptional.isEmpty()) {
            well = addNewWell(wellsName);
        } else {
            well = wellOptional.get();
        }

        for (int i = 0; i < number; i++) {
            try (PreparedStatement statement = connection.prepareStatement("INSERT INTO equipment (name,well_id) VALUES (?,?)")) {
                statement.setString(1, generatedName());
                statement.setInt(2, well.getId());
                statement.execute();
            } catch (SQLException e) {
                throw new DataBaseSaveException("произошла ошибка при сохранении оборудования в БД");
            }
        }
    }

    private Well addNewWell(String wellsName) {

        try (PreparedStatement statement = connection.prepareStatement("INSERT INTO well (name) VALUES (?)")) {
            statement.setString(1, wellsName.toUpperCase());
            statement.execute();

            return getWellByName(wellsName).orElseThrow(() -> new DataBaseSaveException("на предыдущем шаге скважина не сохранилась в БД"));
            // нужно переиспользовать здесь метод getWellByName
//            PreparedStatement getSavedWell = connection.prepareStatement("SELECT * FROM well WHERE name = ?");
//            getSavedWell.setString(1, wellsName.toUpperCase());
//            ResultSet resultSet = getSavedWell.executeQuery();
//            return new Well(resultSet.getInt("id"), resultSet.getString("name"));

        } catch (SQLException e) {
            throw new DataBaseSaveException("произошла ошибка при сохранении скважины в БД");
        }

    }


    /**
     * для обеспечения уникальности наверное стоит завести отдельную табл в БД, где будут храниться только
     * отдельные ID-ки со сквозной нумерацией оборудования
     */
    private String generatedName() {
        StringBuilder newEquipmentName = new StringBuilder("EQ");
        int random = (int) (Math.random() * 10000);
        newEquipmentName.append(random);
        return newEquipmentName.toString();
    }


    public void getWellsInfo(String... wellsNames) {
        List<Integer> arrayWellsId = new ArrayList<>();
        for (String wellsName : wellsNames) {
            Optional<Well> optionalWell = getWellByName(wellsName);
            if (optionalWell.isPresent()) {
                int wellId = optionalWell.get().getId();
                arrayWellsId.add(wellId);
            }
        }

        StringBuilder numberOfArgs = new StringBuilder();
        for (int i = 0; i < arrayWellsId.size(); i++) {
            numberOfArgs.append("?");
            if (i < arrayWellsId.size() - 1) {
                numberOfArgs.append(",");
            }
        }

        String query = String.format("SELECT well.name, COUNT(*) AS count FROM well INNER JOIN equipment e on well.id = e.well_id\n" +
                "GROUP BY well.name HAVING well_id in (%s)", numberOfArgs);


        try (PreparedStatement statement = connection.prepareStatement(query)) {
            for (int i = 0; i < arrayWellsId.size(); i++) {
                int index = i + 1;
                statement.setInt(index, arrayWellsId.get(i));
            }

            ResultSet resultSet = statement.executeQuery();
            AsciiTable table = new AsciiTable();
            table.addRule();
            table.addRow("name", "count");
            table.addRule();
            while (resultSet.next()) {
                table.addRow(resultSet.getString("name"), resultSet.getInt("count"));
                table.addRule();
            }
            System.out.println(table.render());
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
    }

    public List<WellDTO> prepareDataForExport() {
        List<WellDTO> wellDTOList = new ArrayList<>();

        List<Well> allWells = getAllWells();

        for (Well well : allWells) {
            List<Equipment> equipmentList = new ArrayList<>();

            fillListOfEquipmentForSpecificWell(well, equipmentList);

            WellDTO wellDTO = new WellDTO();
            wellDTO.setId(well.getId());
            wellDTO.setName(well.getName());
            wellDTO.setEquipmentList(equipmentList);

            wellDTOList.add(wellDTO);
        }
        return wellDTOList;
    }
    private void fillListOfEquipmentForSpecificWell(Well well, List<Equipment> equipmentList) {
        try (PreparedStatement prepareStatement = connection.prepareStatement("SELECT * FROM equipment WHERE well_id = ?")) {
            prepareStatement.setInt(1, well.getId());
            ResultSet resultSet = prepareStatement.executeQuery();
            while (resultSet.next()) {
                Equipment equipment = new Equipment(resultSet.getInt("id"), resultSet.getString("name"), resultSet.getInt("well_id"));
                equipmentList.add(equipment);
            }
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
    }
    private List<Well> getAllWells() {
        List<Well> wells = new ArrayList<>();
        try (Statement statement = connection.createStatement()) {
            ResultSet resultSet = statement.executeQuery("SELECT * FROM well");
            while (resultSet.next()) {
                wells.add(
                        new Well(
                                resultSet.getInt("id"),
                                resultSet.getString("name"))
                );
            }
            return wells;
        } catch (SQLException e) {
            System.out.println(e.getMessage());
            return wells;
        }
    }

}
