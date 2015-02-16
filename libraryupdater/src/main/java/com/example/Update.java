package com.example;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;

public class Update {

    private static final String DEFAULT_MODULE = "app";

    /**
     *
     * @param args Put one module name as a parameter
     * @throws IOException
     */
    public static void main(String[] args) throws IOException {

        // Module to update
        String module = DEFAULT_MODULE;
        if (args.length > 0) {
            module = args[0];
        }

        System.out.println("\n> Updating module: " + module);

        // Gradlew command
        String gradlew = "./gradlew";
        if (System.getProperty("os.name").toLowerCase().contains("windows")) {
            gradlew = ".\\gradlew.bat";
        }

        Map<String, String> librairies = new HashMap<>();

        File file = new File(module + "/build.gradle");

        FileInputStream fileInputStream = new FileInputStream(file);

        BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(fileInputStream));

        StringBuilder output = new StringBuilder();
        String line;
        System.out.println("\n> Listing dependencies");
        while ((line = bufferedReader.readLine()) != null) {
            if (line.trim().startsWith("compile '")) {
                int version = line.lastIndexOf(':');

                // Removing actual version
                line = line.substring(0, version);

                // from compile 'com.android.support:appcompat-v7:+'
                // to appcompat-v7
                int libraryNameIndex = line.lastIndexOf(':') + 1;
                String libraryName = line.substring(libraryNameIndex).replace('-', '_');
                System.out.println("  * Library found: " + libraryName);
                librairies.put(libraryName, null);

                // Asking for last version
                line += ":+'";
            }
            output.append(line).append("\n");
        }
        bufferedReader.close();
        fileInputStream.close();

        System.out.println("\n> Updating build.gradle dependencies with '+'");
        FileOutputStream fileOutputStream = new FileOutputStream(file);
        fileOutputStream.write(output.toString().getBytes());
        fileOutputStream.close();

        System.out.println("\n> " + gradlew + " --refresh-dependencies clean\n");
        Process process = Runtime.getRuntime().exec(gradlew + " --refresh-dependencies clean");
        BufferedReader processReader = new BufferedReader(new InputStreamReader(process.getInputStream()));
        while ((line = processReader.readLine()) != null) {
            System.out.println(line);
        }

        System.out.println("\n> Getting latest versions from .idea/libraries");
        File ideaLibraries = new File(".idea/libraries");
        for (String library : librairies.keySet()) {
            File[] xmlFiles = ideaLibraries.listFiles();
            if (xmlFiles != null) {
                for (final File fileEntry : xmlFiles) {
                    if (fileEntry.getName().startsWith(library)) {
                        String version = fileEntry.getName().replace(library + "_", "");
                        // Removing .xml extension
                        version = version.substring(0, version.indexOf(".xml"));
                        // Replacing _ with .
                        version = version.replace('_', '.');

                        librairies.put(library, version);
                    }
                }
            }
        }

        System.out.println("\n> Updating build.gradle to latest versions");
        fileInputStream = new FileInputStream(file);
        bufferedReader = new BufferedReader(new InputStreamReader(fileInputStream));

        output = new StringBuilder();
        while ((line = bufferedReader.readLine()) != null) {
            if (line.trim().startsWith("compile '")) {

                for (Map.Entry<String, String> library : librairies.entrySet()) {
                    String name = library.getKey().replace('_', '-');
                    String version = library.getValue();

                    if (line.contains(name)) {
                        line = line.replace(":+'", ":" + version + "'");
                        System.out.println("  * " + name + " updated to " + version);
                    }
                }
            }
            output.append(line).append("\n");
        }
        bufferedReader.close();
        fileInputStream.close();

        fileOutputStream = new FileOutputStream(file);
        fileOutputStream.write(output.toString().getBytes());
        fileOutputStream.close();

        System.exit(1);
    }
}
