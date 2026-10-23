import java.awt.*;
import java.awt.event.*;
import java.util.Random;
import javax.swing.*;

public class MoonFighter extends JFrame {

    private final GamePanel gamePanel;
    private boolean fullscreen;
    private Rectangle previousBounds;
    private int previousState;

    public MoonFighter() {
        setTitle("MoonFighter: A Low Budget Tekken Clone");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setResizable(true);

        gamePanel = new GamePanel();
        setContentPane(gamePanel);

        pack();
        setLocationRelativeTo(null);
        setMinimumSize(new Dimension(700, 420));

        gamePanel.setFocusable(true);
        gamePanel.requestFocusInWindow();
    }

    private void toggleFullscreen() {
        GraphicsDevice device = GraphicsEnvironment
                .getLocalGraphicsEnvironment()
                .getDefaultScreenDevice();

        if (!fullscreen) {
            previousBounds = getBounds();
            previousState = getExtendedState();
            dispose();
            setUndecorated(true);
            device.setFullScreenWindow(this);
            fullscreen = true;
        } else {
            device.setFullScreenWindow(null);
            dispose();
            setUndecorated(false);
            setExtendedState(previousState);
            if (previousBounds != null) {
                setBounds(previousBounds);
            }
            fullscreen = false;
        }

        setVisible(true);
        gamePanel.requestFocusInWindow();
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new MoonFighter().setVisible(true));
    }

    private class GamePanel extends JPanel implements ActionListener {

        private static final int SCENE_WIDTH = 900;
        private static final int SCENE_HEIGHT = 500;
        private static final int GROUND = 416;

        private final Timer timer = new Timer(16, this);
        private final Random random = new Random();

        private final String[] characterNames = {
                "RJS", "AJ", "CARL", "PIERCE"
        };

        private final JComboBox<String> playerOneChoice =
                new JComboBox<>(characterNames);

        private final JComboBox<String> playerTwoChoice =
                new JComboBox<>(characterNames);

        private final JButton startButton = new JButton("START MATCH");
        private final JButton restartButton = new JButton("RESTART");
        private final JLabel statusLabel = new JLabel(
                "Choose your fighters and start the match",
                SwingConstants.CENTER
        );

        private Fighter playerOne;
        private Fighter playerTwo;

        private boolean running;
        private boolean paused;
        private String winner = "";

        private int animationFrame;
        private int starShift;

        private final Color[] fighterColors = {
                new Color(60, 210, 255),
                new Color(255, 90, 100),
                new Color(255, 190, 55),
                new Color(175, 110, 255)
        };

        GamePanel() {
            setPreferredSize(new Dimension(SCENE_WIDTH, SCENE_HEIGHT));
            setBackground(Color.BLACK);
            setLayout(new BorderLayout());

            JPanel topPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 8));
            topPanel.setBackground(new Color(13, 17, 35));

            JLabel title = new JLabel("MOONFIGHTER: A LOW BUDGET TEKKEN CLONE");
            title.setForeground(Color.WHITE);
            title.setFont(new Font("Monospaced", Font.BOLD, 17));

            playerOneChoice.setSelectedIndex(0);
            playerTwoChoice.setSelectedIndex(1);

            startButton.setFocusable(false);
            restartButton.setFocusable(false);

            topPanel.add(title);
            topPanel.add(new JLabel("P1:"));
            topPanel.add(playerOneChoice);
            topPanel.add(new JLabel("P2:"));
            topPanel.add(playerTwoChoice);
            topPanel.add(startButton);
            topPanel.add(restartButton);

            JPanel bottomPanel = new JPanel(new BorderLayout());
            bottomPanel.setBackground(new Color(13, 17, 35));
            statusLabel.setForeground(Color.WHITE);
            statusLabel.setFont(new Font("Monospaced", Font.BOLD, 13));
            statusLabel.setBorder(BorderFactory.createEmptyBorder(7, 5, 7, 5));
            bottomPanel.add(statusLabel, BorderLayout.CENTER);

            add(topPanel, BorderLayout.NORTH);
            add(bottomPanel, BorderLayout.SOUTH);

            startButton.addActionListener(e ->
                    startMatch(
                            (String) playerOneChoice.getSelectedItem(),
                            (String) playerTwoChoice.getSelectedItem()
                    )
            );

            restartButton.addActionListener(e ->
                    startMatch(
                            (String) playerOneChoice.getSelectedItem(),
                            (String) playerTwoChoice.getSelectedItem()
                    )
            );

            setupControls();
            timer.start();
        }

        private void setupControls() {
            bindKey("A", "p1left", () -> move(playerOne, -1));
            bindKey("D", "p1right", () -> move(playerOne, 1));
            bindKey("W", "p1jump", () -> jump(playerOne));
            bindKey("F", "p1attack", () -> attack(playerOne, playerTwo));

            bindKey("LEFT", "p2left", () -> move(playerTwo, -1));
            bindKey("RIGHT", "p2right", () -> move(playerTwo, 1));
            bindKey("UP", "p2jump", () -> jump(playerTwo));
            bindKey("ENTER", "p2attack", () -> attack(playerTwo, playerOne));

            bindKey("SPACE", "pause", () -> {
                if (running && winner.isEmpty()) {
                    paused = !paused;
                    statusLabel.setText(paused ? "PAUSED" : "FIGHT!");
                }
            });

            bindKey("F11", "fullscreen", MoonFighter.this::toggleFullscreen);

            bindKey("ESCAPE", "escape", () -> {
                if (fullscreen) {
                    toggleFullscreen();
                }
            });
        }

        private void bindKey(String key, String name, Runnable action) {
            getInputMap(WHEN_IN_FOCUSED_WINDOW)
                    .put(KeyStroke.getKeyStroke(key), name);

            getActionMap().put(name, new AbstractAction() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    action.run();
                }
            });
        }

        private void move(Fighter fighter, int direction) {
            if (!canControl(fighter)) {
                return;
            }

            fighter.vx = direction * 4;
            fighter.face = direction;
        }

        private void jump(Fighter fighter) {
            if (!canControl(fighter) || !fighter.ground) {
                return;
            }

            fighter.vy = -12;
            fighter.ground = false;
        }

        private void attack(Fighter attacker, Fighter defender) {
            if (!canControl(attacker) || defender == null
                    || attacker.cooldown > 0) {
                return;
            }

            attacker.attack = 9;
            attacker.cooldown = 25;

            int distance = Math.abs(
                    (int) (attacker.x + attacker.w / 2
                            - defender.x - defender.w / 2)
            );

            boolean facingOpponent =
                    attacker.face > 0 ? defender.x >= attacker.x
                            : defender.x <= attacker.x;

            if (distance < 92 && facingOpponent && defender.invincible <= 0) {
                defender.hp = Math.max(0, defender.hp - 10);
                defender.invincible = 12;
                defender.vx = attacker.face * 7;
                defender.vy = -3;

                if (defender.hp <= 0) {
                    winner = attacker.name;
                    running = false;
                    statusLabel.setText(winner + " WINS! Press RESTART to play again.");
                }
            }
        }

        private boolean canControl(Fighter fighter) {
            return running && !paused && winner.isEmpty() && fighter != null;
        }

        private void startMatch(String nameOne, String nameTwo) {
            if (nameOne == null || nameTwo == null) {
                return;
            }

            int indexOne = getCharacterIndex(nameOne);
            int indexTwo = getCharacterIndex(nameTwo);

            playerOne = new Fighter(
                    nameOne,
                    190,
                    fighterColors[indexOne],
                    1
            );

            playerTwo = new Fighter(
                    nameTwo,
                    660,
                    fighterColors[indexTwo],
                    -1
            );

            playerOne.opponent = playerTwo;
            playerTwo.opponent = playerOne;

            running = true;
            paused = false;
            winner = "";

            statusLabel.setText("FIGHT!  P1: A/D MOVE, W JUMP, F PUNCH"
                    + "  |  P2: ARROWS MOVE, UP JUMP, ENTER PUNCH");

            requestFocusInWindow();
            repaint();
        }

        private int getCharacterIndex(String name) {
            for (int i = 0; i < characterNames.length; i++) {
                if (characterNames[i].equals(name)) {
                    return i;
                }
            }
            return 0;
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            animationFrame++;
            starShift = (starShift + 1) % SCENE_WIDTH;

            if (running && !paused && winner.isEmpty()) {
                updateFighter(playerOne);
                updateFighter(playerTwo);
                separateFighters();
            }

            repaint();
        }

        private void updateFighter(Fighter fighter) {
            if (fighter == null) {
                return;
            }

            if (fighter.cooldown > 0) {
                fighter.cooldown--;
            }

            if (fighter.attack > 0) {
                fighter.attack--;
            }

            if (fighter.invincible > 0) {
                fighter.invincible--;
            }

            fighter.x += fighter.vx;
            fighter.vx *= 0.78;

            if (Math.abs(fighter.vx) < 0.25) {
                fighter.vx = 0;
            }

            fighter.y += fighter.vy;
            fighter.vy += 0.55;

            if (fighter.y + fighter.h >= GROUND) {
                fighter.y = GROUND - fighter.h;
                fighter.vy = 0;
                fighter.ground = true;
            }

            fighter.x = Math.max(25, Math.min(
                    SCENE_WIDTH - fighter.w - 25,
                    fighter.x
            ));

            if (fighter.opponent != null) {
                fighter.face = fighter.x < fighter.opponent.x ? 1 : -1;
            }
        }

        private void separateFighters() {
            if (playerOne == null || playerTwo == null) {
                return;
            }

            Rectangle one = new Rectangle(
                    (int) playerOne.x,
                    (int) playerOne.y,
                    playerOne.w,
                    playerOne.h
            );

            Rectangle two = new Rectangle(
                    (int) playerTwo.x,
                    (int) playerTwo.y,
                    playerTwo.w,
                    playerTwo.h
            );

            if (one.intersects(two)) {
                if (playerOne.x < playerTwo.x) {
                    playerOne.x -= 1.5;
                    playerTwo.x += 1.5;
                } else {
                    playerOne.x += 1.5;
                    playerTwo.x -= 1.5;
                }
            }
        }

        @Override
        protected void paintComponent(Graphics graphics) {
            super.paintComponent(graphics);

            Graphics2D g = (Graphics2D) graphics.create();

            g.setRenderingHint(
                    RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_OFF
            );

            double scale = Math.min(
                    getWidth() / (double) SCENE_WIDTH,
                    getHeight() / (double) SCENE_HEIGHT
            );

            int offsetX = (int) ((getWidth() - SCENE_WIDTH * scale) / 2);
            int offsetY = (int) ((getHeight() - SCENE_HEIGHT * scale) / 2);

            g.setColor(Color.BLACK);
            g.fillRect(0, 0, getWidth(), getHeight());

            g.translate(offsetX, offsetY);
            g.scale(scale, scale);

            drawArena(g);
            drawHealthBars(g);

            if (playerOne != null && playerTwo != null) {
                drawFighter(g, playerOne);
                drawFighter(g, playerTwo);
            } else {
                drawIntro(g);
            }

            if (paused) {
                drawOverlay(g, "PAUSED", "PRESS SPACE TO RESUME");
            } else if (!winner.isEmpty()) {
                drawOverlay(g, winner + " WINS!", "PRESS RESTART TO FIGHT AGAIN");
            } else if (!running) {
                drawOverlay(g, "MOONFIGHTER", "SELECT FIGHTERS AND PRESS START");
            }

            g.dispose();
        }

        private void drawArena(Graphics2D g) {
            GradientPaint sky = new GradientPaint(
                    0, 0, new Color(4, 7, 28),
                    0, GROUND, new Color(32, 38, 76)
            );

            g.setPaint(sky);
            g.fillRect(0, 0, SCENE_WIDTH, SCENE_HEIGHT);

            g.setColor(new Color(120, 150, 210));

            for (int i = 0; i < 85; i++) {
                int x = Math.floorMod(i * 127 + starShift / 2, SCENE_WIDTH);
                int y = Math.floorMod(i * 53 + 23, 310);
                int size = i % 7 == 0 ? 3 : 2;
                g.fillRect(x, y, size, size);
            }

            g.setColor(new Color(150, 195, 255));
            g.fillOval(670, 45, 125, 125);

            g.setColor(new Color(95, 140, 200));
            g.fillOval(690, 65, 30, 25);
            g.fillOval(735, 90, 40, 35);
            g.fillOval(700, 120, 20, 18);

            g.setColor(new Color(20, 27, 55));
            int[] mountainX = {0, 100, 180, 270, 370, 480, 570, 690, 800, 900};
            int[] mountainY = {325, 260, 310, 245, 300, 255, 310, 235, 290, 260};
            g.fillPolygon(mountainX, mountainY, mountainX.length);
            
            g.setColor(new Color(44, 48, 72));
            g.fillRect(0, GROUND, SCENE_WIDTH, SCENE_HEIGHT - GROUND);

            g.setColor(new Color(68, 73, 97));
            for (int i = 0; i < 18; i++) {
                int x = i * 59 - 10;
                int y = GROUND + 20 + (i % 3) * 15;
                g.drawOval(x, y, 25 + i % 4 * 7, 7 + i % 3 * 3);
            }

            g.setColor(new Color(125, 130, 153));
            g.drawLine(0, GROUND, SCENE_WIDTH, GROUND);

            g.setFont(new Font("Monospaced", Font.BOLD, 13));
            g.setColor(new Color(200, 215, 255));
            g.drawString("EARTH'S MOON - ARENA 01", 18, 28);
        }

        private void drawHealthBars(Graphics2D g) {
            if (playerOne == null || playerTwo == null) {
                return;
            }

            g.setFont(new Font("Monospaced", Font.BOLD, 15));
            g.setColor(Color.WHITE);
            g.drawString("P1  " + playerOne.name, 28, 58);
            g.drawString("P2  " + playerTwo.name, 715, 58);

            g.setColor(Color.DARK_GRAY);
            g.fillRect(28, 68, 340, 20);
            g.fillRect(532, 68, 340, 20);

            g.setColor(new Color(55, 235, 115));
            g.fillRect(28, 68, playerOne.hp * 340 / 100, 20);
            g.fillRect(872 - playerTwo.hp * 340 / 100, 68,
                    playerTwo.hp * 340 / 100, 20);

            g.setColor(Color.WHITE);
            g.drawRect(28, 68, 340, 20);
            g.drawRect(532, 68, 340, 20);

            g.setColor(new Color(255, 220, 90));
            g.setFont(new Font("Monospaced", Font.BOLD, 18));
            g.drawString("VS", 430, 84);
        }

        private void drawFighter(Graphics2D g, Fighter p) {
            String[] sprite = {
                    "......HHHH......",
                    ".....HHHHHH.....",
                    "....HHHHHHHH....",
                    "....HSSSSSSSH...",
                    "...HSSSSESSSSH..",
                    "...HSSSSSSSSSH..",
                    "....HHHHHHHH....",
                    ".....CCCCCC.....",
                    "...CCCAACCCCCC..",
                    "...CCCCCCCCCC...",
                    "....CCCCCCCC....",
                    ".....CCLLCC.....",
                    ".....CCLLCC.....",
                    "....PP....PP....",
                    "....PP....PP....",
                    "....PP....PP....",
                    "...BBB....BBB...",
                    "...BBB....BBB..."
            };

            int pixel = 4;
            int spriteWidth = 16 * pixel;
            int startX = (int) p.x - 9;
            int startY = (int) p.y + 7;

            Color primary = p.color;
            Color shadow = primary.darker();
            Color skin = new Color(229, 177, 135);
            Color hair = new Color(30, 25, 48);
            Color pants = new Color(37, 40, 66);
            Color boots = new Color(230, 235, 250);

            g.setColor(new Color(0, 0, 0, 100));
            g.fillRect((int) p.x, 411, 42, 5);

            for (int row = 0; row < sprite.length; row++) {
                for (int col = 0; col < sprite[row].length(); col++) {
                    char pixelType = sprite[row].charAt(col);

                    if (pixelType == '.') {
                        continue;
                    }

                    int drawCol = p.face > 0 ? col : 15 - col;
                    int px = startX + drawCol * pixel;
                    int py = startY + row * pixel;

                    switch (pixelType) {
                        case 'H':
                            g.setColor(hair);
                            break;
                        case 'S':
                            g.setColor(skin);
                            break;
                        case 'E':
                            g.setColor(Color.BLACK);
                            break;
                        case 'C':
                            g.setColor(primary);
                            break;
                        case 'A':
                            g.setColor(shadow);
                            break;
                        case 'L':
                            g.setColor(Color.WHITE);
                            break;
                        case 'P':
                            g.setColor(pants);
                            break;
                        case 'B':
                            g.setColor(boots);
                            break;
                        default:
                            continue;
                    }

                    g.fillRect(px, py, pixel, pixel);
                }
            }

            if (p.attack > 0) {
                int fistX = p.face > 0
                        ? startX + spriteWidth + 4
                        : startX - 12;

                int fistY = startY + 9 * pixel;

                g.setColor(skin);
                g.fillRect(fistX, fistY, 8, 8);
                g.fillRect(fistX + 4, fistY - 4, 8, 8);

                g.setColor(primary);
                g.fillRect(fistX + 4, fistY + 8, 8, 4);
            }

            g.setFont(new Font("Monospaced", Font.BOLD, 12));
            g.setColor(Color.WHITE);

            FontMetrics fm = g.getFontMetrics();
            int nameWidth = fm.stringWidth(p.name);

            g.drawString(
                    p.name,
                    startX + (spriteWidth - nameWidth) / 2,
                    startY - 5
            );
        }

        private void drawIntro(Graphics2D g) {
            g.setFont(new Font("Monospaced", Font.BOLD, 34));
            g.setColor(Color.WHITE);
            String title = "MOONFIGHTER";
            FontMetrics fm = g.getFontMetrics();
            g.drawString(title, (SCENE_WIDTH - fm.stringWidth(title)) / 2, 220);

            g.setFont(new Font("Monospaced", Font.BOLD, 16));
            g.setColor(new Color(130, 220, 255));
            String subtitle = "A LOW BUDGET TEKKEN CLONE";
            fm = g.getFontMetrics();
            g.drawString(subtitle, (SCENE_WIDTH - fm.stringWidth(subtitle)) / 2, 252);
        }

        private void drawOverlay(Graphics2D g, String title, String subtitle) {
            g.setColor(new Color(0, 0, 0, 185));
            g.fillRect(0, 0, SCENE_WIDTH, SCENE_HEIGHT);

            g.setFont(new Font("Monospaced", Font.BOLD, 38));
            g.setColor(Color.WHITE);
            FontMetrics fm = g.getFontMetrics();
            g.drawString(title, (SCENE_WIDTH - fm.stringWidth(title)) / 2, 225);

            g.setFont(new Font("Monospaced", Font.BOLD, 15));
            g.setColor(new Color(140, 220, 255));
            fm = g.getFontMetrics();
            g.drawString(subtitle, (SCENE_WIDTH - fm.stringWidth(subtitle)) / 2, 260);
        }
    }

    private static class Fighter {

        String name;
        Color color;
        double x;
        double y;
        double vx;
        double vy;
        int w = 46;
        int h = 86;
        int hp = 100;
        int face;
        boolean ground = true;
        int attack;
        int cooldown;
        int invincible;
        Fighter opponent;

        Fighter(String name, double x, Color color, int face) {
            this.name = name;
            this.x = x;
            this.y = 330;
            this.color = color;
            this.face = face;
        }
    }
}