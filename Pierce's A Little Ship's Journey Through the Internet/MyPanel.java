import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import javax.imageio.ImageIO;
import javax.swing.*;

public class MyPanel extends JPanel implements ActionListener {

    private final int PANEL_WIDTH = 700;
    private final int PANEL_HEIGHT = 500;

    private Image ship;
    private BufferedImage backgroundImage;
    private final Timer timer;

    private int x = 0;
    private int y = 0;
    private int xVelocity = 3;
    private int yVelocity = 2;

    private String scene = "Space";
    private boolean arrived = false;
    private int animationStep = 0;

    public MyPanel() {
        setPreferredSize(
            new Dimension(PANEL_WIDTH, PANEL_HEIGHT)
        );

        ImageIcon icon = new ImageIcon("ship.png");

        if (icon.getIconWidth() > 0) {
            ship = icon.getImage();
        } else {
            System.out.println("Warning: ship.png was not found!");
        }

        timer = new Timer(16, this);
        timer.start();
    }

    public void setScene(String newScene) {
        if (arrived) {
            return;
        }

        scene = newScene;
        backgroundImage = null;
        repaint();
    }

    public String getScene() {
        return scene;
    }

    public boolean hasArrived() {
        return arrived;
    }

    public boolean setCustomBackground(File file) {
        try {
            BufferedImage image = ImageIO.read(file);

            if (image == null) {
                JOptionPane.showMessageDialog(
                    this,
                    "Please select a valid image file.",
                    "Image Error",
                    JOptionPane.ERROR_MESSAGE
                );
                return false;
            }

            backgroundImage = image;
            scene = "Final Destination";
            arrived = true;

            repaint();
            return true;

        } catch (IOException ex) {
            JOptionPane.showMessageDialog(
                this,
                "Could not load the image:\n" + ex.getMessage(),
                "Image Error",
                JOptionPane.ERROR_MESSAGE
            );
            return false;
        }
    }

    public void restartJourney() {
        arrived = false;
        backgroundImage = null;
        scene = "Space";

        x = 0;
        y = 0;
        xVelocity = 3;
        yVelocity = 2;
        animationStep = 0;

        repaint();
    }

    private void drawSpace(Graphics2D g, int w, int h) {
        g.setPaint(new GradientPaint(
            0, 0, new Color(5, 5, 35),
            w, h, new Color(45, 15, 85)
        ));
        g.fillRect(0, 0, w, h);

        for (int i = 0; i < 130; i++) {
            int sx = Math.floorMod(i * 73 + 19, w);
            int sy = Math.floorMod(
                i * 137 + animationStep / 2 + 31, h
            );

            g.setColor(i % 5 == 0
                ? Color.CYAN : Color.WHITE);

            g.fillOval(sx, sy, i % 5 == 0 ? 3 : 2, 2);
        }

        int size = Math.max(65, Math.min(w, h) / 5);
        int px = w * 3 / 4;
        int py = h / 3;

        g.setColor(new Color(70, 100, 210));
        g.fillOval(px, py, size, size);

        g.setColor(new Color(80, 210, 160));
        g.fillOval(px + size / 5, py + size / 4,
                   size / 3, size / 4);
        g.fillOval(px + size / 2, py + size / 2,
                   size / 4, size / 5);

        g.setColor(new Color(180, 180, 255));
        g.drawOval(px - 8, py + size / 3,
                   size + 16, size / 3);
    }

    private void drawEarth(Graphics2D g, int w, int h) {
        g.setPaint(new GradientPaint(
            0, 0, new Color(10, 30, 100),
            0, h, new Color(30, 100, 180)
        ));
        g.fillRect(0, 0, w, h);

        // Earth grows toward the viewer.
        int size = Math.max(180, Math.min(w, h) * 3 / 4);
        int px = (w - size) / 2;
        int py = (h - size) / 2;

        g.setColor(new Color(35, 110, 230));
        g.fillOval(px, py, size, size);

        g.setColor(new Color(55, 180, 115));
        g.fillOval(px + size / 5, py + size / 4,
                   size / 3, size / 5);
        g.fillOval(px + size / 2, py + size / 2,
                   size / 4, size / 3);
        g.fillOval(px + size / 3, py + size / 6,
                   size / 5, size / 6);

        g.setColor(new Color(160, 220, 255, 150));
        g.setStroke(new BasicStroke(4));
        g.drawOval(px, py, size, size);
        g.setStroke(new BasicStroke(1));
    }

    private void drawWires(Graphics2D g, int w, int h) {
        g.setColor(new Color(5, 15, 30));
        g.fillRect(0, 0, w, h);

        g.setStroke(new BasicStroke(2));

        for (int i = -h; i < w + h; i += 65) {
            int offset = animationStep % 65;

            g.setColor(new Color(0, 180, 255, 170));
            g.drawLine(i + offset, 0, i - h + offset, h);

            g.setColor(new Color(0, 255, 160, 150));
            g.drawLine(i - offset, h, i + h - offset, 0);
        }

        g.setColor(new Color(0, 210, 255));

        for (int i = 0; i < 10; i++) {
            int px = Math.floorMod(i * 113 + animationStep, w);
            int py = Math.floorMod(i * 71 + animationStep, h);

            g.fillOval(px, py, 5, 5);
        }

        g.setFont(new Font("Monospaced", Font.BOLD, 18));
        g.setColor(new Color(120, 255, 200));
        g.drawString("ENTERING THE DIGITAL WORLD", 20, 35);
    }

    @Override
    protected void paintComponent(Graphics graphics) {
        super.paintComponent(graphics);

        Graphics2D g = (Graphics2D) graphics.create();

        int w = getWidth();
        int h = getHeight();

        if (backgroundImage != null) {
            g.drawImage(backgroundImage, 0, 0, w, h, this);
        } else {
            switch (scene) {
                case "Earth":
                    drawEarth(g, w, h);
                    break;

                case "Digital Wires":
                    drawWires(g, w, h);
                    break;

                default:
                    drawSpace(g, w, h);
                    break;
            }
        }

        if (ship != null) {
            g.drawImage(ship, x, y, this);
        }

        if (arrived) {
            g.setFont(new Font("Monospaced", Font.BOLD, 16));
            g.setColor(Color.WHITE);
            g.drawString("DESTINATION REACHED", 20, h - 20);
        }

        g.dispose();
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (arrived) {
            return;
        }

        animationStep++;

        if (ship != null) {
            int iw = ship.getWidth(this);
            int ih = ship.getHeight(this);

            if (iw > 0 && ih > 0) {
                int maxX = Math.max(0, getWidth() - iw);
                int maxY = Math.max(0, getHeight() - ih);

                x += xVelocity;
                y += yVelocity;

                if (x >= maxX) {
                    x = maxX;
                    xVelocity = -Math.abs(xVelocity);
                } else if (x <= 0) {
                    x = 0;
                    xVelocity = Math.abs(xVelocity);
                }

                if (y >= maxY) {
                    y = maxY;
                    yVelocity = -Math.abs(yVelocity);
                } else if (y <= 0) {
                    y = 0;
                    yVelocity = Math.abs(yVelocity);
                }
            }
        }

        repaint();
    }
}