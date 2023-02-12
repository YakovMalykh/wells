package dz.systems.converToXML;

import dz.systems.entities.Equipment;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class WellDTO {
    private int id;
    private String name;

    private List<Equipment> equipmentList = new ArrayList<>();
}
