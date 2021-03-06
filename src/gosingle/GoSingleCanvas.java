package gosingle;

import controls.LegendJLabel;
import enums.StoneType;
import java.awt.BasicStroke;
import models.GoModel;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.util.List;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.Timer;
import main.GoMainFrame;
import models.Point;

public class GoSingleCanvas extends JPanel {
    
    private final int CELL_SIZE;
    private static final int CELL_MARGIN = 2;
    private static final int ANIMATION_DELAY = 50;
    private int canvasWidth;
    private int canvasHeight;
    private GoModel goModel;
    private Timer timer;
    private Thread botThread;
    private GoSinglePanel parentContainer;
    private boolean territoryBeingShown = false;
    
    private volatile int mouseX = -1;
    private volatile int mouseY = -1;
    
    private MouseAdapter mouseAdapter = new MouseAdapter() {
        @Override
        public void mousePressed(MouseEvent e) {
            if (parentContainer.isWaitingComputer()) return;
            if (handleUserClick(e.getX(), e.getY())) {
                parentContainer.setWaitingComputer(true);
            }
        }
        @Override
        public void mouseExited(MouseEvent e) {
            mouseX = -1;
            mouseY = -1;
        }
    };
    private MouseMotionAdapter mouseMotionAdapter = new MouseMotionAdapter() {
        @Override
        public void mouseMoved(MouseEvent e) {
            mouseX = e.getX();
            mouseY = e.getY();
        }
    };
    
    public GoSingleCanvas(GoModel goModel, GoSinglePanel parentContainer) {
        this.goModel = goModel;
        this.parentContainer = parentContainer;
        canvasWidth = canvasHeight = GoMainFrame.FRAME_HEIGHT-GoSinglePanel.CONTROL_PANEL_HEIGHT;
        CELL_SIZE = canvasWidth/(goModel.getBoardSize()+2);
        this.setLayout(null);
        this.setPreferredSize(new Dimension(canvasWidth, canvasHeight));
        this.setSize(canvasWidth, canvasHeight);
        this.setLocation(GoMainFrame.FRAME_WIDTH-canvasHeight, 0);
        this.setFocusable(true);
        
        this.addMouseListener(mouseAdapter);
        this.addMouseMotionListener(mouseMotionAdapter);
        
        this.timer = new Timer(ANIMATION_DELAY, e -> {
            repaint();
        });
        this.timer.start();
        
        // bot thread listen for its turn
        this.botThread = new Thread(() -> {
            try {
                while (!Thread.currentThread().isInterrupted()) { //cek stop
                    if (parentContainer.isWaitingComputer()) { // cek computer turn
                        Point bestPoint = Bot.getNextMove(
                                goModel.getImmutableBoard(),
                                goModel.getPassCounter(),
                                goModel.getBlackCapturedScore(),
                                goModel.getWhiteCapturedScore(),
                                goModel.getTurn(),
                                parentContainer.getDifficulty()
                        );
                        if (bestPoint == null) { //pass
                            parentContainer.handlePassBtn();
                        } else {
                            handleBotPlaceAt(bestPoint.r(), bestPoint.c());
                        }
                        parentContainer.setControlButtonsActivated(true);
                        parentContainer.setWaitingComputer(false);
                    }
                    sleep(500);
                }
            } catch (Exception ex) {
                System.out.println(ex+" lol bot thread");
            }
        });
        
        // add Legend labels (ex. A1, A2, C5)
        for (int r = 1; r <= goModel.getBoardSize(); r++) {
            // add alphabets coordinates label
            JLabel label = new LegendJLabel(r*CELL_SIZE, 0, CELL_SIZE, Character.toString((char)('A'+r-1)));
            this.add(label);
            JLabel label2 = new LegendJLabel(r*CELL_SIZE, (goModel.getBoardSize()+1)*CELL_SIZE, CELL_SIZE, Character.toString((char)('A'+r-1)));
            this.add(label2);
            // add digit coordinates label
            JLabel label3 = new LegendJLabel(0, r*CELL_SIZE, CELL_SIZE, Integer.toString(r));
            this.add(label3);
            JLabel label4 = new LegendJLabel((goModel.getBoardSize()+1)*CELL_SIZE, r*CELL_SIZE, CELL_SIZE, Integer.toString(r));
            this.add(label4);
        }
    }
    
    private void handleBotPlaceAt(int botR, int botC) {
        Point botPoint = new Point(botR, botC);
        
        // coba taruh batu dulu
        goModel.setStoneAt(botR, botC, goModel.getCurrentStoneType());
        
        List<List<Point>> connStonesList = goModel.getListOfDeadStones();
        
        //is move suicidal ?
        if (connStonesList.size() == 1 &&
            connStonesList.get(0).contains(botPoint) ) {
                goModel.removeStoneAt(botR, botC);
                JOptionPane.showMessageDialog(this, "Suicidal move is not allowed!");
                return;
        }
        
        int blackCapturedScore = 0;
        int whiteCapturedScore = 0;
        //remove other zero-liberty stones, except current attacking stone
        for (List<Point> connStones : connStonesList) {
            if (!connStones.contains(botPoint)) {
                if (connStones.size() > 0) {
                    Point p = connStones.get(0);
                    if (goModel.getStoneAt(p.r(), p.c()).isBlack())
                        whiteCapturedScore += connStones.size();
                    else if (goModel.getStoneAt(p.r(), p.c()).isWhite())
                        blackCapturedScore += connStones.size();
                }
                goModel.removeAllStones(connStones);
            }
        }
        
        goModel.memorizeCurrentState();
        goModel.scanTerritory();
        goModel.resetPassCounter();
        goModel.toggleTurn();
        goModel.setLastMovePoint(botPoint);
        parentContainer.updatePlayerStatus();
    }
    
    private boolean handleUserClick(int userX, int userY) {
        // translate coordinate backward (-1, -1)
        int userR = userY/CELL_SIZE-1;
        int userC = userX/CELL_SIZE-1;
        // check if user is still inside valid bounds
        if (! (0 <= userR && userR < goModel.getBoardSize() &&
                0 <= userC && userC < goModel.getBoardSize()) ) return false;
        Point userPoint = new Point(userR, userC);
        
        if (goModel.isOccupiedAt(userR, userC)) {
            return false;
        }
        
        goModel.backupBoard();
        
        // coba taruh batu dulu
        goModel.setStoneAt(userR, userC, goModel.getCurrentStoneType());
        
        List<List<Point>> connStonesList = goModel.getListOfDeadStones();
        //is move suicidal ?
        if (connStonesList.size() == 1 &&
            connStonesList.get(0).contains(userPoint) ) {
                goModel.removeStoneAt(userR, userC);
                JOptionPane.showMessageDialog(this, "Suicidal move is not allowed!");
                return false;
        }
        
        int blackCapturedScore = 0;
        int whiteCapturedScore = 0;
        //remove other zero-liberty stones, except current attacking stone
        for (List<Point> connStones : connStonesList) {
            if (!connStones.contains(userPoint)) {
                if (connStones.size() > 0) {
                    Point p = connStones.get(0);
                    if (goModel.getStoneAt(p.r(), p.c()).isBlack())
                        whiteCapturedScore += connStones.size();
                    else if (goModel.getStoneAt(p.r(), p.c()).isWhite())
                        blackCapturedScore += connStones.size();
                }
                goModel.removeAllStones(connStones);
            }
        }
        
        // Ko Rule implementation
        // if infinite cycle detected
        if (goModel.isCurrentStateRecently()) {
            goModel.restoreBoard();
            JOptionPane.showMessageDialog(this, "Ko Rule violated!");
            return false;
        // if no infinite cycle detected
        } else {
            goModel.addBlackCapturedScore(blackCapturedScore);
            goModel.addWhiteCapturedScore(whiteCapturedScore);
        }
        
        goModel.memorizeCurrentState();
        goModel.scanTerritory();
        goModel.resetPassCounter();
        goModel.toggleTurn();
        goModel.setLastMovePoint(userPoint);
        parentContainer.updatePlayerStatus();
        parentContainer.setControlButtonsActivated(false);
        return true;
    }
    
    @Override
    protected void paintComponent(Graphics oldG) {
        super.paintComponent(oldG);
        Graphics2D g = (Graphics2D) oldG;
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        
        //paint background
        g.setColor(new Color(52, 61, 70, 235));
        g.fillRect(0, 0, canvasWidth, canvasHeight);
        
        //paint board grid
        g.setColor(new Color(255, 255, 255, 75));
        g.setStroke(new BasicStroke(2));
        for (int r = 0; r <= goModel.getBoardSize()+1; r++) {
            for (int c = 0; c <= goModel.getBoardSize()+1; c++) {
                // print board grid
                if (1 <= c && c < goModel.getBoardSize()
                            && 1 <= r && r < goModel.getBoardSize()) {
                    g.drawRect(
                            (r*CELL_SIZE)+(CELL_SIZE/2)+1,
                            (c*CELL_SIZE)+(CELL_SIZE/2)+1,
                            CELL_SIZE,
                            CELL_SIZE
                    );
                }
            }
        }
        
        // paint territories
        {
            final int TERRITORY_SIZE = 10;
            if (territoryBeingShown) {
                List<Point> blackTerritoryList = goModel.getBlackTerritoryList();
                if (blackTerritoryList != null) {
                    g.setColor(GoMainFrame.COLOR_4);
                    for (Point p : blackTerritoryList) {
                        g.fillRect(
                                (p.c()+1)*CELL_SIZE+(CELL_SIZE/2)-(TERRITORY_SIZE/2), 
                                (p.r()+1)*CELL_SIZE+(CELL_SIZE/2)-(TERRITORY_SIZE/2), 
                                TERRITORY_SIZE,
                                TERRITORY_SIZE
                        );
                    }
                }
                List<Point> whiteTerritoryList = goModel.getWhiteTerritoryList();
                if (whiteTerritoryList != null) {
                    g.setColor(Color.WHITE);
                    for (Point p : whiteTerritoryList) {
                        g.fillRect(
                                (p.c()+1)*CELL_SIZE+(CELL_SIZE/2)-(TERRITORY_SIZE/2), 
                                (p.r()+1)*CELL_SIZE+(CELL_SIZE/2)-(TERRITORY_SIZE/2), 
                                TERRITORY_SIZE,
                                TERRITORY_SIZE
                        );
                    }
                }
            }
        }
        
        //paint mouse highlight
        if (!parentContainer.isWaitingComputer()) {
            int r = mouseY/CELL_SIZE, c = mouseX/CELL_SIZE;
            // only paint shadow at valid grids
            if ( (1 <= r && r <= goModel.getBoardSize() &&
                    1 <= c && c <= goModel.getBoardSize()) ) {
                if (goModel.getTurn().isBlack()) {
                    drawStoneShadow(g, r, c, 0);
                } else {
                    drawStoneShadow(g, r, c, 1);
                }
            }
        }
            
        //paint stones
        for (int r = 0; r < goModel.getBoardSize(); r++) {
            for (int c = 0; c < goModel.getBoardSize(); c++) {
                if (goModel.getStoneAt(r, c).isBlack()) {
                    drawStone(g, r+1, c+1, 0); // translate coordinate forward (+1,+1)
                } else if (goModel.getStoneAt(r, c).isWhite()) {
                    drawStone(g, r+1, c+1, 1); // translate coordinate forward (+1,+1)
                }
            }
        }
        
        // paint waiting banner
        if (parentContainer.isWaitingComputer()) {
            final int barHeight = 80;
            g.setColor(new Color(30, 194, 163, 210));
            g.fillRect(0, (getHeight()-barHeight)/2, getWidth(), barHeight);
            g.setColor(Color.WHITE);
            g.setFont(new Font("Arial", Font.BOLD, 36));
            g.drawString(waitingComputer, 85, (getHeight()-barHeight)/2+55);
            
            waitingComputerCounter = (waitingComputerCounter+ANIMATION_DELAY)%350;
            if (waitingComputerCounter == 0) {
                waitingComputer += " .";
                if (waitingComputer.length() > initialWaitingComputer.length()+(3*2))
                    waitingComputer = initialWaitingComputer;
            }
        }
    }
    private final String initialWaitingComputer = "Waiting for Computer";
    private String waitingComputer = initialWaitingComputer;
    private int waitingComputerCounter = 0;
    
    private void drawStoneShadow(Graphics2D g, int r, int c, int turn) {
        if (turn == 0) g.setColor(new Color(0, 0, 0, 150));
        else g.setColor(new Color(255, 255, 255, 150));
        g.fillOval(
            c*CELL_SIZE+CELL_MARGIN,
            r*CELL_SIZE+CELL_MARGIN,
            CELL_SIZE-(2*CELL_MARGIN),
            CELL_SIZE-(2*CELL_MARGIN)
        );
    }
    
    private void drawStone(Graphics2D g, int r, int c, int turn) {
        if (turn == 0) g.setColor(GoMainFrame.COLOR_4);
        else g.setColor(Color.decode("#D6BFB0"));
        g.fillOval(
            c*CELL_SIZE+CELL_MARGIN,
            r*CELL_SIZE+CELL_MARGIN,
            CELL_SIZE-(2*CELL_MARGIN),
            CELL_SIZE-(2*CELL_MARGIN)
        );
        if (turn == 0) g.setColor(GoMainFrame.COLOR_3);
        else g.setColor(Color.WHITE);
        g.fillOval(
            c*CELL_SIZE+CELL_MARGIN*3,
            r*CELL_SIZE+CELL_MARGIN*3,
            CELL_SIZE-(6*CELL_MARGIN),
            CELL_SIZE-(6*CELL_MARGIN)
        );
    }
    
    public void toggleTerritoryBeingShown() {
        territoryBeingShown = !territoryBeingShown;
    }
    public boolean isTerritoryBeingShown() { return territoryBeingShown; }
    
    private void sleep(long delay) {
        try {
            Thread.sleep(delay);
        } catch (Exception ex) {
            System.out.println(ex);
        }
    }
    
    public void startBotThread() {
        this.botThread.start();
    }
    public void stopBotThread() {
        this.botThread.interrupt();
    }
}
