package net.openbyte.data.file;

import javax.swing.*;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Properties;

/**
 * Represents a project solution file.
 */
public class OpenProjectSolution {
    /**
     * A properties representation of the file.
     */
    private Properties solution;
    /**
     * The file which is formatted as a OpenProjectSolution
     */
    private File saveToFile;

    /**
     * Creates a new open project solution from a specified file
     *
     * @param file the file that the openprojectsolution format will be read from
     */
    private OpenProjectSolution(File file){
        this.solution = new Properties();
        try {
            solution.load(new FileInputStream(file));
        } catch (IOException e) {
            e.printStackTrace();
        }
        if(solution.get("version") == null){
            solution.setProperty("version", "1.0");
            try {
                solution.store(new FileOutputStream(file), null);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        try {
            solution.load(new FileInputStream(file));
        } catch (Exception e) {
            e.printStackTrace();
        }
        saveToFile = file;
    }

    /**
     * Saves the changes made to the file that the format read from
     */
    private void save(){
        try {
            solution.store(new FileOutputStream(saveToFile), null);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Retrieves the project solution from the specified file.
     *
     * @param file the file where the solution is formatted
     * @return the decoded solution object
     */
    public static OpenProjectSolution getProjectSolutionFromFile(File file){
        return new OpenProjectSolution(file);
    }

    /**
     * Deletes the solution.
     */
    public void deleteSolution(){
        JOptionPane.showMessageDialog(null, "Delete the solution file manually at this location: " + saveToFile.getAbsolutePath() + " after you have closed the application.", "Delete the solution", JOptionPane.INFORMATION_MESSAGE);
    }

    /**
     * Sets the project name.
     *
     * @param name the project name
     */
    public void setProjectName(String name){
        solution.setProperty("name", name);
        save();
    }

    public String getProjectName(){
        return solution.getProperty("name");
    }

    public File getProjectFolder(){
        return new File(solution.getProperty("folderPath"));
    }

    public void setProjectFolder(File file){
        solution.setProperty("folderPath", file.getAbsolutePath());
    }
}
