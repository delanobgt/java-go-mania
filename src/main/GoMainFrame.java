package main;

import gomultiplayeroffline.GoMultiOffMenu;
import java.awt.CardLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.util.HashMap;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

public class GoMainFrame extends JFrame {

    // global constants
    public static final int FRAME_WIDTH = 900;
    public static final int FRAME_HEIGHT = 600;
    
    // main container
    JPanel mainPanel = new JPanel();
    CardLayout cardLayout = new CardLayout();
    
    // different types of cards (Scenes)
    GoMainMenu goMainMenu;
    GoMultiOffMenu goMultiOffMenu;
    
    // map of name -> components
    HashMap<String, Component> componentMap = new HashMap<>();
    
    public GoMainFrame() {
        mainPanel.setLayout(cardLayout);
        mainPanel.setPreferredSize(new Dimension(FRAME_WIDTH, FRAME_HEIGHT));
        mainPanel.setSize(FRAME_WIDTH, FRAME_HEIGHT);
        
        goMainMenu = new GoMainMenu(this);
        goMultiOffMenu = new GoMultiOffMenu(this);
        
        addComponent("mainMenu", goMainMenu);
        addComponent("multiOffMenu", goMultiOffMenu);
        
        this.add(mainPanel);
        this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        this.setResizable(false);
        this.setTitle("Go Mania!");
        this.pack();
        this.setLocationRelativeTo(null);
        
        changeSceneTo("mainMenu");
    }

    public void changeSceneTo(String sceneName) {
        cardLayout.show(mainPanel, sceneName);
    }
    
    public void addComponent(String name, Component component) {
        removeComponent(name);
        mainPanel.add(name, component);
        componentMap.put(name, component);
    }
    public boolean removeComponent(String name) {
        if (componentMap.containsKey(name)) {
            mainPanel.remove(componentMap.get(name));
            componentMap.remove(name);
            return true;
        }
        return false;
    }
    
    // program's main entry 
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            new GoMainFrame().setVisible(true);
        });
    }
    
}