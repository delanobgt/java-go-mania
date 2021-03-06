package gomultiplayeronline;

import controls.ControlButton;
import controls.PersistentButton;
import controls.PlayerPanel;
import enums.BoardSize;
import enums.Player;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import main.GoMainFrame;
import models.GoModel;
import models.Point;

public class GoMultiOnPanel extends JPanel {
    
    public static final int CONTROL_PANEL_HEIGHT = 40;
    GoMainFrame parent;
    private String firstName;
    private String secondName;
    private BoardSize boardSize;
    private GoModel goModel;
    private GoMultiOnCanvas goMultiOnCanvas;
    private PlayerPanel firstPanel;
    private PlayerPanel secondPanel;
    private JLabel passBtn;
    private JLabel surrenderBtn;
    private JPanel controlPanel;
    private Player playerType;
    private GameSocket gameSocket;
    private volatile boolean waitingOpponent;
    
    public GoMultiOnPanel(GoMainFrame parent, Player playerType, String firstName, String secondName, BoardSize boardSize, GameSocket gameSocket) {
        this.parent = parent;
        this.firstName = firstName;
        this.secondName = secondName;
        this.boardSize = boardSize;
        this.playerType = playerType;
        waitingOpponent = playerType.isBlack() ? false : true;
        this.goModel = new GoModel(boardSize);
        this.gameSocket = gameSocket;
        this.setLayout(null);
        this.setSize(new Dimension(GoMainFrame.FRAME_WIDTH, GoMainFrame.FRAME_HEIGHT));
        this.setPreferredSize(new Dimension(GoMainFrame.FRAME_WIDTH, GoMainFrame.FRAME_HEIGHT));
        
        goMultiOnCanvas = new GoMultiOnCanvas(goModel, this);
        this.add(goMultiOnCanvas);
        
        // first player panel
        firstPanel = new PlayerPanel(
                firstName,
                0,
                0,
                GoMainFrame.FRAME_WIDTH-goMultiOnCanvas.getWidth(), 
                GoMainFrame.FRAME_HEIGHT/2,
                Player.BLACK
        );
        this.add(firstPanel);
        firstPanel.activateColor();
        
        // second player panel
        secondPanel = new PlayerPanel(
                secondName,
                0,
                GoMainFrame.FRAME_HEIGHT/2, 
                GoMainFrame.FRAME_WIDTH-goMultiOnCanvas.getWidth(), 
                GoMainFrame.FRAME_HEIGHT/2,
                Player.WHITE
        );
        this.add(secondPanel);
        secondPanel.deactivateColor();
        
        // controlPanel
        controlPanel = new JPanel();
        controlPanel.setLayout(null);
        controlPanel.setBounds(firstPanel.getWidth(),
                goMultiOnCanvas.getHeight(), 
                goMultiOnCanvas.getWidth(),
                GoMainFrame.FRAME_HEIGHT-goMultiOnCanvas.getHeight()
        );
        controlPanel.setBackground(GoMainFrame.COLOR_4);
        this.add(controlPanel);
        
        final int BTN_WIDTH = 150;
        final int V_MARGIN = 7;
        final int BTN_SPACING = (controlPanel.getWidth()-(3*BTN_WIDTH))/4;
        final int BTN_HEIGHT = controlPanel.getHeight()-(2*V_MARGIN);
        
        PersistentButton territoryBtn = new PersistentButton(
                "Show Territory",
                new Font("Arial", Font.BOLD, 12),
                BTN_SPACING, 
                V_MARGIN,
                BTN_WIDTH, 
                BTN_HEIGHT
        );
        territoryBtn.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                territoryBtn.togglePersistent();
                goModel.scanTerritory();
                goMultiOnCanvas.toggleTerritoryBeingShown();
                if (goMultiOnCanvas.isTerritoryBeingShown())
                    territoryBtn.setBackground(GoMainFrame.COLOR_2);
            }
        });
        controlPanel.add(territoryBtn);
        
        passBtn = new ControlButton(
                "Pass",
                new Font("Arial", Font.BOLD, 12),
                2*BTN_SPACING+BTN_WIDTH, 
                V_MARGIN, 
                BTN_WIDTH,
                BTN_HEIGHT
        );
        passBtn.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                if (!goModel.getTurn().equals(playerType)) {
                    return;
                }
                gameSocket.send("PASS:"+goModel.getTurn().toString());
                handlePassBtn();
            }
            @Override
            public void mouseExited(MouseEvent e) {
                if (waitingOpponent) {
                    passBtn.setBackground(GoMainFrame.COLOR_3);
                    passBtn.setForeground(Color.GRAY);
                }
            }
            @Override
            public void mouseEntered(MouseEvent e) {
                if (waitingOpponent) {
                    passBtn.setBackground(GoMainFrame.COLOR_3);
                    passBtn.setForeground(Color.GRAY);
                }
            }
        });
        controlPanel.add(passBtn);
        
        surrenderBtn = new ControlButton(
                "Surrender",
                new Font("Arial", Font.BOLD, 12),
                3*BTN_SPACING+2*BTN_WIDTH,
                V_MARGIN,
                BTN_WIDTH, 
                BTN_HEIGHT
        );
        surrenderBtn.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                if (!goModel.getTurn().equals(playerType)) {
                    return;
                }
                int response = JOptionPane.showConfirmDialog(parent, "Are you sure?", "Surrender", JOptionPane.YES_NO_OPTION);
                if (response == JOptionPane.YES_OPTION) {
                    gameSocket.send("SURRENDER:"+goModel.getTurn().toString());
                    handleSurrenderBtn();
                }
            }
            @Override
            public void mouseExited(MouseEvent e) {
                if (waitingOpponent) {
                    surrenderBtn.setBackground(GoMainFrame.COLOR_3);
                    surrenderBtn.setForeground(Color.GRAY);
                }
            }
            @Override
            public void mouseEntered(MouseEvent e) {
                if (waitingOpponent) {
                    surrenderBtn.setBackground(GoMainFrame.COLOR_3);
                    surrenderBtn.setForeground(Color.GRAY);
                }
            }
        });
        controlPanel.add(surrenderBtn);
        
        if (!playerType.equals(goModel.getTurn())) {
            passBtn.setBackground(GoMainFrame.COLOR_3);
            passBtn.setForeground(Color.GRAY);
            surrenderBtn.setBackground(GoMainFrame.COLOR_3);
            surrenderBtn.setForeground(Color.GRAY);
        }
        updatePlayerStatus();
        startListeningSocket();
    }
    
    private void handlePassBtn() {
        goModel.addPassCounter();
        goModel.toggleTurn();
        updatePlayerStatus();
        if (goModel.getPassCounter() >= 2) {
            double blackTotalScore = goModel.getBlackTotalScore();
            double whiteTotalScore = goModel.getWhiteTotalScore();
            if (whiteTotalScore > blackTotalScore) {
                goModel.winWhite();
            } else {
                goModel.winBlack();
            }
            String msg = String.format(
                    "<html>" +
                    "You %s<br>" +
                    "%s (BLACK): %.1f points<br>" +
                    "%s (WHITE): %.1f points (included +6.5)<br>" +
                    "</html>",
                    goModel.getWin().equals(playerType) ? "won" : "lost",
                    firstName, blackTotalScore,
                    secondName, whiteTotalScore
            );
            JOptionPane.showMessageDialog(parent, msg);
            parent.changeSceneTo("mainMenu");
        }
    }
    
    private void handleSurrenderBtn() {
        goModel.surrenderedBy(goModel.getTurn());
        
        String blackName = String.format(
                "<span style=\"font-size:16px;\">%s</span>",
                firstName
        );
        String whiteName = String.format(
                "<span style=\"font-size:16px;\">%s</span>",
                secondName
        );
        String msg = String.format(
                "<html>You %s because %s surrendered.</html>",
                goModel.getWin().equals(playerType) ? "won" : "lost",
                !goModel.getWin().isBlack() ? blackName : whiteName
        );
        JOptionPane.showMessageDialog(parent, msg);
        
        parent.changeSceneTo("mainMenu");
    }
    
    public void updatePlayerStatus() {
        if (goModel.getTurn().isBlack()) {
            firstPanel.activateColor();
            firstPanel.giveTurn();
            secondPanel.deactivateColor();
            secondPanel.takeAwayTurn();
        } else {
            firstPanel.deactivateColor();
            firstPanel.takeAwayTurn();
            secondPanel.activateColor();
            secondPanel.giveTurn();
        }
        
        if (goModel.getPassCounter() == 2) {
            firstPanel.changeActionText("Last action: ", "yellow-PASS");
            secondPanel.changeActionText("Last action: ", "yellow-PASS");
        } else if (goModel.getPassCounter() == 1) {
            if (goModel.getTurn().isBlack())
                secondPanel.changeActionText("Last action: ", "yellow-PASS");
            else
                firstPanel.changeActionText("Last action: ", "yellow-PASS");
        } else if (goModel.getLastMovePoint() != null) {
            Point p = goModel.getLastMovePoint();
            if (goModel.getTurn().isBlack()) {
                secondPanel.changeActionText(
                        "Last action: <br>", 
                        String.format("white-PLACE STONE AT&nbsp;&nbsp;&nbsp;(%s %s)", p.r()+1, (char)('A'+p.c()))
                );
            } else {
                firstPanel.changeActionText(
                        "Last action: <br>",
                        String.format("white-PLACE STONE AT&nbsp;&nbsp;&nbsp;(%s %s)", p.r()+1, (char)('A'+p.c()))
                );
            }
        }
        
        firstPanel.changeTerritoryText("Territory: ", "white-"+goModel.getBlackTerritoryScore());
        secondPanel.changeTerritoryText("Territory: ", "white-"+goModel.getWhiteTerritoryScore());
        
        firstPanel.changeCapturedText("Captured: ", "white-"+goModel.getBlackCapturedScore());
        secondPanel.changeCapturedText("Captured: ", "white-"+goModel.getWhiteCapturedScore());
    }

    public boolean isWaitingOpponent() {
        return waitingOpponent;
    }
    public void toggleWaitingOpponent() {
        waitingOpponent = !waitingOpponent;
    }
    
    private void startListeningSocket() {
        new Thread(() -> {
            System.out.println("start listening for request..");
            while (true) {
                String request = (String) gameSocket.receive();
                String[] splitted = request.split(":");
                String type = splitted[0];
                if (type.equals("STONE")) {
                    String[] coords = splitted[1].split(",");
                    goMultiOnCanvas.handleUserClick(Integer.parseInt(coords[0]), Integer.parseInt(coords[1]));
                    toggleWaitingOpponent();
                    if (waitingOpponent) System.out.println("anjing salah");
                } else if (type.equals("SURRENDER")) {
                    handleSurrenderBtn();
                } else if (type.equals("PASS")) {
                    toggleWaitingOpponent();
                    handlePassBtn();
                } else {
                    System.out.println("something else!!");
                }
                passBtn.setBackground(GoMainFrame.COLOR_3);
                passBtn.setForeground(Color.WHITE);
                surrenderBtn.setBackground(GoMainFrame.COLOR_3);
                surrenderBtn.setForeground(Color.WHITE);
            }
        }).start();
    }
    
    public void sendRequest(String request) {
        passBtn.setBackground(GoMainFrame.COLOR_3);
        passBtn.setForeground(Color.GRAY);
        surrenderBtn.setBackground(GoMainFrame.COLOR_3);
        surrenderBtn.setForeground(Color.GRAY);
        gameSocket.send(request);
    }
}
