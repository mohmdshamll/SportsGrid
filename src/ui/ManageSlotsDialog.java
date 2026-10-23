package ui;

import model.TimeSlot;
import model.Turf;
import model.TurfOwner;
import service.TurfService;
import ui.theme.Theme;
import ui.theme.ThemeChangeListener;
import ui.theme.ThemeManager;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;

/**
 * Manage Slots Dialog for Turf Owners.
 *
 * Allows owners to:
 *  - View all slots for their turf (both open and booked).
 *  - Automatically generate 1-hour slots for any selected date.
 *  - Remove an unbooked slot if they need to close maintenance hours.
 *  - Supports Light & Dark visual themes with instant runtime switching.
 */
public class ManageSlotsDialog extends JDialog implements ThemeChangeListener {

    private final TurfOwner owner;
    private final Turf turf;
    private final TurfService turfService = new TurfService();

    // Container panels
    private JPanel root;
    private JPanel headerPanel;
    private JPanel generatorCard;
    private JScrollPane scrollPane;

    // UI elements
    private JLabel titleLabel;
    private JLabel subLabel;
    private JLabel dateLbl;
    private JLabel openLbl;
    private JLabel closeLbl;
    private JTable slotTable;
    private DefaultTableModel tableModel;
    private JLabel statusLabel;
    private JTextField dateField;
    private JComboBox<Integer> openHourCombo;
    private JComboBox<Integer> closeHourCombo;
    private JButton generateBtn;
    private JButton deleteBtn;
    private JButton refreshBtn;

    private List<TimeSlot> currentSlots = new ArrayList<>();
    private TimeSlot selectedSlot = null;

    public ManageSlotsDialog(JFrame parent, TurfOwner owner, Turf turf) {
        super(parent, "SportGrid – Manage Slots: " + turf.getName(), true);
        this.owner = owner;
        this.turf = turf;

        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        buildUI();
        setSize(980, 660);
        setMinimumSize(new Dimension(880, 540));
        setLocationRelativeTo(parent);

        ThemeManager.addListener(this);

        loadSlots();
    }

    private void buildUI() {
        Theme theme = ThemeManager.getTheme();

        root = new JPanel(new BorderLayout(0, 14));
        root.setBackground(theme.getBgMain());
        root.setBorder(new EmptyBorder(18, 22, 18, 22));
        setContentPane(root);

        // 1. Header with Turf Info
        headerPanel = buildHeader();
        root.add(headerPanel, BorderLayout.NORTH);

        // 2. Center: Generator Controls + Slot Table
        JPanel center = new JPanel(new BorderLayout(0, 12));
        center.setOpaque(false);

        generatorCard = buildGeneratorCard();
        center.add(generatorCard, BorderLayout.NORTH);
        center.add(buildTablePanel(), BorderLayout.CENTER);

        root.add(center, BorderLayout.CENTER);

        // 3. Bottom Action Bar
        root.add(buildBottomBar(), BorderLayout.SOUTH);
    }

    private JPanel buildHeader() {
        Theme theme = ThemeManager.getTheme();

        JPanel header = new JPanel(new BorderLayout(16, 0));
        header.setBackground(theme.getBgCard());
        header.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(theme.getBorder(), 1, true),
                new EmptyBorder(14, 18, 14, 18)));

        JPanel left = new JPanel(new GridLayout(2, 1, 0, 4));
        left.setOpaque(false);

        titleLabel = new JLabel(turf.getName() + " — Slot Management");
        titleLabel.setFont(StyleConstants.FONT_TITLE);
        titleLabel.setForeground(theme.getAccentGreen());

        subLabel = new JLabel(String.format("Sport: %s  |  Location: %s  |  Rate: ₹%.2f/hr",
                turf.getSportType(), turf.getLocation(), turf.getPricePerHour()));
        subLabel.setFont(StyleConstants.FONT_BODY);
        subLabel.setForeground(theme.getTextPrimary());

        left.add(titleLabel);
        left.add(subLabel);
        header.add(left, BorderLayout.CENTER);

        refreshBtn = StyleConstants.secondaryButton("⟳ Refresh");
        refreshBtn.addActionListener(e -> loadSlots());
        header.add(refreshBtn, BorderLayout.EAST);

        return header;
    }

    private JPanel buildGeneratorCard() {
        Theme theme = ThemeManager.getTheme();

        JPanel card = new JPanel(new GridBagLayout());
        card.setBackground(theme.getBgCard());
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(theme.getBorder(), 1, true),
                new EmptyBorder(12, 16, 12, 16)));

        GridBagConstraints g = new GridBagConstraints();
        g.insets = new Insets(0, 6, 0, 6);
        g.fill = GridBagConstraints.HORIZONTAL;

        // Date input
        g.gridx = 0; g.weightx = 0;
        dateLbl = new JLabel("Date (YYYY-MM-DD):");
        dateLbl.setFont(StyleConstants.FONT_SMALL);
        dateLbl.setForeground(theme.getTextMuted());
        card.add(dateLbl, g);

        g.gridx = 1; g.weightx = 0.3;
        dateField = StyleConstants.styledField();
        dateField.setText(LocalDate.now().plusDays(1).toString());
        dateField.setToolTipText("e.g. 2026-10-25");
        card.add(dateField, g);

        // Open Hour
        g.gridx = 2; g.weightx = 0;
        openLbl = new JLabel("Open Hour:");
        openLbl.setFont(StyleConstants.FONT_SMALL);
        openLbl.setForeground(theme.getTextMuted());
        card.add(openLbl, g);

        g.gridx = 3; g.weightx = 0.15;
        Integer[] hours = new Integer[24];
        for (int i = 0; i < 24; i++) hours[i] = i;
        openHourCombo = new JComboBox<>(hours);
        openHourCombo.setSelectedItem(6); // 6 AM
        StyleConstants.styleComboBox(openHourCombo);
        card.add(openHourCombo, g);

        // Close Hour
        g.gridx = 4; g.weightx = 0;
        closeLbl = new JLabel("Close Hour:");
        closeLbl.setFont(StyleConstants.FONT_SMALL);
        closeLbl.setForeground(theme.getTextMuted());
        card.add(closeLbl, g);

        g.gridx = 5; g.weightx = 0.15;
        closeHourCombo = new JComboBox<>(hours);
        closeHourCombo.setSelectedItem(22); // 10 PM
        StyleConstants.styleComboBox(closeHourCombo);
        card.add(closeHourCombo, g);

        // Generate Button
        g.gridx = 6; g.weightx = 0;
        generateBtn = StyleConstants.primaryButton("⚡ Generate Hourly Slots");
        generateBtn.addActionListener(e -> generateSlots());
        card.add(generateBtn, g);

        return card;
    }

    private JPanel buildTablePanel() {
        Theme theme = ThemeManager.getTheme();

        JPanel panel = new JPanel(new BorderLayout(0, 8));
        panel.setOpaque(false);

        String[] cols = {"Slot ID", "Date", "Start Time", "End Time", "Duration", "Status"};
        tableModel = new DefaultTableModel(cols, 0) {
            @Override public boolean isCellEditable(int row, int col) { return false; }
        };

        slotTable = new JTable(tableModel);
        StyleConstants.styleTable(slotTable);

        // Hide ID
        slotTable.getColumnModel().getColumn(0).setMinWidth(0);
        slotTable.getColumnModel().getColumn(0).setMaxWidth(0);
        slotTable.getColumnModel().getColumn(0).setPreferredWidth(0);

        // Center alignments
        slotTable.getColumnModel().getColumn(1).setCellRenderer(new StyleConstants.StripedTableCellRenderer(SwingConstants.CENTER));
        slotTable.getColumnModel().getColumn(2).setCellRenderer(new StyleConstants.StripedTableCellRenderer(SwingConstants.CENTER));
        slotTable.getColumnModel().getColumn(3).setCellRenderer(new StyleConstants.StripedTableCellRenderer(SwingConstants.CENTER));
        slotTable.getColumnModel().getColumn(4).setCellRenderer(new StyleConstants.StripedTableCellRenderer(SwingConstants.CENTER));
        slotTable.getColumnModel().getColumn(5).setCellRenderer(new StyleConstants.StatusBadgeRenderer());

        slotTable.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                int row = slotTable.getSelectedRow();
                if (row >= 0 && row < currentSlots.size()) {
                    selectedSlot = currentSlots.get(row);
                    deleteBtn.setEnabled(!selectedSlot.isBooked());
                } else {
                    selectedSlot = null;
                    deleteBtn.setEnabled(false);
                }
            }
        });

        scrollPane = new JScrollPane(slotTable);
        StyleConstants.styleScrollPane(scrollPane);
        panel.add(scrollPane, BorderLayout.CENTER);

        return panel;
    }

    private JPanel buildBottomBar() {
        Theme theme = ThemeManager.getTheme();

        JPanel bar = new JPanel(new BorderLayout(12, 0));
        bar.setOpaque(false);

        statusLabel = new JLabel("Loading slots from MySQL...");
        statusLabel.setFont(StyleConstants.FONT_SMALL);
        statusLabel.setForeground(theme.getTextMuted());
        bar.add(statusLabel, BorderLayout.WEST);

        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        right.setOpaque(false);

        deleteBtn = StyleConstants.secondaryButton("🗑️ Delete Selected Slot");
        deleteBtn.setEnabled(false);
        deleteBtn.setToolTipText("Only unbooked slots can be removed");
        deleteBtn.addActionListener(e -> deleteSlot());
        right.add(deleteBtn);

        JButton closeBtn = StyleConstants.primaryButton("Done");
        closeBtn.addActionListener(e -> {
            ThemeManager.removeListener(this);
            dispose();
        });
        right.add(closeBtn);

        bar.add(right, BorderLayout.EAST);
        return bar;
    }

    private void loadSlots() {
        refreshBtn.setEnabled(false);
        generateBtn.setEnabled(false);
        deleteBtn.setEnabled(false);
        statusLabel.setText("Fetching slots from database...");
        statusLabel.setForeground(ThemeManager.getTheme().getTextMuted());

        new SwingWorker<List<TimeSlot>, Void>() {
            @Override
            protected List<TimeSlot> doInBackground() {
                // Returns all upcoming slots (free + booked) for this owner's turf
                return turfService.getSlots(owner, turf.getTurfId());
            }

            @Override
            protected void done() {
                refreshBtn.setEnabled(true);
                generateBtn.setEnabled(true);
                Theme t = ThemeManager.getTheme();
                try {
                    List<TimeSlot> slots = get();
                    currentSlots = slots;
                    tableModel.setRowCount(0);

                    long booked = 0;
                    for (TimeSlot s : slots) {
                        if (s.isBooked()) booked++;
                        tableModel.addRow(new Object[]{
                                s.getSlotId(),
                                s.getDate().toString(),
                                s.getStartTime().toString(),
                                s.getEndTime().toString(),
                                String.format("%.1f hr", s.getDurationHours()),
                                s.isBooked() ? "BOOKED" : "AVAILABLE"
                        });
                    }

                    statusLabel.setText(String.format("Total: %d slots (%d available, %d booked).",
                            slots.size(), slots.size() - booked, booked));
                    statusLabel.setForeground(t.getTextMuted());
                } catch (Exception ex) {
                    statusLabel.setText("Error loading slots: " + StyleConstants.formatErrorMessage(ex, "Slots"));
                    statusLabel.setForeground(t.getStatusError());
                }
            }
        }.execute();
    }

    private void generateSlots() {
        String dateStr = dateField.getText().trim();
        LocalDate date;
        try {
            date = LocalDate.parse(dateStr);
            if (date.isBefore(LocalDate.now())) {
                JOptionPane.showMessageDialog(this, "Cannot generate slots for a date in the past.", "Invalid Date", JOptionPane.WARNING_MESSAGE);
                return;
            }
        } catch (DateTimeParseException e) {
            JOptionPane.showMessageDialog(this, "Please enter a valid date in YYYY-MM-DD format (e.g. 2026-10-25).", "Format Error", JOptionPane.WARNING_MESSAGE);
            return;
        }

        int open = (Integer) openHourCombo.getSelectedItem();
        int close = (Integer) closeHourCombo.getSelectedItem();

        if (open >= close) {
            JOptionPane.showMessageDialog(this, "Opening hour must be before closing hour.", "Invalid Hours", JOptionPane.WARNING_MESSAGE);
            return;
        }

        generateBtn.setEnabled(false);
        statusLabel.setText("Generating hourly slots in MySQL...");

        new SwingWorker<Integer, Void>() {
            @Override
            protected Integer doInBackground() {
                return turfService.generateHourlySlots(owner, turf.getTurfId(), date, open, close);
            }

            @Override
            protected void done() {
                generateBtn.setEnabled(true);
                try {
                    int count = get();
                    JOptionPane.showMessageDialog(ManageSlotsDialog.this,
                            String.format("Created %d new 1-hour slots for %s.\n(Any pre-existing slots were safely skipped).", count, date),
                            "Slots Generated",
                            JOptionPane.INFORMATION_MESSAGE);
                    loadSlots();
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(ManageSlotsDialog.this,
                            "Failed to generate slots: " + StyleConstants.formatErrorMessage(ex, "Generate Slots"),
                            "Error",
                            JOptionPane.ERROR_MESSAGE);
                }
            }
        }.execute();
    }

    private void deleteSlot() {
        if (selectedSlot == null) return;
        if (selectedSlot.isBooked()) {
            JOptionPane.showMessageDialog(this, "Booked slots cannot be deleted.", "Slot Booked", JOptionPane.WARNING_MESSAGE);
            return;
        }

        int confirm = JOptionPane.showConfirmDialog(this,
                String.format("Delete slot on %s from %s to %s?", selectedSlot.getDate(), selectedSlot.getStartTime(), selectedSlot.getEndTime()),
                "Confirm Delete",
                JOptionPane.YES_NO_OPTION);

        if (confirm != JOptionPane.YES_OPTION) return;

        deleteBtn.setEnabled(false);
        statusLabel.setText("Deleting slot from MySQL...");

        new SwingWorker<Boolean, Void>() {
            @Override
            protected Boolean doInBackground() {
                return turfService.removeSlot(owner, turf.getTurfId(), selectedSlot.getSlotId());
            }

            @Override
            protected void done() {
                deleteBtn.setEnabled(true);
                try {
                    boolean ok = get();
                    if (ok) {
                        loadSlots();
                    } else {
                        JOptionPane.showMessageDialog(ManageSlotsDialog.this, "Could not delete slot (it may have been booked in the meantime).", "Delete Failed", JOptionPane.WARNING_MESSAGE);
                    }
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(ManageSlotsDialog.this, "Error: " + StyleConstants.formatErrorMessage(ex, "Delete Slot"), "Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        }.execute();
    }

    @Override
    public void onThemeChanged(Theme newTheme) {
        if (root == null) return;
        root.setBackground(newTheme.getBgMain());

        if (headerPanel != null) {
            headerPanel.setBackground(newTheme.getBgCard());
            headerPanel.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(newTheme.getBorder(), 1, true),
                    new EmptyBorder(14, 18, 14, 18)));
        }

        if (generatorCard != null) {
            generatorCard.setBackground(newTheme.getBgCard());
            generatorCard.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(newTheme.getBorder(), 1, true),
                    new EmptyBorder(12, 16, 12, 16)));
        }

        if (scrollPane != null) {
            scrollPane.setBorder(BorderFactory.createLineBorder(newTheme.getBorder(), 1));
            scrollPane.getViewport().setBackground(newTheme.getBgCard());
            scrollPane.setBackground(newTheme.getBgMain());
        }

        titleLabel.setForeground(newTheme.getAccentGreen());
        subLabel.setForeground(newTheme.getTextPrimary());
        dateLbl.setForeground(newTheme.getTextMuted());
        openLbl.setForeground(newTheme.getTextMuted());
        closeLbl.setForeground(newTheme.getTextMuted());
        statusLabel.setForeground(newTheme.getTextMuted());

        dateField.setBackground(newTheme.getBgField());
        dateField.setForeground(newTheme.getTextPrimary());
        dateField.setCaretColor(newTheme.getAccentGreen());

        openHourCombo.setBackground(newTheme.getBgField());
        openHourCombo.setForeground(newTheme.getTextPrimary());
        openHourCombo.setBorder(BorderFactory.createLineBorder(newTheme.getBorder(), 1));

        closeHourCombo.setBackground(newTheme.getBgField());
        closeHourCombo.setForeground(newTheme.getTextPrimary());
        closeHourCombo.setBorder(BorderFactory.createLineBorder(newTheme.getBorder(), 1));

        StyleConstants.updateTableTheme(slotTable);

        repaint();
    }
}
