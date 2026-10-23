package ui;

import model.Admin;
import model.Booking;
import model.Role;
import model.Turf;
import model.User;
import service.AdminService;
import ui.theme.Theme;
import ui.theme.ThemeChangeListener;
import ui.theme.ThemeManager;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.ActionListener;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * Administrator Control Panel.
 *
 * Provides central moderation, oversight, and governance for SportGrid:
 *  - Real-time platform KPI summary metrics (Users, Turfs, Pending Approvals, Bookings, Revenue).
 *  - Turf Moderation Tab: Review, approve, reject pending listings, and delete existing grounds.
 *  - User Management Tab: View all accounts, filter by role/search, and safely remove users.
 *  - Bookings & Payments Tab: Audit reservations, payment methods, references, and statuses.
 *  - Non-blocking background database operations using SwingWorker.
 *  - Strict role-based security enforcement (Admin-only).
 *  - Full Light and Dark mode theme engine support with instant runtime switching.
 */
public class AdminPanelFrame extends JFrame implements ThemeChangeListener {

    private final Admin admin;
    private final AdminService adminService = new AdminService();

    // Container panels for dynamic recoloring
    private JPanel root;
    private JPanel nav;
    private JPanel kpiRow;
    private List<JPanel> kpiCards = new ArrayList<>();
    private List<JLabel> kpiTitleLabels = new ArrayList<>();
    private JTabbedPane tabbedPane;
    private JPanel turfsTabPanel;
    private JPanel usersTabPanel;
    private JPanel bookingsTabPanel;
    private JPanel bottomBar;
    private JScrollPane turfsScroll;
    private JScrollPane usersScroll;
    private JScrollPane bookingsScroll;

    // Header labels
    private JLabel brand;
    private JLabel adminBadge;
    private JLabel sysNote;

    // Filter labels
    private JLabel turfFilterLbl;
    private JLabel turfSearchLbl;
    private JLabel userRoleLbl;
    private JLabel userSearchLbl;
    private JLabel bookingStatusLbl;
    private JLabel bookingSearchLbl;

    // Summary KPI Labels
    private JLabel totalUsersLabel;
    private JLabel totalTurfsLabel;
    private JLabel pendingTurfsLabel;
    private JLabel totalBookingsLabel;
    private JLabel totalRevenueLabel;
    private JLabel statusLabel;
    private JButton refreshAllBtn;

    // Turfs Tab
    private JTable turfsTable;
    private DefaultTableModel turfsTableModel;
    private JComboBox<String> turfFilterCombo;
    private JTextField turfSearchField;
    private JButton approveTurfBtn;
    private JButton rejectTurfBtn;
    private JButton deleteTurfBtn;
    private List<Turf> allTurfsList = new ArrayList<>();
    private List<Turf> displayedTurfsList = new ArrayList<>();
    private Turf selectedTurf = null;

    // Users Tab
    private JTable usersTable;
    private DefaultTableModel usersTableModel;
    private JComboBox<String> userRoleCombo;
    private JTextField userSearchField;
    private JButton removeUserBtn;
    private List<User> allUsersList = new ArrayList<>();
    private List<User> displayedUsersList = new ArrayList<>();
    private User selectedUser = null;

    // Bookings Tab
    private JTable bookingsTable;
    private DefaultTableModel bookingsTableModel;
    private JComboBox<String> bookingStatusCombo;
    private JTextField bookingSearchField;
    private List<Booking> allBookingsList = new ArrayList<>();
    private List<Booking> displayedBookingsList = new ArrayList<>();

    public AdminPanelFrame(User user) {
        if (user == null || user.getRole() != Role.ADMIN || !(user instanceof Admin)) {
            throw new SecurityException("Unauthorized: Admin privileges required to access Administrator Panel");
        }
        this.admin = (Admin) user;

        setTitle("SportGrid – Administrator Control Panel (" + admin.getName() + ")");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        buildUI();
        setSize(1220, 780);
        setMinimumSize(new Dimension(1060, 640));
        setLocationRelativeTo(null);

        // Register for real-time theme updates
        ThemeManager.addListener(this);

        // Initial background fetch of all administration metrics
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

        brand = new JLabel("SportGrid — Administrator Control Panel");
        brand.setFont(StyleConstants.FONT_TITLE);
        brand.setForeground(theme.getAccentGreen());
        brandPanel.add(brand);

        p.add(brandPanel, BorderLayout.WEST);

        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 0));
        right.setOpaque(false);

        adminBadge = new JLabel("Admin: " + admin.getName() + " (ID: #" + admin.getUserId() + ")");
        adminBadge.setFont(StyleConstants.FONT_BODY);
        adminBadge.setForeground(theme.getTextPrimary());
        right.add(adminBadge);

        right.add(StyleConstants.themeToggleButton());

        refreshAllBtn = StyleConstants.secondaryButton("⟳ Refresh All");
        refreshAllBtn.addActionListener(e -> loadDashboardData());
        right.add(refreshAllBtn);

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

        JPanel row = new JPanel(new GridLayout(1, 5, 12, 0));
        row.setOpaque(false);

        totalUsersLabel = new JLabel("0", SwingConstants.CENTER);
        totalTurfsLabel = new JLabel("0", SwingConstants.CENTER);
        pendingTurfsLabel = new JLabel("0", SwingConstants.CENTER);
        totalBookingsLabel = new JLabel("0", SwingConstants.CENTER);
        totalRevenueLabel = new JLabel("₹0.00", SwingConstants.CENTER);

        row.add(createKpiCard("👥 Total Users", totalUsersLabel, theme.getTextPrimary()));
        row.add(createKpiCard("🏟️ Total Turfs", totalTurfsLabel, theme.getAccentGreen()));
        row.add(createKpiCard("⏳ Pending Approvals", pendingTurfsLabel, theme.getStatusWarning()));
        row.add(createKpiCard("📋 Total Bookings", totalBookingsLabel, theme.getTextPrimary()));
        row.add(createKpiCard("💰 Platform Revenue", totalRevenueLabel, theme.getAccentGreen()));

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

        tabbedPane = new JTabbedPane();
        tabbedPane.setFont(StyleConstants.FONT_HEADING);
        tabbedPane.setBackground(theme.getBgCard());
        tabbedPane.setForeground(theme.getTextPrimary());

        turfsTabPanel = buildTurfsTab();
        usersTabPanel = buildUsersTab();
        bookingsTabPanel = buildBookingsTab();

        tabbedPane.addTab("🏟️ Turf Moderation", turfsTabPanel);
        tabbedPane.addTab("👥 User Management", usersTabPanel);
        tabbedPane.addTab("📋 Bookings & Payments", bookingsTabPanel);

        container.add(tabbedPane, BorderLayout.CENTER);
        return container;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Tab 1: Turf Moderation
    // ─────────────────────────────────────────────────────────────────────────
    private JPanel buildTurfsTab() {
        Theme theme = ThemeManager.getTheme();

        JPanel tab = new JPanel(new BorderLayout(0, 10));
        tab.setBackground(theme.getBgMain());
        tab.setBorder(new EmptyBorder(12, 4, 4, 4));

        // Filter & Action Toolbar
        JPanel toolbar = new JPanel(new BorderLayout(12, 0));
        toolbar.setOpaque(false);

        JPanel leftFilters = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        leftFilters.setOpaque(false);

        turfFilterLbl = new JLabel("Filter Status:");
        turfFilterLbl.setFont(StyleConstants.FONT_BODY);
        turfFilterLbl.setForeground(theme.getTextMuted());
        leftFilters.add(turfFilterLbl);

        turfFilterCombo = new JComboBox<>(new String[]{"All Turfs", "Pending Approval Only", "Approved Only"});
        StyleConstants.styleComboBox(turfFilterCombo);
        turfFilterCombo.addActionListener(e -> applyTurfFilters());
        leftFilters.add(turfFilterCombo);

        turfSearchLbl = new JLabel("Search:");
        turfSearchLbl.setFont(StyleConstants.FONT_BODY);
        turfSearchLbl.setForeground(theme.getTextMuted());
        leftFilters.add(turfSearchLbl);

        turfSearchField = StyleConstants.styledField();
        turfSearchField.setPreferredSize(new Dimension(200, 38));
        turfSearchField.setToolTipText("Search turf by name, sport, location, or owner");
        ActionListener searchAction = e -> applyTurfFilters();
        turfSearchField.addActionListener(searchAction);
        leftFilters.add(turfSearchField);

        JButton searchBtn = StyleConstants.secondaryButton("Search");
        searchBtn.addActionListener(searchAction);
        leftFilters.add(searchBtn);

        toolbar.add(leftFilters, BorderLayout.WEST);

        // Action Buttons on Right
        JPanel rightActions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        rightActions.setOpaque(false);

        approveTurfBtn = StyleConstants.primaryButton("Approve Turf");
        approveTurfBtn.setEnabled(false);
        approveTurfBtn.setToolTipText("Approve the selected pending turf so players can book it");
        approveTurfBtn.addActionListener(e -> attemptApproveSelectedTurf());
        rightActions.add(approveTurfBtn);

        rejectTurfBtn = StyleConstants.secondaryButton("Reject Turf");
        rejectTurfBtn.setEnabled(false);
        rejectTurfBtn.setToolTipText("Reject and permanently remove the selected pending turf");
        rejectTurfBtn.addActionListener(e -> attemptRejectSelectedTurf());
        rightActions.add(rejectTurfBtn);

        deleteTurfBtn = StyleConstants.secondaryButton("Delete Turf");
        deleteTurfBtn.setEnabled(false);
        deleteTurfBtn.setToolTipText("Delete the selected turf and all its slots/bookings");
        deleteTurfBtn.addActionListener(e -> attemptDeleteSelectedTurf());
        rightActions.add(deleteTurfBtn);

        toolbar.add(rightActions, BorderLayout.EAST);
        tab.add(toolbar, BorderLayout.NORTH);

        // Table
        String[] cols = {"Turf ID", "Turf Name", "Owner", "Sport", "Location", "Hourly Rate", "Rating", "Approval Status"};
        turfsTableModel = new DefaultTableModel(cols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };

        turfsTable = new JTable(turfsTableModel);
        StyleConstants.styleTable(turfsTable);

        // Column widths
        turfsTable.getColumnModel().getColumn(0).setPreferredWidth(70);   // ID
        turfsTable.getColumnModel().getColumn(1).setPreferredWidth(210);  // Name
        turfsTable.getColumnModel().getColumn(2).setPreferredWidth(170);  // Owner
        turfsTable.getColumnModel().getColumn(3).setPreferredWidth(120);  // Sport
        turfsTable.getColumnModel().getColumn(4).setPreferredWidth(210);  // Location
        turfsTable.getColumnModel().getColumn(5).setPreferredWidth(120);  // Rate
        turfsTable.getColumnModel().getColumn(6).setPreferredWidth(90);   // Rating
        turfsTable.getColumnModel().getColumn(7).setPreferredWidth(160);  // Status

        // Alignments & status badge
        turfsTable.getColumnModel().getColumn(0).setCellRenderer(new StyleConstants.StripedTableCellRenderer(SwingConstants.CENTER));
        turfsTable.getColumnModel().getColumn(1).setCellRenderer(new StyleConstants.StripedTableCellRenderer(SwingConstants.LEFT));
        turfsTable.getColumnModel().getColumn(2).setCellRenderer(new StyleConstants.StripedTableCellRenderer(SwingConstants.LEFT));
        turfsTable.getColumnModel().getColumn(3).setCellRenderer(new StyleConstants.StripedTableCellRenderer(SwingConstants.CENTER));
        turfsTable.getColumnModel().getColumn(4).setCellRenderer(new StyleConstants.StripedTableCellRenderer(SwingConstants.LEFT));
        turfsTable.getColumnModel().getColumn(5).setCellRenderer(new StyleConstants.StripedTableCellRenderer(SwingConstants.CENTER));
        turfsTable.getColumnModel().getColumn(6).setCellRenderer(new StyleConstants.StripedTableCellRenderer(SwingConstants.CENTER));
        turfsTable.getColumnModel().getColumn(7).setCellRenderer(new StyleConstants.StatusBadgeRenderer());

        // Selection listener
        turfsTable.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                int row = turfsTable.getSelectedRow();
                if (row >= 0 && row < displayedTurfsList.size()) {
                    selectedTurf = displayedTurfsList.get(row);
                    boolean isPending = !selectedTurf.isApproved();
                    approveTurfBtn.setEnabled(isPending);
                    rejectTurfBtn.setEnabled(isPending);
                    deleteTurfBtn.setEnabled(true);
                } else {
                    selectedTurf = null;
                    approveTurfBtn.setEnabled(false);
                    rejectTurfBtn.setEnabled(false);
                    deleteTurfBtn.setEnabled(false);
                }
            }
        });

        turfsScroll = new JScrollPane(turfsTable);
        StyleConstants.styleScrollPane(turfsScroll);
        tab.add(turfsScroll, BorderLayout.CENTER);

        return tab;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Tab 2: User Management
    // ─────────────────────────────────────────────────────────────────────────
    private JPanel buildUsersTab() {
        Theme theme = ThemeManager.getTheme();

        JPanel tab = new JPanel(new BorderLayout(0, 10));
        tab.setBackground(theme.getBgMain());
        tab.setBorder(new EmptyBorder(12, 4, 4, 4));

        // Filter & Action Toolbar
        JPanel toolbar = new JPanel(new BorderLayout(12, 0));
        toolbar.setOpaque(false);

        JPanel leftFilters = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        leftFilters.setOpaque(false);

        userRoleLbl = new JLabel("Filter Role:");
        userRoleLbl.setFont(StyleConstants.FONT_BODY);
        userRoleLbl.setForeground(theme.getTextMuted());
        leftFilters.add(userRoleLbl);

        userRoleCombo = new JComboBox<>(new String[]{"All Roles", "Players", "Turf Owners", "Admins"});
        StyleConstants.styleComboBox(userRoleCombo);
        userRoleCombo.addActionListener(e -> applyUserFilters());
        leftFilters.add(userRoleCombo);

        userSearchLbl = new JLabel("Search:");
        userSearchLbl.setFont(StyleConstants.FONT_BODY);
        userSearchLbl.setForeground(theme.getTextMuted());
        leftFilters.add(userSearchLbl);

        userSearchField = StyleConstants.styledField();
        userSearchField.setPreferredSize(new Dimension(200, 38));
        userSearchField.setToolTipText("Search user by name, email, or phone");
        ActionListener searchAction = e -> applyUserFilters();
        userSearchField.addActionListener(searchAction);
        leftFilters.add(userSearchField);

        JButton searchBtn = StyleConstants.secondaryButton("Search");
        searchBtn.addActionListener(searchAction);
        leftFilters.add(searchBtn);

        toolbar.add(leftFilters, BorderLayout.WEST);

        // Actions on Right
        JPanel rightActions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        rightActions.setOpaque(false);

        removeUserBtn = StyleConstants.secondaryButton("Remove User");
        removeUserBtn.setEnabled(false);
        removeUserBtn.setToolTipText("Safely remove user account and cascade delete their data");
        removeUserBtn.addActionListener(e -> attemptRemoveSelectedUser());
        rightActions.add(removeUserBtn);

        JButton refreshUsersBtn = StyleConstants.secondaryButton("⟳ Refresh Users");
        refreshUsersBtn.addActionListener(e -> loadDashboardData());
        rightActions.add(refreshUsersBtn);

        toolbar.add(rightActions, BorderLayout.EAST);
        tab.add(toolbar, BorderLayout.NORTH);

        // Users Table
        String[] cols = {"User ID", "Full Name", "Email Address", "Phone Number", "Role", "Account Status"};
        usersTableModel = new DefaultTableModel(cols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };

        usersTable = new JTable(usersTableModel);
        StyleConstants.styleTable(usersTable);

        // Column widths
        usersTable.getColumnModel().getColumn(0).setPreferredWidth(80);   // ID
        usersTable.getColumnModel().getColumn(1).setPreferredWidth(220);  // Name
        usersTable.getColumnModel().getColumn(2).setPreferredWidth(240);  // Email
        usersTable.getColumnModel().getColumn(3).setPreferredWidth(140);  // Phone
        usersTable.getColumnModel().getColumn(4).setPreferredWidth(130);  // Role
        usersTable.getColumnModel().getColumn(5).setPreferredWidth(130);  // Status

        // Alignments
        usersTable.getColumnModel().getColumn(0).setCellRenderer(new StyleConstants.StripedTableCellRenderer(SwingConstants.CENTER));
        usersTable.getColumnModel().getColumn(1).setCellRenderer(new StyleConstants.StripedTableCellRenderer(SwingConstants.LEFT));
        usersTable.getColumnModel().getColumn(2).setCellRenderer(new StyleConstants.StripedTableCellRenderer(SwingConstants.LEFT));
        usersTable.getColumnModel().getColumn(3).setCellRenderer(new StyleConstants.StripedTableCellRenderer(SwingConstants.CENTER));
        usersTable.getColumnModel().getColumn(4).setCellRenderer(new StyleConstants.StatusBadgeRenderer());
        usersTable.getColumnModel().getColumn(5).setCellRenderer(new StyleConstants.StatusBadgeRenderer());

        // Selection listener
        usersTable.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                int row = usersTable.getSelectedRow();
                if (row >= 0 && row < displayedUsersList.size()) {
                    selectedUser = displayedUsersList.get(row);
                    removeUserBtn.setEnabled(selectedUser.getUserId() != admin.getUserId());
                } else {
                    selectedUser = null;
                    removeUserBtn.setEnabled(false);
                }
            }
        });

        usersScroll = new JScrollPane(usersTable);
        StyleConstants.styleScrollPane(usersScroll);
        tab.add(usersScroll, BorderLayout.CENTER);

        return tab;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Tab 3: Bookings & Revenue
    // ─────────────────────────────────────────────────────────────────────────
    private JPanel buildBookingsTab() {
        Theme theme = ThemeManager.getTheme();

        JPanel tab = new JPanel(new BorderLayout(0, 10));
        tab.setBackground(theme.getBgMain());
        tab.setBorder(new EmptyBorder(12, 4, 4, 4));

        // Filter & Action Toolbar
        JPanel toolbar = new JPanel(new BorderLayout(12, 0));
        toolbar.setOpaque(false);

        JPanel leftFilters = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        leftFilters.setOpaque(false);

        bookingStatusLbl = new JLabel("Filter Status:");
        bookingStatusLbl.setFont(StyleConstants.FONT_BODY);
        bookingStatusLbl.setForeground(theme.getTextMuted());
        leftFilters.add(bookingStatusLbl);

        bookingStatusCombo = new JComboBox<>(new String[]{"All Bookings", "CONFIRMED Only", "CANCELLED Only", "PENDING Only"});
        StyleConstants.styleComboBox(bookingStatusCombo);
        bookingStatusCombo.addActionListener(e -> applyBookingFilters());
        leftFilters.add(bookingStatusCombo);

        bookingSearchLbl = new JLabel("Search:");
        bookingSearchLbl.setFont(StyleConstants.FONT_BODY);
        bookingSearchLbl.setForeground(theme.getTextMuted());
        leftFilters.add(bookingSearchLbl);

        bookingSearchField = StyleConstants.styledField();
        bookingSearchField.setPreferredSize(new Dimension(200, 38));
        bookingSearchField.setToolTipText("Search by booking ID, player, turf, or payment reference");
        ActionListener searchAction = e -> applyBookingFilters();
        bookingSearchField.addActionListener(searchAction);
        leftFilters.add(bookingSearchField);

        JButton searchBtn = StyleConstants.secondaryButton("Search");
        searchBtn.addActionListener(searchAction);
        leftFilters.add(searchBtn);

        toolbar.add(leftFilters, BorderLayout.WEST);

        // Actions on Right
        JPanel rightActions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        rightActions.setOpaque(false);

        JButton refreshBookingsBtn = StyleConstants.secondaryButton("⟳ Refresh Bookings");
        refreshBookingsBtn.addActionListener(e -> loadDashboardData());
        rightActions.add(refreshBookingsBtn);

        toolbar.add(rightActions, BorderLayout.EAST);
        tab.add(toolbar, BorderLayout.NORTH);

        // Table
        String[] cols = {"Booking #", "Player Name", "Turf Name", "Sport", "Date", "Time Slot", "Amount", "Payment Mode & Ref", "Status"};
        bookingsTableModel = new DefaultTableModel(cols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };

        bookingsTable = new JTable(bookingsTableModel);
        StyleConstants.styleTable(bookingsTable);

        // Column widths
        bookingsTable.getColumnModel().getColumn(0).setPreferredWidth(80);   // ID
        bookingsTable.getColumnModel().getColumn(1).setPreferredWidth(170);  // Player
        bookingsTable.getColumnModel().getColumn(2).setPreferredWidth(190);  // Turf
        bookingsTable.getColumnModel().getColumn(3).setPreferredWidth(100);  // Sport
        bookingsTable.getColumnModel().getColumn(4).setPreferredWidth(100);  // Date
        bookingsTable.getColumnModel().getColumn(5).setPreferredWidth(130);  // Time Slot
        bookingsTable.getColumnModel().getColumn(6).setPreferredWidth(110);  // Amount
        bookingsTable.getColumnModel().getColumn(7).setPreferredWidth(180);  // Payment
        bookingsTable.getColumnModel().getColumn(8).setPreferredWidth(120);  // Status

        // Alignments & status badge
        bookingsTable.getColumnModel().getColumn(0).setCellRenderer(new StyleConstants.StripedTableCellRenderer(SwingConstants.CENTER));
        bookingsTable.getColumnModel().getColumn(1).setCellRenderer(new StyleConstants.StripedTableCellRenderer(SwingConstants.LEFT));
        bookingsTable.getColumnModel().getColumn(2).setCellRenderer(new StyleConstants.StripedTableCellRenderer(SwingConstants.LEFT));
        bookingsTable.getColumnModel().getColumn(3).setCellRenderer(new StyleConstants.StripedTableCellRenderer(SwingConstants.CENTER));
        bookingsTable.getColumnModel().getColumn(4).setCellRenderer(new StyleConstants.StripedTableCellRenderer(SwingConstants.CENTER));
        bookingsTable.getColumnModel().getColumn(5).setCellRenderer(new StyleConstants.StripedTableCellRenderer(SwingConstants.CENTER));
        bookingsTable.getColumnModel().getColumn(6).setCellRenderer(new StyleConstants.StripedTableCellRenderer(SwingConstants.CENTER));
        bookingsTable.getColumnModel().getColumn(7).setCellRenderer(new StyleConstants.StripedTableCellRenderer(SwingConstants.LEFT));
        bookingsTable.getColumnModel().getColumn(8).setCellRenderer(new StyleConstants.StatusBadgeRenderer());

        bookingsScroll = new JScrollPane(bookingsTable);
        StyleConstants.styleScrollPane(bookingsScroll);
        tab.add(bookingsScroll, BorderLayout.CENTER);

        return tab;
    }

    private JPanel buildBottomBar() {
        Theme theme = ThemeManager.getTheme();

        JPanel bar = new JPanel(new BorderLayout(16, 0));
        bar.setOpaque(false);
        bar.setBorder(new EmptyBorder(0, 24, 0, 24));

        statusLabel = new JLabel("Connecting to MySQL...");
        statusLabel.setFont(StyleConstants.FONT_SMALL);
        statusLabel.setForeground(theme.getTextMuted());
        bar.add(statusLabel, BorderLayout.WEST);

        sysNote = new JLabel("SportGrid Administrator Console • Database Connected");
        sysNote.setFont(StyleConstants.FONT_SMALL);
        sysNote.setForeground(theme.getTextMuted());
        bar.add(sysNote, BorderLayout.EAST);

        return bar;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Background Data Loading
    // ─────────────────────────────────────────────────────────────────────────
    private void loadDashboardData() {
        refreshAllBtn.setEnabled(false);
        statusLabel.setText("Refreshing platform metrics from MySQL...");
        statusLabel.setForeground(ThemeManager.getTheme().getTextMuted());

        new SwingWorker<DashboardData, Void>() {
            @Override
            protected DashboardData doInBackground() {
                AdminService.AdminSummary summary = adminService.getSummary(admin);
                List<Turf> turfs = adminService.getAllTurfs(admin);
                List<User> users = adminService.listUsers(admin);
                List<Booking> bookings = adminService.getAllBookings(admin);
                return new DashboardData(summary, turfs, users, bookings);
            }

            @Override
            protected void done() {
                refreshAllBtn.setEnabled(true);
                Theme t = ThemeManager.getTheme();
                try {
                    DashboardData data = get();

                    // 1. Update Summary Cards
                    totalUsersLabel.setText(String.valueOf(data.summary.getTotalUsers()));
                    totalTurfsLabel.setText(String.valueOf(data.summary.getTotalTurfs()));
                    pendingTurfsLabel.setText(String.valueOf(data.summary.getPendingTurfs()));
                    totalBookingsLabel.setText(String.valueOf(data.summary.getTotalBookings()));
                    totalRevenueLabel.setText(String.format("₹%.2f", data.summary.getTotalRevenue()));

                    if (data.summary.getPendingTurfs() > 0) {
                        pendingTurfsLabel.setForeground(t.getStatusWarning());
                    } else {
                        pendingTurfsLabel.setForeground(t.getStatusSuccess());
                    }

                    // 2. Cache full lists
                    allTurfsList = data.turfs;
                    allUsersList = data.users;
                    allBookingsList = data.bookings;

                    // 3. Apply active filters to tables
                    applyTurfFilters();
                    applyUserFilters();
                    applyBookingFilters();

                    statusLabel.setText(String.format("Ready • %d Users, %d Turfs (%d Pending), %d Bookings loaded.",
                            allUsersList.size(), allTurfsList.size(), data.summary.getPendingTurfs(), allBookingsList.size()));
                    statusLabel.setForeground(t.getTextMuted());
                } catch (Exception ex) {
                    statusLabel.setText("Error loading admin data: " + StyleConstants.formatErrorMessage(ex, "Admin Dashboard"));
                    statusLabel.setForeground(t.getStatusError());
                }
            }
        }.execute();
    }

    private static class DashboardData {
        final AdminService.AdminSummary summary;
        final List<Turf> turfs;
        final List<User> users;
        final List<Booking> bookings;

        DashboardData(AdminService.AdminSummary s, List<Turf> t, List<User> u, List<Booking> b) {
            this.summary = s;
            this.turfs = t;
            this.users = u;
            this.bookings = b;
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Filter & Render Helpers
    // ─────────────────────────────────────────────────────────────────────────
    private void applyTurfFilters() {
        int filterIndex = (turfFilterCombo != null) ? turfFilterCombo.getSelectedIndex() : 0;
        String query = (turfSearchField != null) ? turfSearchField.getText().trim().toLowerCase() : "";

        displayedTurfsList.clear();
        turfsTableModel.setRowCount(0);

        for (Turf t : allTurfsList) {
            if (filterIndex == 1 && t.isApproved()) continue;          // Pending only
            if (filterIndex == 2 && !t.isApproved()) continue;         // Approved only

            if (!query.isEmpty()) {
                String matchStr = (t.getName() + " " + t.getSportType() + " " + t.getLocation() + " " + t.getOwnerName()).toLowerCase();
                if (!matchStr.contains(query)) continue;
            }

            displayedTurfsList.add(t);
            turfsTableModel.addRow(new Object[]{
                    "#" + t.getTurfId(),
                    t.getName(),
                    t.getOwnerName() + " (#" + t.getOwnerId() + ")",
                    t.getSportType(),
                    t.getLocation(),
                    String.format("₹%.2f/hr", t.getPricePerHour()),
                    t.getAverageRating() > 0 ? String.format("%.1f ★", t.getAverageRating()) : "New",
                    t.isApproved() ? "APPROVED" : "PENDING"
            });
        }

        selectedTurf = null;
        approveTurfBtn.setEnabled(false);
        rejectTurfBtn.setEnabled(false);
        deleteTurfBtn.setEnabled(false);
    }

    private void applyUserFilters() {
        int roleIndex = (userRoleCombo != null) ? userRoleCombo.getSelectedIndex() : 0;
        String query = (userSearchField != null) ? userSearchField.getText().trim().toLowerCase() : "";

        displayedUsersList.clear();
        usersTableModel.setRowCount(0);

        for (User u : allUsersList) {
            if (roleIndex == 1 && u.getRole() != Role.PLAYER) continue;
            if (roleIndex == 2 && u.getRole() != Role.OWNER) continue;
            if (roleIndex == 3 && u.getRole() != Role.ADMIN) continue;

            if (!query.isEmpty()) {
                String matchStr = (u.getName() + " " + u.getEmail() + " " + (u.getPhone() != null ? u.getPhone() : "")).toLowerCase();
                if (!matchStr.contains(query)) continue;
            }

            displayedUsersList.add(u);
            usersTableModel.addRow(new Object[]{
                    "#" + u.getUserId(),
                    u.getName(),
                    u.getEmail(),
                    u.getPhone() != null && !u.getPhone().isBlank() ? u.getPhone() : "—",
                    u.getRole().name(),
                    "ACTIVE"
            });
        }

        selectedUser = null;
        removeUserBtn.setEnabled(false);
    }

    private void applyBookingFilters() {
        int statusIndex = (bookingStatusCombo != null) ? bookingStatusCombo.getSelectedIndex() : 0;
        String query = (bookingSearchField != null) ? bookingSearchField.getText().trim().toLowerCase() : "";

        displayedBookingsList.clear();
        bookingsTableModel.setRowCount(0);

        DateTimeFormatter timeFmt = DateTimeFormatter.ofPattern("HH:mm");

        for (Booking b : allBookingsList) {
            if (statusIndex == 1 && !"CONFIRMED".equalsIgnoreCase(b.getStatus().name())) continue;
            if (statusIndex == 2 && !"CANCELLED".equalsIgnoreCase(b.getStatus().name())) continue;
            if (statusIndex == 3 && !"PENDING".equalsIgnoreCase(b.getStatus().name())) continue;

            if (!query.isEmpty()) {
                String matchStr = ("#" + b.getBookingId() + " " + b.getPlayerName() + " "
                        + b.getTurf().getName() + " " + b.getPaymentMethodDisplay()).toLowerCase();
                if (!matchStr.contains(query)) continue;
            }

            displayedBookingsList.add(b);

            String timeSlotStr = String.format("%s – %s",
                    b.getSlot().getStartTime().format(timeFmt),
                    b.getSlot().getEndTime().format(timeFmt));

            bookingsTableModel.addRow(new Object[]{
                    "#" + b.getBookingId(),
                    b.getPlayerName() + " (#" + b.getPlayerId() + ")",
                    b.getTurf().getName(),
                    b.getTurf().getSportType(),
                    b.getSlot().getDate().toString(),
                    timeSlotStr,
                    String.format("₹%.2f", b.getTotalAmount()),
                    b.getPaymentMethodDisplay(),
                    b.getStatus().name()
            });
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Admin Action Handlers
    // ─────────────────────────────────────────────────────────────────────────
    private void attemptApproveSelectedTurf() {
        if (selectedTurf == null || selectedTurf.isApproved()) return;

        int confirm = JOptionPane.showConfirmDialog(
                this,
                "Approve turf '" + selectedTurf.getName() + "' for public player bookings?",
                "Confirm Turf Approval",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.QUESTION_MESSAGE);

        if (confirm != JOptionPane.YES_OPTION) return;

        final int turfId = selectedTurf.getTurfId();
        final String turfName = selectedTurf.getName();
        statusLabel.setText("Approving turf '" + turfName + "' in MySQL...");
        approveTurfBtn.setEnabled(false);

        new SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() {
                adminService.approveTurf(admin, turfId);
                return null;
            }

            @Override
            protected void done() {
                try {
                    get();
                    JOptionPane.showMessageDialog(
                            AdminPanelFrame.this,
                            "Turf '" + turfName + "' has been approved successfully!\nPlayers can now view and book slots on this ground.",
                            "Turf Approved",
                            JOptionPane.INFORMATION_MESSAGE);
                    loadDashboardData();
                } catch (Exception ex) {
                    statusLabel.setText("Approval failed: " + StyleConstants.formatErrorMessage(ex, "Approve Turf"));
                    approveTurfBtn.setEnabled(true);
                }
            }
        }.execute();
    }

    private void attemptRejectSelectedTurf() {
        if (selectedTurf == null || selectedTurf.isApproved()) return;

        int confirm = JOptionPane.showConfirmDialog(
                this,
                "Are you sure you want to REJECT and permanently delete the pending turf '" + selectedTurf.getName() + "'?",
                "Confirm Turf Rejection",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE);

        if (confirm != JOptionPane.YES_OPTION) return;

        final int turfId = selectedTurf.getTurfId();
        final String turfName = selectedTurf.getName();
        statusLabel.setText("Rejecting turf '" + turfName + "'...");
        rejectTurfBtn.setEnabled(false);

        new SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() {
                adminService.rejectTurf(admin, turfId);
                return null;
            }

            @Override
            protected void done() {
                try {
                    get();
                    JOptionPane.showMessageDialog(
                            AdminPanelFrame.this,
                            "Turf listing '" + turfName + "' was rejected and deleted from MySQL.",
                            "Turf Rejected",
                            JOptionPane.INFORMATION_MESSAGE);
                    loadDashboardData();
                } catch (Exception ex) {
                    statusLabel.setText("Rejection failed: " + StyleConstants.formatErrorMessage(ex, "Reject Turf"));
                    rejectTurfBtn.setEnabled(true);
                }
            }
        }.execute();
    }

    private void attemptDeleteSelectedTurf() {
        if (selectedTurf == null) return;

        int confirm = JOptionPane.showConfirmDialog(
                this,
                "Are you sure you want to permanently DELETE turf '" + selectedTurf.getName() + "'?\n"
                        + "All associated time slots, reservations, and reviews will be removed via CASCADE.",
                "Confirm Turf Deletion",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE);

        if (confirm != JOptionPane.YES_OPTION) return;

        final int turfId = selectedTurf.getTurfId();
        final String turfName = selectedTurf.getName();
        statusLabel.setText("Deleting turf '" + turfName + "'...");
        deleteTurfBtn.setEnabled(false);

        new SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() {
                adminService.deleteTurf(admin, turfId);
                return null;
            }

            @Override
            protected void done() {
                try {
                    get();
                    JOptionPane.showMessageDialog(
                            AdminPanelFrame.this,
                            "Turf '" + turfName + "' and all dependent records were deleted.",
                            "Turf Deleted",
                            JOptionPane.INFORMATION_MESSAGE);
                    loadDashboardData();
                } catch (Exception ex) {
                    statusLabel.setText("Deletion failed: " + StyleConstants.formatErrorMessage(ex, "Delete Turf"));
                    deleteTurfBtn.setEnabled(true);
                }
            }
        }.execute();
    }

    private void attemptRemoveSelectedUser() {
        if (selectedUser == null) return;

        if (selectedUser.getUserId() == admin.getUserId()) {
            JOptionPane.showMessageDialog(
                    this,
                    "You cannot remove your own active administrator account.",
                    "Action Prohibited",
                    JOptionPane.ERROR_MESSAGE);
            return;
        }

        int confirm = JOptionPane.showConfirmDialog(
                this,
                "Are you sure you want to delete user '" + selectedUser.getName() + "' (" + selectedUser.getEmail() + ")?\n"
                        + "Role: " + selectedUser.getRole() + "\n\n"
                        + "⚠️ All their turfs, bookings, and reviews will be permanently deleted via database CASCADE.",
                "Confirm User Account Deletion",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE);

        if (confirm != JOptionPane.YES_OPTION) return;

        final int targetUserId = selectedUser.getUserId();
        final String targetUserName = selectedUser.getName();
        statusLabel.setText("Removing user account '" + targetUserName + "'...");
        removeUserBtn.setEnabled(false);

        new SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() {
                adminService.removeUser(admin, targetUserId);
                return null;
            }

            @Override
            protected void done() {
                try {
                    get();
                    JOptionPane.showMessageDialog(
                            AdminPanelFrame.this,
                            "User '" + targetUserName + "' was removed successfully.",
                            "User Removed",
                            JOptionPane.INFORMATION_MESSAGE);
                    loadDashboardData();
                } catch (Exception ex) {
                    statusLabel.setText("Failed to remove user: " + StyleConstants.formatErrorMessage(ex, "Remove User"));
                    removeUserBtn.setEnabled(true);
                }
            }
        }.execute();
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

        totalUsersLabel.setForeground(newTheme.getTextPrimary());
        totalTurfsLabel.setForeground(newTheme.getAccentGreen());
        if (pendingTurfsLabel != null) {
            String text = pendingTurfsLabel.getText();
            if (!"0".equals(text)) {
                pendingTurfsLabel.setForeground(newTheme.getStatusWarning());
            } else {
                pendingTurfsLabel.setForeground(newTheme.getStatusSuccess());
            }
        }
        totalBookingsLabel.setForeground(newTheme.getTextPrimary());
        totalRevenueLabel.setForeground(newTheme.getAccentGreen());

        if (tabbedPane != null) {
            tabbedPane.setBackground(newTheme.getBgCard());
            tabbedPane.setForeground(newTheme.getTextPrimary());
        }

        if (turfsTabPanel != null) turfsTabPanel.setBackground(newTheme.getBgMain());
        if (usersTabPanel != null) usersTabPanel.setBackground(newTheme.getBgMain());
        if (bookingsTabPanel != null) bookingsTabPanel.setBackground(newTheme.getBgMain());

        if (turfsScroll != null) {
            turfsScroll.setBorder(BorderFactory.createLineBorder(newTheme.getBorder(), 1));
            turfsScroll.getViewport().setBackground(newTheme.getBgCard());
            turfsScroll.setBackground(newTheme.getBgMain());
        }
        if (usersScroll != null) {
            usersScroll.setBorder(BorderFactory.createLineBorder(newTheme.getBorder(), 1));
            usersScroll.getViewport().setBackground(newTheme.getBgCard());
            usersScroll.setBackground(newTheme.getBgMain());
        }
        if (bookingsScroll != null) {
            bookingsScroll.setBorder(BorderFactory.createLineBorder(newTheme.getBorder(), 1));
            bookingsScroll.getViewport().setBackground(newTheme.getBgCard());
            bookingsScroll.setBackground(newTheme.getBgMain());
        }

        brand.setForeground(newTheme.getAccentGreen());
        adminBadge.setForeground(newTheme.getTextPrimary());
        statusLabel.setForeground(newTheme.getTextMuted());
        sysNote.setForeground(newTheme.getTextMuted());

        if (turfFilterLbl != null) turfFilterLbl.setForeground(newTheme.getTextMuted());
        if (turfSearchLbl != null) turfSearchLbl.setForeground(newTheme.getTextMuted());
        if (userRoleLbl != null) userRoleLbl.setForeground(newTheme.getTextMuted());
        if (userSearchLbl != null) userSearchLbl.setForeground(newTheme.getTextMuted());
        if (bookingStatusLbl != null) bookingStatusLbl.setForeground(newTheme.getTextMuted());
        if (bookingSearchLbl != null) bookingSearchLbl.setForeground(newTheme.getTextMuted());

        if (turfFilterCombo != null) {
            turfFilterCombo.setBackground(newTheme.getBgField());
            turfFilterCombo.setForeground(newTheme.getTextPrimary());
            turfFilterCombo.setBorder(BorderFactory.createLineBorder(newTheme.getBorder(), 1));
        }
        if (turfSearchField != null) {
            turfSearchField.setBackground(newTheme.getBgField());
            turfSearchField.setForeground(newTheme.getTextPrimary());
            turfSearchField.setCaretColor(newTheme.getAccentGreen());
        }

        if (userRoleCombo != null) {
            userRoleCombo.setBackground(newTheme.getBgField());
            userRoleCombo.setForeground(newTheme.getTextPrimary());
            userRoleCombo.setBorder(BorderFactory.createLineBorder(newTheme.getBorder(), 1));
        }
        if (userSearchField != null) {
            userSearchField.setBackground(newTheme.getBgField());
            userSearchField.setForeground(newTheme.getTextPrimary());
            userSearchField.setCaretColor(newTheme.getAccentGreen());
        }

        if (bookingStatusCombo != null) {
            bookingStatusCombo.setBackground(newTheme.getBgField());
            bookingStatusCombo.setForeground(newTheme.getTextPrimary());
            bookingStatusCombo.setBorder(BorderFactory.createLineBorder(newTheme.getBorder(), 1));
        }
        if (bookingSearchField != null) {
            bookingSearchField.setBackground(newTheme.getBgField());
            bookingSearchField.setForeground(newTheme.getTextPrimary());
            bookingSearchField.setCaretColor(newTheme.getAccentGreen());
        }

        StyleConstants.updateTableTheme(turfsTable);
        StyleConstants.updateTableTheme(usersTable);
        StyleConstants.updateTableTheme(bookingsTable);

        repaint();
    }
}
