package dz.systems.bdhandle;

import de.vandermeer.asciitable.AsciiTable;
import dz.systems.bdhandle.DbCreator;
import dz.systems.exceptions.DataBaseSaveException;
import dz.systems.converToXML.WellDTO;
import dz.systems.entities.Equipment;
import dz.systems.entities.Well;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class ManagerDB {
    private final Connection connection = DbCreator.getInstance().getConnection();

    public void addEquipment(int number, String wellsName) {
        Well well;
        Optional<Well> wellOptional = getWellByName(wellsName);

        if (wellOptional.isEmpty()) {
            well = addNewWell(wellsName);
        } else {
            well = wellOptional.get();
        }
        recordingEquipmentIntoDB(number, well);
    }

    private void recordingEquipmentIntoDB(int number, Well well) {
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

    private Well addNewWell(String wellsName) {

        try (PreparedStatement statement = connection.prepareStatement("INSERT INTO well (name) VALUES (?)")) {
            statement.setString(1, wellsName.toUpperCase());
            statement.execute();

            return getWellByName(wellsName).orElseThrow(() -> new DataBaseSaveException("на предыдущем шаге скважина не сохранилась в БД"));

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
        List<Integer> arrayWellsId = getArrayWellsId(wellsNames);

        String query = prepareQuery(arrayWellsId);

        try (PreparedStatement statement = connection.prepareStatement(query)) {
            for (int i = 0; i < arrayWellsId.size(); i++) {
                int index = i + 1;
                statement.setInt(index, arrayWellsId.get(i));
            }

            ResultSet resultSet = statement.executeQuery();

            AsciiTable table = prepareTable(resultSet);

            System.out.println(table.render());

        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
    }
    private List<Integer> getArrayWellsId(String[] wellsNames) {
        List<Integer> arrayWellsId = new ArrayList<>();
        for (String wellsName : wellsNames) {
            Optional<Well> optionalWell = getWellByName(wellsName);
            if (optionalWell.isPresent()) {
                int wellId = optionalWell.get().getId();
                arrayWellsId.add(wellId);
            }
        }
        return arrayWellsId;
    }
    private String prepareQuery(List<Integer> arrayWellsId) {
        StringBuilder numberOfArgs = new StringBuilder();
        for (int i = 0; i < arrayWellsId.size(); i++) {
            numberOfArgs.append("?");
            if (i < arrayWellsId.size() - 1) {
                numberOfArgs.append(",");
            }
        }
        return String.format("SELECT well.name, COUNT(*) AS count FROM well INNER JOIN equipment e on well.id = e.well_id\n" +
                "GROUP BY well.name HAVING well_id in (%s)", numberOfArgs);
    }
    private AsciiTable prepareTable(ResultSet resultSet) throws SQLException {
        AsciiTable table = new AsciiTable();
        table.addRule();
        table.addRow("name", "count");
        table.addRule();
        while (resultSet.next()) {
            table.addRow(resultSet.getString("name"), resultSet.getInt("count"));
            table.addRule();
        }
        return table;
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


}
