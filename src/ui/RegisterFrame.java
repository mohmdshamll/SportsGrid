package ui;

import model.Role;
import model.User;
import service.AuthService;
import ui.theme.Theme;
import ui.theme.ThemeChangeListener;
import ui.theme.ThemeManager;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

/**
 * Registration screen for new Players and Turf Owners.
 *
 * Supports Light & Dark visual themes, visible theme toggle, accessible focus indicators,
 * and responsive non-blocking background registration via SwingWorker.
 */
public class RegisterFrame extends JFrame implements ThemeChangeListener {

    private final AuthService authService = new AuthService();

    private JPanel root;
    private JPanel card;
    private JLabel logo;
    private JLabel tagline;
    private JLabel heading;
    private JLabel nameLabel;
    private JLabel emailLabel;
    private JLabel phoneLabel;
    private JLabel passwordLabel;
    private JLabel confirmPasswordLabel;
    private JLabel roleLabel;
    private JTextField nameField;
    private JTextField emailField;
    private JTextField phoneField;
    private JPasswordField passwordField;
    private JPasswordField confirmPasswordField;
    private JComboBox<String> roleCombo;
    private JLabel errorLabel;
    private JButton registerBtn;
    private JLabel already;
    private JLabel loginLink;

    public RegisterFrame() {
        setTitle("SportGrid – Sign Up");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setResizable(false);

        buildUI();

        pack();
        setMinimumSize(new Dimension(480, 680));
        setPreferredSize(new Dimension(480, 710));
        pack();
        setLocationRelativeTo(null);

        // Register for real-time theme changes
        ThemeManager.addListener(this);
    }

    private void buildUI() {
        Theme theme = ThemeManager.getTheme();

        root = new JPanel(new BorderLayout(0, 8));
        root.setBackground(theme.getBgMain());
        root.setBorder(new EmptyBorder(14, 24, 20, 24));
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

        // Logo
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
        gbc.insets = new Insets(0, 0, 2, 0);
        center.add(logoPanel, gbc);

        tagline = new JLabel("Create your account", SwingConstants.CENTER);
        tagline.setFont(StyleConstants.FONT_SMALL);
        tagline.setForeground(theme.getTextMuted());
        gbc.gridy = 1;
        gbc.insets = new Insets(0, 0, 14, 0);
        center.add(tagline, gbc);

        // Card
        card = new JPanel(new GridBagLayout());
        card.setBackground(theme.getBgCard());
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(theme.getBorder(), 1, true),
                new EmptyBorder(20, 26, 20, 26)));
        gbc.gridy = 2;
        gbc.insets = new Insets(0, 0, 0, 0);
        center.add(card, gbc);

        GridBagConstraints c = new GridBagConstraints();
        c.fill = GridBagConstraints.HORIZONTAL;
        c.weightx = 1;
        c.gridx = 0;

        int row = 0;

        // Heading
        heading = new JLabel("Join the Grid");
        heading.setFont(StyleConstants.FONT_HEADING);
        heading.setForeground(theme.getTextPrimary());
        c.gridy = row++;
        c.insets = new Insets(0, 0, 12, 0);
        card.add(heading, c);

        // Full name
        c.gridy = row++; c.insets = new Insets(2, 0, 2, 0);
        nameLabel = StyleConstants.label("Full Name");
        card.add(nameLabel, c);
        nameField = StyleConstants.styledField();
        nameField.setToolTipText("Enter your full legal or display name");
        c.gridy = row++; c.insets = new Insets(0, 0, 8, 0);
        card.add(nameField, c);

        // Email
        c.gridy = row++; c.insets = new Insets(2, 0, 2, 0);
        emailLabel = StyleConstants.label("Email Address");
        card.add(emailLabel, c);
        emailField = StyleConstants.styledField();
        emailField.setToolTipText("Enter your email (e.g. alex@example.com)");
        c.gridy = row++; c.insets = new Insets(0, 0, 8, 0);
        card.add(emailField, c);

        // Phone
        c.gridy = row++; c.insets = new Insets(2, 0, 2, 0);
        phoneLabel = StyleConstants.label("Phone (10 digits)");
        card.add(phoneLabel, c);
        phoneField = StyleConstants.styledField();
        phoneField.setToolTipText("Enter a 10-digit mobile number");
        c.gridy = row++; c.insets = new Insets(0, 0, 8, 0);
        card.add(phoneField, c);

        // Password
        c.gridy = row++; c.insets = new Insets(2, 0, 2, 0);
        passwordLabel = StyleConstants.label("Password (min 6 characters)");
        card.add(passwordLabel, c);
        passwordField = StyleConstants.styledPasswordField();
        passwordField.setToolTipText("Choose a secure password");
        c.gridy = row++; c.insets = new Insets(0, 0, 8, 0);
        card.add(passwordField, c);

        // Confirm password
        c.gridy = row++; c.insets = new Insets(2, 0, 2, 0);
        confirmPasswordLabel = StyleConstants.label("Confirm Password");
        card.add(confirmPasswordLabel, c);
        confirmPasswordField = StyleConstants.styledPasswordField();
        confirmPasswordField.setToolTipText("Re-type your password");
        c.gridy = row++; c.insets = new Insets(0, 0, 8, 0);
        card.add(confirmPasswordField, c);

        // Role selector
        c.gridy = row++; c.insets = new Insets(2, 0, 2, 0);
        roleLabel = StyleConstants.label("I am a…");
        card.add(roleLabel, c);
        roleCombo = new JComboBox<>(new String[]{"Player", "Turf Owner"});
        StyleConstants.styleComboBox(roleCombo);
        c.gridy = row++; c.insets = new Insets(0, 0, 12, 0);
        card.add(roleCombo, c);

        // Error label
        errorLabel = new JLabel(" ");
        errorLabel.setFont(StyleConstants.FONT_SMALL);
        errorLabel.setForeground(theme.getStatusError());
        errorLabel.setHorizontalAlignment(SwingConstants.CENTER);
        c.gridy = row++; c.insets = new Insets(0, 0, 4, 0);
        card.add(errorLabel, c);

        // Register button
        registerBtn = StyleConstants.primaryButton("Create Account");
        c.gridy = row++; c.insets = new Insets(0, 0, 12, 0);
        card.add(registerBtn, c);

        // Back to login link
        JPanel linkRow = new JPanel(new FlowLayout(FlowLayout.CENTER, 4, 0));
        linkRow.setOpaque(false);
        already = new JLabel("Already have an account?");
        already.setFont(StyleConstants.FONT_SMALL);
        already.setForeground(theme.getTextMuted());
        loginLink = new JLabel("<html><u>Login</u></html>");
        loginLink.setFont(StyleConstants.FONT_SMALL);
        loginLink.setForeground(theme.getAccentGreen());
        loginLink.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        linkRow.add(already);
        linkRow.add(loginLink);
        c.gridy = row;
        c.insets = new Insets(0, 0, 0, 0);
        card.add(linkRow, c);

        root.add(center, BorderLayout.CENTER);

        // ── Events ──────────────────────────────────────────────────────────
        registerBtn.addActionListener(e -> attemptRegister());
        loginLink.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                ThemeManager.removeListener(RegisterFrame.this);
                dispose();
                new LoginFrame().setVisible(true);
            }
        });
    }

    private void attemptRegister() {
        String name     = nameField.getText().trim();
        String email    = emailField.getText().trim();
        String phone    = phoneField.getText().trim();
        String password = new String(passwordField.getPassword());
        String confirm  = new String(confirmPasswordField.getPassword());

        if (name.isEmpty() || email.isEmpty() || phone.isEmpty() || password.isEmpty()) {
            showError("Please fill in all fields.");
            return;
        }
        if (!password.equals(confirm)) {
            showError("Passwords do not match.");
            return;
        }

        Role role = roleCombo.getSelectedIndex() == 0 ? Role.PLAYER : Role.OWNER;

        setLoading(true);
        errorLabel.setText(" ");

        new SwingWorker<User, Void>() {
            @Override
            protected User doInBackground() {
                return authService.register(name, email, phone, password, role);
            }

            @Override
            protected void done() {
                setLoading(false);
                try {
                    get();   // success — we don't need the returned user here
                    JOptionPane.showMessageDialog(
                            RegisterFrame.this,
                            "Account created successfully!\nYou can now log in.",
                            "Registration Successful",
                            JOptionPane.INFORMATION_MESSAGE);
                    ThemeManager.removeListener(RegisterFrame.this);
                    dispose();
                    new LoginFrame(email).setVisible(true);
                } catch (Exception ex) {
                    showError(StyleConstants.formatErrorMessage(ex, "Registration"));
                }
            }
        }.execute();
    }

    private void showError(String msg) {
        errorLabel.setText("<html><center>" + msg + "</center></html>");
    }

    private void setLoading(boolean loading) {
        registerBtn.setEnabled(!loading);
        registerBtn.setText(loading ? "Creating account…" : "Create Account");
    }

    @Override
    public void onThemeChanged(Theme newTheme) {
        if (root == null) return;
        root.setBackground(newTheme.getBgMain());
        card.setBackground(newTheme.getBgCard());
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(newTheme.getBorder(), 1, true),
                new EmptyBorder(20, 26, 20, 26)));

        logo.setForeground(newTheme.getAccentGreen());
        tagline.setForeground(newTheme.getTextMuted());
        heading.setForeground(newTheme.getTextPrimary());
        nameLabel.setForeground(newTheme.getTextMuted());
        emailLabel.setForeground(newTheme.getTextMuted());
        phoneLabel.setForeground(newTheme.getTextMuted());
        passwordLabel.setForeground(newTheme.getTextMuted());
        confirmPasswordLabel.setForeground(newTheme.getTextMuted());
        roleLabel.setForeground(newTheme.getTextMuted());
        errorLabel.setForeground(newTheme.getStatusError());
        already.setForeground(newTheme.getTextMuted());
        loginLink.setForeground(newTheme.getAccentGreen());

        nameField.setBackground(newTheme.getBgField());
        nameField.setForeground(newTheme.getTextPrimary());
        nameField.setCaretColor(newTheme.getAccentGreen());

        emailField.setBackground(newTheme.getBgField());
        emailField.setForeground(newTheme.getTextPrimary());
        emailField.setCaretColor(newTheme.getAccentGreen());

        phoneField.setBackground(newTheme.getBgField());
        phoneField.setForeground(newTheme.getTextPrimary());
        phoneField.setCaretColor(newTheme.getAccentGreen());

        passwordField.setBackground(newTheme.getBgField());
        passwordField.setForeground(newTheme.getTextPrimary());
        passwordField.setCaretColor(newTheme.getAccentGreen());

        confirmPasswordField.setBackground(newTheme.getBgField());
        confirmPasswordField.setForeground(newTheme.getTextPrimary());
        confirmPasswordField.setCaretColor(newTheme.getAccentGreen());

        roleCombo.setBackground(newTheme.getBgField());
        roleCombo.setForeground(newTheme.getTextPrimary());
        roleCombo.setBorder(BorderFactory.createLineBorder(newTheme.getBorder(), 1));

        repaint();
    }
}
