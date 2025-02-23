package selector;
import scissors.ScissorsSelectionModel;
import static selector.SelectionModel.SelectionState.NO_SELECTION;
import static selector.SelectionModel.SelectionState.PROCESSING;
import static selector.SelectionModel.SelectionState.SELECTED;
import static selector.SelectionModel.SelectionState.SELECTING;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import javax.imageio.ImageIO;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.filechooser.FileNameExtensionFilter;
import scissors.ScissorsSelectionModel;
import selector.SelectionModel.SelectionState;

/**
 * A graphical application for selecting and extracting regions of images.
 */
public class SelectorApp implements PropertyChangeListener {

    /**
     * Our application window.  Disposed when application exits.
     */
    private final JFrame frame;

    /**
     * Component for displaying the current image and selection tool.
     */
    private final ImagePanel imgPanel;

    /**
     * The current state of the selection tool.  Must always match the model used by `imgPanel`.
     */
    private SelectionModel model;

    /* Components whose state must be changed during the selection process. */
    private JMenuItem saveItem;
    private JMenuItem undoItem;
    private JButton cancelButton;
    private JButton undoButton;
    private JButton resetButton;
    private JButton finishButton;
    private final JLabel statusLabel;
    // New in A6
    /**
     * Progress bar to indicate the progress of a model that needs to do long calculations in a
     * PROCESSING state.
     */
    private JProgressBar processingProgress;

    /**
     * Construct a new application instance.  Initializes GUI components, so must be invoked on the
     * Swing Event Dispatch Thread.  Does not show the application window (call `start()` to do
     * that).
     */
    public SelectorApp() {
        // Initialize application window
        frame = new JFrame("Selector");
        frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

        // Add status bar
        statusLabel = new JLabel();
        frame.add(statusLabel, BorderLayout.PAGE_END);
        // Add image component with scrollbars
        imgPanel = new ImagePanel();
        JScrollPane scrollImagePanel = new JScrollPane(imgPanel);
        frame.add(scrollImagePanel, BorderLayout.CENTER);
        frame.setPreferredSize(new Dimension(600, 500));

        // Add menu bar
        frame.setJMenuBar(makeMenuBar());
        // Add control buttons
        frame.add(makeControlPanel(), BorderLayout.LINE_END);
        // Controller: Set initial selection tool and update components to reflect its state
        setSelectionModel(new PointToPointSelectionModel(true));
        // New in A6: Add progress bar
        processingProgress = new JProgressBar();
        frame.add(processingProgress, BorderLayout.PAGE_START);
    }

    /**
     * Create and populate a menu bar with our application's menus and items and attach listeners.
     * Should only be called from constructor, as it initializes menu item fields.
     */
    private JMenuBar makeMenuBar() {
        JMenuBar menuBar = new JMenuBar();

        // Create and populate File menu
        JMenu fileMenu = new JMenu("File");
        menuBar.add(fileMenu);
        JMenuItem openItem = new JMenuItem("Open...");
        fileMenu.add(openItem);
        saveItem = new JMenuItem("Save...");
        fileMenu.add(saveItem);
        JMenuItem closeItem = new JMenuItem("Close");
        fileMenu.add(closeItem);
        JMenuItem exitItem = new JMenuItem("Exit");
        fileMenu.add(exitItem);

        // Create and populate Edit menu
        JMenu editMenu = new JMenu("Edit");
        menuBar.add(editMenu);
        undoItem = new JMenuItem("Undo");
        editMenu.add(undoItem);

        // embellishment (keyboard shortcuts)
        openItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_O, InputEvent.ALT_MASK));
        openItem.getAccessibleContext().setAccessibleDescription("Open an existing file");

        saveItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_S, InputEvent.ALT_MASK));
        saveItem.getAccessibleContext().setAccessibleDescription("Save the current file");

        closeItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_C, InputEvent.ALT_MASK));
        closeItem.getAccessibleContext().setAccessibleDescription("Close the current file");

        exitItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_X, InputEvent.ALT_MASK));
        exitItem.getAccessibleContext().setAccessibleDescription("Exit");

        undoItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_Z, InputEvent.ALT_MASK));
        undoItem.getAccessibleContext().setAccessibleDescription("Undo the last action");

        // Controller: Attach menu item listeners
        openItem.addActionListener(e -> openImage());
        closeItem.addActionListener(e -> imgPanel.setImage(null));
        saveItem.addActionListener(e -> saveSelection());
        exitItem.addActionListener(e -> frame.dispose());
        undoItem.addActionListener(e -> model.undo());

        return menuBar;
    }

    /**
     * Return a panel containing buttons for controlling image selection.  Should only be called
     * from constructor, as it initializes button fields.
     */
    private JPanel makeControlPanel() {
        JPanel control = new JPanel();
        GridLayout buttons = new GridLayout(4, 1);
        control.setLayout(buttons);
        cancelButton = new JButton("Cancel");
        cancelButton.addActionListener(e -> model.cancelProcessing());
        undoButton = new JButton("Undo");
        undoButton.addActionListener(e -> model.undo());
        resetButton = new JButton("Reset");
        resetButton.addActionListener(e -> model.reset());
        finishButton = new JButton("Finish");
        finishButton.addActionListener(e -> model.finishSelection());
        control.add(cancelButton);
        control.add(undoButton);
        control.add(resetButton);
        control.add(finishButton);
        // TODO A6.0a: Add a widget to your control panel allowing the user to choose which
        //  selection model to use.  We recommend using a `JComboBox` [1].  To start with, the user
        //  should be able to choose between the following options:
        //  1. Point-to-point (`PointToPointSelectionModel`).
        //  2. Intelligent scissors: gray (`ScissorsSelectionModel` with a "CrossGradMono" weight
        //     name).  You will need to `import scissors.ScissorsSelectionModel` to use this class.
        //  When an item is selected, you should construct a new `SelectionModel` of the appropriate
        //  class, passing the previous `model` object to the constructor so that any existing
        //  selection is preserved.  Then you should call `setSelectionModel()` with your new model
        //  object.
        //  [1] https://docs.oracle.com/javase/tutorial/uiswing/components/combobox.html
        String[] options = {"Point-to-point", "Intelligent scissors: gray",
                "Intelligent scissors: colorful" };
        JComboBox<String> modelList = new JComboBox<>(options);
        modelList.addActionListener(e -> {
            JComboBox<String> cb = (JComboBox<String>) e.getSource();
            String optionName = (String)cb.getSelectedItem();
            if(optionName.equals("Point-to-point")){
                setSelectionModel(new PointToPointSelectionModel(model));
            } else if (optionName.equals("Intelligent scissors: gray")) {
                setSelectionModel(new ScissorsSelectionModel("CrossGradMono",model));
            } else if (optionName.equals("Intelligent scissors: colorful")) {
                setSelectionModel(new ScissorsSelectionModel("ColoredWeight",model));
            }
        });
        control.add(modelList);
        return control;
    }

    /**
     * Start the application by showing its window.
     */
    public void start() {
        // Compute ideal window size
        frame.pack();

        frame.setVisible(true);
    }

    /**
     * React to property changes in an observed model.  Supported properties include: * "state":
     * Update components to reflect the new selection state.
     */
    @Override
    public void propertyChange(PropertyChangeEvent evt) {
        if ("state".equals(evt.getPropertyName())) {
            reflectSelectionState(model.state());
        }
        // TODO A6.0b: Update the progress bar [1] as follows:
        //  * When the model transitions into the PROCESSING state, set the progress bar to
        //    "indeterminate" mode.  That way, the user sees that something is happening even before
        //    the model has an estimate of its progress.
        //  * When the model transitions to any other state, ensure that the progress bar's value is
        //    0 and that it is not in "indeterminate" mode.  The progress bar should look inert if
        //    the model isn't currently processing.
        //  * Upon receiving a "progress" property change, set the progress bar's value to the new
        //    progress value (which will be an integer in [0..100]) and ensure it is not in
        //    "indeterminate" mode.  You need to use the event object to get the new value.
        //  [1] https://docs.oracle.com/javase/tutorial/uiswing/components/progress.html
        if(evt.getPropertyName().equals("state")) {
            if (model.state().equals(SelectionState.PROCESSING)) {
                processingProgress.setIndeterminate(true);
            } else {
                processingProgress.setValue(0);
                processingProgress.setIndeterminate(false);
            }
        }
        if(evt.getPropertyName().equals("progress")){
            if(evt.getNewValue() instanceof Integer){
                processingProgress.setValue((int)evt.getNewValue());
                processingProgress.setStringPainted(true);

            }
            processingProgress.setIndeterminate(false);
        }
    }

    /**
     * Update components to reflect a selection state of `state`.  Disable buttons and menu items
     * whose actions are invalid in that state, and update the status bar.
     */
    private void reflectSelectionState(SelectionState state) {
        // Update status bar to show current state
        statusLabel.setText(state.toString());
        if (state == NO_SELECTION) {
            cancelButton.setEnabled(false);
            undoButton.setEnabled(false);
            resetButton.setEnabled(false);
            finishButton.setEnabled(false);
        } else if (state == SELECTING) {
            cancelButton.setEnabled(true);
            undoButton.setEnabled(true);
            resetButton.setEnabled(true);
            finishButton.setEnabled(true);
        } else if (state == SELECTED) {
            cancelButton.setEnabled(false);
            undoButton.setEnabled(true);
            resetButton.setEnabled(true);
            finishButton.setEnabled(false);
        }
        saveItem.setEnabled(state == SELECTED);
    }

    /**
     * Return the model of the selection tool currently in use.
     */
    public SelectionModel getSelectionModel() {
        return model;
    }

    /**
     * Use `newModel` as the selection tool and update our view to reflect its state.  This
     * application will no longer respond to changes made to its previous selection model and will
     * instead respond to property changes from `newModel`.
     */
    public void setSelectionModel(SelectionModel newModel) {
        // Stop listening to old model
        if (model != null) {
            model.removePropertyChangeListener(this);
        }

        imgPanel.setSelectionModel(newModel);
        model = imgPanel.selection();
        model.addPropertyChangeListener("state", this);
        // New in A6: Listen for "progress" events
        model.addPropertyChangeListener("progress", this);
        // Since the new model's initial state may be different from the old model's state, manually
        //  trigger an update to our state-dependent view.
        reflectSelectionState(model.state());
    }

    /**
     * Start displaying and selecting from `img` instead of any previous image.  Argument may be
     * null, in which case no image is displayed and the current selection is reset.
     */
    public void setImage(BufferedImage img) {
        imgPanel.setImage(img);
    }

    /**
     * Allow the user to choose a new image from an "open" dialog.  If they do, start displaying and
     * selecting from that image.  Show an error message dialog (and retain any previous image) if
     * the chosen image could not be opened.
     */
    private void openImage() {
        JFileChooser chooser = new JFileChooser();
        // Start browsing in current directory
        chooser.setCurrentDirectory(new File(System.getProperty("user.dir")));
        // Filter for file extensions supported by Java's ImageIO readers
        chooser.setFileFilter(new FileNameExtensionFilter("Image files",
                ImageIO.getReaderFileSuffixes()));
        int returnVal = chooser.showOpenDialog(frame);

        BufferedImage img = null;
        try {
            if (returnVal == JFileChooser.APPROVE_OPTION) {
                File file = chooser.getSelectedFile();
                img = ImageIO.read(file);
                if (img != null) {
                    this.setImage(img);
                } else {
                    JOptionPane.showMessageDialog(frame, "Not an image file",
                            "File Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        } catch (IOException e) {
            JOptionPane.showMessageDialog(frame, "Not an image file",
                    "File Error", JOptionPane.ERROR_MESSAGE);
            openImage();
        }


    }

    /**
     * Save the selected region of the current image to a file selected from a "save" dialog. Show
     * an error message dialog if the image could not be saved.
     */
    private void saveSelection() {

        JFileChooser chooser = new JFileChooser();
        chooser.setCurrentDirectory(new File(System.getProperty("user.dir")));
        chooser.setFileFilter(new FileNameExtensionFilter("PNG images", "png"));

        chooser.setDialogTitle("Save file");
        int returnVal = chooser.showSaveDialog(frame);
        if (returnVal == JFileChooser.APPROVE_OPTION) {
            File file = chooser.getSelectedFile();
            if (!file.getName().toLowerCase().endsWith(".png")) {
                file = new File(file.getAbsolutePath() + ".png");
            }
            if (file.exists()) {
                int result = JOptionPane.showConfirmDialog(frame, file.getName() +
                                " already exists. Overwrite?", "Confirm Overwrite",
                        JOptionPane.YES_NO_CANCEL_OPTION);
                if (result != JOptionPane.YES_OPTION) {
                    return;
                }
            }
            try {
                FileOutputStream out = new FileOutputStream(file);
                model.saveSelection(out);
            } catch (IOException e) {
                JOptionPane.showMessageDialog(frame, "Unable to save",
                        "File Error", JOptionPane.ERROR_MESSAGE);
            }
        }


    }

    /**
     * Run an instance of SelectorApp.  No program arguments are expected.
     */
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            // Set Swing theme to look the same (and less old) on all operating systems.
            try {
                UIManager.setLookAndFeel("javax.swing.plaf.nimbus.NimbusLookAndFeel");
            } catch (Exception ignored) {
                /* If the Nimbus theme isn't available, just use the platform default. */
            }

            // Create and start the app
            SelectorApp app = new SelectorApp();
            app.start();
        });
    }
}