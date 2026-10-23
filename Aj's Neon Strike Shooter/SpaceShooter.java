import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Random;
import javax.swing.*;

public class SpaceShooter extends JPanel implements ActionListener {

    // Window and game settings
    static final int WIDTH = 1000;
    static final int HEIGHT = 600;

    JFrame frame;
    Timer timer = new Timer(16, this);
    Random random = new Random();

    // Player
    double playerX = WIDTH / 2.0;
    double playerY = HEIGHT - 90;
    double aimAngle = -Math.PI / 2;
    int playerSpeed = 6;
    int health = 100;
    int score = 0;
    int kills = 0;

    boolean left, right, up, down;
    boolean mouseShooting = false;
    boolean gameOver = false;
    boolean paused = false;

    // Game objects
    ArrayList<Bullet> bullets = new ArrayList<>();
    ArrayList<Enemy> enemies = new ArrayList<>();
    ArrayList<Particle> particles = new ArrayList<>();

    int spawnCounter = 0;
    int fireCooldown = 0;
    int screenShake = 0;

    Color cyan = new Color(0, 245, 255);
    Color pink = new Color(255, 45, 150);
    Color purple = new Color(155, 70, 255);

    public SpaceShooter() {
        setPreferredSize(new Dimension(WIDTH, HEIGHT));
        setBackground(new Color(10, 9, 25));
        setFocusable(true);

        frame = new JFrame("NEON STRIKE | Urban Defense");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setLayout(new BorderLayout());
        frame.add(this, BorderLayout.CENTER);

        frame.pack();
        frame.setLocationRelativeTo(null);
        frame.setMinimumSize(new Dimension(800, 500));
        frame.setExtendedState(JFrame.MAXIMIZED_BOTH);
        frame.setVisible(true);

        setupControls();
        timer.start();
    }

    // Keyboard and mouse controls
    private void setupControls() {
        bind("A", "leftOn", () -> left = true);
        bind("D", "rightOn", () -> right = true);
        bind("W", "upOn", () -> up = true);
        bind("S", "downOn", () -> down = true);

        bind("LEFT", "leftArrowOn", () -> left = true);
        bind("RIGHT", "rightArrowOn", () -> right = true);
        bind("UP", "upArrowOn", () -> up = true);
        bind("DOWN", "downArrowOn", () -> down = true);

        bindRelease("A", "leftOff", () -> left = false);
        bindRelease("D", "rightOff", () -> right = false);
        bindRelease("W", "upOff", () -> up = false);
        bindRelease("S", "downOff", () -> down = false);

        bindRelease("LEFT", "leftArrowOff", () -> left = false);
        bindRelease("RIGHT", "rightArrowOff", () -> right = false);
        bindRelease("UP", "upArrowOff", () -> up = false);
        bindRelease("DOWN", "downArrowOff", () -> down = false);

        bind("ESCAPE", "pause", () -> {
            if (!gameOver) paused = !paused;
        });

        bind("R", "restart", () -> {
            if (gameOver) restartGame();
        });

        addMouseMotionListener(new MouseMotionAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) {
                aimAt(e.getX(), e.getY());
            }

            @Override
            public void mouseDragged(MouseEvent e) {
                aimAt(e.getX(), e.getY());
            }
        });

        addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                if (SwingUtilities.isLeftMouseButton(e)) {
                    mouseShooting = true;
                    aimAt(e.getX(), e.getY());
                    requestFocusInWindow();
                }

                if (gameOver) restartGame();
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                if (SwingUtilities.isLeftMouseButton(e)) {
                    mouseShooting = false;
                }
            }
        });
    }

    private void bind(String key, String name, Runnable action) {
        getInputMap(WHEN_IN_FOCUSED_WINDOW).put(
            KeyStroke.getKeyStroke("pressed " + key), name
        );
        getActionMap().put(name, new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                action.run();
            }
        });
    }

    private void bindRelease(String key, String name, Runnable action) {
        getInputMap(WHEN_IN_FOCUSED_WINDOW).put(
            KeyStroke.getKeyStroke("released " + key), name
        );
        getActionMap().put(name, new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                action.run();
            }
        });
    }

    private void aimAt(int mx, int my) {
        aimAngle = Math.atan2(my - playerY, mx - playerX);
    }

    // Main game loop
    @Override
    public void actionPerformed(ActionEvent e) {
        if (!paused && !gameOver) {
            updateGame();
        }
        repaint();
    }

    private void updateGame() {
        movePlayer();

        if (fireCooldown > 0) fireCooldown--;
        if (mouseShooting && fireCooldown == 0) {
            shoot();
            fireCooldown = 10;
        }

        spawnCounter++;
        int spawnDelay = Math.max(18, 55 - score / 100);

        if (spawnCounter >= spawnDelay) {
            spawnEnemy();
            spawnCounter = 0;
        }

        updateBullets();
        updateEnemies();
        updateParticles();

        if (screenShake > 0) screenShake--;

        if (health <= 0) {
            health = 0;
            gameOver = true;
            mouseShooting = false;
        }
    }

    private void movePlayer() {
        double dx = 0;
        double dy = 0;

        if (left) dx--;
        if (right) dx++;
        if (up) dy--;
        if (down) dy++;

        if (dx != 0 && dy != 0) {
            dx *= 0.7071;
            dy *= 0.7071;
        }

        playerX += dx * playerSpeed;
        playerY += dy * playerSpeed;

        playerX = Math.max(24, Math.min(getWidth() - 24, playerX));
        playerY = Math.max(70, Math.min(getHeight() - 25, playerY));
    }

    private void shoot() {
        double muzzleX = playerX + Math.cos(aimAngle) * 30;
        double muzzleY = playerY + Math.sin(aimAngle) * 30;

        bullets.add(new Bullet(
            muzzleX, muzzleY,
            Math.cos(aimAngle) * 12,
            Math.sin(aimAngle) * 12
        ));

        for (int i = 0; i < 3; i++) {
            particles.add(new Particle(
                muzzleX, muzzleY, cyan, 2
            ));
        }
    }

    private void spawnEnemy() {
        int type = random.nextInt(100);
        int kind = type < 55 ? 0 : type < 82 ? 1 : 2;

        double ex = 30 + random.nextDouble()
            * Math.max(1, getWidth() - 60);

        double ey = -40;

        enemies.add(new Enemy(ex, ey, kind));
    }

    private void updateBullets() {
        Iterator<Bullet> it = bullets.iterator();

        while (it.hasNext()) {
            Bullet b = it.next();
            b.x += b.vx;
            b.y += b.vy;
            b.life--;

            if (b.life <= 0 || b.x < 0 || b.x > getWidth()
                    || b.y < 0 || b.y > getHeight()) {
                it.remove();
                continue;
            }

            boolean hit = false;

            for (Enemy enemy : enemies) {
                if (enemy.dead) continue;

                double distance = Point2D.distance(
                    b.x, b.y, enemy.x, enemy.y
                );

                if (distance < enemy.radius + 5) {
                    enemy.hp--;
                    hit = true;

                    burst(b.x, b.y, cyan, 5);

                    if (enemy.hp <= 0) {
                        enemy.dead = true;
                        score += enemy.points;
                        kills++;
                        burst(enemy.x, enemy.y, enemy.color, 16);
                    }
                    break;
                }
            }

            if (hit) it.remove();
        }

        enemies.removeIf(enemy -> enemy.dead);
    }

    private void updateEnemies() {
        for (Enemy enemy : enemies) {
            enemy.y += enemy.speed;

            // Fast drones drift sideways.
            if (enemy.kind == 0) {
                enemy.x += Math.sin(enemy.y * 0.035) * 1.5;
            }

            // Enemies that reach the bottom damage the player.
            if (enemy.y > getHeight() + 35) {
                enemy.dead = true;
                health -= enemy.kind == 1 ? 20 : 10;
                screenShake = 5;
            }

            // Enemies collide with the player.
            if (Point2D.distance(
                    playerX, playerY, enemy.x, enemy.y
                ) < enemy.radius + 18) {
                enemy.dead = true;
                health -= enemy.kind == 1 ? 25 : 15;
                screenShake = 8;
                burst(enemy.x, enemy.y, pink, 18);
            }
        }

        enemies.removeIf(enemy -> enemy.dead);
    }

    private void updateParticles() {
        Iterator<Particle> it = particles.iterator();

        while (it.hasNext()) {
            Particle p = it.next();
            p.x += p.vx;
            p.y += p.vy;
            p.life--;
            p.vx *= 0.96;
            p.vy *= 0.96;

            if (p.life <= 0) it.remove();
        }
    }

    private void burst(double x, double y, Color color, int count) {
        for (int i = 0; i < count; i++) {
            particles.add(new Particle(x, y, color, 20));
        }
    }

    private void restartGame() {
        bullets.clear();
        enemies.clear();
        particles.clear();

        playerX = getWidth() / 2.0;
        playerY = getHeight() - 90;
        health = 100;
        score = 0;
        kills = 0;
        spawnCounter = 0;
        fireCooldown = 0;

        gameOver = false;
        paused = false;
        mouseShooting = false;
        left = right = up = down = false;

        requestFocusInWindow();
    }

    // Drawing
    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(
            RenderingHints.KEY_ANTIALIASING,
            RenderingHints.VALUE_ANTIALIAS_ON
        );

        if (screenShake > 0) {
            g2.translate(
                random.nextInt(7) - 3,
                random.nextInt(7) - 3
            );
        }

        drawCity(g2);

        for (Particle p : particles) p.draw(g2);
        for (Bullet b : bullets) b.draw(g2);
        for (Enemy enemy : enemies) enemy.draw(g2);

        drawPlayer(g2);
        drawHUD(g2);

        if (paused) drawOverlay(g2, "PAUSED", "Press ESC to resume");
        if (gameOver) drawOverlay(g2, "SYSTEM FAILURE",
                                   "Click or press R to restart");

        g2.dispose();
    }

    private void drawCity(Graphics2D g2) {
        int w = getWidth();
        int h = getHeight();

        GradientPaint bg = new GradientPaint(
            0, 0, new Color(12, 8, 28),
            0, h, new Color(35, 10, 45)
        );
        g2.setPaint(bg);
        g2.fillRect(0, 0, w, h);

        // Distant city skyline.
        for (int i = 0; i < w; i += 75) {
            int buildingH = 90 + (i * 37 % 160);

            g2.setColor(new Color(20, 18, 43));
            g2.fillRect(i, h - buildingH, 62, buildingH);

            g2.setColor(new Color(65, 35, 85));

            for (int wy = h - buildingH + 12; wy < h - 8; wy += 20) {
                for (int wx = i + 9; wx < i + 58; wx += 17) {
                    g2.fillRect(wx, wy, 5, 8);
                }
            }
        }

        // Neon road.
        int roadTop = h - 170;
        g2.setColor(new Color(13, 12, 27));
        g2.fillRect(0, roadTop, w, h - roadTop);

        g2.setColor(new Color(255, 35, 145));
        g2.setStroke(new BasicStroke(2));
        g2.drawLine(0, roadTop, w, roadTop);

        g2.setColor(new Color(0, 210, 255, 100));
        for (int i = 0; i < w; i += 80) {
            g2.drawLine(i, roadTop, i - 35, h);
        }

        // Road markings.
        g2.setColor(new Color(255, 65, 170));
        for (int i = 0; i < w; i += 130) {
            g2.fillRoundRect(i, h - 70, 65, 4, 4, 4);
        }

        // Decorative signs.
        g2.setFont(new Font("Monospaced", Font.BOLD, 14));
        g2.setColor(cyan);
        g2.drawString("CITY SECTOR 07", 22, 90);
        g2.setColor(pink);
        g2.drawString("DANGER // ACTIVE", 22, 112);

        g2.setColor(new Color(255, 255, 255, 22));
        for (int y = 0; y < h; y += 5) {
            g2.drawLine(0, y, w, y);
        }
    }

    private void drawPlayer(Graphics2D g2) {
        Graphics2D p = (Graphics2D) g2.create();
        p.translate(playerX, playerY);

        // Weapon rotates toward the mouse.
        p.rotate(aimAngle);

        p.setColor(new Color(0, 245, 255, 60));
        p.fillRoundRect(0, -8, 42, 16, 8, 8);

        p.setColor(cyan);
        p.fillRoundRect(0, -4, 38, 8, 5, 5);

        p.setColor(Color.WHITE);
        p.fillRect(25, -2, 12, 4);

        p.dispose();

        // Hover vehicle body.
        g2.setColor(new Color(0, 245, 255, 45));
        g2.fillOval((int) playerX - 30, (int) playerY - 22, 60, 44);

        Polygon ship = new Polygon();
        ship.addPoint((int) playerX, (int) playerY - 22);
        ship.addPoint((int) playerX + 22, (int) playerY + 10);
        ship.addPoint((int) playerX, (int) playerY + 17);
        ship.addPoint((int) playerX - 22, (int) playerY + 10);

        g2.setColor(new Color(25, 35, 65));
        g2.fillPolygon(ship);

        g2.setColor(cyan);
        g2.setStroke(new BasicStroke(2));
        g2.drawPolygon(ship);

        g2.setColor(Color.WHITE);
        g2.fillOval((int) playerX - 4, (int) playerY - 5, 8, 8);

        g2.setColor(pink);
        g2.fillOval((int) playerX - 17, (int) playerY + 8, 6, 6);
        g2.fillOval((int) playerX + 11, (int) playerY + 8, 6, 6);
    }

    private void drawHUD(Graphics2D g2) {
        g2.setFont(new Font("Monospaced", Font.BOLD, 16));

        g2.setColor(new Color(0, 0, 0, 170));
        g2.fillRoundRect(15, 15, 235, 75, 12, 12);

        g2.setColor(cyan);
        g2.drawString("NEON STRIKE", 28, 38);

        g2.setColor(Color.WHITE);
        g2.drawString("SCORE: " + score, 28, 60);

        g2.setColor(new Color(60, 30, 50));
        g2.fillRoundRect(28, 70, 190, 10, 5, 5);

        g2.setColor(health > 30 ? new Color(0, 255, 140) : pink);
        g2.fillRoundRect(28, 70, 190 * health / 100, 10, 5, 5);

        g2.setColor(Color.WHITE);
        g2.setFont(new Font("Monospaced", Font.PLAIN, 12));
        g2.drawString("WASD / ARROWS: MOVE", 18, getHeight() - 35);
        g2.drawString("MOUSE: AIM  |  CLICK: FIRE  |  ESC: PAUSE",
                      18, getHeight() - 17);

        String killsText = "ELIMINATIONS: " + kills;
        FontMetrics fm = g2.getFontMetrics();
        g2.setColor(pink);
        g2.drawString(killsText, getWidth() - fm.stringWidth(killsText) - 22,
                      35);
    }

private void drawOverlay(Graphics2D g2, String title, String subtitle) {
    g2.setColor(new Color(5, 5, 20, 210));
    g2.fillRect(0, 0, getWidth(), getHeight());

    g2.setFont(new Font("Monospaced", Font.BOLD, 38));
    FontMetrics titleMetrics = g2.getFontMetrics();
    int titleX = (getWidth() - titleMetrics.stringWidth(title)) / 2;

    g2.setColor(cyan);
    g2.drawString(title, titleX, getHeight() / 2 - 10);

    g2.setFont(new Font("Monospaced", Font.PLAIN, 16));
    FontMetrics subMetrics = g2.getFontMetrics();
    int subX = (getWidth() - subMetrics.stringWidth(subtitle)) / 2;

    g2.setColor(Color.WHITE);
    g2.drawString(subtitle, subX, getHeight() / 2 + 30);
}

    // Main method
    public static void main(String[] args) {
        SwingUtilities.invokeLater(SpaceShooter::new);
    }

    // Game object classes
    class Bullet {
        double x, y, vx, vy;
        int life = 100;

        Bullet(double x, double y, double vx, double vy) {
            this.x = x;
            this.y = y;
            this.vx = vx;
            this.vy = vy;
        }

        void draw(Graphics2D g) {
            g.setColor(new Color(0, 245, 255, 65));
            g.fillOval((int) x - 7, (int) y - 7, 14, 14);
            g.setColor(Color.WHITE);
            g.fillOval((int) x - 3, (int) y - 3, 6, 6);
        }
    }

    class Enemy {
        double x, y, speed;
        int kind, hp, points, radius;
        boolean dead = false;
        Color color;

        Enemy(double x, double y, int kind) {
            this.x = x;
            this.y = y;
            this.kind = kind;

            if (kind == 0) {
                // Fast red drone.
                speed = 2.8 + random.nextDouble() * 1.4;
                hp = 1;
                points = 10;
                radius = 15;
                color = pink;
            } else if (kind == 1) {
                // Heavy armored machine.
                speed = 1.3 + random.nextDouble();
                hp = 3;
                points = 30;
                radius = 21;
                color = purple;
            } else {
                // Small, fast triangular scout.
                speed = 4.2 + random.nextDouble();
                hp = 1;
                points = 20;
                radius = 12;
                color = new Color(255, 170, 35);
            }
        }

        void draw(Graphics2D g) {
            g.setColor(new Color(color.getRed(), color.getGreen(),
                                 color.getBlue(), 55));
            g.fillOval((int) x - radius - 7, (int) y - radius - 7,
                       radius * 2 + 14, radius * 2 + 14);

            g.setColor(color);

            if (kind == 0) {
                g.fillOval((int) x - radius, (int) y - radius,
                           radius * 2, radius * 2);
                g.setColor(Color.WHITE);
                g.fillOval((int) x - 5, (int) y - 4, 4, 4);
                g.fillOval((int) x + 2, (int) y - 4, 4, 4);
            } else if (kind == 1) {
                g.fillRoundRect((int) x - radius, (int) y - radius,
                                radius * 2, radius * 2, 8, 8);
                g.setColor(Color.WHITE);
                g.drawRect((int) x - 8, (int) y - 8, 16, 16);
            } else {
                Polygon triangle = new Polygon();
                triangle.addPoint((int) x, (int) y + radius);
                triangle.addPoint((int) x - radius, (int) y - radius);
                triangle.addPoint((int) x + radius, (int) y - radius);
                g.fillPolygon(triangle);
            }

            if (kind == 1) {
                g.setColor(Color.WHITE);
                g.fillRect((int) x - 12, (int) y - radius - 7,
                           24 * hp / 3, 3);
            }
        }
    }

    class Particle {
        double x, y, vx, vy;
        int life, maxLife;
        Color color;

        Particle(double x, double y, Color color, int life) {
            this.x = x;
            this.y = y;
            this.color = color;
            this.life = life;
            this.maxLife = life;

            double angle = random.nextDouble() * Math.PI * 2;
            double speed = 1 + random.nextDouble() * 4;

            vx = Math.cos(angle) * speed;
            vy = Math.sin(angle) * speed;
        }

        void draw(Graphics2D g) {
            int alpha = Math.max(0, 255 * life / maxLife);
            g.setColor(new Color(color.getRed(), color.getGreen(),
                                 color.getBlue(), alpha));

            int size = Math.max(1, 5 * life / maxLife);
            g.fillOval((int) x - size / 2, (int) y - size / 2,
                       size, size);
        }
    }
}