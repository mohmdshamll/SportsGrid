package ui;

import model.Booking;
import model.Role;
import model.Turf;
import model.TurfOwner;
import model.User;
import service.TurfService;
import ui.theme.Theme;
import ui.theme.ThemeChangeListener;
import ui.theme.ThemeManager;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Turf Owner Dashboard:
 *  - Displays only turfs owned by the logged-in owner.
 *  - Add, edit, and manage slots for owned turfs.
 *  - View booking records and confirmed earnings across all owned grounds.
 *  - Respects admin approval requirement (new turfs start as pending).
 *  - Background SwingWorker queries for non-blocking UI.
 *  - Full Light and Dark mode theme support with immediate runtime toggle.
 */
public class OwnerDashboardFrame extends JFrame implements ThemeChangeListener {

    private final TurfOwner owner;
    private final TurfService turfService = new TurfService();

    // Container panels for dynamic recoloring
    private JPanel root;
    private JPanel nav;
    private JPanel kpiRow;
    private List<JPanel> kpiCards = new ArrayList<>();
    private List<JLabel> kpiTitleLabels = new ArrayList<>();
    private JTabbedPane tabbedPane;
    private JPanel turfsTabPanel;
    private JPanel bookingsTabPanel;
    private JPanel bottomBar;
    private JScrollPane turfsScroll;
    private JScrollPane bookingsScroll;

    // Header labels
    private JLabel brand;
    private JLabel welcome;
    private JLabel turfsHeader;
    private JLabel bookingsHeader;

    // Summary Card Labels
    private JLabel totalTurfsLabel;
    private JLabel approvedTurfsLabel;
    private JLabel pendingTurfsLabel;
    private JLabel totalEarningsLabel;
    private JLabel statusLabel;
    private JLabel isolationLabel;
    private JButton refreshBtn;

    // Turfs Table
    private JTable turfsTable;
    private DefaultTableModel turfsTableModel;
    private JButton addTurfBtn;
    private JButton manageSlotsBtn;
    private JButton editTurfBtn;
    private List<Turf> ownedTurfs = new ArrayList<>();
    private Turf selectedTurf = null;

    // Bookings Table
    private JTable bookingsTable;
    private DefaultTableModel bookingsTableModel;
    private List<Booking> ownerBookings = new ArrayList<>();

    public OwnerDashboardFrame(User user) {
        if (user instanceof TurfOwner) {
            this.owner = (TurfOwner) user;
        } else {
            this.owner = (TurfOwner) User.create(Role.OWNER, user.getUserId(), user.getName(),
                    user.getEmail(), user.getPhone(), user.getPasswordHash());
        }

        setTitle("SportGrid – Turf Owner Dashboard (" + owner.getName() + ")");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        buildUI();
        setSize(1140, 720);
        setMinimumSize(new Dimension(1020, 600));
        setLocationRelativeTo(null);

        // Register for real-time theme switching
        ThemeManager.addListener(this);

        // Initial data load
        loadDashboardData();
    }

    private void buildUI() {
        Theme theme = ThemeManager.getTheme();

        root = new JPanel(new BorderLayout(0, 14));
        root.setBackground(theme.getBgMain());
        root.setBorder(new EmptyBorder(0, 0, 16, 0));
        setContentPane(root);

        // 1. Top Navigation Bar
        nav = buildNavBar();
        root.add(nav, BorderLayout.NORTH);

        // 2. Center Content: Stats Cards + Tabbed Content
        JPanel center = new JPanel(new BorderLayout(0, 14));
        center.setOpaque(false);
        center.setBorder(new EmptyBorder(0, 24, 0, 24));

        kpiRow = buildKpiStatsRow();
        center.add(kpiRow, BorderLayout.NORTH);
        center.add(buildTabbedWorkspace(), BorderLayout.CENTER);

        root.add(center, BorderLayout.CENTER);

        // 3. Bottom Status Bar
        bottomBar = buildBottomBar();
        root.add(bottomBar, BorderLayout.SOUTH);
    }

    private JPanel buildNavBar() {
        Theme theme = ThemeManager.getTheme();

        JPanel p = new JPanel(new BorderLayout(16, 0));
        p.setBackground(theme.getBgCard());
        p.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, theme.getBorder()),
                new EmptyBorder(12, 24, 12, 24)));

        JPanel brandPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        brandPanel.setOpaque(false);

        JLabel icon = new JLabel("⚽");
        icon.setFont(StyleConstants.FONT_TITLE);
        brandPanel.add(icon);

        brand = new JLabel("SportGrid — Owner Dashboard");
        brand.setFont(StyleConstants.FONT_TITLE);
        brand.setForeground(theme.getAccentGreen());
        brandPanel.add(brand);

        p.add(brandPanel, BorderLayout.WEST);

        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 0));
        right.setOpaque(false);

        welcome = new JLabel("Owner: " + owner.getName() + " (ID: #" + owner.getUserId() + ")");
        welcome.setFont(StyleConstants.FONT_BODY);
        welcome.setForeground(theme.getTextPrimary());
        right.add(welcome);

        right.add(StyleConstants.themeToggleButton());

        refreshBtn = StyleConstants.secondaryButton("⟳ Refresh");
        refreshBtn.addActionListener(e -> loadDashboardData());
        right.add(refreshBtn);

        JButton logoutBtn = StyleConstants.secondaryButton("Logout");
        logoutBtn.addActionListener(e -> {
            ThemeManager.removeListener(this);
            dispose();
            new LoginFrame().setVisible(true);
        });
        right.add(logoutBtn);

        p.add(right, BorderLayout.EAST);
        return p;
    }

    private JPanel buildKpiStatsRow() {
        Theme theme = ThemeManager.getTheme();
        kpiCards.clear();
        kpiTitleLabels.clear();

        JPanel row = new JPanel(new GridLayout(1, 4, 14, 0));
        row.setOpaque(false);

        totalTurfsLabel = new JLabel("0", SwingConstants.CENTER);
        approvedTurfsLabel = new JLabel("0", SwingConstants.CENTER);
        pendingTurfsLabel = new JLabel("0", SwingConstants.CENTER);
        totalEarningsLabel = new JLabel("₹0.00", SwingConstants.CENTER);

        row.add(createKpiCard("🏟️ Total Turfs", totalTurfsLabel, theme.getTextPrimary()));
        row.add(createKpiCard("🟢 Approved Turfs", approvedTurfsLabel, theme.getStatusSuccess()));
        row.add(createKpiCard("⏳ Pending Approval", pendingTurfsLabel, theme.getStatusWarning()));
        row.add(createKpiCard("💰 Confirmed Earnings", totalEarningsLabel, theme.getAccentGreen()));

        return row;
    }

    private JPanel createKpiCard(String titleText, JLabel valueLabel, Color valueColor) {
        Theme theme = ThemeManager.getTheme();

        JPanel card = new JPanel(new BorderLayout(0, 6));
        card.setBackground(theme.getBgCard());
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(theme.getBorder(), 1, true),
                new EmptyBorder(12, 16, 12, 16)));

        JLabel titleLbl = new JLabel(titleText);
        titleLbl.setFont(StyleConstants.FONT_SMALL);
        titleLbl.setForeground(theme.getTextMuted());
        card.add(titleLbl, BorderLayout.NORTH);

        valueLabel.setFont(StyleConstants.FONT_TITLE);
        valueLabel.setForeground(valueColor);
        card.add(valueLabel, BorderLayout.CENTER);

        kpiCards.add(card);
        kpiTitleLabels.add(titleLbl);

        return card;
    }

    private JPanel buildTabbedWorkspace() {
        Theme theme = ThemeManager.getTheme();

        JPanel container = new JPanel(new BorderLayout());
        container.setOpaque(false);

        tabbedPane = StyleConstants.styledTabbedPane();

        turfsTabPanel = buildTurfsTab();
        bookingsTabPanel = buildBookingsTab();

        tabbedPane.addTab("🏟️ My Turfs", turfsTabPanel);
        tabbedPane.addTab("📋 Booking Records", bookingsTabPanel);

        container.add(tabbedPane, BorderLayout.CENTER);
        return container;
    }

    private JPanel buildTurfsTab() {
        Theme theme = ThemeManager.getTheme();

        JPanel tab = new JPanel(new BorderLayout(0, 10));
        tab.setBackground(theme.getBgMain());
        tab.setBorder(new EmptyBorder(12, 4, 4, 4));

        // Action Toolbar above table
        JPanel toolbar = new JPanel(new BorderLayout());
        toolbar.setOpaque(false);

        turfsHeader = new JLabel("Your Turf Grounds & Status");
        turfsHeader.setFont(StyleConstants.FONT_HEADING);
        turfsHeader.setForeground(theme.getTextPrimary());
        toolbar.add(turfsHeader, BorderLayout.WEST);

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        actions.setOpaque(false);

        addTurfBtn = StyleConstants.primaryButton("+ Add New Turf");
        addTurfBtn.addActionListener(e -> openAddTurfDialog());
        actions.add(addTurfBtn);

        manageSlotsBtn = StyleConstants.secondaryButton("📅 Manage Slots");
        manageSlotsBtn.setEnabled(false);
        manageSlotsBtn.addActionListener(e -> openManageSlotsDialog());
        actions.add(manageSlotsBtn);

        editTurfBtn = StyleConstants.secondaryButton("✏️ Edit Details");
        editTurfBtn.setEnabled(false);
        editTurfBtn.addActionListener(e -> openEditTurfDialog());
        actions.add(editTurfBtn);

        toolbar.add(actions, BorderLayout.EAST);
        tab.add(toolbar, BorderLayout.NORTH);

        // Table
        String[] cols = {"Turf ID", "Turf Name", "Sport", "Location", "Hourly Rate", "Rating", "Approval Status"};
        turfsTableModel = new DefaultTableModel(cols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };

        turfsTable = new JTable(turfsTableModel);
        StyleConstants.styleTable(turfsTable);

        // Column widths
        turfsTable.getColumnModel().getColumn(0).setPreferredWidth(70);   // ID
        turfsTable.getColumnModel().getColumn(1).setPreferredWidth(220);  // Name
        turfsTable.getColumnModel().getColumn(2).setPreferredWidth(120);  // Sport
        turfsTable.getColumnModel().getColumn(3).setPreferredWidth(240);  // Location
        turfsTable.getColumnModel().getColumn(4).setPreferredWidth(120);  // Rate
        turfsTable.getColumnModel().getColumn(5).setPreferredWidth(100);  // Rating
        turfsTable.getColumnModel().getColumn(6).setPreferredWidth(160);  // Status

        // Alignments & status badge
        turfsTable.getColumnModel().getColumn(0).setCellRenderer(new StyleConstants.StripedTableCellRenderer(SwingConstants.CENTER));
        turfsTable.getColumnModel().getColumn(1).setCellRenderer(new StyleConstants.StripedTableCellRenderer(SwingConstants.LEFT));
        turfsTable.getColumnModel().getColumn(2).setCellRenderer(new StyleConstants.StripedTableCellRenderer(SwingConstants.CENTER));
        turfsTable.getColumnModel().getColumn(3).setCellRenderer(new StyleConstants.StripedTableCellRenderer(SwingConstants.LEFT));
        turfsTable.getColumnModel().getColumn(4).setCellRenderer(new StyleConstants.StripedTableCellRenderer(SwingConstants.CENTER));
        turfsTable.getColumnModel().getColumn(5).setCellRenderer(new StyleConstants.StripedTableCellRenderer(SwingConstants.CENTER));
        turfsTable.getColumnModel().getColumn(6).setCellRenderer(new StyleConstants.StatusBadgeRenderer());

        // Selection listener
        turfsTable.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                int row = turfsTable.getSelectedRow();
                if (row >= 0 && row < ownedTurfs.size()) {
                    selectedTurf = ownedTurfs.get(row);
                    manageSlotsBtn.setEnabled(true);
                    editTurfBtn.setEnabled(true);
                } else {
                    selectedTurf = null;
                    manageSlotsBtn.setEnabled(false);
                    editTurfBtn.setEnabled(false);
                }
            }
        });

        turfsScroll = new JScrollPane(turfsTable);
        StyleConstants.styleScrollPane(turfsScroll);
        tab.add(turfsScroll, BorderLayout.CENTER);

        return tab;
    }

    private JPanel buildBookingsTab() {
        Theme theme = ThemeManager.getTheme();

        JPanel tab = new JPanel(new BorderLayout(0, 10));
        tab.setBackground(theme.getBgMain());
        tab.setBorder(new EmptyBorder(12, 4, 4, 4));

        bookingsHeader = new JLabel("Customer Bookings Across Your Turfs");
        bookingsHeader.setFont(StyleConstants.FONT_HEADING);
        bookingsHeader.setForeground(theme.getTextPrimary());
        tab.add(bookingsHeader, BorderLayout.NORTH);

        String[] cols = {"Booking Ref", "Turf Name", "Date", "Time Window", "Total Amount", "Payment Mode", "Status"};
        bookingsTableModel = new DefaultTableModel(cols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };

        bookingsTable = new JTable(bookingsTableModel);
        StyleConstants.styleTable(bookingsTable);

        bookingsTable.getColumnModel().getColumn(0).setCellRenderer(new StyleConstants.StripedTableCellRenderer(SwingConstants.CENTER));
        bookingsTable.getColumnModel().getColumn(1).setCellRenderer(new StyleConstants.StripedTableCellRenderer(SwingConstants.LEFT));
        bookingsTable.getColumnModel().getColumn(2).setCellRenderer(new StyleConstants.StripedTableCellRenderer(SwingConstants.CENTER));
        bookingsTable.getColumnModel().getColumn(3).setCellRenderer(new StyleConstants.StripedTableCellRenderer(SwingConstants.CENTER));
        bookingsTable.getColumnModel().getColumn(4).setCellRenderer(new StyleConstants.StripedTableCellRenderer(SwingConstants.CENTER));
        bookingsTable.getColumnModel().getColumn(5).setCellRenderer(new StyleConstants.StripedTableCellRenderer(SwingConstants.CENTER));
        bookingsTable.getColumnModel().getColumn(6).setCellRenderer(new StyleConstants.StatusBadgeRenderer());

        bookingsScroll = new JScrollPane(bookingsTable);
        StyleConstants.styleScrollPane(bookingsScroll);
        tab.add(bookingsScroll, BorderLayout.CENTER);

        return tab;
    }

    private JPanel buildBottomBar() {
        Theme theme = ThemeManager.getTheme();

        JPanel bar = new JPanel(new BorderLayout(14, 0));
        bar.setBackground(theme.getBgCard());
        bar.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, theme.getBorder()),
                new EmptyBorder(12, 24, 12, 24)));

        statusLabel = new JLabel("Loading owner data from MySQL...");
        statusLabel.setFont(StyleConstants.FONT_SMALL);
        statusLabel.setForeground(theme.getTextMuted());
        bar.add(statusLabel, BorderLayout.WEST);

        isolationLabel = new JLabel("Owner-level data isolation: Showing records exclusively for Owner #" + owner.getUserId());
        isolationLabel.setFont(StyleConstants.FONT_SMALL);
        isolationLabel.setForeground(theme.getTextMuted());
        bar.add(isolationLabel, BorderLayout.EAST);

        return bar;
    }

    private void loadDashboardData() {
        refreshBtn.setEnabled(false);
        statusLabel.setText("Querying MySQL for owner #" + owner.getUserId() + "...");
        statusLabel.setForeground(ThemeManager.getTheme().getTextMuted());

        new SwingWorker<Void, Void>() {
            private List<Turf> turfs;
            private List<Booking> bookings;

            @Override
            protected Void doInBackground() {
                // 1. Loads only this owner's turfs and calculates confirmed earnings
                turfs = turfService.loadOwnerDashboard(owner);
                // 2. Loads all bookings on this owner's turfs
                bookings = turfService.getOwnerBookings(owner);
                return null;
            }

            @Override
            protected void done() {
                refreshBtn.setEnabled(true);
                Theme t = ThemeManager.getTheme();
                try {
                    get();
                    ownedTurfs = turfs;
                    ownerBookings = bookings;

                    // Update KPI Stats
                    long approved = 0;
                    for (Turf trf : turfs) {
                        if (trf.isApproved()) approved++;
                    }
                    long pending = turfs.size() - approved;

                    totalTurfsLabel.setText(String.valueOf(turfs.size()));
                    approvedTurfsLabel.setText(String.valueOf(approved));
                    pendingTurfsLabel.setText(String.valueOf(pending));
                    totalEarningsLabel.setText(String.format("₹%.2f", owner.getTotalEarnings()));

                    // Populate Turfs Table
                    turfsTableModel.setRowCount(0);
                    for (Turf trf : turfs) {
                        String rating = trf.getAverageRating() > 0 ? String.format("⭐ %.1f / 5.0", trf.getAverageRating()) : "⭐ New";
                        String statusStr = trf.isApproved() ? "APPROVED" : "PENDING";
                        turfsTableModel.addRow(new Object[]{
                                "#" + trf.getTurfId(),
                                trf.getName(),
                                trf.getSportType(),
                                trf.getLocation(),
                                String.format("₹%.2f / hr", trf.getPricePerHour()),
                                rating,
                                statusStr
                        });
                    }

                    // Populate Bookings Table
                    bookingsTableModel.setRowCount(0);
                    for (Booking b : bookings) {
                        bookingsTableModel.addRow(new Object[]{
                                "#" + b.getBookingId(),
                                b.getTurf().getName(),
                                b.getSlot().getDate().toString(),
                                b.getSlot().getStartTime() + " - " + b.getSlot().getEndTime(),
                                String.format("₹%.2f", b.getTotalAmount()),
                                b.getPaymentMethodDisplay(),
                                b.getStatus().name()
                        });
                    }

                    statusLabel.setText(String.format("Dashboard updated. %d turf%s listed, %d booking record%s.",
                            turfs.size(), turfs.size() == 1 ? "" : "s",
                            bookings.size(), bookings.size() == 1 ? "" : "s"));
                    statusLabel.setForeground(t.getTextMuted());
                } catch (Exception ex) {
                    statusLabel.setText("Error loading dashboard: " + StyleConstants.formatErrorMessage(ex, "Owner Dashboard"));
                    statusLabel.setForeground(t.getStatusError());
                }
            }
        }.execute();
    }

    private void openAddTurfDialog() {
        AddTurfDialog dialog = new AddTurfDialog(this, owner, this::loadDashboardData);
        dialog.setVisible(true);
    }

    private void openManageSlotsDialog() {
        if (selectedTurf == null) {
            JOptionPane.showMessageDialog(this, "Please select a turf first.", "No Turf Selected", JOptionPane.WARNING_MESSAGE);
            return;
        }
        ManageSlotsDialog dialog = new ManageSlotsDialog(this, owner, selectedTurf);
        dialog.setVisible(true);
    }

    private void openEditTurfDialog() {
        if (selectedTurf == null) return;
        Theme t = ThemeManager.getTheme();

        JTextField nameField = StyleConstants.styledField();
        nameField.setText(selectedTurf.getName());

        JTextField locField = StyleConstants.styledField();
        locField.setText(selectedTurf.getLocation());

        JTextField priceField = StyleConstants.styledField();
        priceField.setText(String.valueOf(selectedTurf.getPricePerHour()));

        JPanel panel = new JPanel(new GridLayout(3, 2, 8, 8));
        panel.setBackground(t.getBgCard());
        panel.add(StyleConstants.label("Turf Name:"));
        panel.add(nameField);
        panel.add(StyleConstants.label("Location:"));
        panel.add(locField);
        panel.add(StyleConstants.label("Price / Hour (₹):"));
        panel.add(priceField);

        int result = JOptionPane.showConfirmDialog(this, panel,
                "Edit Turf #" + selectedTurf.getTurfId() + ": " + selectedTurf.getName(),
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);

        if (result == JOptionPane.OK_OPTION) {
            String name = nameField.getText().trim();
            String loc = locField.getText().trim();
            String pStr = priceField.getText().trim();

            if (name.isEmpty() || loc.isEmpty() || pStr.isEmpty()) {
                JOptionPane.showMessageDialog(this, "All fields are required.", "Validation Error", JOptionPane.WARNING_MESSAGE);
                return;
            }

            double price;
            try {
                price = Double.parseDouble(pStr);
                if (price <= 0) throw new NumberFormatException();
            } catch (NumberFormatException e) {
                JOptionPane.showMessageDialog(this, "Price must be a positive number.", "Invalid Price", JOptionPane.WARNING_MESSAGE);
                return;
            }

            new SwingWorker<Void, Void>() {
                @Override
                protected Void doInBackground() {
                    turfService.updateTurf(owner, selectedTurf.getTurfId(), name, loc, selectedTurf.getSportType(), price);
                    return null;
                }

                @Override
                protected void done() {
                    try {
                        get();
                        JOptionPane.showMessageDialog(OwnerDashboardFrame.this, "Turf details updated successfully!", "Updated", JOptionPane.INFORMATION_MESSAGE);
                        loadDashboardData();
                    } catch (Exception ex) {
                        JOptionPane.showMessageDialog(OwnerDashboardFrame.this, "Failed to update turf: " + StyleConstants.formatErrorMessage(ex, "Update Turf"), "Error", JOptionPane.ERROR_MESSAGE);
                    }
                }
            }.execute();
        }
    }

    @Override
    public void onThemeChanged(Theme newTheme) {
        if (root == null) return;
        root.setBackground(newTheme.getBgMain());

        if (nav != null) {
            nav.setBackground(newTheme.getBgCard());
            nav.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createMatteBorder(0, 0, 1, 0, newTheme.getBorder()),
                    new EmptyBorder(12, 24, 12, 24)));
        }

        for (JPanel card : kpiCards) {
            card.setBackground(newTheme.getBgCard());
            card.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(newTheme.getBorder(), 1, true),
                    new EmptyBorder(12, 16, 12, 16)));
        }

        for (JLabel lbl : kpiTitleLabels) {
            lbl.setForeground(newTheme.getTextMuted());
        }

        totalTurfsLabel.setForeground(newTheme.getTextPrimary());
        approvedTurfsLabel.setForeground(newTheme.getStatusSuccess());
        pendingTurfsLabel.setForeground(newTheme.getStatusWarning());
        totalEarningsLabel.setForeground(newTheme.getAccentGreen());

        if (tabbedPane != null) {
            tabbedPane.setBackground(newTheme.getBgCard());
            tabbedPane.setForeground(newTheme.getTextPrimary());
            tabbedPane.repaint();
        }

        if (turfsTabPanel != null) {
            turfsTabPanel.setBackground(newTheme.getBgMain());
        }
        if (bookingsTabPanel != null) {
            bookingsTabPanel.setBackground(newTheme.getBgMain());
        }

        if (turfsScroll != null) {
            turfsScroll.setBorder(BorderFactory.createLineBorder(newTheme.getBorder(), 1));
            turfsScroll.getViewport().setBackground(newTheme.getBgCard());
            turfsScroll.setBackground(newTheme.getBgMain());
        }
        if (bookingsScroll != null) {
            bookingsScroll.setBorder(BorderFactory.createLineBorder(newTheme.getBorder(), 1));
            bookingsScroll.getViewport().setBackground(newTheme.getBgCard());
            bookingsScroll.setBackground(newTheme.getBgMain());
        }

        if (bottomBar != null) {
            bottomBar.setBackground(newTheme.getBgCard());
            bottomBar.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createMatteBorder(1, 0, 0, 0, newTheme.getBorder()),
                    new EmptyBorder(12, 24, 12, 24)));
        }

        brand.setForeground(newTheme.getAccentGreen());
        welcome.setForeground(newTheme.getTextPrimary());
        turfsHeader.setForeground(newTheme.getTextPrimary());
        bookingsHeader.setForeground(newTheme.getTextPrimary());
        statusLabel.setForeground(newTheme.getTextMuted());
        isolationLabel.setForeground(newTheme.getTextMuted());

        StyleConstants.updateTableTheme(turfsTable);
        StyleConstants.updateTableTheme(bookingsTable);

        repaint();
    }
}
