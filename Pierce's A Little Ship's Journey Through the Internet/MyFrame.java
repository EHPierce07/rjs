import java.awt.*;
import java.io.File;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.filechooser.FileNameExtensionFilter;

public class MyFrame extends JFrame {

    private final MyPanel panel;
    private final JComboBox<String> sceneMenu;
    private boolean choosingImage = false;

    public MyFrame() {
        panel = new MyPanel();

        setTitle("A Little Ship's Journey Through Tech-alanay");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        JPanel controls = new JPanel(new FlowLayout());
        controls.setBorder(new EmptyBorder(5, 5, 5, 5));

        controls.add(new JLabel("Scene:"));

        String[] scenes = {
            "Space", "Earth", "Digital Wires"
        };

        sceneMenu = new JComboBox<>(scenes);

        sceneMenu.addActionListener(e -> {
            if (choosingImage || panel.hasArrived()) {
                return;
            }

            panel.setScene(
                (String) sceneMenu.getSelectedItem()
            );
        });

        controls.add(sceneMenu);

        JButton chooseButton = new JButton("Choose Final Image");
        chooseButton.addActionListener(e -> chooseFinalImage());
        controls.add(chooseButton);

        JButton restartButton = new JButton("Restart Journey");
        restartButton.addActionListener(e -> {
            panel.restartJourney();
            sceneMenu.setSelectedItem("Space");
            chooseButton.setEnabled(true);
            sceneMenu.setEnabled(true);
        });

        controls.add(restartButton);

        add(controls, BorderLayout.NORTH);
        add(panel, BorderLayout.CENTER);

        pack();
        setMinimumSize(new Dimension(600, 400));
        setLocationRelativeTo(null);
        setVisible(true);
    }

    private void chooseFinalImage() {
        if (choosingImage || panel.hasArrived()) {
            return;
        }

        choosingImage = true;

        try {
            JFileChooser chooser = new JFileChooser();
            chooser.setDialogTitle("Choose Tech-alanay Destination");

            chooser.setFileFilter(new FileNameExtensionFilter(
                "Image Files (PNG, JPG, JPEG, GIF, BMP)",
                "png", "jpg", "jpeg", "gif", "bmp"
            ));

            int result = chooser.showOpenDialog(this);

            if (result == JFileChooser.APPROVE_OPTION) {
                File file = chooser.getSelectedFile();

                if (panel.setCustomBackground(file)) {
                    sceneMenu.setSelectedItem("Space");
                    sceneMenu.setEnabled(false);

                }
            }
        } finally {
            choosingImage = false;
        }
    }
}