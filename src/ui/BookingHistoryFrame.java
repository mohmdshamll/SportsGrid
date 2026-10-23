package ui;

import model.Booking;
import model.BookingStatus;
import model.Player;
import model.Turf;
import service.BookingService;
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
 * My Bookings Screen for Players.
 *
 * Displays only the logged-in player's past and upcoming bookings,
 * loaded live from MySQL via BookingService and BookingDAO.
 *
 * Supports Light & Dark visual themes, runtime switching, striped table rows,
 * and high-contrast status tags.
 */
public class BookingHistoryFrame extends JFrame implements ThemeChangeListener {

    private final Player player;
    private final JFrame parentDashboard;
    private final BookingService bookingService = new BookingService();
    private final service.TurfService turfService = new service.TurfService();

    // Container panels
    private JPanel root;
    private JPanel nav;
    private JPanel emptyStatePanel;
    private JPanel bottomBar;
    private JScrollPane scrollPane;

    // UI elements
    private JLabel title;
    private JLabel userBadge;
    private JLabel header;
    private JLabel selectedBookingLabel;
    private JButton rateTurfBtn;
    private JButton cancelBookingBtn;
    private JTable bookingTable;
    private DefaultTableModel tableModel;
    private JLabel statusLabel;
    private JLabel isolationNotice;
    private JButton refreshBtn;

    // Empty state labels
    private JLabel emptyTitle;
    private JLabel emptyDesc;

    private CardLayout centerCardLayout;
    private JPanel centerCardContainer;
    private List<Booking> currentBookings = new ArrayList<>();
    private Booking selectedBooking = null;

    public BookingHistoryFrame(Player player, JFrame parentDashboard) {
        this.player = player;
        this.parentDashboard = parentDashboard;

        setTitle("SportGrid – My Bookings (" + player.getName() + ")");
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        buildUI();
        setSize(1120, 680);
        setMinimumSize(new Dimension(980, 540));
        setLocationRelativeTo(parentDashboard);

        // Register for runtime theme changes
        ThemeManager.addListener(this);

        // Fetch bookings from MySQL
        loadBookings();
    }

    private void buildUI() {
        Theme theme = ThemeManager.getTheme();

        root = new JPanel(new BorderLayout(0, 14));
        root.setBackground(theme.getBgMain());
        root.setBorder(new EmptyBorder(0, 0, 16, 0));
        setContentPane(root);

        // 1. Top Navbar
        nav = buildNavBar();
        root.add(nav, BorderLayout.NORTH);

        // 2. Center Card Container (switches between Table view and Empty State)
        centerCardLayout = new CardLayout();
        centerCardContainer = new JPanel(centerCardLayout);
        centerCardContainer.setOpaque(false);
        centerCardContainer.setBorder(new EmptyBorder(0, 24, 0, 24));

        centerCardContainer.add(buildTablePanel(), "TABLE");
        emptyStatePanel = buildEmptyStatePanel();
        centerCardContainer.add(emptyStatePanel, "EMPTY");

        root.add(centerCardContainer, BorderLayout.CENTER);

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

        JButton backBtn = StyleConstants.secondaryButton("← Back to Dashboard");
        backBtn.addActionListener(e -> goBack());
        p.add(backBtn, BorderLayout.WEST);

        JPanel centerPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 8, 0));
        centerPanel.setOpaque(false);
        JLabel icon = new JLabel("📅");
        icon.setFont(StyleConstants.FONT_TITLE);
        centerPanel.add(icon);

        title = new JLabel("My Bookings");
        title.setFont(StyleConstants.FONT_TITLE);
        title.setForeground(theme.getAccentGreen());
        centerPanel.add(title);

        p.add(centerPanel, BorderLayout.CENTER);

        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 0));
        right.setOpaque(false);

        userBadge = new JLabel("Player: " + player.getName());
        userBadge.setFont(StyleConstants.FONT_BODY);
        userBadge.setForeground(theme.getTextPrimary());
        right.add(userBadge);

        right.add(StyleConstants.themeToggleButton());

        refreshBtn = StyleConstants.secondaryButton("⟳ Refresh");
        refreshBtn.addActionListener(e -> loadBookings());
        right.add(refreshBtn);

        p.add(right, BorderLayout.EAST);
        return p;
    }

    private JPanel buildTablePanel() {
        Theme theme = ThemeManager.getTheme();

        JPanel panel = new JPanel(new BorderLayout(0, 10));
        panel.setOpaque(false);

        // Header toolbar with title and cancellation controls
        JPanel headerToolbar = new JPanel(new BorderLayout(12, 0));
        headerToolbar.setOpaque(false);

        header = new JLabel("Your Booking History");
        header.setFont(StyleConstants.FONT_HEADING);
        header.setForeground(theme.getTextPrimary());
        headerToolbar.add(header, BorderLayout.WEST);

        JPanel actionRow = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        actionRow.setOpaque(false);

        selectedBookingLabel = new JLabel("Select a booking to manage");
        selectedBookingLabel.setFont(StyleConstants.FONT_SMALL);
        selectedBookingLabel.setForeground(theme.getTextMuted());
        actionRow.add(selectedBookingLabel);

        rateTurfBtn = StyleConstants.secondaryButton("⭐ Rate Turf");
        rateTurfBtn.setEnabled(false);
        rateTurfBtn.setToolTipText("Select a confirmed booking to rate the turf");
        rateTurfBtn.addActionListener(e -> openRateTurfDialog());
        actionRow.add(rateTurfBtn);

        cancelBookingBtn = StyleConstants.dangerButton("✕ Cancel Booking");
        cancelBookingBtn.setEnabled(false);
        cancelBookingBtn.setToolTipText("Select a confirmed booking from the table to cancel");
        cancelBookingBtn.addActionListener(e -> cancelSelectedBooking());
        actionRow.add(cancelBookingBtn);

        headerToolbar.add(actionRow, BorderLayout.EAST);
        panel.add(headerToolbar, BorderLayout.NORTH);

        String[] columns = {
                "Ref #", "Turf Name", "Sport", "Location", "Date", "Time Slot", "Amount Paid", "Payment Mode", "Status"
        };

        tableModel = new DefaultTableModel(columns, 0) {
            @Override public boolean isCellEditable(int row, int col) { return false; }
        };

        bookingTable = new JTable(tableModel);
        StyleConstants.styleTable(bookingTable);

        // Column widths
        bookingTable.getColumnModel().getColumn(0).setPreferredWidth(70);   // Ref #
        bookingTable.getColumnModel().getColumn(1).setPreferredWidth(200);  // Turf Name
        bookingTable.getColumnModel().getColumn(2).setPreferredWidth(100);  // Sport
        bookingTable.getColumnModel().getColumn(3).setPreferredWidth(180);  // Location
        bookingTable.getColumnModel().getColumn(4).setPreferredWidth(100);  // Date
        bookingTable.getColumnModel().getColumn(5).setPreferredWidth(130);  // Time Slot
        bookingTable.getColumnModel().getColumn(6).setPreferredWidth(110);  // Amount
        bookingTable.getColumnModel().getColumn(7).setPreferredWidth(160);  // Payment
        bookingTable.getColumnModel().getColumn(8).setPreferredWidth(110);  // Status

        // Alignments and custom renderer
        bookingTable.getColumnModel().getColumn(0).setCellRenderer(new StyleConstants.StripedTableCellRenderer(SwingConstants.CENTER));
        bookingTable.getColumnModel().getColumn(1).setCellRenderer(new StyleConstants.StripedTableCellRenderer(SwingConstants.LEFT));
        bookingTable.getColumnModel().getColumn(2).setCellRenderer(new StyleConstants.StripedTableCellRenderer(SwingConstants.CENTER));
        bookingTable.getColumnModel().getColumn(3).setCellRenderer(new StyleConstants.StripedTableCellRenderer(SwingConstants.LEFT));
        bookingTable.getColumnModel().getColumn(4).setCellRenderer(new StyleConstants.StripedTableCellRenderer(SwingConstants.CENTER));
        bookingTable.getColumnModel().getColumn(5).setCellRenderer(new StyleConstants.StripedTableCellRenderer(SwingConstants.CENTER));
        bookingTable.getColumnModel().getColumn(6).setCellRenderer(new StyleConstants.StripedTableCellRenderer(SwingConstants.CENTER));
        bookingTable.getColumnModel().getColumn(7).setCellRenderer(new StyleConstants.StripedTableCellRenderer(SwingConstants.CENTER));
        bookingTable.getColumnModel().getColumn(8).setCellRenderer(new StyleConstants.StatusBadgeRenderer());

        // Selection listener to enable/disable cancellation based on eligibility
        bookingTable.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                int selectedRow = bookingTable.getSelectedRow();
                if (selectedRow >= 0 && selectedRow < bookingTable.getRowCount()) {
                    int modelRow = bookingTable.convertRowIndexToModel(selectedRow);
                    if (modelRow >= 0 && modelRow < currentBookings.size()) {
                        selectedBooking = currentBookings.get(modelRow);
                        updateCancelButtonState();
                        return;
                    }
                }
                selectedBooking = null;
                updateCancelButtonState();
            }
        });

        scrollPane = new JScrollPane(bookingTable);
        StyleConstants.styleScrollPane(scrollPane);
        panel.add(scrollPane, BorderLayout.CENTER);

        return panel;
    }

    private JPanel buildEmptyStatePanel() {
        Theme theme = ThemeManager.getTheme();

        JPanel empty = new JPanel(new GridBagLayout());
        empty.setBackground(theme.getBgCard());
        empty.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(theme.getBorder(), 1, true),
                new EmptyBorder(40, 40, 40, 40)));

        GridBagConstraints g = new GridBagConstraints();
        g.gridx = 0;
        g.gridy = 0;
        g.insets = new Insets(0, 0, 12, 0);

        JLabel icon = new JLabel("⚽", SwingConstants.CENTER);
        icon.setFont(StyleConstants.FONT_TITLE.deriveFont(48f));
        empty.add(icon, g);

        g.gridy++;
        g.insets = new Insets(0, 0, 8, 0);
        emptyTitle = new JLabel("No Bookings Yet", SwingConstants.CENTER);
        emptyTitle.setFont(StyleConstants.FONT_TITLE);
        emptyTitle.setForeground(theme.getAccentGreen());
        empty.add(emptyTitle, g);

        g.gridy++;
        g.insets = new Insets(0, 0, 24, 0);
        emptyDesc = new JLabel(
                "<html><center>You have not made any turf reservations yet.<br>"
                        + "Browse approved turfs on your dashboard and book your first slot!</center></html>",
                SwingConstants.CENTER);
        emptyDesc.setFont(StyleConstants.FONT_BODY);
        emptyDesc.setForeground(theme.getTextMuted());
        empty.add(emptyDesc, g);

        g.gridy++;
        g.insets = new Insets(0, 0, 0, 0);
        JButton exploreBtn = StyleConstants.primaryButton("Explore Turfs →");
        exploreBtn.setPreferredSize(new Dimension(200, 42));
        exploreBtn.addActionListener(e -> goBack());
        empty.add(exploreBtn, g);

        return empty;
    }

    private JPanel buildBottomBar() {
        Theme theme = ThemeManager.getTheme();

        JPanel bar = new JPanel(new BorderLayout(16, 0));
        bar.setBackground(theme.getBgCard());
        bar.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, theme.getBorder()),
                new EmptyBorder(12, 24, 12, 24)));

        statusLabel = new JLabel("Loading bookings from database...");
        statusLabel.setFont(StyleConstants.FONT_SMALL);
        statusLabel.setForeground(theme.getTextMuted());
        bar.add(statusLabel, BorderLayout.WEST);

        isolationNotice = new JLabel("Showing personal bookings for User #" + player.getUserId());
        isolationNotice.setFont(StyleConstants.FONT_SMALL);
        isolationNotice.setForeground(theme.getTextMuted());
        bar.add(isolationNotice, BorderLayout.EAST);

        return bar;
    }

    private void loadBookings() {
        refreshBtn.setEnabled(false);
        if (cancelBookingBtn != null) {
            cancelBookingBtn.setEnabled(false);
        }
        if (rateTurfBtn != null) {
            rateTurfBtn.setEnabled(false);
        }
        selectedBooking = null;
        statusLabel.setText("Querying MySQL database for player #" + player.getUserId() + "...");
        statusLabel.setForeground(ThemeManager.getTheme().getTextMuted());

        new SwingWorker<List<Booking>, Void>() {
            @Override
            protected List<Booking> doInBackground() {
                // Queries MySQL using only the logged-in player's userId
                return bookingService.getBookingHistory(player);
            }

            @Override
            protected void done() {
                refreshBtn.setEnabled(true);
                Theme t = ThemeManager.getTheme();
                try {
                    List<Booking> bookings = get();
                    currentBookings = bookings;
                    tableModel.setRowCount(0);

                    if (bookings.isEmpty()) {
                        centerCardLayout.show(centerCardContainer, "EMPTY");
                        statusLabel.setText("No bookings found in database for this player.");
                    } else {
                        centerCardLayout.show(centerCardContainer, "TABLE");
                        long confirmedCount = 0;

                        for (Booking b : bookings) {
                            if (b.getStatus() == BookingStatus.CONFIRMED) {
                                confirmedCount++;
                            }
                            tableModel.addRow(new Object[]{
                                    "#" + b.getBookingId(),
                                    b.getTurf().getName(),
                                    b.getTurf().getSportType(),
                                    b.getTurf().getLocation(),
                                    b.getSlot().getDate().toString(),
                                    b.getSlot().getStartTime() + " - " + b.getSlot().getEndTime(),
                                    String.format("₹%.2f", b.getTotalAmount()),
                                    b.getPaymentMethodDisplay(),
                                    b.getStatus().name()
                            });
                        }

                        statusLabel.setText(String.format("Total: %d booking%s (%d confirmed).",
                                bookings.size(), bookings.size() > 1 ? "s" : "", confirmedCount));
                    }
                    selectedBooking = null;
                    updateCancelButtonState();
                    statusLabel.setForeground(t.getTextMuted());
                } catch (Exception ex) {
                    statusLabel.setText("Error loading bookings: " + StyleConstants.formatErrorMessage(ex, "Bookings"));
                    statusLabel.setForeground(t.getStatusError());
                    selectedBooking = null;
                    updateCancelButtonState();
                }
            }
        }.execute();
    }

    private void updateCancelButtonState() {
        Theme t = ThemeManager.getTheme();
        if (cancelBookingBtn == null || selectedBookingLabel == null) return;

        if (selectedBooking == null) {
            cancelBookingBtn.setEnabled(false);
            cancelBookingBtn.setToolTipText("Select a confirmed booking from the table to cancel");
            if (rateTurfBtn != null) {
                rateTurfBtn.setEnabled(false);
                rateTurfBtn.setText("⭐ Rate Turf");
                rateTurfBtn.setToolTipText("Select a confirmed booking from the table to rate the turf");
            }
            selectedBookingLabel.setText("Select a booking to manage");
            selectedBookingLabel.setForeground(t.getTextMuted());
            return;
        }

        String turfInfo = selectedBooking.getTurf() != null ? selectedBooking.getTurf().getName() : "Turf";
        boolean isConfirmed = selectedBooking.getStatus() == BookingStatus.CONFIRMED;
        boolean isPast = selectedBooking.getSlot() != null && selectedBooking.getSlot().isInPast();

        // Rate Turf button state
        if (rateTurfBtn != null) {
            if (!isConfirmed) {
                rateTurfBtn.setEnabled(false);
                rateTurfBtn.setText("⭐ Rate Turf");
                rateTurfBtn.setToolTipText("Only confirmed bookings can be reviewed");
            } else {
                int turfId = selectedBooking.getTurf().getTurfId();
                boolean alreadyReviewed = turfService.hasReviewed(player, turfId);
                if (alreadyReviewed) {
                    rateTurfBtn.setEnabled(true);
                    rateTurfBtn.setText("✓ Reviewed");
                    rateTurfBtn.setToolTipText("Click to view your submitted review for " + turfInfo);
                } else {
                    rateTurfBtn.setEnabled(true);
                    rateTurfBtn.setText("⭐ Rate Turf");
                    rateTurfBtn.setToolTipText("Rate and review " + turfInfo);
                }
            }
        }

        if (isConfirmed && !isPast) {
            cancelBookingBtn.setEnabled(true);
            cancelBookingBtn.setToolTipText("Cancel booking #" + selectedBooking.getBookingId()
                    + " (" + selectedBooking.getSlot().getDate() + " " + selectedBooking.getSlot().getStartTime() + ") and refund payment");
            selectedBookingLabel.setText(String.format("Selected: #%d %s • ₹%.2f (Eligible for cancellation)",
                    selectedBooking.getBookingId(), turfInfo, selectedBooking.getTotalAmount()));
            selectedBookingLabel.setForeground(t.getTextPrimary());
        } else if (selectedBooking.getStatus() == BookingStatus.CANCELLED) {
            cancelBookingBtn.setEnabled(false);
            cancelBookingBtn.setToolTipText("Booking #" + selectedBooking.getBookingId() + " has already been cancelled");
            selectedBookingLabel.setText("Selected: #" + selectedBooking.getBookingId() + " " + turfInfo + " (Already Cancelled)");
            selectedBookingLabel.setForeground(t.getStatusError());
        } else if (isPast) {
            cancelBookingBtn.setEnabled(false);
            cancelBookingBtn.setToolTipText("Cannot cancel a slot that has already started or passed");
            selectedBookingLabel.setText("Selected: #" + selectedBooking.getBookingId() + " " + turfInfo + " (Past Slot - Completed)");
            selectedBookingLabel.setForeground(t.getTextMuted());
        } else {
            cancelBookingBtn.setEnabled(false);
            cancelBookingBtn.setToolTipText("Only confirmed upcoming bookings can be cancelled");
            selectedBookingLabel.setText("Selected: #" + selectedBooking.getBookingId() + " (" + selectedBooking.getStatus() + ")");
            selectedBookingLabel.setForeground(t.getTextMuted());
        }
    }

    private void openRateTurfDialog() {
        Turf turf = selectedBooking != null ? selectedBooking.getTurf() : null;
        new RateTurfDialog(this, player, turf, () -> {
            loadBookings();
            if (parentDashboard instanceof PlayerDashboardFrame) {
                ((PlayerDashboardFrame) parentDashboard).reloadTurfs();
            }
        }).setVisible(true);
    }

    private void cancelSelectedBooking() {
        if (selectedBooking == null) {
            JOptionPane.showMessageDialog(this,
                    "Please select a booking from the table first.",
                    "No Booking Selected",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }

        if (selectedBooking.getStatus() != BookingStatus.CONFIRMED) {
            JOptionPane.showMessageDialog(this,
                    "Only confirmed bookings can be cancelled.\nThis booking is currently: " + selectedBooking.getStatus(),
                    "Cannot Cancel Booking",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }

        if (selectedBooking.getSlot() != null && selectedBooking.getSlot().isInPast()) {
            JOptionPane.showMessageDialog(this,
                    "You cannot cancel a booking whose time slot has already started or passed.",
                    "Slot Already Started",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }

        final int bookingId = selectedBooking.getBookingId();
        final String turfName = selectedBooking.getTurf() != null ? selectedBooking.getTurf().getName() : "Turf";
        final String sport = selectedBooking.getTurf() != null ? selectedBooking.getTurf().getSportType() : "";
        final String dateStr = selectedBooking.getSlot() != null ? selectedBooking.getSlot().getDate().toString() : "";
        final String timeStr = selectedBooking.getSlot() != null ? (selectedBooking.getSlot().getStartTime() + " - " + selectedBooking.getSlot().getEndTime()) : "";
        final double refundAmount = selectedBooking.getTotalAmount();

        Theme t = ThemeManager.getTheme();
        String greenHex = String.format("#%02x%02x%02x", t.getAccentGreen().getRed(), t.getAccentGreen().getGreen(), t.getAccentGreen().getBlue());
        String errHex = String.format("#%02x%02x%02x", t.getStatusError().getRed(), t.getStatusError().getGreen(), t.getStatusError().getBlue());

        String message = String.format(
                "<html>"
                + "<h3 style='margin:0; color:%s;'>Confirm Booking Cancellation</h3>"
                + "<p style='margin-top:6px; font-size:12px;'>Are you sure you want to cancel this booking?</p>"
                + "<table cellpadding='3' style='font-size:12px;'>"
                + "<tr><td><b>Booking Ref:</b></td><td>#%d</td></tr>"
                + "<tr><td><b>Turf:</b></td><td>%s (%s)</td></tr>"
                + "<tr><td><b>Date:</b></td><td>%s</td></tr>"
                + "<tr><td><b>Time Slot:</b></td><td>%s</td></tr>"
                + "<tr><td><b>Refund Amount:</b></td><td style='color:%s;'><b>₹%.2f</b></td></tr>"
                + "</table>"
                + "<p style='color:%s; font-size:11px; margin-top:8px;'>"
                + "⚠️ The reserved time slot will immediately become available again for other players,<br>"
                + "and your simulated payment will be marked as REFUNDED in the database."
                + "</p>"
                + "</html>",
                errHex,
                bookingId,
                turfName, sport,
                dateStr,
                timeStr,
                greenHex, refundAmount,
                errHex
        );

        int choice = JOptionPane.showConfirmDialog(
                this,
                message,
                "Cancel Booking #" + bookingId,
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE
        );

        if (choice != JOptionPane.YES_OPTION) {
            return;
        }

        cancelBookingBtn.setEnabled(false);
        refreshBtn.setEnabled(false);
        statusLabel.setText("Cancelling booking #" + bookingId + " in MySQL database...");
        statusLabel.setForeground(t.getTextMuted());

        new SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() {
                bookingService.cancelBooking(player, bookingId);
                return null;
            }

            @Override
            protected void done() {
                refreshBtn.setEnabled(true);
                try {
                    get(); // Throws exception if cancelBooking failed
                    JOptionPane.showMessageDialog(
                            BookingHistoryFrame.this,
                            "<html>"
                            + "<h3 style='color:" + greenHex + "; margin:0;'>Booking #" + bookingId + " Cancelled</h3>"
                            + "<p style='margin-top:6px; font-size:12px;'>"
                            + "Your booking has been cancelled successfully.<br>"
                            + "The time slot is now open for new bookings, and the payment status has been set to <b>REFUNDED</b>."
                            + "</p>"
                            + "</html>",
                            "Cancellation Confirmed",
                            JOptionPane.INFORMATION_MESSAGE
                    );
                    loadBookings();
                } catch (Exception ex) {
                    statusLabel.setText("Cancellation failed: " + StyleConstants.formatErrorMessage(ex, "Cancel Booking"));
                    statusLabel.setForeground(ThemeManager.getTheme().getStatusError());
                    JOptionPane.showMessageDialog(
                            BookingHistoryFrame.this,
                            "Could not cancel booking: " + StyleConstants.formatErrorMessage(ex, "Cancel Booking"),
                            "Cancellation Failed",
                            JOptionPane.ERROR_MESSAGE
                    );
                    loadBookings();
                }
            }
        }.execute();
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

        if (emptyStatePanel != null) {
            emptyStatePanel.setBackground(newTheme.getBgCard());
            emptyStatePanel.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(newTheme.getBorder(), 1, true),
                    new EmptyBorder(40, 40, 40, 40)));
        }

        if (bottomBar != null) {
            bottomBar.setBackground(newTheme.getBgCard());
            bottomBar.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createMatteBorder(1, 0, 0, 0, newTheme.getBorder()),
                    new EmptyBorder(12, 24, 12, 24)));
        }

        if (scrollPane != null) {
            scrollPane.setBorder(BorderFactory.createLineBorder(newTheme.getBorder(), 1));
            scrollPane.getViewport().setBackground(newTheme.getBgCard());
            scrollPane.setBackground(newTheme.getBgMain());
        }

        title.setForeground(newTheme.getAccentGreen());
        userBadge.setForeground(newTheme.getTextPrimary());
        header.setForeground(newTheme.getTextPrimary());
        statusLabel.setForeground(newTheme.getTextMuted());
        isolationNotice.setForeground(newTheme.getTextMuted());

        if (selectedBookingLabel != null) {
            updateCancelButtonState();
        }

        if (rateTurfBtn != null) {
            rateTurfBtn.repaint();
        }

        if (emptyTitle != null) emptyTitle.setForeground(newTheme.getAccentGreen());
        if (emptyDesc != null) emptyDesc.setForeground(newTheme.getTextMuted());

        StyleConstants.updateTableTheme(bookingTable);

        repaint();
    }
}
