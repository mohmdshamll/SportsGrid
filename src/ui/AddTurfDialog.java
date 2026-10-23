package ui;

import model.Turf;
import model.TurfOwner;
import service.TurfService;
import ui.theme.Theme;
import ui.theme.ThemeChangeListener;
import ui.theme.ThemeManager;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

/**
 * Dialog for turf owners to list a new sports turf.
 *
 * All new turfs start with is_approved = FALSE in MySQL.
 * An administrator must review and approve them before they are
 * made visible to players on the booking platform.
 */
public class AddTurfDialog extends JDialog implements ThemeChangeListener {

    private final TurfOwner owner;
    private final TurfService turfService = new TurfService();
    private final Runnable onTurfAdded;

    // Container panels
    private JPanel root;
    private JPanel card;

    // UI elements
    private JLabel title;
    private JLabel subtitle;
    private JLabel nameLabel;
    private JTextField nameField;
    private JLabel sportLabel;
    private JComboBox<String> sportCombo;
    private JLabel locationLabel;
    private JTextField locationField;
    private JLabel priceLabel;
    private JTextField priceField;
    private JLabel notice;
    private JLabel errorLabel;
    private JButton submitBtn;
    private JButton cancelBtn;

    public AddTurfDialog(JFrame parent, TurfOwner owner, Runnable onTurfAdded) {
        super(parent, "SportGrid – Add New Turf", true);
        this.owner = owner;
        this.onTurfAdded = onTurfAdded;

        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setResizable(false);
        buildUI();

        pack();
        setSize(520, 620);
        setMinimumSize(new Dimension(480, 580));
        setLocationRelativeTo(parent);

        ThemeManager.addListener(this);
    }

    private void buildUI() {
        Theme theme = ThemeManager.getTheme();

        root = new JPanel(new BorderLayout(0, 14));
        root.setBackground(theme.getBgMain());
        root.setBorder(new EmptyBorder(22, 28, 22, 28));
        setContentPane(root);

        // Header
        JPanel header = new JPanel(new BorderLayout(0, 4));
        header.setOpaque(false);

        JPanel titlePanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        titlePanel.setOpaque(false);
        JLabel icon = new JLabel("🏟️");
        icon.setFont(StyleConstants.FONT_TITLE);
        titlePanel.add(icon);

        title = new JLabel("Add New Turf Listing");
        title.setFont(StyleConstants.FONT_TITLE);
        title.setForeground(theme.getAccentGreen());
        titlePanel.add(title);
        header.add(titlePanel, BorderLayout.NORTH);

        subtitle = new JLabel("List your sports ground for player reservations");
        subtitle.setFont(StyleConstants.FONT_SMALL);
        subtitle.setForeground(theme.getTextMuted());
        header.add(subtitle, BorderLayout.SOUTH);
        root.add(header, BorderLayout.NORTH);

        // Form Card
        card = new JPanel(new GridBagLayout());
        card.setBackground(theme.getBgCard());
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(theme.getBorder(), 1, true),
                new EmptyBorder(18, 20, 18, 20)));

        GridBagConstraints c = new GridBagConstraints();
        c.fill = GridBagConstraints.HORIZONTAL;
        c.weightx = 1;
        c.gridx = 0;
        int row = 0;

        // Turf Name
        c.gridy = row++; c.insets = new Insets(0, 0, 2, 0);
        nameLabel = StyleConstants.label("Turf / Arena Name:");
        card.add(nameLabel, c);
        nameField = StyleConstants.styledField();
        nameField.setToolTipText("e.g. Green Arena, KickOff Turf");
        c.gridy = row++; c.insets = new Insets(0, 0, 10, 0);
        card.add(nameField, c);

        // Sport Type
        c.gridy = row++; c.insets = new Insets(0, 0, 2, 0);
        sportLabel = StyleConstants.label("Sport Category:");
        card.add(sportLabel, c);
        String[] sports = {"Football", "Cricket", "Badminton", "Tennis", "Basketball", "Swimming", "Futsal", "Volleyball"};
        sportCombo = new JComboBox<>(sports);
        StyleConstants.styleComboBox(sportCombo);
        c.gridy = row++; c.insets = new Insets(0, 0, 10, 0);
        card.add(sportCombo, c);

        // Location
        c.gridy = row++; c.insets = new Insets(0, 0, 2, 0);
        locationLabel = StyleConstants.label("Location / Address:");
        card.add(locationLabel, c);
        locationField = StyleConstants.styledField();
        locationField.setToolTipText("e.g. Kakkanad, Kochi or MG Road, Bangalore");
        c.gridy = row++; c.insets = new Insets(0, 0, 10, 0);
        card.add(locationField, c);

        // Price per Hour
        c.gridy = row++; c.insets = new Insets(0, 0, 2, 0);
        priceLabel = StyleConstants.label("Hourly Rate (₹ / hour):");
        card.add(priceLabel, c);
        priceField = StyleConstants.styledField();
        priceField.setToolTipText("e.g. 1200.00");
        c.gridy = row++; c.insets = new Insets(0, 0, 12, 0);
        card.add(priceField, c);

        // Notice about approval requirement
        String greenHex = String.format("#%02x%02x%02x", theme.getAccentGreen().getRed(), theme.getAccentGreen().getGreen(), theme.getAccentGreen().getBlue());
        String mutedHex = String.format("#%02x%02x%02x", theme.getTextMuted().getRed(), theme.getTextMuted().getGreen(), theme.getTextMuted().getBlue());
        notice = new JLabel(
                String.format("<html><font color='%s'>ℹ️ Admin Approval Required:</font><br>"
                        + "<font color='%s'>Newly created turfs are reviewed by administrators before being published to players.</font></html>",
                        greenHex, mutedHex));
        notice.setFont(StyleConstants.FONT_SMALL);
        c.gridy = row++; c.insets = new Insets(0, 0, 0, 0);
        card.add(notice, c);

        root.add(card, BorderLayout.CENTER);

        // Bottom error + actions
        JPanel bottom = new JPanel(new BorderLayout(0, 8));
        bottom.setOpaque(false);

        errorLabel = new JLabel(" ");
        errorLabel.setFont(StyleConstants.FONT_SMALL);
        errorLabel.setForeground(theme.getStatusError());
        errorLabel.setHorizontalAlignment(SwingConstants.CENTER);
        bottom.add(errorLabel, BorderLayout.NORTH);

        JPanel btnRow = new JPanel(new GridLayout(1, 2, 12, 0));
        btnRow.setOpaque(false);

        cancelBtn = StyleConstants.secondaryButton("Cancel");
        cancelBtn.addActionListener(e -> {
            ThemeManager.removeListener(this);
            dispose();
        });
        btnRow.add(cancelBtn);

        submitBtn = StyleConstants.primaryButton("Save Turf Listing");
        submitBtn.addActionListener(e -> attemptSave());
        btnRow.add(submitBtn);

        bottom.add(btnRow, BorderLayout.SOUTH);
        root.add(bottom, BorderLayout.SOUTH);
    }

    private void attemptSave() {
        String name = nameField.getText().trim();
        String sport = (String) sportCombo.getSelectedItem();
        String location = locationField.getText().trim();
        String priceStr = priceField.getText().trim();

        // Client-side quick checks
        if (name.isEmpty() || location.isEmpty() || priceStr.isEmpty()) {
            showError("Please fill in all required fields.");
            return;
        }

        double price;
        try {
            price = Double.parseDouble(priceStr);
            if (price <= 0) {
                showError("Hourly rate must be greater than 0.");
                return;
            }
        } catch (NumberFormatException e) {
            showError("Please enter a valid numeric price (e.g. 1200 or 1500.50).");
            return;
        }

        setLoading(true);
        errorLabel.setText(" ");

        new SwingWorker<Turf, Void>() {
            @Override
            protected Turf doInBackground() {
                // Inserts turf into MySQL with is_approved = false
                return turfService.addTurf(owner, name, location, sport, price);
            }

            @Override
            protected void done() {
                setLoading(false);
                try {
                    Turf created = get();
                    JOptionPane.showMessageDialog(
                            AddTurfDialog.this,
                            String.format("Turf '%s' created successfully!\n\n"
                                    + "Turf ID: #%d\n"
                                    + "Status: Pending Admin Approval\n\n"
                                    + "Once approved by an administrator, players will be able to view and book it.",
                                    created.getName(), created.getTurfId()),
                            "Turf Listed Successfully",
                            JOptionPane.INFORMATION_MESSAGE);
                    ThemeManager.removeListener(AddTurfDialog.this);
                    dispose();
                    if (onTurfAdded != null) {
                        onTurfAdded.run();
                    }
                } catch (Exception ex) {
                    showError(StyleConstants.formatErrorMessage(ex, "Add Turf"));
                }
            }
        }.execute();
    }

    private void showError(String msg) {
        errorLabel.setText("<html><center>" + msg + "</center></html>");
    }

    private void setLoading(boolean loading) {
        submitBtn.setEnabled(!loading);
        cancelBtn.setEnabled(!loading);
        submitBtn.setText(loading ? "Saving to Database…" : "Save Turf Listing");
    }

    @Override
    public void onThemeChanged(Theme newTheme) {
        if (root == null) return;
        root.setBackground(newTheme.getBgMain());

        if (card != null) {
            card.setBackground(newTheme.getBgCard());
            card.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(newTheme.getBorder(), 1, true),
                    new EmptyBorder(18, 20, 18, 20)));
        }

        title.setForeground(newTheme.getAccentGreen());
        subtitle.setForeground(newTheme.getTextMuted());
        if (nameLabel != null) nameLabel.setForeground(newTheme.getTextMuted());
        if (sportLabel != null) sportLabel.setForeground(newTheme.getTextMuted());
        if (locationLabel != null) locationLabel.setForeground(newTheme.getTextMuted());
        if (priceLabel != null) priceLabel.setForeground(newTheme.getTextMuted());

        nameField.setBackground(newTheme.getBgField());
        nameField.setForeground(newTheme.getTextPrimary());
        nameField.setCaretColor(newTheme.getAccentGreen());

        sportCombo.setBackground(newTheme.getBgField());
        sportCombo.setForeground(newTheme.getTextPrimary());
        sportCombo.setBorder(BorderFactory.createLineBorder(newTheme.getBorder(), 1));

        locationField.setBackground(newTheme.getBgField());
        locationField.setForeground(newTheme.getTextPrimary());
        locationField.setCaretColor(newTheme.getAccentGreen());

        priceField.setBackground(newTheme.getBgField());
        priceField.setForeground(newTheme.getTextPrimary());
        priceField.setCaretColor(newTheme.getAccentGreen());

        String greenHex = String.format("#%02x%02x%02x", newTheme.getAccentGreen().getRed(), newTheme.getAccentGreen().getGreen(), newTheme.getAccentGreen().getBlue());
        String mutedHex = String.format("#%02x%02x%02x", newTheme.getTextMuted().getRed(), newTheme.getTextMuted().getGreen(), newTheme.getTextMuted().getBlue());
        if (notice != null) {
            notice.setText(String.format("<html><font color='%s'>ℹ️ Admin Approval Required:</font><br>"
                    + "<font color='%s'>Newly created turfs are reviewed by administrators before being published to players.</font></html>",
                    greenHex, mutedHex));
        }

        errorLabel.setForeground(newTheme.getStatusError());

        repaint();
    }
}
