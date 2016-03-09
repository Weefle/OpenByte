/*
 * Created by JFormDesigner on Tue Jan 05 19:21:12 ICT 2016
 */

package net.openbyte.gui;

import net.lingala.zip4j.core.ZipFile;
import net.openbyte.Launch;
import net.openbyte.data.Files;
import net.openbyte.data.file.OpenProjectSolution;
import net.openbyte.enums.MinecraftVersion;
import net.openbyte.enums.ModificationAPI;
import org.apache.commons.exec.CommandLine;
import org.apache.commons.exec.DefaultExecutor;
import org.apache.commons.io.FileUtils;
import org.gradle.tooling.GradleConnector;

import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.net.URL;
import javax.swing.*;

/**
 * @author Gary Lee
 */
public class CreateProjectFrame extends JFrame {
    private JFrame previousFrame;
    private File projectFolder;
    private String projectName;
    private MinecraftVersion version;
    private ModificationAPI api;
    private final ProgressMonitor monitor = new ProgressMonitor(this, "Setting up your project...", "Creating project directory...", 0, 100);;

    public CreateProjectFrame(JFrame previousFrame) {
        this.previousFrame = previousFrame;
        initComponents();
    }

    private void button2ActionPerformed(ActionEvent e) {
        setVisible(false);
        previousFrame.setVisible(true);
    }

    private void button1ActionPerformed(ActionEvent e) {
        if(!(Launch.nameToSolution.get(textField1.getText()) == null)){
            JOptionPane.showMessageDialog(this, "Project has already been created.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        ModificationAPI modificationAPI = ModificationAPI.MINECRAFT_FORGE;
        if(comboBox1.getSelectedIndex() == 1) {
            modificationAPI = ModificationAPI.MCP;
        }
        MinecraftVersion minecraftVersion = MinecraftVersion.BOUNTIFUL_UPDATE;
        if(((String)comboBox2.getSelectedItem()).equals("1.7.10")){
            minecraftVersion = MinecraftVersion.THE_UPDATE_THAT_CHANGED_THE_WORLD;
        }
        if(modificationAPI == ModificationAPI.MCP) {
            minecraftVersion = MinecraftVersion.COMBAT_UPDATE;
            String version = (String) comboBox2.getSelectedItem();
            if (version.equals("1.8.9")){
                minecraftVersion = MinecraftVersion.BOUNTIFUL_UPDATE;
            }
            if(version.equals("1.7.10")) {
                minecraftVersion = MinecraftVersion.THE_UPDATE_THAT_CHANGED_THE_WORLD;
            }
        }
        this.version = minecraftVersion;
        this.api = modificationAPI;
        setVisible(false);
        //JOptionPane.showMessageDialog(this, "We're working on setting up the project. When we are ready, a window will show up.", "Working on project creation", JOptionPane.INFORMATION_MESSAGE);
        this.projectName = textField1.getText();
        SwingWorker<Void, Void> worker = new SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() throws Exception {
                doOperations();
                return null;
            }
        };
        monitor.setMillisToPopup(0);
        monitor.setMillisToDecideToPopup(0);
        monitor.setProgress(1);
        worker.execute();
        if(monitor.isCanceled()) {
            projectFolder.delete();
            new File(Files.WORKSPACE_DIRECTORY, projectName + ".openproj").delete();
            return;
        }
    }

    public void doOperations(){
        createProjectFolder();
        monitor.setProgress(20);
        monitor.setNote("Creating solution for project...");
        createSolutionFile();
        monitor.setProgress(60);
        monitor.setNote("Cloning API...");
        cloneAPI();
        monitor.setProgress(90);
        monitor.setNote("Decompiling Minecraft...");
        if(this.api == ModificationAPI.MCP) {
            if(System.getProperty("os.name").startsWith("Windows")) {
                try {
                    Runtime.getRuntime().exec("cmd /c decompile.bat", null, projectFolder);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            } else {
                try {
                    CommandLine commandLine = CommandLine.parse("sh " + new File(projectFolder, "decompile.sh").getAbsolutePath());
                    DefaultExecutor executor = new DefaultExecutor();
                    executor.setWorkingDirectory(projectFolder);
                    executor.execute(commandLine);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        } else {
            GradleConnector.newConnector().forProjectDirectory(projectFolder).connect().newBuild().setJvmArguments("-XX:-UseGCOverheadLimit").forTasks("setupDecompWorkspace").run();
        }
        monitor.close();
        WelcomeFrame welcomeFrame = new WelcomeFrame();
        welcomeFrame.setVisible(true);
    }

    private void createProjectFolder(){
        this.projectFolder = new File(Files.WORKSPACE_DIRECTORY, projectName);
        projectFolder.mkdir();
    }

    private void createSolutionFile(){
        File solutionFile = new File(Files.WORKSPACE_DIRECTORY, projectName + ".openproj");
        Files.createNewFile(projectName + ".openproj", Files.WORKSPACE_DIRECTORY);
        OpenProjectSolution solution = OpenProjectSolution.getProjectSolutionFromFile(solutionFile);
        solution.setProjectFolder(this.projectFolder);
        solution.setProjectName(projectName);
        solution.setModificationAPI(this.api);
    }

    private void cloneAPI(){
        if(this.api == ModificationAPI.MINECRAFT_FORGE){
            String link;
            if(this.version == MinecraftVersion.BOUNTIFUL_UPDATE){
                // too old -> link = "http://files.minecraftforge.net/maven/net/minecraftforge/forge/1.8.9-11.15.0.1684/forge-1.8.9-11.15.0.1684-mdk.zip";
                link = "http://files.minecraftforge.net/maven/net/minecraftforge/forge/1.8.9-11.15.1.1722/forge-1.8.9-11.15.1.1722-mdk.zip";
            } else {
                link = "http://files.minecraftforge.net/maven/net/minecraftforge/forge/1.7.10-10.13.4.1614-1.7.10/forge-1.7.10-10.13.4.1614-1.7.10-src.zip";
            }
            File forgeZip = new File(projectFolder, "forge.zip");
            try{
                URL url = new URL(link);
                FileUtils.copyURLToFile(url, forgeZip);
                ZipFile zipFile = new ZipFile(forgeZip);
                zipFile.extractAll(projectFolder.getAbsolutePath());
                forgeZip.delete();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        if(this.api == ModificationAPI.MCP) {
            String link = "http://download933.mediafire.com/130haka1szmg/3ww1inazlkamkcc/mcp924-beta1.zip";;
            if(this.version == MinecraftVersion.BOUNTIFUL_UPDATE) {
                link = "http://www.modcoderpack.com/website/sites/default/files/releases/mcp918.zip";
            }
            if(this.version == MinecraftVersion.THE_UPDATE_THAT_CHANGED_THE_WORLD) {
                link = "http://www.modcoderpack.com/website/sites/default/files/releases/mcp908.zip";
            }
            File mcpZip = new File(projectFolder, "mcp.zip");
            try {
                URL url = new URL(link);
                FileUtils.copyURLToFile(url, mcpZip);
                ZipFile zipFile = new ZipFile(mcpZip);
                zipFile.extractAll(projectFolder.getAbsolutePath());
                mcpZip.delete();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private void comboBox1ItemStateChanged(ItemEvent e) {
        if(e.getStateChange() == ItemEvent.SELECTED) {
            if(comboBox1.getSelectedIndex() == 1) {
                // mcp
                if(!System.getProperty("os.name").startsWith("Windows")) {
                    JOptionPane.showMessageDialog(this, "Python is required for MCP, please make sure you have Python installed.", "Notification", JOptionPane.INFORMATION_MESSAGE);
                }
                String[] versions = new String[] { "1.9", "1.8.9", "1.7.10" };
                DefaultComboBoxModel mcpModel = new DefaultComboBoxModel(versions);
                comboBox2.setModel(mcpModel);
                return;
            }
            if(comboBox1.getSelectedIndex() == 0) {
                // mcforge
                String[] versions = new String[] { "1.8.9", "1.7.10" };
                DefaultComboBoxModel forgeModel = new DefaultComboBoxModel(versions);
                comboBox2.setModel(forgeModel);
                return;
            }
        }
    }

    private void initComponents() {
        // JFormDesigner - Component initialization - DO NOT MODIFY  //GEN-BEGIN:initComponents
        // Generated using JFormDesigner Evaluation license - Gary Lee
        button1 = new JButton();
        textField1 = new JTextField();
        label1 = new JLabel();
        comboBox1 = new JComboBox<>();
        label2 = new JLabel();
        comboBox2 = new JComboBox<>();
        label3 = new JLabel();
        button2 = new JButton();

        //======== this ========
        setTitle("Project Creation");
        setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
        setResizable(false);
        Container contentPane = getContentPane();
        contentPane.setLayout(null);

        //---- button1 ----
        button1.setText("Create project");
        button1.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                button1ActionPerformed(e);
            }
        });
        contentPane.add(button1);
        button1.setBounds(new Rectangle(new Point(340, 170), button1.getPreferredSize()));
        contentPane.add(textField1);
        textField1.setBounds(10, 25, 245, textField1.getPreferredSize().height);

        //---- label1 ----
        label1.setText("Project Name");
        contentPane.add(label1);
        label1.setBounds(new Rectangle(new Point(10, 5), label1.getPreferredSize()));

        //---- comboBox1 ----
        comboBox1.setModel(new DefaultComboBoxModel<>(new String[] {
            "Minecraft Forge",
            "MCP"
        }));
        comboBox1.addItemListener(new ItemListener() {
            @Override
            public void itemStateChanged(ItemEvent e) {
                comboBox1ItemStateChanged(e);
            }
        });
        contentPane.add(comboBox1);
        comboBox1.setBounds(10, 70, 245, comboBox1.getPreferredSize().height);

        //---- label2 ----
        label2.setText("Modification API");
        contentPane.add(label2);
        label2.setBounds(new Rectangle(new Point(10, 50), label2.getPreferredSize()));

        //---- comboBox2 ----
        comboBox2.setModel(new DefaultComboBoxModel<>(new String[] {
            "1.8.9",
            "1.7.10"
        }));
        contentPane.add(comboBox2);
        comboBox2.setBounds(10, 120, 245, comboBox2.getPreferredSize().height);

        //---- label3 ----
        label3.setText("Minecraft Version");
        contentPane.add(label3);
        label3.setBounds(new Rectangle(new Point(10, 100), label3.getPreferredSize()));

        //---- button2 ----
        button2.setText("Cancel");
        button2.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                button2ActionPerformed(e);
            }
        });
        contentPane.add(button2);
        button2.setBounds(new Rectangle(new Point(10, 170), button2.getPreferredSize()));

        { // compute preferred size
            Dimension preferredSize = new Dimension();
            for(int i = 0; i < contentPane.getComponentCount(); i++) {
                Rectangle bounds = contentPane.getComponent(i).getBounds();
                preferredSize.width = Math.max(bounds.x + bounds.width, preferredSize.width);
                preferredSize.height = Math.max(bounds.y + bounds.height, preferredSize.height);
            }
            Insets insets = contentPane.getInsets();
            preferredSize.width += insets.right;
            preferredSize.height += insets.bottom;
            contentPane.setMinimumSize(preferredSize);
            contentPane.setPreferredSize(preferredSize);
        }
        setSize(465, 240);
        setLocationRelativeTo(getOwner());
        // JFormDesigner - End of component initialization  //GEN-END:initComponents
    }

    // JFormDesigner - Variables declaration - DO NOT MODIFY  //GEN-BEGIN:variables
    // Generated using JFormDesigner Evaluation license - Gary Lee
    private JButton button1;
    private JTextField textField1;
    private JLabel label1;
    private JComboBox<String> comboBox1;
    private JLabel label2;
    private JComboBox<String> comboBox2;
    private JLabel label3;
    private JButton button2;
    // JFormDesigner - End of variables declaration  //GEN-END:variables
}
