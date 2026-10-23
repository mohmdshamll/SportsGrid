package ui;

import model.Booking;
import model.Player;
import model.TimeSlot;
import model.Turf;
import service.BookingService;
import ui.theme.Theme;
import ui.theme.ThemeChangeListener;
import ui.theme.ThemeManager;
import ui.theme.ThemeTableHeaderRenderer;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableRowSorter;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;

/**
 * Screen where a player views available time slots for a selected turf
 * and proceeds to slot booking and payment.
 *
 * Supports Light & Dark themes, runtime switching, striped table rows,
 * and high-contrast status tags.
 */
public class SlotBookingFrame extends JFrame implements ThemeChangeListener {

    private final Player player;
    private final Turf turf;
    private final JFrame parentDashboard;
    private final BookingService bookingService = new BookingService();

    // Container panels for dynamic recoloring
    private JPanel root;
    private JPanel nav;
    private JPanel banner;
    private JPanel bottomBar;
    private JScrollPane scrollPane;

    // UI elements
    private JLabel navTitle;
    private JLabel userTag;
    private JLabel nameLabel;
    private JLabel metaLabel;
    private JLabel priceLabel;
    private JLabel ratingLabel;
    private JLabel headerTitle;
    private JTable slotTable;
    private DefaultTableModel tableModel;
    private JLabel statusLabel;
    private JLabel selectedSlotLabel;
    private JButton bookBtn;
    private JButton refreshBtn;

    private List<TimeSlot> currentSlots = new ArrayList<>();
    private TimeSlot selectedSlot = null;

    public SlotBookingFrame(Player player, Turf turf) {
        this(player, turf, null);
    }

    public SlotBookingFrame(Player player, Turf turf, JFrame parentDashboard) {
        this.player = player;
        this.turf = turf;
        this.parentDashboard = parentDashboard;

        setTitle("SportGrid – Available Slots: " + turf.getName());
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        buildUI();
        setSize(1020, 680);
        setMinimumSize(new Dimension(900, 540));
        setLocationRelativeTo(parentDashboard);

        // Register for runtime theme updates
        ThemeManager.addListener(this);

        // Load slots on frame open
        loadSlots();
    }

    private void buildUI() {
        Theme theme = ThemeManager.getTheme();

        root = new JPanel(new BorderLayout(0, 16));
        root.setBackground(theme.getBgMain());
        root.setBorder(new EmptyBorder(0, 0, 16, 0));
        setContentPane(root);

        // 1. Top navigation
        root.add(buildNavBar(), BorderLayout.NORTH);

        // 2. Center content: Turf Info Banner + Slot Table
        JPanel centerPanel = new JPanel(new BorderLayout(0, 14));
        centerPanel.setOpaque(false);
        centerPanel.setBorder(new EmptyBorder(0, 24, 0, 24));

        banner = buildTurfInfoBanner();
        centerPanel.add(banner, BorderLayout.NORTH);
        centerPanel.add(buildSlotTablePanel(), BorderLayout.CENTER);

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

        JButton backBtn = StyleConstants.secondaryButton("← Back to Turfs");
        backBtn.addActionListener(e -> goBack());
        nav.add(backBtn, BorderLayout.WEST);

        navTitle = new JLabel("Available Time Slots", SwingConstants.CENTER);
        navTitle.setFont(StyleConstants.FONT_HEADING);
        navTitle.setForeground(theme.getTextPrimary());
        nav.add(navTitle, BorderLayout.CENTER);

        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        right.setOpaque(false);

        userTag = new JLabel("Player: " + player.getName());
        userTag.setFont(StyleConstants.FONT_BODY);
        userTag.setForeground(theme.getTextMuted());
        right.add(userTag);

        right.add(StyleConstants.themeToggleButton());

        nav.add(right, BorderLayout.EAST);

        return nav;
    }

    private JPanel buildTurfInfoBanner() {
        Theme theme = ThemeManager.getTheme();

        JPanel p = new JPanel(new BorderLayout(16, 8));
        p.setBackground(theme.getBgCard());
        p.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(theme.getBorder(), 1, true),
                new EmptyBorder(16, 20, 16, 20)));

        // Left: Turf name, sport, location
        JPanel left = new JPanel();
        left.setLayout(new BoxLayout(left, BoxLayout.Y_AXIS));
        left.setOpaque(false);

        nameLabel = new JLabel(turf.getName());
        nameLabel.setFont(StyleConstants.FONT_TITLE);
        nameLabel.setForeground(theme.getAccentGreen());

        metaLabel = new JLabel("⚽ " + turf.getSportType() + "   |   📍 " + turf.getLocation());
        metaLabel.setFont(StyleConstants.FONT_BODY);
        metaLabel.setForeground(theme.getTextPrimary());

        left.add(nameLabel);
        left.add(Box.createVerticalStrut(4));
        left.add(metaLabel);

        // Right: Price and rating badges
        JPanel right = new JPanel();
        right.setLayout(new BoxLayout(right, BoxLayout.Y_AXIS));
        right.setOpaque(false);

        priceLabel = new JLabel(String.format("₹%.2f / hour", turf.getPricePerHour()), SwingConstants.RIGHT);
        priceLabel.setFont(StyleConstants.FONT_HEADING);
        priceLabel.setForeground(theme.getAccentGreen());
        priceLabel.setAlignmentX(Component.RIGHT_ALIGNMENT);

        String ratingText = turf.getAverageRating() > 0
                ? String.format("⭐ %.1f / 5.0", turf.getAverageRating())
                : "⭐ New Turf";
        ratingLabel = new JLabel(ratingText, SwingConstants.RIGHT);
        ratingLabel.setFont(StyleConstants.FONT_SMALL);
        ratingLabel.setForeground(theme.getTextMuted());
        ratingLabel.setAlignmentX(Component.RIGHT_ALIGNMENT);

        right.add(priceLabel);
        right.add(Box.createVerticalStrut(4));
        right.add(ratingLabel);

        p.add(left, BorderLayout.WEST);
        p.add(right, BorderLayout.EAST);
        return p;
    }

    private JPanel buildSlotTablePanel() {
        Theme theme = ThemeManager.getTheme();

        JPanel panel = new JPanel(new BorderLayout(0, 8));
        panel.setOpaque(false);

        // Subheader with title and refresh button
        JPanel subheader = new JPanel(new BorderLayout());
        subheader.setOpaque(false);

        headerTitle = new JLabel("Select a Slot for Booking");
        headerTitle.setFont(StyleConstants.FONT_HEADING);
        headerTitle.setForeground(theme.getTextPrimary());
        subheader.add(headerTitle, BorderLayout.WEST);

        refreshBtn = StyleConstants.secondaryButton("⟳ Refresh Slots");
        refreshBtn.addActionListener(e -> loadSlots());
        subheader.add(refreshBtn, BorderLayout.EAST);

        panel.add(subheader, BorderLayout.NORTH);

        // Table setup
        String[] columns = {"Slot ID", "Date", "Start Time", "End Time", "Duration", "Total Price", "Availability"};
        tableModel = new DefaultTableModel(columns, 0) {
            @Override public boolean isCellEditable(int row, int col) { return false; }
        };

        slotTable = new JTable(tableModel);
        StyleConstants.styleTable(slotTable);

        // Hide slot ID column (internal use)
        slotTable.getColumnModel().getColumn(0).setMinWidth(0);
        slotTable.getColumnModel().getColumn(0).setMaxWidth(0);
        slotTable.getColumnModel().getColumn(0).setPreferredWidth(0);

        // Proportional column widths
        slotTable.getColumnModel().getColumn(1).setPreferredWidth(160); // Date
        slotTable.getColumnModel().getColumn(2).setPreferredWidth(120); // Start Time
        slotTable.getColumnModel().getColumn(3).setPreferredWidth(120); // End Time
        slotTable.getColumnModel().getColumn(4).setPreferredWidth(110); // Duration
        slotTable.getColumnModel().getColumn(5).setPreferredWidth(130); // Total Price
        slotTable.getColumnModel().getColumn(6).setPreferredWidth(150); // Availability

        // Center align data columns with SlotTableCellRenderer
        for (int i = 1; i <= 6; i++) {
            slotTable.getColumnModel().getColumn(i).setCellRenderer(new SlotTableCellRenderer(SwingConstants.CENTER));
        }

        // Configure RowSorter with numerical comparators for Duration and Total Price
        TableRowSorter<DefaultTableModel> sorter = new TableRowSorter<>(tableModel);
        sorter.setSortable(0, false); // Hidden ID
        sorter.setComparator(4, (o1, o2) -> Double.compare(parseDuration(String.valueOf(o1)), parseDuration(String.valueOf(o2))));
        sorter.setComparator(5, (o1, o2) -> Double.compare(parsePrice(String.valueOf(o1)), parsePrice(String.valueOf(o2))));
        slotTable.setRowSorter(sorter);

        // Row selection listener
        slotTable.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                int selectedRow = slotTable.getSelectedRow();
                if (selectedRow >= 0 && selectedRow < slotTable.getRowCount()) {
                    int modelRow = (slotTable.getRowSorter() != null)
                            ? slotTable.convertRowIndexToModel(selectedRow)
                            : selectedRow;
                    if (modelRow >= 0 && modelRow < currentSlots.size()) {
                        TimeSlot slot = currentSlots.get(modelRow);
                        if (slot.isAvailable() && !slot.isInPast()) {
                            selectedSlot = slot;
                            double amount = turf.getPricePerHour() * selectedSlot.getDurationHours();
                            selectedSlotLabel.setText(String.format("Selected: %s (%s - %s) • Total: ₹%.2f",
                                    selectedSlot.getDate(), selectedSlot.getStartTime(), selectedSlot.getEndTime(), amount));
                            selectedSlotLabel.setForeground(ThemeManager.getTheme().getTextPrimary());
                            bookBtn.setEnabled(true);
                        } else {
                            selectedSlot = null;
                            selectedSlotLabel.setText("Selected slot is " + (slot.isBooked() ? "already booked" : "unavailable") + " — please choose an available slot");
                            selectedSlotLabel.setForeground(ThemeManager.getTheme().getStatusWarning());
                            bookBtn.setEnabled(false);
                        }
                    }
                } else {
                    selectedSlot = null;
                    selectedSlotLabel.setText("Click on an available slot above to select it");
                    selectedSlotLabel.setForeground(ThemeManager.getTheme().getTextMuted());
                    bookBtn.setEnabled(false);
                }
            }
        });

        // Double click to proceed
        slotTable.addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2 && slotTable.getSelectedRow() >= 0) {
                    proceedToBooking();
                }
            }
        });

        scrollPane = new JScrollPane(slotTable);
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

        // Left: status and selection label
        JPanel left = new JPanel();
        left.setLayout(new BoxLayout(left, BoxLayout.Y_AXIS));
        left.setOpaque(false);

        selectedSlotLabel = new JLabel("Click on an available slot above to select it");
        selectedSlotLabel.setFont(StyleConstants.FONT_BODY);
        selectedSlotLabel.setForeground(theme.getTextMuted());

        statusLabel = new JLabel("Loading slots...");
        statusLabel.setFont(StyleConstants.FONT_SMALL);
        statusLabel.setForeground(theme.getTextMuted());

        left.add(selectedSlotLabel);
        left.add(Box.createVerticalStrut(4));
        left.add(statusLabel);
        bar.add(left, BorderLayout.CENTER);

        // Right: Proceed to book button
        bookBtn = StyleConstants.primaryButton("Proceed to Payment →");
        bookBtn.setPreferredSize(new Dimension(220, 42));
        bookBtn.setEnabled(false);
        bookBtn.addActionListener(e -> proceedToBooking());
        bar.add(bookBtn, BorderLayout.EAST);

        return bar;
    }

    private void loadSlots() {
        refreshBtn.setEnabled(false);
        bookBtn.setEnabled(false);
        statusLabel.setText("Fetching available slots from MySQL...");
        statusLabel.setForeground(ThemeManager.getTheme().getTextMuted());

        new SwingWorker<List<TimeSlot>, Void>() {
            @Override
            protected List<TimeSlot> doInBackground() {
                return bookingService.getAvailableSlots(turf.getTurfId());
            }

            @Override
            protected void done() {
                refreshBtn.setEnabled(true);
                Theme t = ThemeManager.getTheme();
                try {
                    List<TimeSlot> slots = get();
                    currentSlots = slots;
                    tableModel.setRowCount(0);

                    for (TimeSlot s : slots) {
                        double duration = s.getDurationHours();
                        double totalCost = turf.getPricePerHour() * duration;
                        String availability = s.isBooked() ? "BOOKED" : (s.isInPast() ? "UNAVAILABLE" : "AVAILABLE");
                        tableModel.addRow(new Object[]{
                                s.getSlotId(),
                                s.getDate().toString(),
                                s.getStartTime().toString(),
                                s.getEndTime().toString(),
                                String.format("%.1f hr", duration),
                                String.format("₹%.2f", totalCost),
                                availability
                        });
                    }

                    if (slots.isEmpty()) {
                        statusLabel.setText("No open slots currently available for this turf.");
                    } else {
                        statusLabel.setText(String.format("Found %d open slot%s for booking.",
                                slots.size(), slots.size() > 1 ? "s" : ""));
                    }
                    selectedSlotLabel.setForeground(t.getTextMuted());
                    statusLabel.setForeground(t.getTextMuted());
                } catch (Exception ex) {
                    statusLabel.setText("Error loading slots: " + StyleConstants.formatErrorMessage(ex, "Slots"));
                    statusLabel.setForeground(t.getStatusError());
                }
            }
        }.execute();
    }

    private void proceedToBooking() {
        if (selectedSlot == null || !selectedSlot.isAvailable() || selectedSlot.isInPast()) {
            JOptionPane.showMessageDialog(this,
                    "Please select an available slot first.",
                    "No Slot Selected",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }

        // Open Payment Simulation Dialog
        PaymentDialog dialog = new PaymentDialog(this, player, turf, selectedSlot, booking -> {
            showBookingConfirmation(booking);
            // Refresh slots from MySQL so the newly booked slot disappears from availability
            loadSlots();
        });
        dialog.setVisible(true);
    }

    private void showBookingConfirmation(Booking booking) {
        Theme t = ThemeManager.getTheme();
        String greenHex = String.format("#%02x%02x%02x", t.getAccentGreen().getRed(), t.getAccentGreen().getGreen(), t.getAccentGreen().getBlue());
        String borderHex = String.format("#%02x%02x%02x", t.getBorder().getRed(), t.getBorder().getGreen(), t.getBorder().getBlue());
        String mutedHex = String.format("#%02x%02x%02x", t.getTextMuted().getRed(), t.getTextMuted().getGreen(), t.getTextMuted().getBlue());

        String details = String.format(
                "<html>"
                + "<h2 style='color:%s; margin:0;'>🎉 Booking Confirmed!</h2>"
                + "<hr style='border:1px solid %s; margin:8px 0;'>"
                + "<table cellpadding='4' style='font-size:12px;'>"
                + "<tr><td><b>Booking Ref:</b></td><td style='color:%s;'><b>#%d</b></td></tr>"
                + "<tr><td><b>Turf:</b></td><td>%s (%s)</td></tr>"
                + "<tr><td><b>Location:</b></td><td>%s</td></tr>"
                + "<tr><td><b>Date:</b></td><td>%s</td></tr>"
                + "<tr><td><b>Time:</b></td><td>%s - %s</td></tr>"
                + "<tr><td><b>Amount Paid:</b></td><td style='color:%s;'><b>₹%.2f</b></td></tr>"
                + "<tr><td><b>Payment Mode:</b></td><td>%s (%s)</td></tr>"
                + "<tr><td><b>Status:</b></td><td style='color:%s;'><b>%s</b></td></tr>"
                + "</table>"
                + "<p style='color:%s; font-size:11px; margin-top:10px;'>Saved to MySQL database with atomic transaction safety.</p>"
                + "</html>",
                greenHex,
                borderHex,
                greenHex, booking.getBookingId(),
                booking.getTurf().getName(),
                booking.getTurf().getSportType(),
                booking.getTurf().getLocation(),
                booking.getSlot().getDate(),
                booking.getSlot().getStartTime(),
                booking.getSlot().getEndTime(),
                greenHex, booking.getTotalAmount(),
                booking.getPayment().getMethod(),
                booking.getPayment().getReference(),
                greenHex, booking.getStatus(),
                mutedHex
        );

        JOptionPane.showMessageDialog(
                this,
                details,
                "Booking Confirmed – Reference #" + booking.getBookingId(),
                JOptionPane.INFORMATION_MESSAGE
        );
    }

    private void goBack() {
        ThemeManager.removeListener(this);
        dispose();
        if (parentDashboard != null) {
            parentDashboard.setVisible(true);
            parentDashboard.toFront();
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

        if (banner != null) {
            banner.setBackground(newTheme.getBgCard());
            banner.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(newTheme.getBorder(), 1, true),
                    new EmptyBorder(16, 20, 16, 20)));
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
                        newTheme.isDark() ? newTheme.getBgMain() : ThemeTableHeaderRenderer.LIGHT_HEADER_BG);
            }
        }

        navTitle.setForeground(newTheme.getTextPrimary());
        userTag.setForeground(newTheme.getTextMuted());
        nameLabel.setForeground(newTheme.getAccentGreen());
        metaLabel.setForeground(newTheme.getTextPrimary());
        priceLabel.setForeground(newTheme.getAccentGreen());
        ratingLabel.setForeground(newTheme.getTextMuted());
        headerTitle.setForeground(newTheme.getTextPrimary());

        if (selectedSlot != null) {
            selectedSlotLabel.setForeground(newTheme.getTextPrimary());
        } else {
            selectedSlotLabel.setForeground(newTheme.getTextMuted());
        }
        statusLabel.setForeground(newTheme.getTextMuted());

        StyleConstants.updateTableTheme(slotTable);

        repaint();
    }

    private static double parseDuration(String text) {
        if (text == null) return 0.0;
        try {
            String clean = text.replaceAll("[^0-9.]", "");
            return clean.isEmpty() ? 0.0 : Double.parseDouble(clean);
        } catch (Exception e) {
            return 0.0;
        }
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

    /**
     * Dedicated theme-aware cell renderer for the Available Time Slots table.
     *
     * Provides distinct visual treatments:
     * - Available slots: high-contrast text with emerald green status badge and alternating row backgrounds.
     * - Selected slot: fully highlighted with SportGrid emerald accent and high-contrast text.
     * - Booked / unavailable slots: distinct muted appearance with muted text and clear status indicator.
     */
    public static class SlotTableCellRenderer extends DefaultTableCellRenderer {
        private final int horizontalAlignment;

        public SlotTableCellRenderer(int horizontalAlignment) {
            this.horizontalAlignment = horizontalAlignment;
            setHorizontalAlignment(horizontalAlignment);
        }

        public int getHorizontalAlignment() {
            return horizontalAlignment;
        }

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value,
                                                       boolean isSelected, boolean hasFocus,
                                                       int row, int column) {
            super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            setHorizontalAlignment(horizontalAlignment);

            Theme t = ThemeManager.getTheme();

            // Determine slot availability from status column (column 6)
            String status = "AVAILABLE";
            if (table != null && row >= 0 && row < table.getRowCount()) {
                try {
                    int statusCol = table.convertColumnIndexToView(6);
                    Object statusVal = (statusCol >= 0) ? table.getValueAt(row, statusCol) : table.getModel().getValueAt(row, 6);
                    if (statusVal != null) {
                        status = statusVal.toString().trim().toUpperCase();
                    }
                } catch (Exception e) {
                    Object statusVal = table.getModel().getValueAt(row, 6);
                    if (statusVal != null) status = statusVal.toString().trim().toUpperCase();
                }
            }
            boolean isAvailable = "AVAILABLE".equals(status);

            // Column typography: bold for Price and Status, body for date & times
            if (column == 5 || column == 6) {
                setFont(StyleConstants.FONT_HEADING);
            } else {
                setFont(StyleConstants.FONT_BODY);
            }

            // Cell border padding
            setBorder(new EmptyBorder(0, 12, 0, 12));

            // Visual treatment
            if (isSelected) {
                // Requirement 4: Selected slot clearly highlighted with emerald accent & high-contrast text
                setBackground(t.getAccentGreen());
                setForeground(t.isDark() ? t.getTableSelectionFg() : Color.WHITE);
            } else {
                // Requirement 6: Subtle alternating row striping
                Color bg = (row % 2 == 1) ? t.getTableRowAlt() : t.getBgCard();
                setBackground(bg);

                if (!isAvailable) {
                    // Requirement 5: Distinct muted appearance for booked / unavailable slots
                    if (column == 6) {
                        setForeground(status.contains("BOOKED") ? t.getStatusError() : t.getTextMuted());
                    } else {
                        setForeground(t.getTextMuted());
                    }
                } else {
                    // Requirement 3 & 7: Available slot treatment
                    if (column == 6) {
                        setForeground(t.getStatusSuccess()); // Emerald green status tag
                    } else if (column == 5) {
                        setForeground(t.getTextPrimary());   // Crisp bold total price
                    } else {
                        setForeground(t.getTextPrimary());   // Crisp readable date, times, duration
                    }
                }
            }

            return this;
        }
    }
}
