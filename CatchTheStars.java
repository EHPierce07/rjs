import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Random;

public class CatchTheStars extends JPanel implements ActionListener {

    private static final int WIDTH = 900;
    private static final int HEIGHT = 650;
    private static final int TARGET = 20;

    private final Random random = new Random();
    private final Timer timer = new Timer(16, this);
    private final ArrayList<FallingObject> objects = new ArrayList<>();
    private final ArrayList<Twinkle> twinkles = new ArrayList<>();

    private int basketX = 420;
    private int score = 0;
    private int frameCount = 0;
    private int owlX = -150;
    private int owlY = 145;
    private int owlDirection = 1;
    private int owlDelay = 0;
    private int screenShake = 0;

    private boolean leftPressed;
    private boolean rightPressed;
    private boolean gameStarted = false;
    private boolean paused = false;
    private boolean gameOver = false;
    private boolean won = false;
    private boolean owlActive = false;

    private String message = "";
    private float boyBounce = 0;

    private final Font titleFont = new Font("Serif", Font.BOLD, 43);
    private final Font subtitleFont = new Font("Serif", Font.PLAIN, 19);
    private final Font uiFont = new Font("Monospaced", Font.BOLD, 17);
    private final Font smallFont = new Font("Monospaced", Font.PLAIN, 13);

    private final Color night = new Color(10, 14, 43);
    private final Color moonGold = new Color(255, 235, 167);
    private final Color starGold = new Color(255, 221, 88);

    private enum ObjectType {
        STAR, ASTEROID
    }

    private class FallingObject {
        double x;
        double y;
        double speed;
        double rotation;
        double spin;
        int size;
        ObjectType type;

        FallingObject(double x, double y, double speed,
                      int size, ObjectType type) {
            this.x = x;
            this.y = y;
            this.speed = speed;
            this.size = size;
            this.type = type;
            this.rotation = random.nextDouble() * Math.PI * 2;
            this.spin = (random.nextDouble() - 0.5) * 0.07;
        }
    }

    private class Twinkle {
        double x;
        double y;
        double phase;
        double speed;
        int size;

        Twinkle(double x, double y) {
            this.x = x;
            this.y = y;
            this.phase = random.nextDouble() * Math.PI * 2;
            this.speed = 0.025 + random.nextDouble() * 0.06;
            this.size = 1 + random.nextInt(3);
        }
    }

    public CatchTheStars() {
        setPreferredSize(new Dimension(WIDTH, HEIGHT));
        setFocusable(true);
        setDoubleBuffered(true);

        for (int i = 0; i < 145; i++) {
            twinkles.add(new Twinkle(
                    random.nextInt(WIDTH),
                    random.nextInt(HEIGHT - 120)
            ));
        }

        setupControls();
        timer.start();
    }

    private void setupControls() {
        bind("pressed LEFT", "leftDown", () -> leftPressed = true);
        bind("released LEFT", "leftUp", () -> leftPressed = false);
        bind("pressed RIGHT", "rightDown", () -> rightPressed = true);
        bind("released RIGHT", "rightUp", () -> rightPressed = false);

        bind("pressed A", "aDown", () -> leftPressed = true);
        bind("released A", "aUp", () -> leftPressed = false);
        bind("pressed D", "dDown", () -> rightPressed = true);
        bind("released D", "dUp", () -> rightPressed = false);

        bind("pressed SPACE", "space", () -> {
            if (!gameStarted) {
                startGame();
            } else if (!gameOver) {
                paused = !paused;
                leftPressed = false;
                rightPressed = false;
            }
        });

        bind("pressed ENTER", "enter", () -> {
            if (!gameStarted || gameOver) {
                startGame();
            }
        });

        bind("pressed R", "restart", this::startGame);
    }

    private void bind(String keyStroke, String name, Runnable action) {
        getInputMap(WHEN_IN_FOCUSED_WINDOW).put(
                KeyStroke.getKeyStroke(keyStroke), name
        );

        getActionMap().put(name, new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                action.run();
            }
        });
    }

    private void startGame() {
        score = 0;
        frameCount = 0;
        basketX = WIDTH / 2 - 35;
        objects.clear();
        gameStarted = true;
        paused = false;
        gameOver = false;
        won = false;
        owlActive = false;
        owlDelay = 0;
        owlX = -150;
        screenShake = 0;
        leftPressed = false;
        rightPressed = false;
        message = "A new night of collecting begins!";
        repaint();
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        frameCount++;

        for (Twinkle t : twinkles) {
            t.phase += t.speed;
        }

        if (screenShake > 0) {
            screenShake--;
        }

        if (!gameStarted || paused || gameOver) {
            repaint();
            return;
        }

        if (leftPressed) {
            basketX -= 6;
        }

        if (rightPressed) {
            basketX += 6;
        }

        basketX = Math.max(35, Math.min(WIDTH - 105, basketX));

        boyBounce = (float) (Math.sin(frameCount * 0.09) * 2.2);

        if (frameCount % 48 == 0) {
            spawnObject();
        }

        if (frameCount % 240 == 0 && random.nextDouble() < 0.75) {
            spawnObject();
        }

        updateObjects();
        updateOwl();

        if (score >= TARGET) {
            won = true;
            gameOver = true;
            message = "The night sky shines because of you!";
        }

        repaint();
    }

    private void spawnObject() {
        int size = 23 + random.nextInt(12);
        ObjectType type = random.nextDouble() < 0.15
                ? ObjectType.ASTEROID
                : ObjectType.STAR;

        double speed = type == ObjectType.ASTEROID
                ? 3.0 + random.nextDouble() * 2.1
                : 2.0 + random.nextDouble() * 2.2;

        objects.add(new FallingObject(
                40 + random.nextInt(WIDTH - 80),
                -size - random.nextInt(100),
                speed,
                size,
                type
        ));
    }

    private void updateObjects() {
        Iterator<FallingObject> iterator = objects.iterator();

        while (iterator.hasNext()) {
            FallingObject o = iterator.next();

            o.y += o.speed;
            o.rotation += o.spin;

            if (owlActive && Math.abs(o.x - owlX) < 36
                    && Math.abs(o.y - owlY) < 32
                    && o.type == ObjectType.STAR) {
                iterator.remove();
                message = "A cheeky owl stole a star!";
                continue;
            }

            Rectangle basket = new Rectangle(
                    basketX - 2, 500, 80, 32
            );

            Rectangle falling = new Rectangle(
                    (int) o.x - o.size / 2,
                    (int) o.y - o.size / 2,
                    o.size,
                    o.size
            );

            if (basket.intersects(falling) && o.y < 550) {
                if (o.type == ObjectType.ASTEROID) {
                    gameOver = true;
                    won = false;
                    screenShake = 18;
                    message = "OUCH! A burning asteroid!";
                } else {
                    score++;
                    message = random.nextBoolean()
                            ? "Another little star!"
                            : "Into the basket it goes!";
                }

                iterator.remove();
                continue;
            }

            if (o.y > HEIGHT + 40) {
                iterator.remove();
            }
        }
    }

    private void updateOwl() {
        if (!owlActive) {
            owlDelay++;

            if (owlDelay > 450 + random.nextInt(300)) {
                owlActive = true;
                owlDirection = random.nextBoolean() ? 1 : -1;
                owlX = owlDirection == 1 ? -60 : WIDTH + 60;
                owlY = 110 + random.nextInt(140);
            }
        } else {
            owlX += owlDirection * 4;

            owlY += Math.sin(frameCount * 0.08) * 1.2;

            if ((owlDirection == 1 && owlX > WIDTH + 80)
                    || (owlDirection == -1 && owlX < -80)) {
                owlActive = false;
                owlDelay = 0;
            }
        }
    }

    @Override
    protected void paintComponent(Graphics graphics) {
        super.paintComponent(graphics);

        Graphics2D g = (Graphics2D) graphics.create();

        g.setRenderingHint(
                RenderingHints.KEY_ANTIALIASING,
                RenderingHints.VALUE_ANTIALIAS_ON
        );
        g.setRenderingHint(
                RenderingHints.KEY_RENDERING,
                RenderingHints.VALUE_RENDER_QUALITY
        );

        if (screenShake > 0) {
            g.translate(random.nextInt(7) - 3, random.nextInt(7) - 3);
        }

        drawSky(g);
        drawStars(g);
        drawMoon(g);
        drawClouds(g);
        drawHills(g);
        drawFlowers(g);

        if (owlActive) {
            drawOwl(g, owlX, (int) owlY);
        }

        for (FallingObject o : objects) {
            if (o.type == ObjectType.STAR) {
                drawFallingStar(g, o);
            } else {
                drawAsteroid(g, o);
            }
        }

        drawBoy(g);
        drawBasket(g);
        drawHUD(g);

        if (!gameStarted) {
            drawStartScreen(g);
        } else if (paused) {
            drawOverlay(g, "TAKE A BREATHER", "Press SPACE to continue");
        } else if (gameOver) {
            if (won) {
                drawOverlay(g, "YOU CAUGHT THE STARS!",
                        "You collected " + score + " stars! Press ENTER to play again");
            } else {
                drawOverlay(g, "OH, SHOOT!",
                        "An asteroid got you! Final stars: " + score
                                + "   |   Press ENTER to try again");
            }
        }

        g.dispose();
    }

    private void drawSky(Graphics2D g) {
        GradientPaint gradient = new GradientPaint(
                0, 0, new Color(8, 12, 39),
                0, HEIGHT, new Color(47, 54, 102)
        );

        g.setPaint(gradient);
        g.fillRect(0, 0, WIDTH, HEIGHT);
    }

    private void drawStars(Graphics2D g) {
        for (Twinkle t : twinkles) {
            float brightness = (float) (0.35 + 0.65
                    * (Math.sin(t.phase) + 1) / 2);

            g.setColor(new Color(
                    255, 247, 203, (int) (brightness * 240)
            ));

            g.fillOval(
                    (int) t.x, (int) t.y,
                    t.size, t.size
            );

            if (t.size >= 3 && brightness > 0.85) {
                g.drawLine((int) t.x - 3, (int) t.y,
                        (int) t.x + 5, (int) t.y);
                g.drawLine((int) t.x, (int) t.y - 3,
                        (int) t.x, (int) t.y + 5);
            }
        }
    }

    private void drawMoon(Graphics2D g) {
        int mx = 720;
        int my = 78;

        g.setColor(new Color(255, 233, 159, 28));
        g.fillOval(mx - 24, my - 24, 100, 100);

        g.setColor(moonGold);
        g.fillOval(mx, my, 54, 54);

        g.setColor(new Color(18, 25, 62));
        g.fillOval(mx + 19, my - 8, 50, 54);

        g.setColor(new Color(255, 239, 177));
        g.setFont(new Font("Serif", Font.ITALIC, 17));
        g.drawString("Good evening!", mx - 48, my + 77);
    }

    private void drawClouds(Graphics2D g) {
        g.setColor(new Color(95, 102, 151, 65));

        drawCloud(g, 100, 105, 1.0);
        drawCloud(g, 430, 155, 0.75);
        drawCloud(g, 640, 240, 0.9);
    }

    private void drawCloud(Graphics2D g, int x, int y, double scale) {
        Graphics2D c = (Graphics2D) g.create();
        c.translate(x, y);
        c.scale(scale, scale);

        c.fillOval(0, 15, 80, 28);
        c.fillOval(18, 0, 40, 40);
        c.fillOval(43, 8, 48, 35);

        c.dispose();
    }

    private void drawHills(Graphics2D g) {
        g.setColor(new Color(22, 45, 54));

        Polygon hill1 = new Polygon();
        hill1.addPoint(0, 430);
        hill1.addPoint(0, 350);
        hill1.addPoint(150, 290);
        hill1.addPoint(290, 390);
        hill1.addPoint(440, 325);
        hill1.addPoint(610, 390);
        hill1.addPoint(760, 300);
        hill1.addPoint(WIDTH, 370);
        hill1.addPoint(WIDTH, 460);
        g.fillPolygon(hill1);

        g.setColor(new Color(18, 58, 48));

        Polygon hill2 = new Polygon();
        hill2.addPoint(0, 445);
        hill2.addPoint(100, 390);
        hill2.addPoint(260, 430);
        hill2.addPoint(400, 370);
        hill2.addPoint(570, 435);
        hill2.addPoint(750, 385);
        hill2.addPoint(WIDTH, 415);
        hill2.addPoint(WIDTH, HEIGHT);
        hill2.addPoint(0, HEIGHT);
        g.fillPolygon(hill2);

        g.setColor(new Color(14, 45, 37));
        g.fillRect(0, 480, WIDTH, HEIGHT - 480);
    }

    private void drawFlowers(Graphics2D g) {
        for (int i = 0; i < 27; i++) {
            int x = (i * 73 + 31) % WIDTH;
            int y = 520 + (i * 19) % 100;

            g.setColor(new Color(43, 107, 63));
            g.drawLine(x, y, x, y + 12);

            g.setColor(i % 3 == 0
                    ? new Color(248, 180, 207)
                    : new Color(255, 224, 111));

            g.fillOval(x - 4, y - 3, 7, 7);
            g.fillOval(x + 1, y - 3, 7, 7);
            g.fillOval(x - 2, y - 7, 7, 7);
            g.fillOval(x - 2, y + 1, 7, 7);

            g.setColor(new Color(255, 245, 190));
            g.fillOval(x, y - 2, 4, 4);
        }
    }

    private Shape starShape(double cx, double cy, double outer,
                            double inner, int points) {
        Path2D path = new Path2D.Double();

        for (int i = 0; i < points * 2; i++) {
            double angle = -Math.PI / 2 + i * Math.PI / points;
            double radius = i % 2 == 0 ? outer : inner;
            double x = cx + Math.cos(angle) * radius;
            double y = cy + Math.sin(angle) * radius;

            if (i == 0) {
                path.moveTo(x, y);
            } else {
                path.lineTo(x, y);
            }
        }

        path.closePath();
        return path;
    }

    private void drawFallingStar(Graphics2D g, FallingObject o) {
        Graphics2D s = (Graphics2D) g.create();

        s.translate(o.x, o.y);
        s.rotate(o.rotation);

        s.setColor(new Color(255, 210, 50, 45));
        s.fill(starShape(0, 0, o.size * 0.95, o.size * 0.4, 5));

        s.setColor(new Color(255, 239, 140));
        s.fill(starShape(0, 0, o.size * 0.63, o.size * 0.27, 5));

        s.setColor(Color.WHITE);
        s.fillOval(-3, -o.size / 3, 6, 6);

        s.dispose();
    }

    private void drawAsteroid(Graphics2D g, FallingObject o) {
        Graphics2D a = (Graphics2D) g.create();

        a.translate(o.x, o.y);
        a.rotate(o.rotation);

        g.setColor(new Color(255, 75, 24, 65));
        g.fillOval((int) o.x - o.size, (int) o.y - o.size,
                o.size * 2, o.size * 2);

        a.setColor(new Color(255, 84, 22, 115));
        a.fillOval(-o.size / 2, -o.size * 2, o.size, o.size * 2);

        a.setColor(new Color(255, 117, 32));
        a.fill(starShape(0, 0, o.size * 0.7, o.size * 0.5, 7));

        a.setColor(new Color(173, 57, 34));
        a.fillOval(-o.size / 4, -o.size / 4,
                o.size / 3, o.size / 4);

        a.setColor(new Color(255, 219, 105));
        a.fillOval(-o.size / 4, -o.size / 3,
                o.size / 3, o.size / 3);

        a.dispose();
    }

    private void drawBoy(Graphics2D g) {
        int cx = basketX + 35;
        int ground = 535;
        int bounce = (int) boyBounce;

        g.setColor(new Color(0, 0, 0, 50));
        g.fillOval(cx - 38, ground + 1, 78, 12);

        g.setColor(new Color(65, 42, 43));
        g.fillRoundRect(cx - 22, ground - 51 + bounce,
                17, 44, 12, 12);
        g.fillRoundRect(cx + 4, ground - 51 + bounce,
                17, 44, 12, 12);

        g.setColor(new Color(70, 43, 35));
        g.fillRoundRect(cx - 26, ground - 13 + bounce,
                24, 12, 8, 8);
        g.fillRoundRect(cx + 1, ground - 13 + bounce,
                24, 12, 8, 8);

        g.setColor(new Color(78, 112, 199));
        g.fillRoundRect(cx - 27, ground - 103 + bounce,
                54, 57, 19, 19);

        g.setColor(new Color(247, 190, 76));
        g.fillOval(cx - 32, ground - 119 + bounce,
                64, 64);

        g.setColor(new Color(91, 49, 35));
        g.fillOval(cx - 32, ground - 123 + bounce,
                65, 38);

        g.fillOval(cx - 32, ground - 105 + bounce,
                16, 24);
        g.fillOval(cx + 17, ground - 105 + bounce,
                16, 24);

        g.setColor(new Color(255, 220, 148));
        g.fillOval(cx - 25, ground - 112 + bounce,
                50, 48);

        g.setColor(new Color(255, 149, 137));
        g.fillOval(cx - 20, ground - 91 + bounce, 12, 7);
        g.fillOval(cx + 9, ground - 91 + bounce, 12, 7);

        g.setColor(new Color(62, 47, 36));
        g.fillOval(cx - 12, ground - 99 + bounce, 6, 8);
        g.fillOval(cx + 7, ground - 99 + bounce, 6, 8);

        g.setColor(new Color(132, 60, 52));
        g.setStroke(new BasicStroke(2.5f));
        g.drawArc(cx - 10, ground - 91 + bounce,
                20, 13, 200, 140);

        g.setColor(new Color(255, 220, 148));
        g.setStroke(new BasicStroke(9, BasicStroke.CAP_ROUND,
                BasicStroke.JOIN_ROUND));
        g.drawLine(cx - 24, ground - 91 + bounce,
                cx - 39, ground - 65 + bounce);
        g.drawLine(cx + 24, ground - 91 + bounce,
                cx + 39, ground - 65 + bounce);
    }

    private void drawBasket(Graphics2D g) {
        int x = basketX;
        int y = 495;

        g.setColor(new Color(255, 222, 155));
        g.setStroke(new BasicStroke(5));
        g.drawArc(x + 9, y - 21, 52, 38, 0, 180);

        GradientPaint basketPaint = new GradientPaint(
                x, y, new Color(205, 132, 56),
                x, y + 34, new Color(132, 75, 32)
        );

        g.setPaint(basketPaint);

        Polygon basket = new Polygon();
        basket.addPoint(x, y);
        basket.addPoint(x + 70, y);
        basket.addPoint(x + 61, y + 34);
        basket.addPoint(x + 9, y + 34);
        g.fillPolygon(basket);

        g.setColor(new Color(255, 211, 135));
        g.setStroke(new BasicStroke(3));
        g.drawLine(x + 1, y + 3, x + 69, y + 3);

        for (int i = 1; i < 7; i++) {
            int xx = x + i * 9;

            g.setStroke(new BasicStroke(2));
            g.drawLine(xx, y + 5, xx - 2, y + 30);
        }

        g.setColor(new Color(255, 213, 139));
        g.setStroke(new BasicStroke(2));
        g.drawLine(x + 8, y + 14, x + 63, y + 14);
        g.drawLine(x + 6, y + 25, x + 63, y + 25);
    }

    private void drawOwl(Graphics2D g, int x, int y) {
        g.setColor(new Color(111, 80, 65));
        g.fillOval(x - 27, y - 22, 54, 47);

        g.setColor(new Color(93, 62, 53));
        g.fillPolygon(new int[]{x - 20, x - 25, x - 6},
                new int[]{y - 13, y - 35, y - 20}, 3);
        g.fillPolygon(new int[]{x + 20, x + 25, x + 6},
                new int[]{y - 13, y - 35, y - 20}, 3);

        g.setColor(new Color(236, 216, 175));
        g.fillOval(x - 19, y - 12, 18, 20);
        g.fillOval(x + 1, y - 12, 18, 20);

        g.setColor(Color.WHITE);
        g.fillOval(x - 15, y - 10, 12, 13);
        g.fillOval(x + 3, y - 10, 12, 13);

        g.setColor(new Color(25, 24, 35));
        g.fillOval(x - 11, y - 6, 6, 7);
        g.fillOval(x + 7, y - 6, 6, 7);

        g.setColor(new Color(238, 169, 61));
        g.fillPolygon(new int[]{x - 5, x + 5, x},
                new int[]{y + 1, y + 1, y + 9}, 3);

        g.setColor(new Color(112, 74, 54));
        g.fillOval(x - 42, y - 1, 24, 12);
        g.fillOval(x + 18, y - 1, 24, 12);
    }

    private void drawHUD(Graphics2D g) {
        g.setColor(new Color(7, 12, 33, 195));
        g.fillRoundRect(18, 17, 245, 73, 18, 18);

        g.setColor(new Color(255, 224, 111));
        g.setFont(uiFont);
        g.drawString("STAR COLLECTION", 34, 43);

        g.setColor(Color.WHITE);
        g.setFont(new Font("Serif", Font.BOLD, 27));
        g.drawString(score + " / " + TARGET, 34, 73);

        int barX = 135;
        int barY = 59;
        int barW = 105;

        g.setColor(new Color(73, 78, 108));
        g.fillRoundRect(barX, barY, barW, 12, 10, 10);

        g.setColor(new Color(255, 211, 80));
        g.fillRoundRect(barX, barY,
                (int) (barW * Math.min(1, score / (double) TARGET)),
                12, 10, 10);

        g.setFont(smallFont);
        g.setColor(new Color(235, 235, 255));
        g.drawString("MOVE:  ← →  OR  A / D", 22, HEIGHT - 19);
        g.drawString("PAUSE: SPACE", WIDTH - 165, HEIGHT - 19);

        if (!message.isEmpty() && gameStarted && !gameOver) {
            g.setFont(new Font("Serif", Font.ITALIC, 17));
            g.setColor(new Color(255, 240, 186));
            g.drawString(message, WIDTH / 2 - 155, 35);
        }
    }

    private void drawStartScreen(Graphics2D g) {
        g.setColor(new Color(5, 8, 27, 175));
        g.fillRect(0, 0, WIDTH, HEIGHT);

        g.setColor(new Color(255, 231, 143));
        g.setFont(titleFont);
        drawCentered(g, "CATCH THE STARS", 210);

        g.setColor(new Color(255, 240, 202));
        g.setFont(subtitleFont);
        drawCentered(g, "A little adventure beneath the moon", 252);

        g.setFont(uiFont);
        g.setColor(new Color(255, 223, 104));
        drawCentered(g, "COLLECT " + TARGET + " STARS TO WIN", 319);

        g.setColor(Color.WHITE);
        g.setFont(subtitleFont);
        drawCentered(g, "Catch the golden stars. Dodge the burning asteroids!", 359);
        drawCentered(g, "Watch out for cheeky owls that steal your stars.", 388);

        g.setColor(new Color(17, 24, 46));
        g.fillRoundRect(WIDTH / 2 - 115, 431, 230, 54, 20, 20);

        g.setColor(new Color(255, 224, 111));
        g.setStroke(new BasicStroke(2));
        g.drawRoundRect(WIDTH / 2 - 115, 431, 230, 54, 20, 20);

        g.setFont(uiFont);
        drawCentered(g, "PRESS SPACE TO BEGIN", 464);

        g.setFont(smallFont);
        g.setColor(new Color(220, 221, 244));
        drawCentered(g, "A burning asteroid ends the game instantly!", 521);
    }

    private void drawOverlay(Graphics2D g, String heading, String subheading) {
        g.setColor(new Color(4, 7, 23, 205));
        g.fillRect(0, 0, WIDTH, HEIGHT);

        g.setColor(won
                ? new Color(255, 226, 119)
                : new Color(255, 153, 115));

        g.setFont(new Font("Serif", Font.BOLD, 37));
        drawCentered(g, heading, 274);

        g.setColor(Color.WHITE);
        g.setFont(subtitleFont);
        drawCentered(g, subheading, 325);

        if (won) {
            g.setFont(new Font("Serif", Font.BOLD, 25));
            g.setColor(new Color(255, 232, 150));
            drawCentered(g, "✦  ✧  ✦  ✧  ✦", 371);
        }

        g.setFont(uiFont);
        g.setColor(new Color(255, 224, 111));
        drawCentered(g, "PRESS ENTER OR R TO PLAY AGAIN", 424);
    }

    private void drawCentered(Graphics2D g, String text, int y) {
        FontMetrics fm = g.getFontMetrics();
        g.drawString(text, (WIDTH - fm.stringWidth(text)) / 2, y);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            CatchTheStars game = new CatchTheStars();

            JFrame frame = new JFrame("Catch the Stars");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setContentPane(game);
            frame.pack();
            frame.setMinimumSize(new Dimension(750, 570));
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);

            game.requestFocusInWindow();
        });
    }
}