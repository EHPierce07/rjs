import java.awt.*;
import java.awt.event.*;
import javax.swing.*;

public class DunkAnimation extends JFrame {

    private final GraphicsDevice device =
            GraphicsEnvironment.getLocalGraphicsEnvironment()
                    .getDefaultScreenDevice();

    private boolean fullscreen = false;
    private Rectangle previousBounds;

    private final GamePanel panel = new GamePanel();

    public DunkAnimation() {
        setTitle("Basketball Dunk Challenge");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setResizable(true);

        add(panel);
        pack();
        setMinimumSize(new Dimension(800, 500));
        setLocationRelativeTo(null);

        setVisible(true);
        panel.requestFocusInWindow();
    }

    private void toggleFullscreen() {
        if (!fullscreen) {
            previousBounds = getBounds();

            dispose();
            setUndecorated(true);
            fullscreen = true;

            device.setFullScreenWindow(this);
            setVisible(true);
        } else {
            exitFullscreen();
        }

        panel.requestFocusInWindow();
        panel.repaint();
    }

    private void exitFullscreen() {
        if (!fullscreen) {
            return;
        }

        device.setFullScreenWindow(null);
        dispose();
        setUndecorated(false);
        fullscreen = false;

        setBounds(previousBounds);
        setVisible(true);
        panel.requestFocusInWindow();
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(DunkAnimation::new);
    }

    class GamePanel extends JPanel implements ActionListener {

        // Virtual scene size. The graphics scale to the screen.
        private static final int SCENE_WIDTH = 1000;
        private static final int SCENE_HEIGHT = 600;

        private static final int FLOOR_Y = 455;
        private static final int GROUND_Y = 370;

        private static final int HOOP_X = 800;
        private static final int HOOP_Y = 250;

        private static final int BALL_SIZE = 24;

        private final Timer timer = new Timer(16, this);

        private int playerX;
        private double playerY;
        private double velocityY;

        private double ballX;
        private double ballY;
        private double ballVelocityX;
        private double ballVelocityY;

        private boolean jumping;
        private boolean ballReleased;
        private boolean dunked;
        private boolean finished;
        private boolean paused;

        private int score;
        private int flash = 0;

        private final double gravity = 0.32;

        public GamePanel() {
            setPreferredSize(new Dimension(1000, 600));
            setBackground(new Color(8, 13, 30));
            setFocusable(true);

            setupControls();
            restart();
        }

        private void setupControls() {
            bindKey("F", "fullscreen", () -> toggleFullscreen());
            bindKey("ESCAPE", "exitFullscreen", () -> exitFullscreen());
            bindKey("R", "restart", () -> restart());
            bindKey("SPACE", "pause", () -> {
                if (!finished) {
                    paused = !paused;
                }
                repaint();
            });
        }

        private void bindKey(
                String key, String name, Runnable action) {

            getInputMap(WHEN_IN_FOCUSED_WINDOW).put(
                    KeyStroke.getKeyStroke(key), name);

            getActionMap().put(name, new AbstractAction() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    action.run();
                }
            });
        }

        private void restart() {
            playerX = 75;
            playerY = GROUND_Y;
            velocityY = 0;

            ballX = playerX + 45;
            ballY = playerY + 20;
            ballVelocityX = 0;
            ballVelocityY = 0;

            jumping = false;
            ballReleased = false;
            dunked = false;
            finished = false;
            paused = false;

            score = 0;
            flash = 0;

            timer.start();
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);

            Graphics2D screen = (Graphics2D) g.create();

            screen.setRenderingHint(
                    RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_ON);

            screen.setRenderingHint(
                    RenderingHints.KEY_RENDERING,
                    RenderingHints.VALUE_RENDER_QUALITY);

            double scale = Math.min(
        getWidth() / (double) SCENE_WIDTH,
        getHeight() / (double) SCENE_HEIGHT);

double offsetX =
        (getWidth() - SCENE_WIDTH * scale) / 2.0;

double offsetY =
        (getHeight() - SCENE_HEIGHT * scale) / 2.0;

            screen.translate(offsetX, offsetY);
            screen.scale(scale, scale);

            drawBackground(screen);
            drawCrowd(screen);
            drawCourt(screen);
            drawHoop(screen);
            drawPlayer(screen);
            drawBall(screen);
            drawFrontNet(screen);
            drawHUD(screen);

            screen.dispose();
        }

        private void drawBackground(Graphics2D g) {
            GradientPaint sky = new GradientPaint(
                    0, 0, new Color(7, 12, 32),
                    0, FLOOR_Y, new Color(48, 57, 86));

            g.setPaint(sky);
            g.fillRect(0, 0, SCENE_WIDTH, SCENE_HEIGHT);

            // Arena lights
            int[] lightX = {100, 300, 500, 700, 900};

            for (int x : lightX) {
                g.setColor(new Color(255, 255, 220, 18));
                g.fillOval(x - 65, 25, 130, 130);

                g.setColor(new Color(255, 255, 225, 100));
                g.fillRoundRect(x - 34, 35, 68, 10, 8, 8);
            }

            // Arena upper deck
            g.setColor(new Color(19, 23, 42));
            g.fillRect(0, 105, SCENE_WIDTH, 225);

            g.setColor(new Color(50, 58, 85));
            g.fillRect(0, 105, SCENE_WIDTH, 8);

            g.setColor(new Color(110, 120, 155));
            g.drawLine(0, 140, SCENE_WIDTH, 140);
        }

        private void drawCrowd(Graphics2D g) {
            // Crowd silhouettes
            for (int i = 0; i < 65; i++) {
                int x = i * 16;
                int y = 170 + (i % 4) * 9;
                int head = 5 + (i % 3);

                g.setColor(new Color(11, 14, 27));
                g.fillOval(x, y, head * 2, head * 2);
                g.fillRoundRect(
                        x - 3, y + head, head * 2 + 6, 35, 8, 8);

                if (i % 5 == 0) {
                    g.setColor(new Color(190, 55, 70));
                    g.fillRect(x + 2, y + 17, 5, 12);
                } else if (i % 4 == 0) {
                    g.setColor(new Color(60, 130, 210));
                    g.fillRect(x + 2, y + 17, 5, 12);
                }
            }

            // Lower crowd
            g.setColor(new Color(9, 12, 23));
            g.fillRect(0, 300, SCENE_WIDTH, 75);

            for (int x = 5; x < SCENE_WIDTH; x += 23) {
                g.setColor(new Color(20, 25, 42));
                g.fillOval(x, 305 + (x % 3) * 5, 18, 18);
                g.fillRoundRect(x - 3, 320, 24, 45, 8, 8);
            }
        }

        private void drawCourt(Graphics2D g) {
            // Court floor
            GradientPaint wood = new GradientPaint(
                    0, FLOOR_Y, new Color(209, 139, 77),
                    0, SCENE_HEIGHT, new Color(118, 67, 40));

            g.setPaint(wood);
            g.fillRect(0, FLOOR_Y, SCENE_WIDTH,
                    SCENE_HEIGHT - FLOOR_Y);

            g.setColor(new Color(255, 224, 175));
            g.setStroke(new BasicStroke(3));
            g.drawLine(0, FLOOR_Y, SCENE_WIDTH, FLOOR_Y);

            // Court markings
            g.setColor(new Color(255, 235, 200, 180));
            g.setStroke(new BasicStroke(3));

            g.drawLine(0, FLOOR_Y + 90, SCENE_WIDTH, FLOOR_Y + 90);
            g.drawOval(340, FLOOR_Y + 20, 300, 220);
            g.drawLine(110, FLOOR_Y, 110, SCENE_HEIGHT);
            g.drawLine(890, FLOOR_Y, 890, SCENE_HEIGHT);

            // Key area near the hoop
            g.drawRect(670, FLOOR_Y, 220, 110);
            g.drawArc(630, FLOOR_Y - 40, 300, 180, 180, 180);

            // Floor shine
            g.setColor(new Color(255, 255, 255, 18));
            for (int x = 0; x < SCENE_WIDTH; x += 70) {
                g.drawLine(x, FLOOR_Y + 12, x + 35, FLOOR_Y + 12);
            }
        }

        private void drawHoop(Graphics2D g) {
            // Support pole
            g.setColor(new Color(165, 173, 190));
            g.fillRoundRect(HOOP_X + 67, HOOP_Y - 55,
                    13, FLOOR_Y - HOOP_Y + 55, 5, 5);

            g.setColor(new Color(95, 104, 125));
            g.fillRect(HOOP_X + 28, FLOOR_Y - 12, 90, 12);

            // Backboard
            g.setColor(new Color(220, 231, 244));
            g.fillRoundRect(
                    HOOP_X + 20, HOOP_Y - 75, 14, 115, 5, 5);

            g.setColor(Color.WHITE);
            g.setStroke(new BasicStroke(3));
            g.drawRect(HOOP_X + 13, HOOP_Y - 45, 20, 34);

            // Rim is drawn again in front of the ball later.
            g.setColor(new Color(255, 105, 35));
            g.setStroke(new BasicStroke(5));
            g.drawOval(HOOP_X - 25, HOOP_Y + 10, 65, 15);
        }

        private void drawFrontNet(Graphics2D g) {
            // Draw the net in front of the ball after it passes the rim.
            g.setColor(new Color(245, 245, 255, 210));
            g.setStroke(new BasicStroke(1.5f));

            int topY = HOOP_Y + 22;
            int bottomY = HOOP_Y + 72;

            for (int i = 0; i <= 5; i++) {
                int topX = HOOP_X - 21 + i * 11;
                int bottomX = HOOP_X - 10 + i * 8;

                g.drawLine(topX, topY, bottomX, bottomY);
            }

            g.drawLine(HOOP_X - 10, bottomY,
                    HOOP_X + 30, bottomY);

            // Front edge of the rim
            g.setColor(new Color(255, 105, 35));
            g.setStroke(new BasicStroke(4));
            g.drawArc(HOOP_X - 25, HOOP_Y + 10, 65, 15, 180, 180);
        }

        private void drawPlayer(Graphics2D g) {
            int px = playerX;
            int py = (int) playerY;

            // Shadow moves closer to the player while jumping.
            int shadowWidth = jumping ? 30 : 48;

            g.setColor(new Color(0, 0, 0, 90));
            g.fillOval(px + 5, FLOOR_Y - 5, shadowWidth, 10);

            // Legs
            g.setColor(new Color(25, 27, 39));

            if (jumping) {
                g.fillRoundRect(px + 12, py + 51, 12, 23, 6, 6);
                g.fillRoundRect(px + 32, py + 49, 12, 23, 6, 6);
            } else {
                g.fillRoundRect(px + 15, py + 51, 10, 28, 5, 5);
                g.fillRoundRect(px + 31, py + 51, 10, 28, 5, 5);
            }

            // Shoes
            g.setColor(Color.WHITE);
            g.fillRoundRect(px + 7, py + 74, 22, 9, 4, 4);
            g.fillRoundRect(px + 28, py + 74, 22, 9, 4, 4);

            // Jersey
            g.setColor(new Color(215, 35, 55));
            g.fillRoundRect(px + 10, py + 19, 36, 39, 8, 8);

            g.setColor(Color.WHITE);
            g.setFont(new Font("Arial", Font.BOLD, 15));
            g.drawString("10", px + 20, py + 43);

            // Arms
            g.setColor(new Color(166, 108, 75));
            g.setStroke(new BasicStroke(
                    9, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));

            if (jumping && !ballReleased) {
                g.drawLine(px + 39, py + 29, px + 51, py + 2);
            } else {
                g.drawLine(px + 13, py + 29, px + 2, py + 43);
                g.drawLine(px + 42, py + 29, px + 53, py + 40);
            }

            // Head and hair
            g.setColor(new Color(166, 108, 75));
            g.fillOval(px + 14, py - 7, 29, 29);

            g.setColor(new Color(35, 23, 18));
            g.fillArc(px + 13, py - 9, 31, 20, 0, 180);
        }

        private void drawBall(Graphics2D g) {
            int bx = (int) ballX;
            int by = (int) ballY;

            g.setColor(new Color(255, 128, 15));
            g.fillOval(bx, by, BALL_SIZE, BALL_SIZE);

            g.setColor(new Color(75, 39, 15));
            g.setStroke(new BasicStroke(2));
            g.drawOval(bx, by, BALL_SIZE, BALL_SIZE);
            g.drawArc(bx + 5, by, 14, BALL_SIZE, 90, 180);
            g.drawArc(bx, by + 5, BALL_SIZE, 14, 0, 180);
        }

        private void drawHUD(Graphics2D g) {
            // Header
            g.setColor(new Color(0, 0, 0, 110));
            g.fillRoundRect(20, 18, 355, 75, 15, 15);

            g.setFont(new Font("Arial", Font.BOLD, 27));
            g.setColor(Color.WHITE);
            g.drawString("DUNK CHALLENGE", 35, 51);

            g.setFont(new Font("Arial", Font.PLAIN, 15));
            g.setColor(new Color(210, 220, 245));
            g.drawString("PLAYER #10", 37, 76);

            // Scoreboard
            g.setColor(new Color(5, 10, 25, 210));
            g.fillRoundRect(795, 20, 180, 72, 12, 12);

            g.setColor(new Color(255, 190, 55));
            g.setFont(new Font("Arial", Font.BOLD, 14));
            g.drawString("SCORE", 815, 44);

            g.setColor(Color.WHITE);
            g.setFont(new Font("Arial", Font.BOLD, 30));
            g.drawString(String.valueOf(score), 815, 77);

            if (dunked) {
                g.setFont(new Font("Arial", Font.BOLD, 48));
                g.setColor(Color.YELLOW);
                g.drawString("SLAM DUNK!", 300, 130);

                g.setFont(new Font("Arial", Font.BOLD, 18));
                g.setColor(Color.WHITE);
                g.drawString("+100 POINTS", 425, 160);
            } else if (finished) {
                g.setFont(new Font("Arial", Font.BOLD, 32));
                g.setColor(Color.WHITE);
                g.drawString("ATTEMPT COMPLETE", 300, 130);
            } else if (!ballReleased) {
                g.setFont(new Font("Arial", Font.BOLD, 18));
                g.setColor(new Color(255, 195, 75));
                g.drawString("DRIVE TO THE HOOP!", 350, 105);
            }

            if (paused) {
                g.setColor(new Color(0, 0, 0, 175));
                g.fillRect(0, 0, SCENE_WIDTH, SCENE_HEIGHT);

                g.setColor(Color.WHITE);
                g.setFont(new Font("Arial", Font.BOLD, 48));
                g.drawString("PAUSED", 400, 280);

                g.setFont(new Font("Arial", Font.PLAIN, 20));
                g.drawString("Press SPACE to resume", 375, 320);
            }

            // Controls
            g.setColor(new Color(255, 255, 255, 210));
            g.setFont(new Font("Arial", Font.PLAIN, 14));
            g.drawString("F: Fullscreen   ESC: Exit fullscreen"
                    + "   SPACE: Pause   R: Restart", 25, 580);
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            if (paused || finished) {
                repaint();
                return;
            }

            // Player runs toward the basket.
            if (!jumping && !ballReleased) {
                if (playerX < HOOP_X - 100) {
                    playerX += 4;
                } else {
                    jumping = true;
                    velocityY = -12.5;
                }

                ballX = playerX + 45;
                ballY = playerY + 20;
            }

            if (jumping) {
                playerY += velocityY;
                velocityY += gravity;

                if (!ballReleased) {
                    ballX = playerX + 40;
                    ballY = playerY + 5;
                }

                if (!ballReleased && velocityY > -2.0) {
                    ballReleased = true;

                    ballX = playerX + 40;
                    ballY = playerY + 5;

                    double targetX = HOOP_X + 7;
                    double targetY = HOOP_Y + 34;
                    double travelFrames = 19.0;

                    ballVelocityX =
                            (targetX - ballX) / travelFrames;

                    ballVelocityY =
                            (targetY - ballY
                            - 0.5 * gravity
                            * travelFrames * travelFrames)
                            / travelFrames;
                }

                if (playerY >= GROUND_Y) {
                    playerY = GROUND_Y;
                    velocityY = 0;
                    jumping = false;
                }
            }

            if (ballReleased) {
                double previousBallCenterY = ballY + BALL_SIZE / 2.0;

                ballX += ballVelocityX;
                ballY += ballVelocityY;
                ballVelocityY += gravity;

                double currentBallCenterY = ballY + BALL_SIZE / 2.0;

                double rimCrossingY = HOOP_Y + 24;
                double openingLeft = HOOP_X - 13;
                double openingRight = HOOP_X + 29;
                double ballCenterX = ballX + BALL_SIZE / 2.0;

                boolean crossingDownward =
                        previousBallCenterY < rimCrossingY
                        && currentBallCenterY >= rimCrossingY
                        && ballVelocityY > 0;

                boolean insideOpening =
                        ballCenterX > openingLeft
                        && ballCenterX < openingRight;

                if (!dunked && crossingDownward && insideOpening) {
                    dunked = true;
                    score += 100;
                    flash = 18;
                }

                if (!dunked && ballX > SCENE_WIDTH + 50) {
                    finished = true;
                }
            }
        
            if (!jumping && ballReleased
                    && (ballY > FLOOR_Y || ballY > SCENE_HEIGHT)) {
                finished = true;
            }      

            if (flash > 0) {
                flash--;
            }

            if (finished) {
                timer.stop();
            }

            repaint();
        }
    }
}