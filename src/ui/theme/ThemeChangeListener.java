package ui.theme;

/**
 * Listener interface for components and windows that react to theme changes at runtime.
 */
@FunctionalInterface
public interface ThemeChangeListener {
    /**
     * Invoked immediately when the user or system switches between Light and Dark themes.
     * Implementations should reapply theme colors and call repaint() on the container hierarchy.
     */
    void onThemeChanged(Theme newTheme);
}
