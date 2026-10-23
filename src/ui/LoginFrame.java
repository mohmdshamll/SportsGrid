package ui;

import model.Role;
import model.User;
import service.AuthService;
import ui.theme.Theme;
import ui.theme.ThemeChangeListener;
import ui.theme.ThemeManager;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

/**
 * Authentication Entry Point for SportGrid.
 *
 * Supports Light & Dark visual themes, visible theme toggle, accessible focus indicators,
 * and responsive non-blocking background authentication via SwingWorker.
 */
public class LoginFrame extends JFrame implements ThemeChangeListener {

    private final AuthService authService = new AuthService();

    private JPanel root;
    private JPanel card;
    private JLabel logo;
    private JLabel tagline;
    private JLabel heading;
    private JLabel emailLabel;
    private JLabel passwordLabel;
    private JTextField emailField;
    private JPasswordField passwordField;
    private JLabel errorLabel;
    private JButton loginBtn;
    private JLabel noAccount;
    private JLabel registerLink;

    public LoginFrame() {
        this(null);
    }

    public LoginFrame(String prefilledEmail) {
        setTitle("SportGrid – Login");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setResizable(false);

        buildUI();

        if (prefilledEmail != null && !prefilledEmail.isBlank()) {
            emailField.setText(prefilledEmail.trim());
            passwordField.requestFocusInWindow();
        }

        pack();
        setMinimumSize(new Dimension(480, 560));
        setPreferredSize(new Dimension(480, 580));
        pack();
        setLocationRelativeTo(null);

        // Register for real-time theme changes
        ThemeManager.addListener(this);
    }

    private void buildUI() {
        Theme theme = ThemeManager.getTheme();

        root = new JPanel(new BorderLayout(0, 10));
        root.setBackground(theme.getBgMain());
        root.setBorder(new EmptyBorder(16, 24, 24, 24));
        setContentPane(root);

        // ── Top Bar with Theme Toggle ─────────────────────────────────────────
        JPanel topBar = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        topBar.setOpaque(false);
        topBar.add(StyleConstants.themeToggleButton());
        root.add(topBar, BorderLayout.NORTH);

        // ── Center Form Wrapper ───────────────────────────────────────────────
        JPanel center = new JPanel(new GridBagLayout());
        center.setOpaque(false);

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1;
        gbc.gridx = 0;

        // Logo & Title
        JPanel logoPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 8, 0));
        logoPanel.setOpaque(false);
        JLabel logoIcon = new JLabel("⚽");
        logoIcon.setFont(StyleConstants.FONT_TITLE);
        logoPanel.add(logoIcon);

        logo = new JLabel("SportGrid");
        logo.setFont(StyleConstants.FONT_TITLE);
        logo.setForeground(theme.getAccentGreen());
        logoPanel.add(logo);

        gbc.gridy = 0;
        gbc.insets = new Insets(0, 0, 4, 0);
        center.add(logoPanel, gbc);

        tagline = new JLabel("Turf Booking & Ground Management", SwingConstants.CENTER);
        tagline.setFont(StyleConstants.FONT_SMALL);
        tagline.setForeground(theme.getTextMuted());
        gbc.gridy = 1;
        gbc.insets = new Insets(0, 0, 20, 0);
        center.add(tagline, gbc);

        // ── Elevated Card ────────────────────────────────────────────────────
        card = new JPanel(new GridBagLayout());
        card.setBackground(theme.getBgCard());
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(theme.getBorder(), 1, true),
                new EmptyBorder(26, 28, 26, 28)));
        gbc.gridy = 2;
        gbc.insets = new Insets(0, 0, 0, 0);
        center.add(card, gbc);

        GridBagConstraints c = new GridBagConstraints();
        c.fill = GridBagConstraints.HORIZONTAL;
        c.weightx = 1;
        c.gridx = 0;

        // Heading
        heading = new JLabel("Sign In");
        heading.setFont(StyleConstants.FONT_HEADING);
        heading.setForeground(theme.getTextPrimary());
        c.gridy = 0;
        c.insets = new Insets(0, 0, 16, 0);
        card.add(heading, c);

        // Email
        c.gridy = 1; c.insets = new Insets(4, 0, 2, 0);
        emailLabel = StyleConstants.label("Email Address");
        card.add(emailLabel, c);
        emailField = StyleConstants.styledField();
        emailField.setToolTipText("Enter your registered email address");
        c.gridy = 2; c.insets = new Insets(0, 0, 12, 0);
        card.add(emailField, c);

        // Password
        c.gridy = 3; c.insets = new Insets(4, 0, 2, 0);
        passwordLabel = StyleConstants.label("Password");
        card.add(passwordLabel, c);
        passwordField = StyleConstants.styledPasswordField();
        passwordField.setToolTipText("Enter your password");
        c.gridy = 4; c.insets = new Insets(0, 0, 16, 0);
        card.add(passwordField, c);

        // Error message label
        errorLabel = new JLabel(" ");
        errorLabel.setFont(StyleConstants.FONT_SMALL);
        errorLabel.setForeground(theme.getStatusError());
        errorLabel.setHorizontalAlignment(SwingConstants.CENTER);
        c.gridy = 5; c.insets = new Insets(0, 0, 6, 0);
        card.add(errorLabel, c);

        // Login Button
        loginBtn = StyleConstants.primaryButton("Sign In to SportGrid");
        c.gridy = 6; c.insets = new Insets(0, 0, 16, 0);
        card.add(loginBtn, c);

        // Sign Up Link Row
        JPanel linkRow = new JPanel(new FlowLayout(FlowLayout.CENTER, 6, 0));
        linkRow.setOpaque(false);
        noAccount = new JLabel("Don't have an account?");
        noAccount.setFont(StyleConstants.FONT_SMALL);
        noAccount.setForeground(theme.getTextMuted());

        registerLink = new JLabel("<html><u>Sign Up</u></html>");
        registerLink.setFont(StyleConstants.FONT_SMALL);
        registerLink.setForeground(theme.getAccentGreen());
        registerLink.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        linkRow.add(noAccount);
        linkRow.add(registerLink);
        c.gridy = 7; c.insets = new Insets(0, 0, 0, 0);
        card.add(linkRow, c);

        root.add(center, BorderLayout.CENTER);

        // ── Event Wiring ─────────────────────────────────────────────────────
        loginBtn.addActionListener(e -> attemptLogin());
        passwordField.addActionListener(e -> attemptLogin());
        emailField.addActionListener(e -> passwordField.requestFocusInWindow());

        registerLink.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                dispose();
                new RegisterFrame().setVisible(true);
            }
        });
    }

    private void attemptLogin() {
        String email = emailField.getText().trim();
        String password = new String(passwordField.getPassword());

        if (email.isEmpty() || password.isEmpty()) {
            showError("Please enter both email and password.");
            return;
        }

        setLoading(true);
        errorLabel.setText(" ");

        new SwingWorker<User, Void>() {
            @Override
            protected User doInBackground() {
                return authService.login(email, password);
            }

            @Override
            protected void done() {
                setLoading(false);
                try {
                    User user = get();
                    openDashboard(user);
                } catch (Exception ex) {
                    showError(StyleConstants.formatErrorMessage(ex, "Login"));
                }
            }
        }.execute();
    }

    private void openDashboard(User user) {
        ThemeManager.removeListener(this);
        dispose();
        switch (user.getRole()) {
            case PLAYER:
                new PlayerDashboardFrame(user).setVisible(true);
                break;
            case OWNER:
                new OwnerDashboardFrame(user).setVisible(true);
                break;
            case ADMIN:
                new AdminPanelFrame(user).setVisible(true);
                break;
            default:
                new LoginFrame().setVisible(true);
        }
    }

    private void showError(String msg) {
        errorLabel.setText("<html><center>" + msg + "</center></html>");
    }

    private void setLoading(boolean loading) {
        loginBtn.setEnabled(!loading);
        loginBtn.setText(loading ? "Signing in…" : "Sign In to SportGrid");
        emailField.setEnabled(!loading);
        passwordField.setEnabled(!loading);
    }

    @Override
    public void onThemeChanged(Theme newTheme) {
        if (root == null) return;
        root.setBackground(newTheme.getBgMain());
        card.setBackground(newTheme.getBgCard());
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(newTheme.getBorder(), 1, true),
                new EmptyBorder(26, 28, 26, 28)));

        logo.setForeground(newTheme.getAccentGreen());
        tagline.setForeground(newTheme.getTextMuted());
        heading.setForeground(newTheme.getTextPrimary());
        emailLabel.setForeground(newTheme.getTextMuted());
        passwordLabel.setForeground(newTheme.getTextMuted());
        errorLabel.setForeground(newTheme.getStatusError());
        noAccount.setForeground(newTheme.getTextMuted());
        registerLink.setForeground(newTheme.getAccentGreen());

        emailField.setBackground(newTheme.getBgField());
        emailField.setForeground(newTheme.getTextPrimary());
        emailField.setCaretColor(newTheme.getAccentGreen());

        passwordField.setBackground(newTheme.getBgField());
        passwordField.setForeground(newTheme.getTextPrimary());
        passwordField.setCaretColor(newTheme.getAccentGreen());

        repaint();
    }
}
