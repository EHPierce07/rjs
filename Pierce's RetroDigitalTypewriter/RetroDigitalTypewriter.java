import java.awt.*;
import java.awt.event.*;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import javax.sound.sampled.*;
import javax.swing.*;
import javax.swing.border.*;
import javax.swing.event.*;

public class RetroDigitalTypewriter extends JFrame {

    private final Color olive = new Color(126, 128, 99);
    private final Color oliveDark = new Color(75, 77, 61);
    private final Color metal = new Color(185, 179, 153);
    private final Color ink = new Color(35, 38, 30);
    private final Color paperColor = new Color(250, 247, 231);
    private final Color machineBackground = new Color(222, 216, 195);

    private final JTextArea paperText = new JTextArea();
    private final JLabel wordCount = new JLabel("WORDS 0");
    private final JLabel charCount = new JLabel("CHARACTERS 0");
    private final JLabel status = new JLabel("READY");
    private final MachinePanel machine = new MachinePanel();

    private boolean soundEnabled = true;
    private boolean cursorBlink = true;
    private boolean lineWrap = true;
    private boolean capsLock = false;
    private boolean shiftDown = false;
    private int fontSize = 18;
    private File currentFile;
    private Timer cursorTimer;
    private long lastSoundTime;

    public RetroDigitalTypewriter() {
        super("Retro Digital Typewriter");

        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        setSize(1050, 850);
        setMinimumSize(new Dimension(780, 700));
        setLocationRelativeTo(null);
        getContentPane().setBackground(machineBackground);
        setLayout(new BorderLayout());

        add(buildToolbar(), BorderLayout.NORTH);
        add(machine, BorderLayout.CENTER);
        add(buildStatusBar(), BorderLayout.SOUTH);

        configurePaper();
        configurePhysicalKeyboard();
        configureCursor();

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                exitApplication();
            }
        });

        updateCounts();
    }

    private JPanel buildToolbar() {
        JPanel toolbar = new JPanel(new FlowLayout(FlowLayout.LEFT, 9, 8));
        toolbar.setBackground(new Color(38, 40, 32));
        toolbar.setBorder(new MatteBorder(0, 0, 3, 0, metal));

        JButton fileButton = toolbarButton("▰  FILE");
        JButton settingsButton = toolbarButton("⚙  SETTINGS");

        fileButton.addActionListener(e -> {
            JPopupMenu menu = new JPopupMenu();
            addFileItem(menu, "New Page", e2 -> newPage());
            addFileItem(menu, "Open...", e2 -> openFile());
            addFileItem(menu, "Save As...", e2 -> saveFile());
            menu.show(fileButton, 0, fileButton.getHeight());
        });

        settingsButton.addActionListener(e -> openSettings());

        toolbar.add(fileButton);
        toolbar.add(settingsButton);

        return toolbar;
    }

    private JButton toolbarButton(String text) {
        JButton button = new JButton(text);
        button.setFont(new Font("Monospaced", Font.BOLD, 14));
        button.setForeground(new Color(224, 222, 190));
        button.setBackground(new Color(25, 27, 22));
        button.setFocusPainted(false);
        button.setBorder(new CompoundBorder(
                new LineBorder(new Color(122, 125, 94), 2),
                new EmptyBorder(6, 12, 6, 12)
        ));
        return button;
    }

    private void addFileItem(
            JPopupMenu menu,
            String name,
            ActionListener action
    ) {
        JMenuItem item = new JMenuItem(name);
        item.setFont(new Font("Monospaced", Font.PLAIN, 14));
        item.addActionListener(action);
        menu.add(item);
    }

    private JPanel buildStatusBar() {
        JPanel bar = new JPanel(new BorderLayout());
        bar.setBackground(new Color(38, 40, 32));
        bar.setBorder(new EmptyBorder(8, 14, 8, 14));

        JPanel counters = new JPanel(new FlowLayout(FlowLayout.LEFT, 18, 0));
        counters.setOpaque(false);

        wordCount.setForeground(new Color(225, 223, 190));
        charCount.setForeground(new Color(225, 223, 190));
        status.setForeground(new Color(175, 185, 143));

        wordCount.setFont(new Font("Monospaced", Font.BOLD, 12));
        charCount.setFont(new Font("Monospaced", Font.BOLD, 12));
        status.setFont(new Font("Monospaced", Font.BOLD, 12));

        counters.add(wordCount);
        counters.add(charCount);

        bar.add(counters, BorderLayout.WEST);
        bar.add(status, BorderLayout.EAST);

        return bar;
    }

    private void configurePaper() {
        paperText.setFont(new Font(Font.MONOSPACED, Font.PLAIN, fontSize));
        paperText.setForeground(ink);
        paperText.setBackground(paperColor);
        paperText.setCaretColor(ink);
        paperText.setSelectionColor(new Color(215, 214, 187));
        paperText.setSelectedTextColor(Color.BLACK);
        paperText.setMargin(new Insets(10, 12, 12, 12));
        paperText.setLineWrap(lineWrap);
        paperText.setWrapStyleWord(true);
        paperText.setTabSize(4);
        paperText.setBorder(null);
        paperText.setFocusTraversalKeysEnabled(false);

        JScrollPane paperScroll = new JScrollPane(paperText);
        paperScroll.setBorder(BorderFactory.createEmptyBorder());
        paperScroll.setHorizontalScrollBarPolicy(
                JScrollPane.HORIZONTAL_SCROLLBAR_NEVER
        );
        paperScroll.setVerticalScrollBarPolicy(
                JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED
        );

        machine.setPaper(paperScroll);

        paperText.getDocument().addDocumentListener(new DocumentListener() {
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
        });
    }

    private void configurePhysicalKeyboard() {
        paperText.getInputMap().put(
                KeyStroke.getKeyStroke("CAPS_LOCK"),
                "toggleCaps"
        );

        paperText.getActionMap().put("toggleCaps", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                capsLock = !capsLock;
                status.setText(capsLock ? "CAPS LOCK ON" : "CAPS LOCK OFF");
            }
        });

        paperText.getInputMap().put(
                KeyStroke.getKeyStroke("pressed SHIFT"),
                "shiftOn"
        );

        paperText.getActionMap().put("shiftOn", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                shiftDown = true;
            }
        });

        paperText.getInputMap().put(
                KeyStroke.getKeyStroke("released SHIFT"),
                "shiftOff"
        );

        paperText.getActionMap().put("shiftOff", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                shiftDown = false;
            }
        });
    }

    private void configureCursor() {
        cursorTimer = new Timer(500, e -> {
            if (cursorBlink) {
                paperText.getCaret().setVisible(
                        !paperText.getCaret().isVisible()
                );
            } else {
                paperText.getCaret().setVisible(true);
            }
        });
        cursorTimer.start();
    }

    private void updateCounts() {
        String text = paperText.getText();
        String trimmed = text.trim();

        int words = trimmed.isEmpty()
                ? 0
                : trimmed.split("\\s+").length;

        wordCount.setText("WORDS " + words);
        charCount.setText("CHARACTERS " + text.length());
    }

    private void pressKey(String key) {
        paperText.requestFocusInWindow();

        switch (key) {
            case "CAPS":
                capsLock = !capsLock;
                status.setText(capsLock ? "CAPS LOCK ON" : "CAPS LOCK OFF");
                machine.updateKeyLabels();
                return;

            case "SHIFT":
                shiftDown = !shiftDown;
                status.setText(shiftDown ? "SHIFT ON" : "SHIFT OFF");
                machine.updateKeyLabels();
                return;

            case "SPACE":
                paperText.replaceSelection(" ");
                break;

            case "ENTER":
                paperText.replaceSelection("\n");
                break;

            case "BACKSPACE":
                deletePrevious();
                break;

            case "TAB":
                paperText.replaceSelection("    ");
                break;

            case "↑":
            case "↓":
                status.setText(key + " KEY");
                return;

            default:
                String output = key;

                if (key.length() == 1) {
                    char character = key.charAt(0);

                    if (Character.isLetter(character)) {
                        output = (capsLock ^ shiftDown)
                                ? key.toUpperCase()
                                : key.toLowerCase();
                    } else if (shiftDown) {
                        output = shiftedCharacter(key);
                    }

                    paperText.replaceSelection(output);

                    if (shiftDown) {
                        shiftDown = false;
                        machine.updateKeyLabels();
                    }
                } else {
                    paperText.replaceSelection(key);
                }
                break;
        }

        paperText.requestFocusInWindow();
    }

    private String shiftedCharacter(String key) {
        switch (key) {
            case "1": return "!";
            case "2": return "@";
            case "3": return "#";
            case "4": return "$";
            case "5": return "%";
            case "6": return "^";
            case "7": return "&";
            case "8": return "*";
            case "9": return "(";
            case "0": return ")";
            case "-": return "_";
            case "=": return "+";
            case "[": return "{";
            case "]": return "}";
            case "\\": return "|";
            case ";": return ":";
            case "'": return "\"";
            case ",": return "<";
            case ".": return ">";
            case "/": return "?";
            case "`": return "~";
            default: return key;
        }
    }

    private void deletePrevious() {
        int start = paperText.getSelectionStart();
        int end = paperText.getSelectionEnd();

        if (start != end) {
            paperText.replaceSelection("");
        } else if (start > 0) {
            paperText.select(start - 1, start);
            paperText.replaceSelection("");
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

        Thread thread = new Thread(() -> {
            float rate = 22050f;
            int count = (int) (rate * 0.024);
            byte[] samples = new byte[count];

            double frequency = ThreadLocalRandom.current()
                    .nextDouble(650, 1500);

            for (int i = 0; i < count; i++) {
                double decay = Math.exp(-i / (rate * 0.006));
                double tone = Math.sin(2 * Math.PI * frequency * i / rate);
                double noise = ThreadLocalRandom.current()
                        .nextDouble(-0.35, 0.35);

                samples[i] = (byte) ((tone * 0.65 + noise * 0.35)
                        * decay * 105);
            }

            AudioFormat format = new AudioFormat(rate, 8, 1, true, false);

            try (SourceDataLine line = AudioSystem.getSourceDataLine(format)) {
                line.open(format, samples.length);
                line.start();
                line.write(samples, 0, samples.length);
                line.drain();
            } catch (Exception ignored) {
                Toolkit.getDefaultToolkit().beep();
            }
        }, "TypewriterSound");

        thread.setDaemon(true);
        thread.start();
    }

    private void openSettings() {
        JDialog dialog = new JDialog(this, "TYPEWRITER SETTINGS", true);
        dialog.setSize(430, 350);
        dialog.setResizable(false);
        dialog.setLocationRelativeTo(this);
        dialog.getContentPane().setBackground(new Color(38, 40, 32));
        dialog.setLayout(new BorderLayout(10, 10));

        JLabel heading = new JLabel(
                "MACHINE PREFERENCES",
                SwingConstants.CENTER
        );
        heading.setForeground(new Color(232, 225, 190));
        heading.setFont(new Font("Monospaced", Font.BOLD, 17));
        heading.setBorder(new EmptyBorder(14, 8, 4, 8));

        JPanel options = new JPanel();
        options.setBackground(new Color(38, 40, 32));
        options.setBorder(new EmptyBorder(10, 22, 10, 22));
        options.setLayout(new BoxLayout(options, BoxLayout.Y_AXIS));

        JCheckBox sound = settingBox("Typing sounds", soundEnabled);
        JCheckBox blink = settingBox("Blinking cursor", cursorBlink);
        JCheckBox wrap = settingBox("Wrap text on paper", lineWrap);

        JLabel fontLabel = new JLabel("TYPE SIZE: " + fontSize);
        fontLabel.setForeground(new Color(232, 225, 190));
        fontLabel.setFont(new Font("Monospaced", Font.BOLD, 13));

        JSlider fontSlider = new JSlider(12, 30, fontSize);
        fontSlider.setBackground(new Color(38, 40, 32));
        fontSlider.setForeground(new Color(232, 225, 190));
        fontSlider.setMajorTickSpacing(6);
        fontSlider.setPaintTicks(true);
        fontSlider.setPaintLabels(true);
        fontSlider.addChangeListener(e ->
                fontLabel.setText("TYPE SIZE: " + fontSlider.getValue())
        );

        options.add(sound);
        options.add(Box.createVerticalStrut(8));
        options.add(blink);
        options.add(Box.createVerticalStrut(8));
        options.add(wrap);
        options.add(Box.createVerticalStrut(14));
        options.add(fontLabel);
        options.add(fontSlider);

        JButton apply = toolbarButton("APPLY");
        JButton cancel = toolbarButton("CANCEL");

        JPanel actions = new JPanel(new FlowLayout());
        actions.setBackground(new Color(38, 40, 32));
        actions.add(apply);
        actions.add(cancel);

        apply.addActionListener(e -> {
            soundEnabled = sound.isSelected();
            cursorBlink = blink.isSelected();
            lineWrap = wrap.isSelected();
            fontSize = fontSlider.getValue();

            paperText.setFont(
                    new Font(Font.MONOSPACED, Font.PLAIN, fontSize)
            );
            paperText.setLineWrap(lineWrap);
            paperText.setWrapStyleWord(lineWrap);
            paperText.getCaret().setVisible(true);

            status.setText("PREFERENCES UPDATED");
            dialog.dispose();
            paperText.requestFocusInWindow();
        });

        cancel.addActionListener(e -> dialog.dispose());

        dialog.add(heading, BorderLayout.NORTH);
        dialog.add(options, BorderLayout.CENTER);
        dialog.add(actions, BorderLayout.SOUTH);
        dialog.setVisible(true);
    }

    private JCheckBox settingBox(String text, boolean selected) {
        JCheckBox box = new JCheckBox(text, selected);
        box.setBackground(new Color(38, 40, 32));
        box.setForeground(new Color(232, 225, 190));
        box.setFont(new Font("Monospaced", Font.PLAIN, 13));
        box.setFocusPainted(false);
        return box;
    }

    private void newPage() {
        if (!paperText.getText().isEmpty()) {
            int answer = JOptionPane.showConfirmDialog(
                    this,
                    "Start a new page? Make sure your work is saved.",
                    "NEW PAGE",
                    JOptionPane.YES_NO_OPTION
            );

            if (answer != JOptionPane.YES_OPTION) {
                return;
            }
        }

        paperText.setText("");
        currentFile = null;
        status.setText("NEW PAGE");
        paperText.requestFocusInWindow();
    }

    private void openFile() {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("OPEN DOCUMENT");

        if (chooser.showOpenDialog(this) != JFileChooser.APPROVE_OPTION) {
            return;
        }

        File file = chooser.getSelectedFile();

        try {
            paperText.setText(Files.readString(
                    file.toPath(),
                    StandardCharsets.UTF_8
            ));
            paperText.setCaretPosition(0);
            currentFile = file;
            status.setText("OPENED " + file.getName());
        } catch (IOException ex) {
            showError("Could not open the file:\n" + ex.getMessage());
        }

        paperText.requestFocusInWindow();
    }

    private void saveFile() {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("SAVE DOCUMENT");
        chooser.setSelectedFile(
                currentFile == null
                        ? new File("my-document.txt")
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
                    JOptionPane.YES_NO_OPTION
            );

            if (answer != JOptionPane.YES_OPTION) {
                return;
            }
        }

        try {
            Files.writeString(
                    file.toPath(),
                    paperText.getText(),
                    StandardCharsets.UTF_8
            );
            currentFile = file;
            status.setText("SAVED " + file.getName());
        } catch (IOException ex) {
            showError("Could not save the file:\n" + ex.getMessage());
        }

        paperText.requestFocusInWindow();
    }

    private void exitApplication() {
        if (!paperText.getText().isEmpty()) {
            int answer = JOptionPane.showConfirmDialog(
                    this,
                    "Exit the typewriter? Make sure your work is saved.",
                    "EXIT",
                    JOptionPane.YES_NO_OPTION
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
    }

    private class MachinePanel extends JPanel {

        private JScrollPane paper;
        private final List<JButton> keys = new ArrayList<>();
        private final List<String> labels = new ArrayList<>();

        private final String[][] rows = {
                {"1", "2", "3", "4", "5", "6", "7", "8", "9", "0", "-", "=", "BACKSPACE"},
                {"TAB", "Q", "W", "E", "R", "T", "Y", "U", "I", "O", "P", "[", "]"},
                {"CAPS", "A", "S", "D", "F", "G", "H", "J", "K", "L", ";", "'", "ENTER"},
                {"SHIFT", "Z", "X", "C", "V", "B", "N", "M", ",", ".", "/", "↑", "↓"},
                {"SPACE"}
        };

        MachinePanel() {
            setLayout(null);
            setBackground(machineBackground);

            for (String[] row : rows) {
                for (String label : row) {
                    JButton key = new JButton(displayLabel(label));

                    key.setFont(new Font(
                            "Monospaced",
                            Font.BOLD,
                            label.length() > 4 ? 9 : 13
                    ));
                    key.setForeground(Color.BLACK);
                    key.setBackground(new Color(145, 145, 111));
                    key.setOpaque(true);
                    key.setFocusPainted(false);
                    key.setBorder(new CompoundBorder(
                            new LineBorder(new Color(91, 91, 70), 2),
                            new EmptyBorder(3, 1, 3, 1)
                    ));

                    key.addActionListener(e -> pressKey(label));

                    keys.add(key);
                    labels.add(label);
                    add(key);
                }
            }
        }

        String displayLabel(String label) {
            if (label.length() == 1 && Character.isLetter(label.charAt(0))) {
                return (capsLock ^ shiftDown)
                        ? label.toUpperCase()
                        : label.toLowerCase();
            }

            if (shiftDown) {
                return shiftedCharacter(label);
            }

            return label;
        }

        void updateKeyLabels() {
            for (int i = 0; i < keys.size(); i++) {
                keys.get(i).setText(displayLabel(labels.get(i)));
            }
        }

        void setPaper(JScrollPane paper) {
            this.paper = paper;
            add(paper);
        }

        @Override
        public void doLayout() {
            int w = getWidth();
            int h = getHeight();

            if (paper != null) {
                int paperW = (int) (w * 0.43);
                int paperH = (int) (h * 0.30);
                int paperX = (w - paperW) / 2;

                paper.setBounds(paperX, 22, paperW, paperH);
            }

            int bodyY = (int) (h * 0.35);
            int keyTop = (int) (h * 0.64);
            int keyBottom = h - 25;
            int rowHeight = Math.max(28, (keyBottom - keyTop - 20) / 5);
            int gap = 4;
            int index = 0;

            for (int r = 0; r < rows.length; r++) {
                String[] row = rows[r];
                int availableW = w - 100;
                int totalGaps = gap * (row.length - 1);
                int keyW = Math.max(24, (availableW - totalGaps) / row.length);
                int x = (w - (keyW * row.length + totalGaps)) / 2;
                int y = keyTop + r * (rowHeight + 4);

                if (row.length == 1 && row[0].equals("SPACE")) {
                    keyW = Math.min(270, w - 180);
                    x = (w - keyW) / 2;
                }

                for (String label : row) {
                    JButton key = keys.get(index++);
                    int actualW = keyW;

                    if (label.equals("BACKSPACE")) {
                        actualW = (int) (keyW * 1.25);
                    } else if (label.equals("ENTER")) {
                        actualW = (int) (keyW * 1.05);
                    } else if (label.equals("CAPS") || label.equals("SHIFT")
                            || label.equals("TAB")) {
                        actualW = (int) (keyW * 1.15);
                    }

                    key.setBounds(x, y, actualW, rowHeight);
                    x += actualW + gap;
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

            int w = getWidth();
            int h = getHeight();

            g.setColor(machineBackground);
            g.fillRect(0, 0, w, h);

            int bodyX = 35;
            int bodyY = (int) (h * 0.36);
            int bodyW = w - 70;
            int bodyH = h - bodyY - 12;

            g.setColor(new Color(72, 68, 55, 65));
            g.fillRoundRect(
                    bodyX + 8, bodyY + 12, bodyW, bodyH, 35, 35
            );

            GradientPaint bodyGradient = new GradientPaint(
                    bodyX, bodyY, olive,
                    bodyX, bodyY + bodyH, oliveDark
            );

            g.setPaint(bodyGradient);
            g.fillRoundRect(bodyX, bodyY, bodyW, bodyH, 35, 35);

            g.setColor(metal);
            g.setStroke(new BasicStroke(4));
            g.drawRoundRect(
                    bodyX + 2, bodyY + 2, bodyW - 4, bodyH - 4, 33, 33
            );

            g.setColor(oliveDark);
            g.fillRoundRect(
                    bodyX + 25, bodyY + 15,
                    bodyW - 50, (int) (bodyH * 0.25), 18, 18
            );

            int rollerY = bodyY + (int) (bodyH * 0.27);

            g.setColor(new Color(37, 36, 30));
            g.fillRoundRect(
                    bodyX + 42, rollerY, bodyW - 84, 26, 12, 12
            );

            g.setColor(new Color(165, 160, 134));
            g.setStroke(new BasicStroke(3));
            g.drawLine(
                    bodyX + 44, rollerY + 5,
                    bodyX + bodyW - 44, rollerY + 5
            );

            g.setColor(new Color(51, 49, 39));
            g.fillRoundRect(
                    bodyX + 45, bodyY + 45, bodyW - 90, 22, 8, 8
            );

            g.setColor(new Color(178, 172, 145));
            g.fillRoundRect(
                    bodyX + 35, bodyY + 65, bodyW - 70, 13, 7, 7
            );

            g.setColor(new Color(94, 95, 72));
            g.fillRoundRect(
                    bodyX + 25, bodyY + 80, bodyW - 50, 12, 5, 5
            );

            g.setColor(new Color(54, 54, 43));
            g.fillRoundRect(
                    bodyX + 50, bodyY + 95, bodyW - 100, 18, 8, 8
            );

            g.setColor(new Color(189, 182, 151));
            g.setFont(new Font("Serif", Font.ITALIC | Font.BOLD, 19));
            g.drawString("OLYMPIA", bodyX + bodyW - 145, bodyY + 48);

            g.setColor(new Color(61, 61, 48));
            g.fillRoundRect(
                    bodyX + 45, bodyY + bodyH - 17,
                    bodyW - 90, 12, 5, 5
            );

            g.dispose();
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(
                        UIManager.getSystemLookAndFeelClassName()
                );
            } catch (Exception ignored) {
            }

            new RetroDigitalTypewriter().setVisible(true);
        });
    }
}