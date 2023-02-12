package dz.systems.converToXML;

import com.sun.xml.txw2.output.IndentingXMLStreamWriter;
import dz.systems.bdhandle.ManagerDB;
import dz.systems.entities.Equipment;

import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;

public class ConverterToXML {

    public static void exportToXml(String filePath) {
        ManagerDB manager = new ManagerDB();
        List<WellDTO> wellDTOList = manager.prepareDataForExport();

        try {
            XMLOutputFactory output = XMLOutputFactory.newInstance();
            XMLStreamWriter writer = output.createXMLStreamWriter(new FileWriter(filePath));

            IndentingXMLStreamWriter indentingWriter = new IndentingXMLStreamWriter(writer);

            indentingWriter.writeStartDocument("1.0");
            indentingWriter.writeStartElement("dbinfo");

            for (WellDTO wellDTO : wellDTOList) {

                indentingWriter.writeStartElement("well");

                indentingWriter.writeAttribute("name", wellDTO.getName());
                indentingWriter.writeAttribute("id", String.valueOf(wellDTO.getId()));

                List<Equipment> equipmentList = wellDTO.getEquipmentList();
                for (Equipment equipment : equipmentList) {
                    indentingWriter.writeStartElement("equipment");
                    indentingWriter.writeAttribute("name", equipment.getName());
                    indentingWriter.writeAttribute("id", String.valueOf(equipment.getId()));
                    indentingWriter.writeEndElement();
                }

                indentingWriter.writeEndElement();
            }
            indentingWriter.writeEndElement();
            indentingWriter.writeEndDocument();
            indentingWriter.flush();
        } catch (IOException e) {
            System.out.println(e.getMessage());
        } catch (XMLStreamException e) {
            System.out.println(e.getMessage());
        }
    }
}
