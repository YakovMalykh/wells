package dz.systems;

import dz.systems.bdhandle.ManagerDB;
import dz.systems.converToXML.ConverterToXML;
import org.apache.commons.cli.*;

import java.util.*;

public class CommandLineWorker {

    public static void run(String[] args) {
        ManagerDB manager = new ManagerDB();

        Options options = setCommandLineOptions();

        CommandLineParser parser = new DefaultParser();

        CommandLine cmd = null;
        try {
            cmd = parser.parse(options, args);
        } catch (ParseException e) {
            System.out.println(e.getMessage());
        }

        Map<String, String> mapOfCommand = parsArgs(args);

        chooseAppropriateMethod(args, manager, cmd, mapOfCommand);
    }

    private static Options setCommandLineOptions() {
        Options options = new Options();

        options.addOption("q", "quantity", true, "the amount of equipment that needs to be created");
        options.addOption("w", "well", true, "name of well");
        options.addOption("i", "info", true, "get info about wells, array names of wells is required");
        options.addOption("e", "export", true, "export data into xml, file path is required");
        return options;
    }

    private static void chooseAppropriateMethod(String[] args, ManagerDB manager, CommandLine cmd, Map<String, String> mapOfCommand) {
        if (cmd.hasOption("q") && cmd.hasOption("w")) {
            int numder = Integer.parseInt(mapOfCommand.get("q"));
            String nameWell = mapOfCommand.get("w");

            manager.addEquipment(numder, nameWell);
        } else if (cmd.hasOption("i")) {
            manager.getWellsInfo(splitByComma(args));
        } else if (cmd.hasOption("e")) {
            String pathToFile = mapOfCommand.get("e");
            ConverterToXML.exportToXml(pathToFile);
        }
    }

    private static Map<String, String> parsArgs(String[] args) {
        Map<String, String> map = new HashMap<>();
        for (int i = 0; i < args.length; i++) {
            if (args[i].equals("-q") || args[i].equals("-quantity")) {
                map.put("q", args[i + 1]);
            } else if (args[i].equals("-w") || args[i].equals("-well")) {
                map.put("w", args[i + 1]);
            } else if (args[i].equals("-e") || args[i].equals("-export")) {
                map.put("e", args[i + 1]);
            }
        }
        return map;
    }

    private static String[] splitByComma(String[] argument) {
        List<String> list = new ArrayList<>();
        for (int i = 0; i < argument.length; i++) {
            String[] splited = argument[i].split(",");
            list.addAll(List.of(splited));
        }
        return list.toArray(new String[0]);
    }
}
