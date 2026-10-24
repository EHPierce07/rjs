import java.awt.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.concurrent.ThreadLocalRandom;
import javax.sound.sampled.*;
import javax.swing.*;
import javax.swing.border.*;
import javax.swing.text.*;

public class ClassicTypewriter extends JFrame {

    private final JTextPane paper = new JTextPane();
    private final JLabel wordCount = new JLabel("Words: 0");
    private final JLabel charCount = new JLabel("Characters: 0");
    private final JLabel status = new JLabel("Ready to write");
    private final Timer cursorTimer;

    private boolean cursorVisible = true;
    private boolean soundEnabled = true;
    private long lastSoundTime;

    private final Color ink = new Color(48, 42, 35);
    private final Color paperColor = new Color(250, 244, 226);
    private final Color frameColor = new Color(55, 48, 42);
    private final Color brass = new Color(190, 151, 83);

    public ClassicTypewriter() {
        super("The Classic Typewriter");

        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setMinimumSize(new Dimension(720, 600));
        setSize(900, 720);
        setLocationRelativeTo(null);
        getContentPane().setBackground(frameColor);
        setLayout(new BorderLayout());

        add(buildHeader(), BorderLayout.NORTH);
        add(buildMachine(), BorderLayout.CENTER);
        add(buildFooter(), BorderLayout.SOUTH);

        cursorTimer = new Timer(530, e -> {
            cursorVisible = !cursorVisible;
            paper.repaint();
        });
        cursorTimer.start();

        paper.getDocument().addDocumentListener(
                new javax.swing.event.DocumentListener() {
                    public void insertUpdate(
                            javax.swing.event.DocumentEvent e) {
                        updateCounts();
                        playTypingSound();
                    }

                    public void removeUpdate(
                            javax.swing.event.DocumentEvent e) {
                        updateCounts();
                    }

                    public void changedUpdate(
                            javax.swing.event.DocumentEvent e) {
                        updateCounts();
                    }
                }
        );

        paper.addCaretListener(e -> paper.repaint());

        paper.setCaret(new DefaultCaret() {
            @Override
            public void paint(Graphics g) {
                if (!cursorVisible || !isVisible()) {
                    return;
                }

                try {
                    Rectangle r = paper.modelToView2D(
                            getDot()
                    ).getBounds();

                    g.setColor(ink);
                    g.fillRect(r.x, r.y, 2, r.height);
                } catch (BadLocationException ignored) {
                }
            }
        });

        updateCounts();
    }

    private JPanel buildHeader() {
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(frameColor);
        header.setBorder(new EmptyBorder(18, 24, 15, 24));

        JLabel title = new JLabel(
                "THE CLASSIC TYPEWRITER",
                SwingConstants.CENTER
        );
        title.setForeground(new Color(247, 232, 201));
        title.setFont(new Font("Serif", Font.BOLD, 25));

        JLabel subtitle = new JLabel(
                "A QUIET PLACE FOR YOUR WORDS",
                SwingConstants.CENTER
        );
        subtitle.setForeground(brass);
        subtitle.setFont(new Font("Monospaced", Font.PLAIN, 11));

        JPanel labels = new JPanel(new GridLayout(2, 1, 0, 5));
        labels.setOpaque(false);
        labels.add(title);
        labels.add(subtitle);

        header.add(labels, BorderLayout.CENTER);
        return header;
    }

    private JPanel buildMachine() {
        JPanel machine = new JPanel(new BorderLayout());
        machine.setBackground(frameColor);
        machine.setBorder(new CompoundBorder(
                new EmptyBorder(0, 24, 12, 24),
                new CompoundBorder(
                        new LineBorder(new Color(25, 22, 20), 3, true),
                        new EmptyBorder(12, 12, 12, 12)
                )
        ));

        JPanel topRail = new JPanel(new BorderLayout());
        topRail.setBackground(new Color(77, 68, 58));
        topRail.setBorder(new EmptyBorder(7, 12, 7, 12));

        JLabel railText = new JLabel(
                "PAPER FEED  •  INK RIBBON  •  LINE GUIDE"
        );
        railText.setForeground(new Color(229, 207, 167));
        railText.setFont(new Font("Monospaced", Font.BOLD, 10));
        topRail.add(railText, BorderLayout.WEST);

        JPanel paperHolder = new JPanel(new BorderLayout());
        paperHolder.setBackground(paperColor);
        paperHolder.setBorder(new CompoundBorder(
                new LineBorder(new Color(214, 202, 176), 1),
                new EmptyBorder(22, 25, 20, 25)
        ));

        paper.setBackground(paperColor);
        paper.setForeground(ink);
        paper.setCaretColor(ink);
        paper.setFont(new Font("Monospaced", Font.PLAIN, 17));
        paper.setMargin(new Insets(0, 0, 0, 0));
        paper.setBorder(null);
        paper.setEditorKit(new StyledEditorKit());
        paper.setParagraphAttributes(createParagraphAttributes(), true);
        paper.setText("");
        paper.setFocusable(true);
        paper.setToolTipText("Start typing your story here.");

        JScrollPane scrollPane = new JScrollPane(
                paper,
                JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                JScrollPane.HORIZONTAL_SCROLLBAR_NEVER
        );
        scrollPane.setBorder(null);
        scrollPane.getViewport().setBackground(paperColor);
        scrollPane.getVerticalScrollBar().setUnitIncrement(18);

        paperHolder.add(scrollPane, BorderLayout.CENTER);

        JPanel paperBottom = new JPanel(new BorderLayout());
        paperBottom.setBackground(paperColor);
        paperBottom.setBorder(new EmptyBorder(9, 0, 0, 0));

        JLabel pageLabel = new JLabel(
                "—  YOUR PAGE  —",
                SwingConstants.CENTER
        );
        pageLabel.setForeground(new Color(157, 143, 119));
        pageLabel.setFont(new Font("Serif", Font.ITALIC, 11));

        paperBottom.add(pageLabel, BorderLayout.CENTER);
        paperHolder.add(paperBottom, BorderLayout.SOUTH);

        JPanel lowerRail = new JPanel(new BorderLayout());
        lowerRail.setBackground(new Color(77, 68, 58));
        lowerRail.setBorder(new EmptyBorder(8, 12, 8, 12));

        JLabel lowerText = new JLabel(
                "◀  RETURN  •  TYPE YOUR NEXT LINE  ▶"
        );
        lowerText.setForeground(new Color(229, 207, 167));
        lowerText.setFont(new Font("Monospaced", Font.BOLD, 10));
        lowerRail.add(lowerText, BorderLayout.CENTER);

        machine.add(topRail, BorderLayout.NORTH);
        machine.add(paperHolder, BorderLayout.CENTER);
        machine.add(lowerRail, BorderLayout.SOUTH);

        return machine;
    }

    private SimpleAttributeSet createParagraphAttributes() {
        SimpleAttributeSet attributes = new SimpleAttributeSet();
        StyleConstants.setLineSpacing(attributes, 0.25f);
        StyleConstants.setSpaceAbove(attributes, 0f);
        StyleConstants.setSpaceBelow(attributes, 4f);
        return attributes;
    }

    private JPanel buildFooter() {
        JPanel footer = new JPanel(new BorderLayout(8, 8));
        footer.setBackground(frameColor);
        footer.setBorder(new EmptyBorder(5, 24, 18, 24));

        JPanel counts = new JPanel(
                new FlowLayout(FlowLayout.LEFT, 14, 4)
        );
        counts.setOpaque(false);

        wordCount.setForeground(new Color(247, 232, 201));
        charCount.setForeground(new Color(247, 232, 201));
        status.setForeground(brass);

        wordCount.setFont(new Font("Monospaced", Font.BOLD, 12));
        charCount.setFont(new Font("Monospaced", Font.BOLD, 12));
        status.setFont(new Font("Monospaced", Font.PLAIN, 11));

        counts.add(wordCount);
        counts.add(charCount);

        JPanel buttons = new JPanel(
                new FlowLayout(FlowLayout.CENTER, 8, 4)
        );
        buttons.setOpaque(false);

        JButton newPage = createButton("NEW PAGE");
        JButton save = createButton("SAVE TEXT");
        JButton sound = createButton("SOUND: ON");

        newPage.addActionListener(e -> newPage());
        save.addActionListener(e -> saveText());

        sound.addActionListener(e -> {
            soundEnabled = !soundEnabled;
            sound.setText(soundEnabled ? "SOUND: ON" : "SOUND: OFF");
            status.setText(
                    soundEnabled
                            ? "Typing sounds enabled"
                            : "Typing sounds disabled"
            );
            paper.requestFocusInWindow();
        });

        buttons.add(newPage);
        buttons.add(save);
        buttons.add(sound);

        footer.add(counts, BorderLayout.NORTH);
        footer.add(buttons, BorderLayout.CENTER);
        footer.add(status, BorderLayout.SOUTH);

        return footer;
    }

    private JButton createButton(String text) {
        JButton button = new JButton(text);
        button.setFont(new Font("Monospaced", Font.BOLD, 11));
        button.setForeground(new Color(247, 232, 201));
        button.setBackground(new Color(91, 76, 60));
        button.setFocusPainted(false);
        button.setBorder(new CompoundBorder(
                new LineBorder(brass, 1),
                new EmptyBorder(8, 12, 8, 12)
        ));
        button.setCursor(
                Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)
        );
        return button;
    }

    private void updateCounts() {
        String text = paper.getText();
        String trimmed = text.trim();
        int words = trimmed.isEmpty()
                ? 0
                : trimmed.split("\\s+").length;

        wordCount.setText("Words: " + words);
        charCount.setText("Characters: " + text.length());
    }

    private void newPage() {
        if (!paper.getText().isEmpty()) {
            int choice = JOptionPane.showConfirmDialog(
                    this,
                    "Start a new page? Unsaved writing will be cleared.",
                    "New Page",
                    JOptionPane.YES_NO_OPTION,
                    JOptionPane.WARNING_MESSAGE
            );

            if (choice != JOptionPane.YES_OPTION) {
                paper.requestFocusInWindow();
                return;
            }
        }

        paper.setText("");
        status.setText("A fresh page awaits");
        paper.requestFocusInWindow();
    }

    private void saveText() {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Save Your Writing");
        chooser.setSelectedFile(new File("my-writing.txt"));

        int result = chooser.showSaveDialog(this);

        if (result != JFileChooser.APPROVE_OPTION) {
            status.setText("Save cancelled");
            paper.requestFocusInWindow();
            return;
        }

        File file = chooser.getSelectedFile();

        if (file.exists()) {
            int overwrite = JOptionPane.showConfirmDialog(
                    this,
                    "This file already exists. Overwrite it?",
                    "Confirm Overwrite",
                    JOptionPane.YES_NO_OPTION,
                    JOptionPane.WARNING_MESSAGE
            );

            if (overwrite != JOptionPane.YES_OPTION) {
                paper.requestFocusInWindow();
                return;
            }
        }

        try {
            Files.writeString(
                    file.toPath(),
                    paper.getText(),
                    StandardCharsets.UTF_8
            );
            status.setText("Saved: " + file.getName());
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(
                    this,
                    "Could not save the file:\n" + ex.getMessage(),
                    "Save Error",
                    JOptionPane.ERROR_MESSAGE
            );
            status.setText("Save failed");
        }

        paper.requestFocusInWindow();
    }

    private void playTypingSound() {
        if (!soundEnabled) {
            return;
        }

        long now = System.nanoTime();

        if (now - lastSoundTime < 18_000_000L) {
            return;
        }

        lastSoundTime = now;

        Thread soundThread = new Thread(() -> {
            float sampleRate = 22050f;
            int durationMs = ThreadLocalRandom.current().nextInt(12, 27);
            int samples = (int) (sampleRate * durationMs / 1000);
            byte[] data = new byte[samples];

            double frequency =
                    ThreadLocalRandom.current().nextDouble(900, 2200);
            double volume =
                    ThreadLocalRandom.current().nextDouble(0.12, 0.32);

            for (int i = 0; i < samples; i++) {
                double envelope = 1.0 - (double) i / samples;
                double wave = Math.sin(
                        2.0 * Math.PI * frequency * i / sampleRate
                );
                data[i] = (byte) (wave * envelope * volume * 127);
            }

            AudioFormat format = new AudioFormat(
                    sampleRate, 8, 1, true, false
            );

            try (SourceDataLine line =
                         AudioSystem.getSourceDataLine(format)) {
                line.open(format, data.length);
                line.start();
                line.write(data, 0, data.length);
                line.drain();
            } catch (LineUnavailableException
                     | IllegalArgumentException ignored) {
                Toolkit.getDefaultToolkit().beep();
            }
        }, "TypewriterSound");

        soundThread.setDaemon(true);
        soundThread.start();
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(
                        UIManager.getSystemLookAndFeelClassName()
                );
            } catch (Exception ignored) {
            }

            ClassicTypewriter app = new ClassicTypewriter();
            app.setVisible(true);
            app.paper.requestFocusInWindow();
        });
    }
}