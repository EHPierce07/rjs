
import java.awt.*;
import java.awt.event.*;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.concurrent.ThreadLocalRandom;
import javax.sound.sampled.*;
import javax.swing.*;
import javax.swing.border.*;
import javax.swing.event.*;

public class RetroDigitalTypewriter extends JFrame {

    private final Color background = new Color(8, 12, 11);
    private final Color panel = new Color(18, 25, 21);
    private final Color keyBackground = new Color(5, 9, 7);
    private final Color green = new Color(105, 255, 145);
    private final Color dimGreen = new Color(49, 133, 75);
    private final Color textGreen = new Color(185, 255, 199);

    private final JTextArea editor = new JTextArea();
    private final JLabel wordCount = new JLabel("WORDS: 0");
    private final JLabel charCount = new JLabel("CHARACTERS: 0");
    private final JLabel status = new JLabel("READY");

    private boolean soundEnabled = true;
    private boolean cursorBlinkEnabled = true;
    private boolean lineWrapEnabled = true;
    private int fontSize = 19;
    private long lastSoundTime;
    private File currentFile;
    private Timer cursorTimer;

    public RetroDigitalTypewriter() {
        super("Retro Digital Typewriter");

        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        setMinimumSize(new Dimension(760, 620));
        setSize(1000, 790);
        setLocationRelativeTo(null);
        getContentPane().setBackground(background);
        setLayout(new BorderLayout(0, 0));

        setJMenuBar(buildMenuBar());
        add(buildTerminal(), BorderLayout.CENTER);
        add(buildBottomPanel(), BorderLayout.SOUTH);

        configureEditor();
        configureCursor();

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                exitApplication();
            }
        });

        updateCounts();
    }

    private JMenuBar buildMenuBar() {
        JMenuBar menuBar = new JMenuBar();
        menuBar.setBackground(background);
        menuBar.setBorder(new EmptyBorder(5, 12, 5, 12));

        JMenu fileMenu = createMenu("FILE");
        addMenuItem(fileMenu, "New", e -> newPage());
        addMenuItem(fileMenu, "Open...", e -> openFile());
        addMenuItem(fileMenu, "Save...", e -> saveFile());
        fileMenu.addSeparator();
        addMenuItem(fileMenu, "Exit", e -> exitApplication());

        JMenu settingsMenu = createMenu("SETTINGS");
        addMenuItem(settingsMenu, "Preferences...", e -> openSettings());
        addMenuItem(settingsMenu, "Increase Font Size", e -> changeFontSize(1));
        addMenuItem(settingsMenu, "Decrease Font Size", e -> changeFontSize(-1));

        menuBar.add(fileMenu);
        menuBar.add(settingsMenu);

        return menuBar;
    }

    private JMenu createMenu(String title) {
        JMenu menu = new JMenu(title);
        menu.setForeground(green);
        menu.setBackground(background);
        menu.setFont(new Font("Monospaced", Font.BOLD, 14));
        menu.setOpaque(true);
        return menu;
    }

    private void addMenuItem(JMenu menu, String title, ActionListener action) {
        JMenuItem item = new JMenuItem(title);
        item.setFont(new Font("Monospaced", Font.PLAIN, 14));
        item.setBackground(panel);
        item.setForeground(green);
        item.addActionListener(action);
        menu.add(item);
    }

    private JPanel buildTerminal() {
        JPanel outer = new JPanel(new BorderLayout());
        outer.setBackground(background);
        outer.setBorder(new EmptyBorder(5, 14, 8, 14));

        JPanel screen = new JPanel(new BorderLayout(0, 6));
        screen.setBackground(panel);
        screen.setBorder(new CompoundBorder(
                new LineBorder(dimGreen, 2),
                new EmptyBorder(10, 10, 10, 10)
        ));

        JPanel screenHeader = new JPanel(new BorderLayout());
        screenHeader.setBackground(panel);

        JLabel screenLabel = new JLabel("TEXT MODE");
        screenLabel.setForeground(dimGreen);
        screenLabel.setFont(new Font("Monospaced", Font.BOLD, 12));

        screenHeader.add(screenLabel, BorderLayout.WEST);

        editor.setBackground(background);
        editor.setForeground(textGreen);
        editor.setCaretColor(green);
        editor.setSelectionColor(new Color(37, 92, 51));
        editor.setSelectedTextColor(Color.WHITE);
        editor.setFont(new Font("Monospaced", Font.PLAIN, fontSize));
        editor.setLineWrap(lineWrapEnabled);
        editor.setWrapStyleWord(true);
        editor.setTabSize(4);
        editor.setMargin(new Insets(12, 12, 12, 12));
        editor.setBorder(null);
        editor.setFocusTraversalKeysEnabled(false);

        JScrollPane scroll = new JScrollPane(editor);
        scroll.setBorder(new LineBorder(new Color(34, 63, 43), 1));
        scroll.getViewport().setBackground(background);
        scroll.getVerticalScrollBar().setBackground(panel);
        scroll.getVerticalScrollBar().setUnitIncrement(18);

        screen.add(screenHeader, BorderLayout.NORTH);
        screen.add(scroll, BorderLayout.CENTER);

        outer.add(screen, BorderLayout.CENTER);

        return outer;
    }

    private JPanel buildBottomPanel() {
        JPanel bottom = new JPanel(new BorderLayout(0, 8));
        bottom.setBackground(background);
        bottom.setBorder(new EmptyBorder(0, 14, 12, 14));

        JPanel stats = new JPanel(new BorderLayout());
        stats.setBackground(panel);
        stats.setBorder(new CompoundBorder(
                new LineBorder(dimGreen, 1),
                new EmptyBorder(7, 10, 7, 10)
        ));

        JPanel counters = new JPanel(new FlowLayout(FlowLayout.LEFT, 14, 0));
        counters.setOpaque(false);

        wordCount.setForeground(green);
        charCount.setForeground(green);
        status.setForeground(dimGreen);

        wordCount.setFont(new Font("Monospaced", Font.BOLD, 12));
        charCount.setFont(new Font("Monospaced", Font.BOLD, 12));
        status.setFont(new Font("Monospaced", Font.BOLD, 12));

        counters.add(wordCount);
        counters.add(charCount);

        stats.add(counters, BorderLayout.WEST);
        stats.add(status, BorderLayout.EAST);

        JPanel keyboard = new JPanel(new GridLayout(5, 1, 4, 4));
        keyboard.setBackground(background);
        keyboard.setBorder(new EmptyBorder(2, 0, 0, 0));

        addKeyboardRow(keyboard, new String[]{
                "ESC", "1", "2", "3", "4", "5", "6", "7", "8", "9", "0",
                "-", "=", "BACKSPACE"
        });

        addKeyboardRow(keyboard, new String[]{
                "TAB", "Q", "W", "E", "R", "T", "Y", "U", "I", "O", "P",
                "[", "]", "\\"
        });

        addKeyboardRow(keyboard, new String[]{
                "CAPS", "A", "S", "D", "F", "G", "H", "J", "K", "L",
                ";", "'", "ENTER"
        });

        addKeyboardRow(keyboard, new String[]{
                "SHIFT", "Z", "X", "C", "V", "B", "N", "M", ",", ".",
                "/", "UP", "DEL"
        });

        addKeyboardRow(keyboard, new String[]{
                "CTRL", "FN", "ALT", "SPACE", "LEFT", "DOWN", "RIGHT"
        });

        bottom.add(stats, BorderLayout.NORTH);
        bottom.add(keyboard, BorderLayout.CENTER);

        return bottom;
    }

    private void addKeyboardRow(JPanel keyboard, String[] keys) {
        JPanel row = new JPanel(new GridLayout(1, keys.length, 4, 0));
        row.setOpaque(false);

        for (String key : keys) {
            JButton button = createKey(key);
            button.addActionListener(e -> pressKey(key));
            row.add(button);
        }

        keyboard.add(row);
    }

    private JButton createKey(String label) {
        JButton button = new JButton(label);
        button.setFont(new Font("Monospaced", Font.BOLD, 13));
        button.setForeground(green);
        button.setBackground(keyBackground);
        button.setOpaque(true);
        button.setFocusPainted(false);
        button.setMargin(new Insets(3, 2, 3, 2));
        button.setBorder(new LineBorder(dimGreen, 1));
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        button.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                button.setBackground(new Color(20, 43, 27));
            }

            @Override
            public void mouseExited(MouseEvent e) {
                button.setBackground(keyBackground);
            }
        });

        return button;
    }

    private void configureEditor() {
        editor.getDocument().addDocumentListener(
                new DocumentListener() {
                    public void insertUpdate(DocumentEvent e) {
                        updateCounts();
                        playKeySound();
                    }

                    public void removeUpdate(DocumentEvent e) {
                        updateCounts();
                        playKeySound();
                    }

                    public void changedUpdate(DocumentEvent e) {
                        updateCounts();
                    }
                }
        );

        editor.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ESCAPE) {
                    editor.transferFocus();
                }
            }
        });
    }

    private void configureCursor() {
        cursorTimer = new Timer(500, e -> {
            if (cursorBlinkEnabled) {
                editor.getCaret().setVisible(
                        !editor.getCaret().isVisible()
                );
            } else {
                editor.getCaret().setVisible(true);
            }
        });

        cursorTimer.start();
    }

    private void pressKey(String key) {
        editor.requestFocusInWindow();

        switch (key) {
            case "BACKSPACE":
                deletePreviousCharacter();
                break;

            case "DEL":
                deleteNextCharacter();
                break;

            case "ENTER":
                editor.replaceSelection("\n");
                break;

            case "SPACE":
                editor.replaceSelection(" ");
                break;

            case "TAB":
                editor.replaceSelection("    ");
                break;

            case "CAPS":
            case "SHIFT":
            case "CTRL":
            case "FN":
            case "ALT":
            case "ESC":
            case "UP":
            case "DOWN":
            case "LEFT":
            case "RIGHT":
                status.setText(key + " KEY");
                break;

            default:
                editor.replaceSelection(key);
                break;
        }

        editor.requestFocusInWindow();
    }

    private void deletePreviousCharacter() {
        int start = editor.getSelectionStart();
        int end = editor.getSelectionEnd();

        if (start != end) {
            editor.replaceSelection("");
        } else if (start > 0) {
            editor.select(start - 1, start);
            editor.replaceSelection("");
        }
    }

    private void deleteNextCharacter() {
        int start = editor.getSelectionStart();
        int end = editor.getSelectionEnd();

        if (start != end) {
            editor.replaceSelection("");
        } else if (start < editor.getDocument().getLength()) {
            editor.select(start, start + 1);
            editor.replaceSelection("");
        }
    }

    private void playKeySound() {
        if (!soundEnabled) {
            return;
        }

        long now = System.nanoTime();

        if (now - lastSoundTime < 12_000_000L) {
            return;
        }

        lastSoundTime = now;

        Thread soundThread = new Thread(() -> {
            float sampleRate = 22050f;
            int duration = ThreadLocalRandom.current().nextInt(16, 32);
            int count = (int) (sampleRate * duration / 1000);
            byte[] samples = new byte[count];

            double frequency = ThreadLocalRandom.current()
                    .nextDouble(650, 1700);

            for (int i = 0; i < count; i++) {
                double decay = Math.exp(-i / (sampleRate * 0.006));
                double noise = ThreadLocalRandom.current()
                        .nextDouble(-0.4, 0.4);
                double tone = Math.sin(
                        2 * Math.PI * frequency * i / sampleRate
                );

                samples[i] = (byte) (
                        (tone * 0.65 + noise * 0.35) * decay * 105
                );
            }

            AudioFormat format = new AudioFormat(
                    sampleRate, 8, 1, true, false
            );

            try (SourceDataLine line =
                         AudioSystem.getSourceDataLine(format)) {
                line.open(format, samples.length);
                line.start();
                line.write(samples, 0, samples.length);
                line.drain();
            } catch (Exception ignored) {
                Toolkit.getDefaultToolkit().beep();
            }
        }, "RetroKeySound");

        soundThread.setDaemon(true);
        soundThread.start();
    }

    private void updateCounts() {
        String text = editor.getText();
        String trimmed = text.trim();

        int words = trimmed.isEmpty()
                ? 0
                : trimmed.split("\\s+").length;

        wordCount.setText("WORDS: " + words);
        charCount.setText("CHARACTERS: " + text.length());
    }

    private void changeFontSize(int direction) {
        fontSize = Math.max(12, Math.min(36, fontSize + direction * 2));
        editor.setFont(new Font("Monospaced", Font.PLAIN, fontSize));
        status.setText("FONT SIZE: " + fontSize);
        editor.requestFocusInWindow();
    }

    private void openSettings() {
        JDialog dialog = new JDialog(this, "SETTINGS", true);
        dialog.setSize(440, 360);
        dialog.setResizable(false);
        dialog.setLocationRelativeTo(this);
        dialog.getContentPane().setBackground(panel);
        dialog.setLayout(new BorderLayout(10, 10));

        JLabel heading = new JLabel("DISPLAY & SOUND", SwingConstants.CENTER);
        heading.setForeground(green);
        heading.setFont(new Font("Monospaced", Font.BOLD, 18));
        heading.setBorder(new EmptyBorder(15, 10, 5, 10));

        JPanel options = new JPanel();
        options.setLayout(new BoxLayout(options, BoxLayout.Y_AXIS));
        options.setBackground(panel);
        options.setBorder(new EmptyBorder(10, 22, 10, 22));

        JCheckBox soundOption = createCheckBox(
                "Keyboard sound effects", soundEnabled
        );

        JCheckBox cursorOption = createCheckBox(
                "Blinking cursor", cursorBlinkEnabled
        );

        JCheckBox wrapOption = createCheckBox(
                "Automatic line wrapping", lineWrapEnabled
        );

        JLabel sizeLabel = new JLabel("FONT SIZE: " + fontSize);
        sizeLabel.setForeground(green);
        sizeLabel.setFont(new Font("Monospaced", Font.BOLD, 14));

        JSlider sizeSlider = new JSlider(12, 36, fontSize);
        sizeSlider.setBackground(panel);
        sizeSlider.setForeground(green);
        sizeSlider.setMajorTickSpacing(6);
        sizeSlider.setPaintTicks(true);
        sizeSlider.setPaintLabels(true);

        sizeSlider.addChangeListener(e ->
                sizeLabel.setText("FONT SIZE: " + sizeSlider.getValue())
        );

        options.add(soundOption);
        options.add(Box.createVerticalStrut(8));
        options.add(cursorOption);
        options.add(Box.createVerticalStrut(8));
        options.add(wrapOption);
        options.add(Box.createVerticalStrut(18));
        options.add(sizeLabel);
        options.add(sizeSlider);

        JPanel buttons = new JPanel(new FlowLayout());
        buttons.setBackground(panel);

        JButton apply = createMenuButton("APPLY");
        JButton cancel = createMenuButton("CANCEL");

        apply.addActionListener(e -> {
            soundEnabled = soundOption.isSelected();
            cursorBlinkEnabled = cursorOption.isSelected();
            lineWrapEnabled = wrapOption.isSelected();
            fontSize = sizeSlider.getValue();

            editor.setFont(new Font("Monospaced", Font.PLAIN, fontSize));
            editor.setLineWrap(lineWrapEnabled);
            editor.setWrapStyleWord(lineWrapEnabled);
            editor.getCaret().setVisible(true);

            status.setText("SETTINGS UPDATED");
            dialog.dispose();
            editor.requestFocusInWindow();
        });

        cancel.addActionListener(e -> dialog.dispose());

        buttons.add(apply);
        buttons.add(cancel);

        dialog.add(heading, BorderLayout.NORTH);
        dialog.add(options, BorderLayout.CENTER);
        dialog.add(buttons, BorderLayout.SOUTH);
        dialog.setVisible(true);
    }

    private JCheckBox createCheckBox(String text, boolean selected) {
        JCheckBox box = new JCheckBox(text, selected);
        box.setBackground(panel);
        box.setForeground(textGreen);
        box.setFont(new Font("Monospaced", Font.PLAIN, 14));
        box.setFocusPainted(false);
        return box;
    }

    private JButton createMenuButton(String text) {
        JButton button = new JButton(text);
        button.setFont(new Font("Monospaced", Font.BOLD, 13));
        button.setForeground(green);
        button.setBackground(keyBackground);
        button.setBorder(new CompoundBorder(
                new LineBorder(dimGreen, 1),
                new EmptyBorder(7, 14, 7, 14)
        ));
        button.setFocusPainted(false);
        return button;
    }

    private void newPage() {
        if (!editor.getText().isEmpty()) {
            int answer = JOptionPane.showConfirmDialog(
                    this,
                    "Start a new page? Unsaved writing may be lost.",
                    "NEW PAGE",
                    JOptionPane.YES_NO_OPTION,
                    JOptionPane.WARNING_MESSAGE
            );

            if (answer != JOptionPane.YES_OPTION) {
                return;
            }
        }

        editor.setText("");
        currentFile = null;
        status.setText("NEW PAGE READY");
        editor.requestFocusInWindow();
    }

    private void openFile() {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("OPEN TEXT FILE");

        if (chooser.showOpenDialog(this) != JFileChooser.APPROVE_OPTION) {
            return;
        }

        File file = chooser.getSelectedFile();

        try {
            String content = Files.readString(
                    file.toPath(),
                    StandardCharsets.UTF_8
            );

            editor.setText(content);
            editor.setCaretPosition(0);
            currentFile = file;
            status.setText("OPENED: " + file.getName());
        } catch (IOException ex) {
            showError("Could not open file:\n" + ex.getMessage());
        }

        editor.requestFocusInWindow();
    }

    private void saveFile() {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("SAVE TEXT FILE");

        chooser.setSelectedFile(
                currentFile == null
                        ? new File("my-writing.txt")
                        : currentFile
        );

        if (chooser.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) {
            return;
        }

        File file = chooser.getSelectedFile();

        if (file.exists()) {
            int answer = JOptionPane.showConfirmDialog(
                    this,
                    "This file already exists. Overwrite it?",
                    "CONFIRM SAVE",
                    JOptionPane.YES_NO_OPTION,
                    JOptionPane.WARNING_MESSAGE
            );

            if (answer != JOptionPane.YES_OPTION) {
                return;
            }
        }

        try {
            Files.writeString(
                    file.toPath(),
                    editor.getText(),
                    StandardCharsets.UTF_8
            );

            currentFile = file;
            status.setText("SAVED: " + file.getName());
        } catch (IOException ex) {
            showError("Could not save file:\n" + ex.getMessage());
        }

        editor.requestFocusInWindow();
    }

    private void exitApplication() {
        if (!editor.getText().isEmpty()) {
            int answer = JOptionPane.showConfirmDialog(
                    this,
                    "Exit now? Make sure your writing is saved.",
                    "EXIT",
                    JOptionPane.YES_NO_OPTION,
                    JOptionPane.QUESTION_MESSAGE
            );

            if (answer != JOptionPane.YES_OPTION) {
                return;
            }
        }

        if (cursorTimer != null) {
            cursorTimer.stop();
        }

        dispose();
    }

    private void showError(String message) {
        JOptionPane.showMessageDialog(
                this,
                message,
                "FILE ERROR",
                JOptionPane.ERROR_MESSAGE
        );

        status.setText("FILE OPERATION FAILED");
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(
                        UIManager.getSystemLookAndFeelClassName()
                );
            } catch (Exception ignored) {
            }

            RetroDigitalTypewriter app = new RetroDigitalTypewriter();
            app.setVisible(true);
        });
    }
}