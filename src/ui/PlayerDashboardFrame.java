package ui;

import dao.BookingDAO;
import model.Player;
import model.Role;
import model.Turf;
import model.User;
import service.BookingService;
import service.TurfService;
import ui.theme.Theme;
import ui.theme.ThemeChangeListener;
import ui.theme.ThemeManager;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableRowSorter;
import java.awt.*;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;

/**
 * Player Dashboard:
 *  - Displays approved turfs directly from MySQL
 *  - Search and filter by sport and location
 *  - Selection to view open time slots and proceed to booking
 *  - Responsive database queries running off the Swing Event Dispatch Thread (SwingWorker)
 *  - Full Light and Dark mode runtime switching with persistent preferences
 */
public class PlayerDashboardFrame extends JFrame implements ThemeChangeListener {

    private final Player player;
    private final BookingService bookingService = new BookingService();
    private final TurfService turfService = new TurfService();
    private final BookingDAO bookingDAO = new BookingDAO();

    // Container panels for dynamic recoloring
    private JPanel root;
    private JPanel nav;
    private JPanel filterBar;
    private JPanel bottomBar;
    private JScrollPane scrollPane;

    // UI Components
    private JLabel brand;
    private JLabel welcome;
    private JLabel headerTitle;
    private JLabel sportLbl;
    private JLabel locLbl;
    private JComboBox<String> sportFilterCombo;
    private JTextField locationSearchField;
    private JButton searchBtn;
    private JButton resetBtn;
    private JTable turfTable;
    private DefaultTableModel tableModel;
    private JLabel statusLabel;
    private JLabel selectedTurfLabel;
    private JButton rateTurfBtn;
    private JButton proceedBtn;

    // Loaded turf data
    private List<Turf> currentTurfs = new ArrayList<>();
    private Turf selectedTurf = null;

    public PlayerDashboardFrame(User user) {
        if (user instanceof Player) {
            this.player = (Player) user;
        } else {
            this.player = (Player) User.create(Role.PLAYER, user.getUserId(), user.getName(),
                    user.getEmail(), user.getPhone(), user.getPasswordHash());
        }

        setTitle("SportGrid – Player Dashboard (" + player.getName() + ")");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        buildUI();
        setSize(1100, 720);
        setMinimumSize(new Dimension(980, 560));
        setLocationRelativeTo(null);

        // Register for runtime theme changes
        ThemeManager.addListener(this);

        // Initial fetch of approved turfs
        loadTurfs(null, null);
    }

    private void buildUI() {
        Theme theme = ThemeManager.getTheme();

        root = new JPanel(new BorderLayout(0, 16));
        root.setBackground(theme.getBgMain());
        root.setBorder(new EmptyBorder(0, 0, 16, 0));
        setContentPane(root);

        // 1. Top navigation
        root.add(buildNavBar(), BorderLayout.NORTH);

        // 2. Center workspace: Filter Bar + Turf Table
        JPanel centerPanel = new JPanel(new BorderLayout(0, 14));
        centerPanel.setOpaque(false);
        centerPanel.setBorder(new EmptyBorder(0, 24, 0, 24));

        filterBar = buildFilterBar();
        centerPanel.add(filterBar, BorderLayout.NORTH);
        centerPanel.add(buildTablePanel(), BorderLayout.CENTER);

        root.add(centerPanel, BorderLayout.CENTER);

        // 3. Bottom action bar
        bottomBar = buildBottomActionBar();
        root.add(bottomBar, BorderLayout.SOUTH);
    }

    private JPanel buildNavBar() {
        Theme theme = ThemeManager.getTheme();

        nav = new JPanel(new BorderLayout(16, 0));
        nav.setBackground(theme.getBgCard());
        nav.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, theme.getBorder()),
                new EmptyBorder(12, 24, 12, 24)));

        JPanel brandPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        brandPanel.setOpaque(false);

        JLabel icon = new JLabel("⚽");
        icon.setFont(StyleConstants.FONT_TITLE);
        brandPanel.add(icon);

        brand = new JLabel("SportGrid");
        brand.setFont(StyleConstants.FONT_TITLE);
        brand.setForeground(theme.getAccentGreen());
        brandPanel.add(brand);

        nav.add(brandPanel, BorderLayout.WEST);

        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 0));
        right.setOpaque(false);

        welcome = new JLabel("Welcome, " + player.getName() + " (Player)");
        welcome.setFont(StyleConstants.FONT_BODY);
        welcome.setForeground(theme.getTextPrimary());
        right.add(welcome);

        // Theme toggle button
        right.add(StyleConstants.themeToggleButton());

        JButton myBookingsBtn = StyleConstants.secondaryButton("📅 My Bookings");
        myBookingsBtn.setToolTipText("View all your turf bookings and confirmation references");
        myBookingsBtn.addActionListener(e -> {
            new BookingHistoryFrame(player, this).setVisible(true);
        });
        right.add(myBookingsBtn);

        JButton logoutBtn = StyleConstants.secondaryButton("Logout");
        logoutBtn.addActionListener(e -> {
            ThemeManager.removeListener(this);
            dispose();
            new LoginFrame().setVisible(true);
        });
        right.add(logoutBtn);

        nav.add(right, BorderLayout.EAST);
        return nav;
    }

    private JPanel buildFilterBar() {
        Theme theme = ThemeManager.getTheme();

        JPanel bar = new JPanel(new GridBagLayout());
        bar.setBackground(theme.getBgCard());
        bar.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(theme.getBorder(), 1, true),
                new EmptyBorder(14, 18, 14, 18)));

        GridBagConstraints g = new GridBagConstraints();
        g.insets = new Insets(0, 8, 0, 8);
        g.fill = GridBagConstraints.HORIZONTAL;

        // Sport dropdown
        g.gridx = 0; g.weightx = 0;
        sportLbl = new JLabel("Sport:");
        sportLbl.setFont(StyleConstants.FONT_BODY);
        sportLbl.setForeground(theme.getTextMuted());
        bar.add(sportLbl, g);

        g.gridx = 1; g.weightx = 0.2;
        String[] sports = {"All Sports", "Football", "Cricket", "Badminton", "Tennis", "Basketball", "Swimming", "Futsal"};
        sportFilterCombo = StyleConstants.styledComboBox(sports);
        bar.add(sportFilterCombo, g);

        // Location text field
        g.gridx = 2; g.weightx = 0;
        locLbl = new JLabel("Location:");
        locLbl.setFont(StyleConstants.FONT_BODY);
        locLbl.setForeground(theme.getTextMuted());
        bar.add(locLbl, g);

        g.gridx = 3; g.weightx = 1.0;
        locationSearchField = StyleConstants.styledField();
        locationSearchField.setToolTipText("Enter city, district or area (e.g. Kochi, Kakkanad)");
        bar.add(locationSearchField, g);

        // Search Button
        g.gridx = 4; g.weightx = 0;
        searchBtn = StyleConstants.primaryButton("🔍 Search");
        bar.add(searchBtn, g);

        // Reset Button
        g.gridx = 5; g.weightx = 0;
        resetBtn = StyleConstants.secondaryButton("⟲ Reset");
        bar.add(resetBtn, g);

        // Actions
        ActionListener performSearch = e -> onSearchClicked();
        searchBtn.addActionListener(performSearch);
        locationSearchField.addActionListener(performSearch);
        sportFilterCombo.addActionListener(performSearch);

        resetBtn.addActionListener(e -> {
            sportFilterCombo.setSelectedIndex(0);
            locationSearchField.setText("");
            loadTurfs(null, null);
        });

        return bar;
    }

    private JPanel buildTablePanel() {
        Theme theme = ThemeManager.getTheme();

        JPanel panel = new JPanel(new BorderLayout(0, 8));
        panel.setOpaque(false);

        headerTitle = new JLabel("Approved Sports Turfs");
        headerTitle.setFont(StyleConstants.FONT_HEADING);
        headerTitle.setForeground(theme.getTextPrimary());
        panel.add(headerTitle, BorderLayout.NORTH);

        String[] columns = {"Turf ID", "Turf Name", "Sport", "Location", "Price / Hour", "Rating"};
        tableModel = new DefaultTableModel(columns, 0) {
            @Override public boolean isCellEditable(int row, int col) { return false; }
        };

        turfTable = new JTable(tableModel);
        StyleConstants.styleTable(turfTable);

        // Hide ID column
        turfTable.getColumnModel().getColumn(0).setMinWidth(0);
        turfTable.getColumnModel().getColumn(0).setMaxWidth(0);
        turfTable.getColumnModel().getColumn(0).setPreferredWidth(0);

        // Column widths
        turfTable.getColumnModel().getColumn(1).setPreferredWidth(240); // Name
        turfTable.getColumnModel().getColumn(2).setPreferredWidth(130); // Sport
        turfTable.getColumnModel().getColumn(3).setPreferredWidth(260); // Location
        turfTable.getColumnModel().getColumn(4).setPreferredWidth(140); // Price
        turfTable.getColumnModel().getColumn(5).setPreferredWidth(120); // Rating

        // Alignments using StripedTableCellRenderer
        turfTable.getColumnModel().getColumn(1).setCellRenderer(new StyleConstants.StripedTableCellRenderer(SwingConstants.LEFT));
        turfTable.getColumnModel().getColumn(2).setCellRenderer(new StyleConstants.StripedTableCellRenderer(SwingConstants.CENTER));
        turfTable.getColumnModel().getColumn(3).setCellRenderer(new StyleConstants.StripedTableCellRenderer(SwingConstants.LEFT));
        turfTable.getColumnModel().getColumn(4).setCellRenderer(new StyleConstants.StripedTableCellRenderer(SwingConstants.CENTER));
        turfTable.getColumnModel().getColumn(5).setCellRenderer(new StyleConstants.StripedTableCellRenderer(SwingConstants.CENTER));

        // Configure RowSorter with numerical sorting for Price and Rating columns
        TableRowSorter<DefaultTableModel> sorter = new TableRowSorter<>(tableModel);
        sorter.setSortable(0, false);
        sorter.setComparator(4, (o1, o2) -> Double.compare(parsePrice(String.valueOf(o1)), parsePrice(String.valueOf(o2))));
        sorter.setComparator(5, (o1, o2) -> Double.compare(parseRating(String.valueOf(o1)), parseRating(String.valueOf(o2))));
        turfTable.setRowSorter(sorter);

        // Selection Listener
        turfTable.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                int selectedRow = turfTable.getSelectedRow();
                if (selectedRow >= 0 && selectedRow < turfTable.getRowCount()) {
                    int modelRow = turfTable.convertRowIndexToModel(selectedRow);
                    if (modelRow >= 0 && modelRow < currentTurfs.size()) {
                        selectedTurf = currentTurfs.get(modelRow);
                        selectedTurfLabel.setText(String.format("Selected: %s (%s) • ₹%.2f/hr",
                                selectedTurf.getName(), selectedTurf.getSportType(), selectedTurf.getPricePerHour()));
                        selectedTurfLabel.setForeground(ThemeManager.getTheme().getTextPrimary());
                        proceedBtn.setEnabled(true);
                        updateRateButton();
                    }
                } else {
                    selectedTurf = null;
                    selectedTurfLabel.setText("Click on any turf above to select it");
                    selectedTurfLabel.setForeground(ThemeManager.getTheme().getTextMuted());
                    proceedBtn.setEnabled(false);
                    updateRateButton();
                }
            }
        });

        // Double click to open slots
        turfTable.addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2 && turfTable.getSelectedRow() >= 0) {
                    proceedToSlots();
                }
            }
        });

        scrollPane = new JScrollPane(turfTable);
        StyleConstants.styleScrollPane(scrollPane);
        panel.add(scrollPane, BorderLayout.CENTER);

        return panel;
    }

    private JPanel buildBottomActionBar() {
        Theme theme = ThemeManager.getTheme();

        JPanel bar = new JPanel(new BorderLayout(16, 0));
        bar.setBackground(theme.getBgCard());
        bar.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, theme.getBorder()),
                new EmptyBorder(14, 24, 14, 24)));

        // Left info
        JPanel left = new JPanel();
        left.setLayout(new BoxLayout(left, BoxLayout.Y_AXIS));
        left.setOpaque(false);

        selectedTurfLabel = new JLabel("Click on any turf above to select it");
        selectedTurfLabel.setFont(StyleConstants.FONT_BODY);
        selectedTurfLabel.setForeground(theme.getTextMuted());

        statusLabel = new JLabel("Loading turfs from database...");
        statusLabel.setFont(StyleConstants.FONT_SMALL);
        statusLabel.setForeground(theme.getTextMuted());

        left.add(selectedTurfLabel);
        left.add(Box.createVerticalStrut(4));
        left.add(statusLabel);
        bar.add(left, BorderLayout.CENTER);

        // Right buttons
        JPanel rightActions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        rightActions.setOpaque(false);

        rateTurfBtn = StyleConstants.secondaryButton("⭐ Rate Turf");
        rateTurfBtn.setPreferredSize(new Dimension(140, 42));
        rateTurfBtn.setEnabled(false);
        rateTurfBtn.setToolTipText("Select a turf you have booked to rate it");
        rateTurfBtn.addActionListener(e -> openRateTurfDialog());
        rightActions.add(rateTurfBtn);

        proceedBtn = StyleConstants.primaryButton("View Slots & Book →");
        proceedBtn.setPreferredSize(new Dimension(200, 42));
        proceedBtn.setEnabled(false);
        proceedBtn.addActionListener(e -> proceedToSlots());
        rightActions.add(proceedBtn);

        bar.add(rightActions, BorderLayout.EAST);

        return bar;
    }

    private void updateRateButton() {
        if (selectedTurf == null) {
            rateTurfBtn.setEnabled(false);
            rateTurfBtn.setText("⭐ Rate Turf");
            rateTurfBtn.setToolTipText("Select a turf you have booked to rate it");
            return;
        }

        boolean hasBooking = bookingDAO.hasConfirmedBooking(player.getUserId(), selectedTurf.getTurfId());
        if (!hasBooking) {
            rateTurfBtn.setEnabled(false);
            rateTurfBtn.setText("⭐ Rate Turf");
            rateTurfBtn.setToolTipText("You can review this turf after booking it");
        } else {
            boolean alreadyReviewed = turfService.hasReviewed(player, selectedTurf.getTurfId());
            if (alreadyReviewed) {
                rateTurfBtn.setEnabled(true);
                rateTurfBtn.setText("✓ Reviewed");
                rateTurfBtn.setToolTipText("Click to view your submitted review for " + selectedTurf.getName());
            } else {
                rateTurfBtn.setEnabled(true);
                rateTurfBtn.setText("⭐ Rate Turf");
                rateTurfBtn.setToolTipText("Rate and review " + selectedTurf.getName());
            }
        }
    }

    private void openRateTurfDialog() {
        if (selectedTurf == null) return;
        new RateTurfDialog(this, player, selectedTurf, this::reloadTurfs).setVisible(true);
    }

    /** Reloads the approved turfs list to refresh updated average ratings. */
    public void reloadTurfs() {
        onSearchClicked();
    }

    private void onSearchClicked() {
        String sport = (String) sportFilterCombo.getSelectedItem();
        String location = locationSearchField.getText().trim();
        loadTurfs(sport, location);
    }

    private void loadTurfs(String sport, String location) {
        searchBtn.setEnabled(false);
        resetBtn.setEnabled(false);
        proceedBtn.setEnabled(false);
        if (rateTurfBtn != null) rateTurfBtn.setEnabled(false);
        statusLabel.setText("Searching approved turfs in MySQL...");
        statusLabel.setForeground(ThemeManager.getTheme().getTextMuted());

        final String sportParam = (sport == null || sport.equalsIgnoreCase("All Sports") || sport.isBlank())
                ? null : sport.trim();
        final String locParam = (location == null || location.isBlank()) ? null : location.trim();

        new SwingWorker<List<Turf>, Void>() {
            @Override
            protected List<Turf> doInBackground() {
                return bookingService.searchTurfs(sportParam, locParam);
            }

            @Override
            protected void done() {
                searchBtn.setEnabled(true);
                resetBtn.setEnabled(true);
                Theme t = ThemeManager.getTheme();
                try {
                    List<Turf> turfs = get();
                    currentTurfs = turfs;
                    tableModel.setRowCount(0);

                    for (Turf trf : turfs) {
                        String rating = trf.getAverageRating() > 0
                                ? String.format("⭐ %.1f / 5.0", trf.getAverageRating())
                                : "⭐ New";
                        tableModel.addRow(new Object[]{
                                trf.getTurfId(),
                                trf.getName(),
                                trf.getSportType(),
                                trf.getLocation(),
                                String.format("₹%.2f / hr", trf.getPricePerHour()),
                                rating
                        });
                    }

                    if (turfs.isEmpty()) {
                        statusLabel.setText("No approved turfs found matching the search criteria.");
                        selectedTurfLabel.setText("Try clearing or broadening your filters");
                    } else {
                        statusLabel.setText(String.format("Found %d approved turf%s.",
                                turfs.size(), turfs.size() > 1 ? "s" : ""));
                        selectedTurfLabel.setText("Click on any turf above to select it");
                    }
                    selectedTurfLabel.setForeground(t.getTextMuted());
                    statusLabel.setForeground(t.getTextMuted());
                } catch (Exception ex) {
                    statusLabel.setText("Error loading turfs: " + StyleConstants.formatErrorMessage(ex, "Turf Search"));
                    statusLabel.setForeground(t.getStatusError());
                }
            }
        }.execute();
    }

    private void proceedToSlots() {
        if (selectedTurf == null) {
            JOptionPane.showMessageDialog(this,
                    "Please select a turf first.",
                    "No Turf Selected",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }

        // Open SlotBookingFrame
        new SlotBookingFrame(player, selectedTurf, this).setVisible(true);
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

        if (filterBar != null) {
            filterBar.setBackground(newTheme.getBgCard());
            filterBar.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(newTheme.getBorder(), 1, true),
                    new EmptyBorder(14, 18, 14, 18)));
        }

        if (bottomBar != null) {
            bottomBar.setBackground(newTheme.getBgCard());
            bottomBar.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createMatteBorder(1, 0, 0, 0, newTheme.getBorder()),
                    new EmptyBorder(14, 24, 14, 24)));
        }

        if (scrollPane != null) {
            scrollPane.setBorder(BorderFactory.createLineBorder(newTheme.getBorder(), 1));
            scrollPane.getViewport().setBackground(newTheme.getBgCard());
            scrollPane.setBackground(newTheme.getBgMain());
            if (scrollPane.getCorner(JScrollPane.UPPER_RIGHT_CORNER) != null) {
                scrollPane.getCorner(JScrollPane.UPPER_RIGHT_CORNER).setBackground(
                        newTheme.isDark() ? newTheme.getBgMain() : ui.theme.ThemeTableHeaderRenderer.LIGHT_HEADER_BG);
            }
        }

        brand.setForeground(newTheme.getAccentGreen());
        welcome.setForeground(newTheme.getTextPrimary());
        headerTitle.setForeground(newTheme.getTextPrimary());
        sportLbl.setForeground(newTheme.getTextMuted());
        locLbl.setForeground(newTheme.getTextMuted());

        sportFilterCombo.setBackground(newTheme.getBgField());
        sportFilterCombo.setForeground(newTheme.getTextPrimary());
        sportFilterCombo.repaint();

        locationSearchField.setBackground(newTheme.getBgField());
        locationSearchField.setForeground(newTheme.getTextPrimary());
        locationSearchField.setCaretColor(newTheme.getAccentGreen());

        if (selectedTurf != null) {
            selectedTurfLabel.setForeground(newTheme.getTextPrimary());
        } else {
            selectedTurfLabel.setForeground(newTheme.getTextMuted());
        }
        statusLabel.setForeground(newTheme.getTextMuted());

        if (rateTurfBtn != null) {
            rateTurfBtn.repaint();
        }
        if (proceedBtn != null) {
            proceedBtn.repaint();
        }

        StyleConstants.updateTableTheme(turfTable);

        repaint();
    }

    private static double parsePrice(String text) {
        if (text == null) return 0.0;
        try {
            String clean = text.replaceAll("[^0-9.]", "");
            return clean.isEmpty() ? 0.0 : Double.parseDouble(clean);
        } catch (Exception e) {
            return 0.0;
        }
    }

    private static double parseRating(String text) {
        if (text == null || text.contains("New")) return 0.0;
        try {
            String clean = text.replaceAll("[^0-9.]", "");
            return clean.isEmpty() ? 0.0 : Double.parseDouble(clean);
        } catch (Exception e) {
            return 0.0;
        }
    }
}
