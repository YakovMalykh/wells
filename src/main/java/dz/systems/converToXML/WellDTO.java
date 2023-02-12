package dz.systems;

import lombok.Data;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;
import java.util.ArrayList;
import java.util.List;

@Data
@XmlRootElement
@XmlType(name = "well")
public class WellDTO {
    @XmlAttribute(name = "id")
    private String id;
    @XmlAttribute(name ="name")
    private String name;

    @XmlElement(name = "equipment")
    private List<Equipment> equipmentList = new ArrayList<>();
}
