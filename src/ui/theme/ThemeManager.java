package ui.theme;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.prefs.Preferences;

/**
 * Central Theme Manager for SportGrid.
 *
 * Capabilities:
 *  - Manages active visual theme (Light vs Dark).
 *  - Persists the selected theme across application restarts using standard Java Preferences.
 *  - Dispatches immediate runtime theme changes across all open windows, dialogs, and components.
 *  - Updates global UIManager tables to ensure dialogs, popups, and inputs stay contrast-compliant.
 */
public final class ThemeManager {

    private static final String PREF_THEME_KEY = "sportgrid_theme_mode";
    private static final Preferences PREFS = Preferences.userNodeForPackage(ThemeManager.class);

    private static Theme activeTheme;
    private static final List<ThemeChangeListener> listeners = new ArrayList<>();

    static {
        // Load persisted preference on startup, defaulting to DARK
        String savedMode = PREFS.get(PREF_THEME_KEY, ThemeMode.DARK.name());
        ThemeMode mode;
        try {
            mode = ThemeMode.valueOf(savedMode);
        } catch (Exception e) {
            mode = ThemeMode.DARK;
        }
        activeTheme = (mode == ThemeMode.LIGHT) ? Theme.light() : Theme.dark();
    }

    private ThemeManager() {}

    public static Theme getTheme() {
        return activeTheme;
    }

    public static ThemeMode getMode() {
        return activeTheme.getMode();
    }

    public static boolean isDark() {
        return activeTheme.isDark();
    }

    /**
     * Toggles between Dark and Light mode and triggers an immediate application-wide update.
     */
    public static void toggleTheme() {
        setTheme(activeTheme.isDark() ? ThemeMode.LIGHT : ThemeMode.DARK);
    }

    /**
     * Sets the active theme mode, persists the choice, and updates all active UI components.
     */
    public static void setTheme(ThemeMode mode) {
        if (activeTheme.getMode() == mode) return;

        activeTheme = (mode == ThemeMode.LIGHT) ? Theme.light() : Theme.dark();

        // 1. Save preference
        try {
            PREFS.put(PREF_THEME_KEY, mode.name());
        } catch (Exception ignored) {}

        // 2. Update global UIManager defaults
        applyUIManagerDefaults();

        // 3. Update legacy static colors in StyleConstants
        ui.StyleConstants.updateStaticColors();

        // 4. Notify registered listeners and open windows on the Event Dispatch Thread
        Runnable updateTask = () -> {
            for (ThemeChangeListener listener : new ArrayList<>(listeners)) {
                try {
                    listener.onThemeChanged(activeTheme);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            // Update all open frames and dialogs
            for (Window window : Window.getWindows()) {
                if (window.isDisplayable()) {
                    if (window instanceof ThemeChangeListener) {
                        try {
                            ((ThemeChangeListener) window).onThemeChanged(activeTheme);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                    try {
                        SwingUtilities.updateComponentTreeUI(window);
                        window.repaint();
                    } catch (Exception ignored) {}
                }
            }
        };

        if (SwingUtilities.isEventDispatchThread()) {
            updateTask.run();
        } else {
            SwingUtilities.invokeLater(updateTask);
        }
    }

    public static void addListener(ThemeChangeListener listener) {
        if (listener != null && !listeners.contains(listener)) {
            listeners.add(listener);
        }
    }

    public static void removeListener(ThemeChangeListener listener) {
        listeners.remove(listener);
    }

    /**
     * Applies comprehensive UIManager color mappings for the active theme.
     * Prevents white-on-white text issues on macOS Aqua and ensures all default Swing widgets match.
     */
    public static void applyUIManagerDefaults() {
        Theme t = activeTheme;

        // System property hints for crisp subpixel text
        System.setProperty("awt.useSystemAAFontSettings", "on");
        System.setProperty("swing.aatext", "true");
        System.setProperty("apple.awt.textantialiasing", "true");
        System.setProperty("apple.awt.antialiasing", "true");
        System.setProperty("apple.laf.useScreenMenuBar", "true");

        // Global backgrounds and foregrounds
        UIManager.put("Panel.background",             t.getBgMain());
        UIManager.put("Panel.foreground",             t.getTextPrimary());
        UIManager.put("OptionPane.background",         t.getBgCard());
        UIManager.put("OptionPane.messageForeground",    t.getTextPrimary());
        UIManager.put("Button.background",             t.getBgCard());
        UIManager.put("Button.foreground",             t.getTextPrimary());
        UIManager.put("Label.foreground",              t.getTextPrimary());

        // Text & Password fields
        UIManager.put("TextField.background",          t.getBgField());
        UIManager.put("TextField.foreground",          t.getTextPrimary());
        UIManager.put("TextField.caretForeground",     t.getAccentGreen());
        UIManager.put("PasswordField.background",      t.getBgField());
        UIManager.put("PasswordField.foreground",      t.getTextPrimary());
        UIManager.put("PasswordField.caretForeground", t.getAccentGreen());
        UIManager.put("TextArea.background",           t.getBgField());
        UIManager.put("TextArea.foreground",           t.getTextPrimary());
        UIManager.put("FormattedTextField.background", t.getBgField());
        UIManager.put("FormattedTextField.foreground", t.getTextPrimary());

        // Tables
        UIManager.put("Table.background",              t.getBgCard());
        UIManager.put("Table.foreground",              t.getTextPrimary());
        UIManager.put("Table.gridColor",               t.isDark() ? t.getBorderSubtle() : t.getBorder());
        UIManager.put("Table.selectionBackground",     t.getTableSelectionBg());
        UIManager.put("Table.selectionForeground",     t.getTableSelectionFg());
        UIManager.put("TableHeader.background",        t.isDark() ? t.getBgMain() : ThemeTableHeaderRenderer.LIGHT_HEADER_BG);
        UIManager.put("TableHeader.foreground",        t.isDark() ? t.getTextMuted() : ThemeTableHeaderRenderer.LIGHT_HEADER_FG);
        UIManager.put("TableHeader.cellBorder",        BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, t.getBorder()),
                new javax.swing.border.EmptyBorder(8, 12, 8, 12)));

        // Tabbed Pane (replaces native Aqua tabs and ensures readable contrast in Light & Dark modes)
        UIManager.put("TabbedPaneUI",                  ThemeTabbedPaneUI.class.getName());
        UIManager.put("TabbedPane.background",         t.getBgMain());
        UIManager.put("TabbedPane.foreground",         t.getTextPrimary());

        // ScrollPane & ScrollBar
        UIManager.put("ScrollPane.background",         t.getBgMain());
        UIManager.put("Viewport.background",           t.getBgMain());
        UIManager.put("ScrollBar.background",          t.getScrollTrack());
        UIManager.put("ScrollBar.track",               t.getScrollTrack());
        UIManager.put("ScrollBar.thumb",               t.getScrollThumb());

        // Dropdowns, Popups, Lists (replaces native Aqua UI and fixes white-on-white text)
        UIManager.put("ComboBoxUI",                    ThemeComboBoxUI.class.getName());
        UIManager.put("ComboBox.background",           t.getBgField());
        UIManager.put("ComboBox.foreground",           t.getTextPrimary());
        UIManager.put("ComboBox.selectionBackground",  t.getAccentGreen());
        UIManager.put("ComboBox.selectionForeground",  t.getAccentText());
        UIManager.put("List.background",               t.getBgCard());
        UIManager.put("List.foreground",               t.getTextPrimary());
        UIManager.put("List.selectionBackground",      t.getAccentGreen());
        UIManager.put("List.selectionForeground",      t.getAccentText());
        UIManager.put("PopupMenu.background",          t.getBgCard());
        UIManager.put("PopupMenu.foreground",          t.getTextPrimary());
        UIManager.put("MenuItem.background",           t.getBgCard());
        UIManager.put("MenuItem.foreground",           t.getTextPrimary());

        // Controls & Tooltips
        UIManager.put("RadioButton.background",        t.getBgMain());
        UIManager.put("RadioButton.foreground",        t.getTextPrimary());
        UIManager.put("CheckBox.background",           t.getBgMain());
        UIManager.put("CheckBox.foreground",           t.getTextPrimary());
        UIManager.put("ToolTip.background",            t.getBgCard());
        UIManager.put("ToolTip.foreground",            t.getTextPrimary());
        UIManager.put("ToolTip.border",                BorderFactory.createLineBorder(t.getBorder()));
    }
}
