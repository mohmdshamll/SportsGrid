package ui;

import model.Player;
import model.Review;
import model.Turf;
import service.BookingService;
import service.TurfService;
import ui.theme.Theme;
import ui.theme.ThemeChangeListener;
import ui.theme.ThemeManager;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.List;
import java.util.Optional;

/**
 * Dialog allowing an authenticated Player to submit a 1-5 star rating
 * and an optional written review (up to 500 characters) for a sports turf they have booked.
 *
 * Implements ThemeChangeListener to dynamically respond to Light and Dark mode transitions.
 */
public class RateTurfDialog extends JDialog implements ThemeChangeListener {

    private final Player player;
    private final Turf preselectedTurf;
    private final Runnable onSuccess;
    private final TurfService turfService = new TurfService();
    private final BookingService bookingService = new BookingService();

    // State
    private int selectedRating = 0;
    private int hoveredRating = 0;
    private boolean isAlreadyReviewed = false;

    // UI Panels
    private JPanel root;
    private JPanel card;
    private JPanel headerPanel;
    private JPanel bottomBar;

    // Header labels
    private JLabel iconLabel;
    private JLabel titleLabel;
    private JLabel subtitleLabel;

    // Turf selection
    private JLabel turfPromptLabel;
    private JComboBox<TurfComboItem> turfCombo;
    private JLabel turfAvgRatingLabel;

    // Star rating
    private JLabel ratingPromptLabel;
    private JPanel starsContainer;
    private JButton[] starButtons = new JButton[5];
    private JLabel ratingScoreLabel;

    // Comment
    private JLabel commentPromptLabel;
    private JTextArea commentArea;
    private JScrollPane commentScrollPane;
    private JLabel charCountLabel;

    // Status & Actions
    private JLabel statusBanner;
    private JLabel errorLabel;
    private JButton submitBtn;
    private JButton cancelBtn;

    public RateTurfDialog(JFrame parent, Player player, Turf preselectedTurf, Runnable onSuccess) {
        super(parent, "SportGrid – Rate & Review Turf", true);
        this.player = player;
        this.preselectedTurf = preselectedTurf;
        this.onSuccess = onSuccess;
        initDialog(parent);
    }

    public RateTurfDialog(JDialog parent, Player player, Turf preselectedTurf, Runnable onSuccess) {
        super(parent, "SportGrid – Rate & Review Turf", true);
        this.player = player;
        this.preselectedTurf = preselectedTurf;
        this.onSuccess = onSuccess;
        initDialog(parent);
    }

    public RateTurfDialog(Window parent, Player player, Turf preselectedTurf, Runnable onSuccess) {
        super(parent, "SportGrid – Rate & Review Turf", ModalityType.APPLICATION_MODAL);
        this.player = player;
        this.preselectedTurf = preselectedTurf;
        this.onSuccess = onSuccess;
        initDialog(parent);
    }

    private void initDialog(Component parent) {
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setResizable(false);
        buildUI();

        pack();
        setSize(520, 620);
        setMinimumSize(new Dimension(500, 600));
        setLocationRelativeTo(parent);

        ThemeManager.addListener(this);
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosed(WindowEvent e) {
                ThemeManager.removeListener(RateTurfDialog.this);
            }
        });
    }

    private void buildUI() {
        Theme theme = ThemeManager.getTheme();

        root = new JPanel(new BorderLayout(0, 14));
        root.setBackground(theme.getBgMain());
        root.setBorder(new EmptyBorder(20, 24, 20, 24));
        setContentPane(root);

        // Header
        headerPanel = buildHeader(theme);
        root.add(headerPanel, BorderLayout.NORTH);

        // Center card with form fields
        card = buildFormCard(theme);
        root.add(card, BorderLayout.CENTER);

        // Bottom action buttons
        bottomBar = buildBottomBar(theme);
        root.add(bottomBar, BorderLayout.SOUTH);

        // Populate turfs and check review status
        populateTurfs();
    }

    private JPanel buildHeader(Theme theme) {
        JPanel header = new JPanel(new BorderLayout(12, 0));
        header.setOpaque(false);

        JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        left.setOpaque(false);

        iconLabel = new JLabel("⭐");
        iconLabel.setFont(StyleConstants.FONT_TITLE.deriveFont(26f));
        left.add(iconLabel);

        JPanel textPanel = new JPanel();
        textPanel.setLayout(new BoxLayout(textPanel, BoxLayout.Y_AXIS));
        textPanel.setOpaque(false);

        titleLabel = new JLabel("Rate & Review Turf");
        titleLabel.setFont(StyleConstants.FONT_TITLE);
        titleLabel.setForeground(theme.getTextPrimary());
        textPanel.add(titleLabel);

        subtitleLabel = new JLabel("Share your verified experience with other players");
        subtitleLabel.setFont(StyleConstants.FONT_SMALL);
        subtitleLabel.setForeground(theme.getTextMuted());
        textPanel.add(subtitleLabel);

        left.add(textPanel);
        header.add(left, BorderLayout.WEST);

        return header;
    }

    private JPanel buildFormCard(Theme theme) {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBackground(theme.getBgCard());
        panel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(theme.getBorder(), 1, true),
                new EmptyBorder(16, 20, 16, 20)));

        // 1. Turf Selection
        turfPromptLabel = new JLabel("Select Booked Turf:");
        turfPromptLabel.setFont(StyleConstants.FONT_BODY);
        turfPromptLabel.setForeground(theme.getTextPrimary());
        turfPromptLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        panel.add(turfPromptLabel);

        panel.add(Box.createVerticalStrut(6));

        turfCombo = StyleConstants.styledComboBox(new TurfComboItem[0]);
        turfCombo.setAlignmentX(Component.LEFT_ALIGNMENT);
        turfCombo.setMaximumSize(new Dimension(Integer.MAX_VALUE, 38));
        turfCombo.addActionListener(e -> onTurfSelected());
        panel.add(turfCombo);

        panel.add(Box.createVerticalStrut(4));

        turfAvgRatingLabel = new JLabel("Community Rating: Loading...");
        turfAvgRatingLabel.setFont(StyleConstants.FONT_SMALL);
        turfAvgRatingLabel.setForeground(theme.getTextMuted());
        turfAvgRatingLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        panel.add(turfAvgRatingLabel);

        panel.add(Box.createVerticalStrut(14));

        // 2. Star Rating Selector
        ratingPromptLabel = new JLabel("Your Rating (1 to 5 Stars): *");
        ratingPromptLabel.setFont(StyleConstants.FONT_BODY);
        ratingPromptLabel.setForeground(theme.getTextPrimary());
        ratingPromptLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        panel.add(ratingPromptLabel);

        panel.add(Box.createVerticalStrut(6));

        starsContainer = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        starsContainer.setOpaque(false);
        starsContainer.setAlignmentX(Component.LEFT_ALIGNMENT);

        for (int i = 0; i < 5; i++) {
            final int starIndex = i + 1;
            JButton btn = new JButton("★");
            btn.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 28));
            btn.setFocusPainted(false);
            btn.setBorderPainted(false);
            btn.setContentAreaFilled(false);
            btn.setOpaque(false);
            btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            btn.setMargin(new Insets(0, 0, 0, 0));
            btn.setForeground(theme.getBorder());

            btn.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseEntered(MouseEvent e) {
                    if (!isAlreadyReviewed) {
                        hoveredRating = starIndex;
                        updateStarDisplay();
                    }
                }

                @Override
                public void mouseExited(MouseEvent e) {
                    if (!isAlreadyReviewed) {
                        hoveredRating = 0;
                        updateStarDisplay();
                    }
                }
            });

            btn.addActionListener(e -> {
                if (!isAlreadyReviewed) {
                    selectedRating = starIndex;
                    hoveredRating = 0;
                    errorLabel.setText(" ");
                    updateStarDisplay();
                }
            });

            starButtons[i] = btn;
            starsContainer.add(btn);
        }

        panel.add(starsContainer);

        panel.add(Box.createVerticalStrut(4));

        ratingScoreLabel = new JLabel("Click a star to choose your rating");
        ratingScoreLabel.setFont(StyleConstants.FONT_SMALL);
        ratingScoreLabel.setForeground(theme.getTextMuted());
        ratingScoreLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        panel.add(ratingScoreLabel);

        panel.add(Box.createVerticalStrut(14));

        // 3. Written Comment Field
        commentPromptLabel = new JLabel("Written Review (Optional, max 500 chars):");
        commentPromptLabel.setFont(StyleConstants.FONT_BODY);
        commentPromptLabel.setForeground(theme.getTextPrimary());
        commentPromptLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        panel.add(commentPromptLabel);

        panel.add(Box.createVerticalStrut(6));

        commentArea = new JTextArea(4, 25);
        commentArea.setLineWrap(true);
        commentArea.setWrapStyleWord(true);
        commentArea.setFont(StyleConstants.FONT_BODY);
        commentArea.setBackground(theme.getBgField());
        commentArea.setForeground(theme.getTextPrimary());
        commentArea.setCaretColor(theme.getTextPrimary());
        commentArea.setBorder(new EmptyBorder(8, 8, 8, 8));

        commentScrollPane = new JScrollPane(commentArea);
        commentScrollPane.setAlignmentX(Component.LEFT_ALIGNMENT);
        commentScrollPane.setMaximumSize(new Dimension(Integer.MAX_VALUE, 110));
        StyleConstants.styleScrollPane(commentScrollPane);
        panel.add(commentScrollPane);

        panel.add(Box.createVerticalStrut(4));

        charCountLabel = new JLabel("0 / 500 characters");
        charCountLabel.setFont(StyleConstants.FONT_SMALL);
        charCountLabel.setForeground(theme.getTextMuted());
        charCountLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        panel.add(charCountLabel);

        commentArea.getDocument().addDocumentListener(new DocumentListener() {
            @Override public void insertUpdate(DocumentEvent e) { updateCharCount(); }
            @Override public void removeUpdate(DocumentEvent e) { updateCharCount(); }
            @Override public void changedUpdate(DocumentEvent e) { updateCharCount(); }
        });

        panel.add(Box.createVerticalStrut(10));

        // 4. Status Banner & Error
        statusBanner = new JLabel(" ");
        statusBanner.setFont(StyleConstants.FONT_SMALL);
        statusBanner.setForeground(theme.getAccentGreen());
        statusBanner.setAlignmentX(Component.LEFT_ALIGNMENT);
        panel.add(statusBanner);

        panel.add(Box.createVerticalStrut(2));

        errorLabel = new JLabel(" ");
        errorLabel.setFont(StyleConstants.FONT_SMALL);
        errorLabel.setForeground(new Color(0xEF, 0x44, 0x44));
        errorLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        panel.add(errorLabel);

        return panel;
    }

    private JPanel buildBottomBar(Theme theme) {
        JPanel bar = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        bar.setOpaque(false);

        cancelBtn = StyleConstants.secondaryButton("Cancel");
        cancelBtn.setPreferredSize(new Dimension(100, 38));
        cancelBtn.addActionListener(e -> dispose());
        bar.add(cancelBtn);

        submitBtn = StyleConstants.primaryButton("Submit Review");
        submitBtn.setPreferredSize(new Dimension(150, 38));
        submitBtn.addActionListener(e -> onSubmitClicked());
        bar.add(submitBtn);

        return bar;
    }

    private void updateCharCount() {
        int length = commentArea.getText().length();
        if (length > 500) {
            charCountLabel.setText(length + " / 500 characters (exceeds limit)");
            charCountLabel.setForeground(new Color(0xEF, 0x44, 0x44));
            submitBtn.setEnabled(false);
        } else {
            charCountLabel.setText(length + " / 500 characters");
            charCountLabel.setForeground(ThemeManager.getTheme().getTextMuted());
            if (!isAlreadyReviewed) {
                submitBtn.setEnabled(true);
            }
        }
    }

    private void updateStarDisplay() {
        Theme theme = ThemeManager.getTheme();
        Color activeStarColor = theme.isDark() ? new Color(0xFB, 0xBF, 0x24) : new Color(0xF5, 0x9E, 0x0B);
        Color inactiveColor = theme.isDark() ? new Color(0x37, 0x41, 0x51) : new Color(0xD1, 0xD5, 0xDB);

        int displayRating = hoveredRating > 0 ? hoveredRating : selectedRating;

        for (int i = 0; i < 5; i++) {
            if (i < displayRating) {
                starButtons[i].setForeground(activeStarColor);
            } else {
                starButtons[i].setForeground(inactiveColor);
            }
        }

        switch (displayRating) {
            case 1 -> ratingScoreLabel.setText("★☆☆☆☆  1 / 5 – Poor");
            case 2 -> ratingScoreLabel.setText("★★☆☆☆  2 / 5 – Fair");
            case 3 -> ratingScoreLabel.setText("★★★☆☆  3 / 5 – Good");
            case 4 -> ratingScoreLabel.setText("★★★★☆  4 / 5 – Very Good");
            case 5 -> ratingScoreLabel.setText("★★★★★  5 / 5 – Excellent!");
            default -> ratingScoreLabel.setText("Click a star to choose your rating");
        }

        if (displayRating > 0) {
            ratingScoreLabel.setForeground(activeStarColor);
        } else {
            ratingScoreLabel.setForeground(theme.getTextMuted());
        }
    }

    private void populateTurfs() {
        List<Turf> bookedTurfs = bookingService.getBookedTurfs(player);

        turfCombo.removeAllItems();

        if (bookedTurfs.isEmpty()) {
            if (preselectedTurf != null) {
                turfCombo.addItem(new TurfComboItem(preselectedTurf));
            } else {
                turfCombo.addItem(new TurfComboItem(null, "No booked turfs available"));
                submitBtn.setEnabled(false);
                errorLabel.setText("You can only review turfs after booking them.");
                return;
            }
        } else {
            TurfComboItem targetItem = null;
            for (Turf t : bookedTurfs) {
                TurfComboItem item = new TurfComboItem(t);
                turfCombo.addItem(item);
                if (preselectedTurf != null && t.getTurfId() == preselectedTurf.getTurfId()) {
                    targetItem = item;
                }
            }

            if (targetItem != null) {
                turfCombo.setSelectedItem(targetItem);
            } else if (preselectedTurf != null) {
                TurfComboItem specificItem = new TurfComboItem(preselectedTurf);
                turfCombo.addItem(specificItem);
                turfCombo.setSelectedItem(specificItem);
            }
        }

        onTurfSelected();
    }

    private void onTurfSelected() {
        TurfComboItem item = (TurfComboItem) turfCombo.getSelectedItem();
        if (item == null || item.turf == null) {
            turfAvgRatingLabel.setText("Community Rating: N/A");
            return;
        }

        Turf turf = item.turf;
        String avgText = turf.getAverageRating() > 0
                ? String.format("Community Rating: ★ %.1f / 5.0", turf.getAverageRating())
                : "Community Rating: ★ New (No reviews yet)";
        turfAvgRatingLabel.setText(avgText);

        // Check if player has already reviewed this turf
        Optional<Review> existing = turfService.getPlayerReview(player, turf.getTurfId());
        if (existing.isPresent()) {
            Review r = existing.get();
            isAlreadyReviewed = true;
            selectedRating = r.getRating();
            hoveredRating = 0;
            updateStarDisplay();

            commentArea.setText(r.getComment() != null ? r.getComment() : "");
            commentArea.setEditable(false);
            for (JButton b : starButtons) {
                b.setEnabled(false);
            }

            statusBanner.setText("✓ You reviewed this turf (" + r.getRating() + "/5 stars)");
            statusBanner.setForeground(ThemeManager.getTheme().getAccentGreen());
            submitBtn.setEnabled(false);
            submitBtn.setText("Already Reviewed");
            errorLabel.setText(" ");
        } else {
            isAlreadyReviewed = false;
            selectedRating = 0;
            hoveredRating = 0;
            updateStarDisplay();

            commentArea.setText("");
            commentArea.setEditable(true);
            for (JButton b : starButtons) {
                b.setEnabled(true);
            }

            statusBanner.setText(" ");
            submitBtn.setEnabled(true);
            submitBtn.setText("Submit Review");
            errorLabel.setText(" ");
        }
    }

    private void onSubmitClicked() {
        TurfComboItem item = (TurfComboItem) turfCombo.getSelectedItem();
        if (item == null || item.turf == null) {
            errorLabel.setText("Please select an eligible turf.");
            return;
        }

        if (selectedRating < 1 || selectedRating > 5) {
            errorLabel.setText("Please select a star rating between 1 and 5.");
            return;
        }

        String comment = commentArea.getText().trim();
        if (comment.length() > 500) {
            errorLabel.setText("Comment cannot exceed 500 characters.");
            return;
        }

        submitBtn.setEnabled(false);
        submitBtn.setText("Submitting...");
        errorLabel.setText(" ");

        final int turfId = item.turf.getTurfId();
        final String turfName = item.turf.getName();
        final int rating = selectedRating;

        new SwingWorker<Review, Void>() {
            @Override
            protected Review doInBackground() {
                return turfService.addReview(player, turfId, rating, comment);
            }

            @Override
            protected void done() {
                try {
                    Review review = get();
                    JOptionPane.showMessageDialog(
                            RateTurfDialog.this,
                            String.format("Thank you! Your review for \"%s\" has been submitted.\n\nRating: %d / 5 Stars",
                                    turfName, rating),
                            "Review Submitted",
                            JOptionPane.INFORMATION_MESSAGE);

                    if (onSuccess != null) {
                        onSuccess.run();
                    }
                    dispose();
                } catch (Exception e) {
                    submitBtn.setEnabled(true);
                    submitBtn.setText("Submit Review");
                    Throwable cause = e.getCause() != null ? e.getCause() : e;
                    String message = cause.getMessage();
                    if (message == null || message.isBlank()) {
                        message = "Could not submit review. Please try again.";
                    }
                    errorLabel.setText(message);
                    JOptionPane.showMessageDialog(
                            RateTurfDialog.this,
                            message,
                            "Review Submission Failed",
                            JOptionPane.ERROR_MESSAGE);
                }
            }
        }.execute();
    }

    @Override
    public void onThemeChanged(Theme newTheme) {
        root.setBackground(newTheme.getBgMain());
        card.setBackground(newTheme.getBgCard());
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(newTheme.getBorder(), 1, true),
                new EmptyBorder(16, 20, 16, 20)));

        titleLabel.setForeground(newTheme.getTextPrimary());
        subtitleLabel.setForeground(newTheme.getTextMuted());
        turfPromptLabel.setForeground(newTheme.getTextPrimary());
        turfAvgRatingLabel.setForeground(newTheme.getTextMuted());
        ratingPromptLabel.setForeground(newTheme.getTextPrimary());
        commentPromptLabel.setForeground(newTheme.getTextPrimary());

        commentArea.setBackground(newTheme.getBgField());
        commentArea.setForeground(newTheme.getTextPrimary());
        commentArea.setCaretColor(newTheme.getTextPrimary());
        StyleConstants.styleScrollPane(commentScrollPane);

        charCountLabel.setForeground(newTheme.getTextMuted());
        statusBanner.setForeground(newTheme.getAccentGreen());

        cancelBtn.repaint();
        submitBtn.repaint();

        updateStarDisplay();
        repaint();
    }

    @Override
    public void dispose() {
        ThemeManager.removeListener(this);
        super.dispose();
    }

    private static class TurfComboItem {
        final Turf turf;
        final String label;

        TurfComboItem(Turf turf) {
            this.turf = turf;
            this.label = turf != null
                    ? String.format("%s (%s, %s)", turf.getName(), turf.getSportType(), turf.getLocation())
                    : "Unknown";
        }

        TurfComboItem(Turf turf, String customLabel) {
            this.turf = turf;
            this.label = customLabel;
        }

        @Override
        public String toString() {
            return label;
        }
    }
}
