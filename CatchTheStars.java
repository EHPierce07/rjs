import java.awt.*;
import java.awt.event.*;
import java.util.Random;
import javax.swing.*;

public class CatchTheStars extends JPanel
        implements ActionListener, KeyListener {

    int basketX = 160;
    int starX = 100;
    int starY = 0;

    int score = 0;
    int lives = 3;

    boolean gameOver = false;

    Random random = new Random();
    Timer timer = new Timer(30, this);

    public CatchTheStars() {
        JFrame frame = new JFrame("Catch the Stars");

        setPreferredSize(new Dimension(400, 500));
        setBackground(Color.BLACK);

        frame.add(this);
        frame.pack();
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);

        setFocusable(true);
        addKeyListener(this);

        timer.start();
        requestFocusInWindow();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        // Basket
        g.setColor(Color.ORANGE);
        g.fillRect(basketX, 420, 70, 30);

        // Falling star
        g.setColor(Color.YELLOW);
        g.fillOval(starX, starY, 25, 25);

        // Score and lives
        g.setColor(Color.WHITE);
        g.setFont(new Font("Arial", Font.BOLD, 16));
        g.drawString("Score: " + score, 10, 20);
        g.drawString("Lives: " + lives, 300, 20);

        g.setFont(new Font("Arial", Font.PLAIN, 12));
        g.drawString("LEFT / RIGHT = Move Basket", 10, 475);

        if (gameOver) {
            g.setColor(Color.RED);
            g.setFont(new Font("Arial", Font.BOLD, 28));
            g.drawString("GAME OVER", 95, 220);

            g.setColor(Color.WHITE);
            g.setFont(new Font("Arial", Font.BOLD, 18));
            g.drawString("Final Score: " + score, 125, 255);

            g.setFont(new Font("Arial", Font.PLAIN, 14));
            g.drawString("Close the window to exit", 105, 285);
        }
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (gameOver) {
            return;
        }

        // Move star downward
        starY += 5;

        // Catch the star
        if (starY + 25 >= 420
                && starX + 25 >= basketX
                && starX <= basketX + 70
                && starY <= 450) {

            score += 10;
            resetStar();
        }

        // Missed star
        if (starY > 500) {
            lives--;
            resetStar();
        }

        // End game
        if (lives <= 0) {
            gameOver = true;
            timer.stop();
        }

        repaint();
    }

    void resetStar() {
        starX = random.nextInt(375);
        starY = -25;
    }

    @Override
    public void keyPressed(KeyEvent e) {
        if (gameOver) {
            return;
        }

        if (e.getKeyCode() == KeyEvent.VK_LEFT) {
            basketX -= 20;
        }

        if (e.getKeyCode() == KeyEvent.VK_RIGHT) {
            basketX += 20;
        }

        basketX = Math.max(0, Math.min(330, basketX));

        repaint();
    }

    @Override
    public void keyReleased(KeyEvent e) {
    }

    @Override
    public void keyTyped(KeyEvent e) {
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new CatchTheStars());
    }
}