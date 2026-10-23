package ui;

import model.Booking;
import model.CardPayment;
import model.Payment;
import model.Player;
import model.TimeSlot;
import model.Turf;
import model.UPIPayment;
import service.BookingService;
import service.PaymentFailedException;
import ui.theme.Theme;
import ui.theme.ThemeChangeListener;
import ui.theme.ThemeManager;

import javax.swing.*;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

/**
 * Payment Simulation Dialog.
 *
 * Provides simulated payment options (UPI and Credit/Debit Card)
 * to book an available turf time slot.
 *
 * Visual improvements:
 *  - Custom vector emerald radio button controls eliminating native Aqua widgets.
 *  - Compact, proportionate form layout with balanced spacing and no wasted space.
 *  - Clear hierarchy in payment summary between turf name, booking metadata, and total amount.
 *  - High-contrast inputs with emerald focus indicators.
 *  - Seamless, instant runtime switching between Light and Dark modes.
 *
 * NOTE: Payments are 100% simulated. No real money moves.
 * Validates UPI format and Card number (Luhn checksum + expiry) before
 * creating the booking atomically in MySQL.
 */
public class PaymentDialog extends JDialog implements ThemeChangeListener {

    @FunctionalInterface
    public interface BookingSuccessCallback {
        void onBookingSuccess(Booking booking);
    }

    private final Player player;
    private final Turf turf;
    private final TimeSlot slot;
    private final double totalAmount;
    private final BookingSuccessCallback callback;
    private final BookingService bookingService = new BookingService();

    // Container panels
    private JPanel root;
    private JPanel summaryCard;
    private JPanel selectorPanel;
    private JPanel upiCard;
    private JPanel cardCard;
    private JPanel upiPanel;
    private JPanel cardPanel;
    private CardLayout methodCardLayout;
    private JPanel methodSwitchPanel;

    // UI elements
    private JLabel turfTitle;
    private JLabel amountLabel;
    private JLabel sportLoc;
    private JLabel timeLabel;
    private JLabel rateNotice;
    private JLabel simNote;
    private JRadioButton upiRadio;
    private JRadioButton cardRadio;

    // UPI Fields
    private JLabel upiLabel;
    private JTextField upiField;
    private JLabel upiHint;
    private JLabel quickLabel;

    // Card Fields
    private JLabel cardHolderLabel;
    private JTextField cardHolderField;
    private JLabel cardNumberLabel;
    private JTextField cardNumberField;
    private JLabel expiryLabel;
    private JTextField expiryField;
    private JLabel cvvLabel;
    private JTextField cvvField;
    private JLabel cardHint;

    private JLabel errorLabel;
    private JButton payBtn;
    private JButton cancelBtn;

    public PaymentDialog(JFrame parent, Player player, Turf turf, TimeSlot slot, BookingSuccessCallback callback) {
        super(parent, "SportGrid – Payment Simulation", true);
        this.player = player;
        this.turf = turf;
        this.slot = slot;
        this.totalAmount = Math.round(turf.getPricePerHour() * slot.getDurationHours() * 100.0) / 100.0;
        this.callback = callback;

        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setResizable(false);
        buildUI();

        pack();
        setSize(520, 580);
        setMinimumSize(new Dimension(480, 540));
        setLocationRelativeTo(parent);

        ThemeManager.addListener(this);
    }

    private void buildUI() {
        Theme theme = ThemeManager.getTheme();

        root = new JPanel(new BorderLayout(0, 12));
        root.setBackground(theme.getBgMain());
        root.setBorder(new EmptyBorder(16, 20, 16, 20));
        setContentPane(root);

        // 1. Top Summary Card
        summaryCard = buildSummaryCard();
        root.add(summaryCard, BorderLayout.NORTH);

        // 2. Center: Payment Method Selector + Forms
        JPanel center = new JPanel(new BorderLayout(0, 10));
        center.setOpaque(false);

        selectorPanel = buildMethodSelector();
        center.add(selectorPanel, BorderLayout.NORTH);
        center.add(buildMethodForms(), BorderLayout.CENTER);

        root.add(center, BorderLayout.CENTER);

        // 3. Bottom: Error label & Action Buttons
        root.add(buildBottomPanel(), BorderLayout.SOUTH);
    }

    private JPanel buildSummaryCard() {
        Theme theme = ThemeManager.getTheme();

        JPanel card = new JPanel(new BorderLayout(0, 8));
        card.setBackground(theme.getBgCard());
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(theme.getBorder(), 1, true),
                new EmptyBorder(12, 16, 12, 16)));

        // Header: Turf Name (Left) & Total Amount (Right)
        JPanel topRow = new JPanel(new BorderLayout(10, 0));
        topRow.setOpaque(false);

        turfTitle = new JLabel(turf.getName());
        turfTitle.setFont(StyleConstants.FONT_TITLE);
        turfTitle.setForeground(theme.getTextPrimary());
        topRow.add(turfTitle, BorderLayout.CENTER);

        JPanel amountCol = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        amountCol.setOpaque(false);
        amountLabel = new JLabel(String.format("₹%.2f", totalAmount));
        amountLabel.setFont(StyleConstants.FONT_TITLE);
        amountLabel.setForeground(theme.getAccentGreen());
        amountCol.add(amountLabel);
        topRow.add(amountCol, BorderLayout.EAST);
        card.add(topRow, BorderLayout.NORTH);

        // Details grid: compact 3-row layout
        JPanel details = new JPanel(new GridLayout(3, 1, 0, 3));
        details.setOpaque(false);

        sportLoc = new JLabel("⚽ " + turf.getSportType() + "   •   📍 " + turf.getLocation());
        sportLoc.setFont(StyleConstants.FONT_BODY);
        sportLoc.setForeground(theme.getTextPrimary());

        timeLabel = new JLabel(String.format("📅 %s   •   ⏰ %s - %s (%.1f hr)",
                slot.getDate(), slot.getStartTime(), slot.getEndTime(), slot.getDurationHours()));
        timeLabel.setFont(StyleConstants.FONT_BODY);
        timeLabel.setForeground(theme.getTextPrimary());

        rateNotice = new JLabel(String.format("Hourly Rate: ₹%.2f/hr   •   Player: %s",
                turf.getPricePerHour(), player.getName()));
        rateNotice.setFont(StyleConstants.FONT_SMALL);
        rateNotice.setForeground(theme.getTextMuted());

        details.add(sportLoc);
        details.add(timeLabel);
        details.add(rateNotice);
        card.add(details, BorderLayout.CENTER);

        // Simulation banner
        simNote = new JLabel("⚡ Simulated Payment: Test Mode • Instant confirmation • No real money charged", SwingConstants.CENTER);
        simNote.setFont(StyleConstants.FONT_SMALL);
        simNote.setForeground(theme.getTextMuted());
        simNote.setBorder(new EmptyBorder(6, 0, 0, 0));
        card.add(simNote, BorderLayout.SOUTH);

        return card;
    }

    private JPanel buildMethodSelector() {
        Theme theme = ThemeManager.getTheme();

        JPanel selector = new JPanel(new GridLayout(1, 2, 10, 0));
        selector.setOpaque(false);

        // Custom Radio Buttons
        upiRadio = new JRadioButton("UPI Payment", true);
        configureRadioButton(upiRadio);

        cardRadio = new JRadioButton("Credit / Debit Card", false);
        configureRadioButton(cardRadio);

        ButtonGroup group = new ButtonGroup();
        group.add(upiRadio);
        group.add(cardRadio);

        // UPI Card Container
        upiCard = new JPanel(new BorderLayout(8, 0));
        upiCard.setOpaque(true);
        upiCard.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        upiCard.add(upiRadio, BorderLayout.CENTER);
        upiCard.addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) {
                selectMethod("UPI");
            }
        });

        // Card Card Container
        cardCard = new JPanel(new BorderLayout(8, 0));
        cardCard.setOpaque(true);
        cardCard.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        cardCard.add(cardRadio, BorderLayout.CENTER);
        cardCard.addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) {
                selectMethod("CARD");
            }
        });

        selector.add(upiCard);
        selector.add(cardCard);

        upiRadio.addActionListener(e -> selectMethod("UPI"));
        cardRadio.addActionListener(e -> selectMethod("CARD"));

        updateSelectorStyling(theme, true);

        return selector;
    }

    private void configureRadioButton(JRadioButton radio) {
        Theme theme = ThemeManager.getTheme();
        radio.setIcon(new ThemeRadioIcon(18));
        radio.setSelectedIcon(new ThemeRadioIcon(18));
        radio.setPressedIcon(new ThemeRadioIcon(18));
        radio.setRolloverIcon(new ThemeRadioIcon(18));
        radio.setRolloverSelectedIcon(new ThemeRadioIcon(18));
        radio.setFocusPainted(false);
        radio.setOpaque(false);
        radio.setFont(StyleConstants.FONT_HEADING);
        radio.setForeground(theme.getTextPrimary());
        radio.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        radio.setIconTextGap(10);
    }

    private void selectMethod(String method) {
        Theme t = ThemeManager.getTheme();
        boolean isUpi = "UPI".equals(method);
        upiRadio.setSelected(isUpi);
        cardRadio.setSelected(!isUpi);

        updateSelectorStyling(t, isUpi);

        methodCardLayout.show(methodSwitchPanel, method);
        errorLabel.setText(" ");
        repaint();
    }

    private void updateSelectorStyling(Theme t, boolean isUpi) {
        if (upiCard != null) {
            upiCard.setBackground(isUpi ? t.getBgCardAlt() : t.getBgCard());
            upiCard.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(isUpi ? t.getAccentGreen() : t.getBorder(), isUpi ? 2 : 1, true),
                    new EmptyBorder(8, 12, 8, 12)));
        }
        if (cardCard != null) {
            cardCard.setBackground(!isUpi ? t.getBgCardAlt() : t.getBgCard());
            cardCard.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(!isUpi ? t.getAccentGreen() : t.getBorder(), !isUpi ? 2 : 1, true),
                    new EmptyBorder(8, 12, 8, 12)));
        }
    }

    private JPanel buildMethodForms() {
        methodCardLayout = new CardLayout();
        methodSwitchPanel = new JPanel(methodCardLayout);
        methodSwitchPanel.setOpaque(false);

        methodSwitchPanel.add(buildUpiPanel(), "UPI");
        methodSwitchPanel.add(buildCardPanel(), "CARD");

        return methodSwitchPanel;
    }

    private JPanel buildUpiPanel() {
        Theme theme = ThemeManager.getTheme();

        upiPanel = new JPanel(new GridBagLayout());
        upiPanel.setBackground(theme.getBgCard());
        upiPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(theme.getBorder(), 1, true),
                new EmptyBorder(14, 18, 14, 18)));

        GridBagConstraints c = new GridBagConstraints();
        c.fill = GridBagConstraints.HORIZONTAL;
        c.weightx = 1;
        c.gridx = 0;

        c.gridy = 0; c.insets = new Insets(0, 0, 4, 0);
        upiLabel = StyleConstants.label("UPI ID (Virtual Payment Address):");
        upiPanel.add(upiLabel, c);

        c.gridy = 1; c.insets = new Insets(0, 0, 6, 0);
        upiField = StyleConstants.styledField();
        upiField.setText(player.getName().toLowerCase().replaceAll("\\s+", "") + "@okhdfc");
        upiPanel.add(upiField, c);

        c.gridy = 2; c.insets = new Insets(0, 0, 10, 0);
        upiHint = new JLabel("<html>Format: <i>username@bank</i> (e.g. <b>player@okhdfc</b>, <b>test@okicici</b>, <b>user@paytm</b>)</html>");
        upiHint.setFont(StyleConstants.FONT_SMALL);
        upiHint.setForeground(theme.getTextMuted());
        upiPanel.add(upiHint, c);

        // Quick sample chips
        JPanel chips = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        chips.setOpaque(false);
        quickLabel = new JLabel("Quick fill:");
        quickLabel.setFont(StyleConstants.FONT_SMALL);
        quickLabel.setForeground(theme.getTextMuted());
        chips.add(quickLabel);

        chips.add(createChipButton("user@okhdfc", upiField));
        chips.add(createChipButton("player@upi", upiField));
        chips.add(createChipButton("demo@paytm", upiField));

        c.gridy = 3; c.insets = new Insets(0, 0, 0, 0);
        upiPanel.add(chips, c);

        return upiPanel;
    }

    private JPanel buildCardPanel() {
        Theme theme = ThemeManager.getTheme();

        cardPanel = new JPanel(new GridBagLayout());
        cardPanel.setBackground(theme.getBgCard());
        cardPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(theme.getBorder(), 1, true),
                new EmptyBorder(12, 18, 12, 18)));

        GridBagConstraints c = new GridBagConstraints();
        c.fill = GridBagConstraints.HORIZONTAL;
        c.weightx = 1;
        c.gridx = 0;

        int row = 0;

        // Cardholder Name
        c.gridy = row++; c.insets = new Insets(0, 0, 2, 0);
        cardHolderLabel = StyleConstants.label("Cardholder Name:");
        cardPanel.add(cardHolderLabel, c);
        cardHolderField = StyleConstants.styledField();
        cardHolderField.setText(player.getName());
        c.gridy = row++; c.insets = new Insets(0, 0, 6, 0);
        cardPanel.add(cardHolderField, c);

        // Card Number
        c.gridy = row++; c.insets = new Insets(0, 0, 2, 0);
        cardNumberLabel = StyleConstants.label("Card Number (passes Luhn algorithm):");
        cardPanel.add(cardNumberLabel, c);
        cardNumberField = StyleConstants.styledField();
        cardNumberField.setText("4111 1111 1111 1111");
        c.gridy = row++; c.insets = new Insets(0, 0, 6, 0);
        cardPanel.add(cardNumberField, c);

        // Expiry & CVV in two columns
        JPanel expCvvRow = new JPanel(new GridLayout(1, 2, 10, 0));
        expCvvRow.setOpaque(false);

        JPanel expCol = new JPanel(new BorderLayout(0, 2));
        expCol.setOpaque(false);
        expiryLabel = StyleConstants.label("Expiry (MM/yy):");
        expCol.add(expiryLabel, BorderLayout.NORTH);
        expiryField = StyleConstants.styledField();
        expiryField.setText("12/30");
        expCol.add(expiryField, BorderLayout.CENTER);

        JPanel cvvCol = new JPanel(new BorderLayout(0, 2));
        cvvCol.setOpaque(false);
        cvvLabel = StyleConstants.label("CVV (3 digits):");
        cvvCol.add(cvvLabel, BorderLayout.NORTH);
        cvvField = StyleConstants.styledField();
        cvvField.setText("123");
        cvvCol.add(cvvField, BorderLayout.CENTER);

        expCvvRow.add(expCol);
        expCvvRow.add(cvvCol);

        c.gridy = row++; c.insets = new Insets(0, 0, 6, 0);
        cardPanel.add(expCvvRow, c);

        // Card Hint & Quick Fill
        JPanel cardFooter = new JPanel(new BorderLayout(8, 0));
        cardFooter.setOpaque(false);

        cardHint = new JLabel("<html>Test Card: <b>4111 1111 1111 1111</b> &bull; Expiry: <b>12/30</b> &bull; CVV: <b>123</b></html>");
        cardHint.setFont(StyleConstants.FONT_SMALL);
        cardHint.setForeground(theme.getTextMuted());
        cardFooter.add(cardHint, BorderLayout.CENTER);

        JButton fillCardBtn = createChipButton("Fill Test Card", null);
        fillCardBtn.addActionListener(e -> {
            cardHolderField.setText(player.getName());
            cardNumberField.setText("4111 1111 1111 1111");
            expiryField.setText("12/30");
            cvvField.setText("123");
            errorLabel.setText(" ");
        });
        cardFooter.add(fillCardBtn, BorderLayout.EAST);

        c.gridy = row++; c.insets = new Insets(0, 0, 0, 0);
        cardPanel.add(cardFooter, c);

        return cardPanel;
    }

    private JButton createChipButton(String text, JTextField targetField) {
        JButton chip = new JButton(text) {
            @Override
            protected void paintComponent(Graphics g) {
                Theme t = ThemeManager.getTheme();
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

                int w = getWidth();
                int h = getHeight();

                g2.setColor(getModel().isRollover() ? t.getBgCardAlt() : t.getBgField());
                g2.fillRoundRect(0, 0, w, h, 8, 8);
                g2.setColor(getModel().isRollover() ? t.getAccentGreen() : t.getBorder());
                g2.drawRoundRect(0, 0, w - 1, h - 1, 8, 8);
                g2.dispose();

                setForeground(getModel().isRollover() ? t.getAccentGreen() : t.getTextPrimary());
                super.paintComponent(g);
            }

            @Override
            public Dimension getPreferredSize() {
                FontMetrics fm = getFontMetrics(getFont());
                int textW = (getText() != null) ? fm.stringWidth(getText()) : 0;
                return new Dimension(textW + 18, 26);
            }
        };
        chip.setFont(StyleConstants.FONT_SMALL);
        chip.setOpaque(false);
        chip.setContentAreaFilled(false);
        chip.setBorderPainted(false);
        chip.setFocusPainted(false);
        chip.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        if (targetField != null) {
            chip.addActionListener(e -> {
                targetField.setText(text);
                errorLabel.setText(" ");
            });
        }
        return chip;
    }

    private JPanel buildBottomPanel() {
        Theme theme = ThemeManager.getTheme();

        JPanel bottom = new JPanel(new BorderLayout(0, 8));
        bottom.setOpaque(false);

        errorLabel = new JLabel(" ");
        errorLabel.setFont(StyleConstants.FONT_SMALL);
        errorLabel.setForeground(theme.getStatusError());
        errorLabel.setHorizontalAlignment(SwingConstants.CENTER);
        errorLabel.setPreferredSize(new Dimension(0, 18));
        bottom.add(errorLabel, BorderLayout.NORTH);

        JPanel btnRow = new JPanel(new GridLayout(1, 2, 12, 0));
        btnRow.setOpaque(false);

        cancelBtn = StyleConstants.secondaryButton("Cancel");
        cancelBtn.setPreferredSize(new Dimension(130, 40));
        cancelBtn.addActionListener(e -> {
            ThemeManager.removeListener(this);
            dispose();
        });

        payBtn = StyleConstants.primaryButton(String.format("Pay ₹%.2f →", totalAmount));
        payBtn.setPreferredSize(new Dimension(200, 40));
        payBtn.addActionListener(e -> attemptPayment());

        btnRow.add(cancelBtn);
        btnRow.add(payBtn);
        bottom.add(btnRow, BorderLayout.SOUTH);

        return bottom;
    }

    private void attemptPayment() {
        Payment payment;

        if (upiRadio.isSelected()) {
            String upi = upiField.getText().trim();
            if (upi.isEmpty()) {
                showError("Please enter your UPI ID.");
                return;
            }
            if (!upi.contains("@") || upi.length() < 5) {
                showError("Invalid UPI ID format. Expected format: username@bank (e.g. user@okhdfc)");
                return;
            }
            payment = new UPIPayment(upi);
        } else {
            String holder = cardHolderField.getText().trim();
            String number = cardNumberField.getText().trim().replaceAll("[\\s-]", "");
            String expiry = expiryField.getText().trim();
            String cvv    = cvvField.getText().trim();

            if (holder.isEmpty()) {
                showError("Cardholder name is required.");
                return;
            }
            if (!number.matches("\\d{13,19}")) {
                showError("Card number must be 13 to 19 digits.");
                return;
            }
            if (!expiry.matches("^(0[1-9]|1[0-2])/\\d{2}$")) {
                showError("Expiry must be in MM/yy format (e.g. 12/30).");
                return;
            }
            if (!cvv.matches("\\d{3,4}")) {
                showError("CVV must be 3 or 4 digits.");
                return;
            }
            payment = new CardPayment(holder, number, expiry);
        }

        setLoading(true);
        errorLabel.setText(" ");

        new SwingWorker<Booking, Void>() {
            @Override
            protected Booking doInBackground() {
                // Calls BookingService.bookSlot inside a single database transaction
                return bookingService.bookSlot(player, slot.getSlotId(), payment);
            }

            @Override
            protected void done() {
                setLoading(false);
                try {
                    Booking confirmedBooking = get();
                    ThemeManager.removeListener(PaymentDialog.this);
                    dispose();
                    if (callback != null) {
                        callback.onBookingSuccess(confirmedBooking);
                    }
                } catch (java.util.concurrent.ExecutionException ex) {
                    Throwable cause = ex.getCause();
                    if (cause instanceof PaymentFailedException) {
                        showError("Payment rejected: Please check your UPI ID or Card details.");
                    } else if (cause instanceof IllegalStateException) {
                        showError("Booking Refused: " + cause.getMessage());
                    } else {
                        showError(StyleConstants.formatErrorMessage(cause, "Payment"));
                    }
                } catch (Exception ex) {
                    showError("Booking failed: " + ex.getMessage());
                }
            }
        }.execute();
    }

    private void showError(String msg) {
        errorLabel.setText("<html><center>" + msg + "</center></html>");
    }

    private void setLoading(boolean loading) {
        payBtn.setEnabled(!loading);
        cancelBtn.setEnabled(!loading);
        payBtn.setText(loading ? "Processing Payment…" : String.format("Pay ₹%.2f →", totalAmount));
    }

    @Override
    public void onThemeChanged(Theme newTheme) {
        if (root == null) return;
        root.setBackground(newTheme.getBgMain());

        if (summaryCard != null) {
            summaryCard.setBackground(newTheme.getBgCard());
            summaryCard.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(newTheme.getBorder(), 1, true),
                    new EmptyBorder(12, 16, 12, 16)));
        }

        updateSelectorStyling(newTheme, upiRadio.isSelected());

        if (upiPanel != null) {
            upiPanel.setBackground(newTheme.getBgCard());
            upiPanel.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(newTheme.getBorder(), 1, true),
                    new EmptyBorder(14, 18, 14, 18)));
        }

        if (cardPanel != null) {
            cardPanel.setBackground(newTheme.getBgCard());
            cardPanel.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(newTheme.getBorder(), 1, true),
                    new EmptyBorder(12, 18, 12, 18)));
        }

        turfTitle.setForeground(newTheme.getTextPrimary());
        amountLabel.setForeground(newTheme.getAccentGreen());
        sportLoc.setForeground(newTheme.getTextPrimary());
        timeLabel.setForeground(newTheme.getTextPrimary());
        rateNotice.setForeground(newTheme.getTextMuted());
        simNote.setForeground(newTheme.getTextMuted());

        upiRadio.setForeground(newTheme.getTextPrimary());
        cardRadio.setForeground(newTheme.getTextPrimary());

        if (upiLabel != null) upiLabel.setForeground(newTheme.getTextMuted());
        if (upiHint != null) upiHint.setForeground(newTheme.getTextMuted());
        if (quickLabel != null) quickLabel.setForeground(newTheme.getTextMuted());

        if (cardHolderLabel != null) cardHolderLabel.setForeground(newTheme.getTextMuted());
        if (cardNumberLabel != null) cardNumberLabel.setForeground(newTheme.getTextMuted());
        if (expiryLabel != null) expiryLabel.setForeground(newTheme.getTextMuted());
        if (cvvLabel != null) cvvLabel.setForeground(newTheme.getTextMuted());
        if (cardHint != null) cardHint.setForeground(newTheme.getTextMuted());

        updateFieldTheme(upiField, newTheme);
        updateFieldTheme(cardHolderField, newTheme);
        updateFieldTheme(cardNumberField, newTheme);
        updateFieldTheme(expiryField, newTheme);
        updateFieldTheme(cvvField, newTheme);

        errorLabel.setForeground(newTheme.getStatusError());

        repaint();
    }

    private void updateFieldTheme(JTextField field, Theme t) {
        if (field == null) return;
        field.setBackground(t.getBgField());
        field.setForeground(t.getTextPrimary());
        field.setCaretColor(t.getAccentGreen());
        field.setBorder(new CompoundBorder(
                new LineBorder(t.getBorder(), 1, true),
                new EmptyBorder(6, 12, 6, 12)));
    }

    /**
     * Custom theme-aware vector icon for radio buttons.
     * Replaces native macOS Aqua radio circle with a crisp vector emerald indicator.
     */
    public static class ThemeRadioIcon implements Icon {
        private final int size;

        public ThemeRadioIcon() {
            this(18);
        }

        public ThemeRadioIcon(int size) {
            this.size = size;
        }

        @Override
        public void paintIcon(Component c, Graphics g, int x, int y) {
            AbstractButton b = (AbstractButton) c;
            ButtonModel m = b.getModel();
            Theme t = ThemeManager.getTheme();

            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            boolean selected = m.isSelected();
            boolean hover = m.isRollover();

            int drawY = y + (c.getHeight() - size) / 2;

            // Background circle
            g2.setColor(t.getBgField());
            g2.fillOval(x, drawY, size - 1, size - 1);

            // Outer border
            if (selected) {
                g2.setColor(t.getAccentGreen());
                g2.setStroke(new BasicStroke(2.0f));
            } else if (hover) {
                g2.setColor(t.getBorderFocus());
                g2.setStroke(new BasicStroke(1.5f));
            } else {
                g2.setColor(t.getBorder());
                g2.setStroke(new BasicStroke(1.5f));
            }
            g2.drawOval(x + 1, drawY + 1, size - 3, size - 3);

            // Center indicator dot when selected
            if (selected) {
                g2.setColor(t.getAccentGreen());
                int dotSize = 8;
                int offset = (size - dotSize) / 2;
                g2.fillOval(x + offset, drawY + offset, dotSize, dotSize);
            }

            g2.dispose();
        }

        @Override
        public int getIconWidth() {
            return size;
        }

        @Override
        public int getIconHeight() {
            return size;
        }
    }
}
