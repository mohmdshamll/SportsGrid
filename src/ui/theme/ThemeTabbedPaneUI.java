package ui.theme;

import ui.StyleConstants;

import javax.swing.*;
import javax.swing.plaf.ComponentUI;
import javax.swing.plaf.basic.BasicTabbedPaneUI;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionListener;

/**
 * Custom Theme-Aware TabbedPaneUI for SportGrid.
 *
 * Resolves the macOS Aqua Look-and-Feel issue where tabs appear washed-out,
 * greyed out, or unreadable in Light Mode.
 *
 * Features:
 *  - High-contrast, WCAG-compliant typography in both Light and Dark themes.
 *  - Light Mode: Clean white selected tab with emerald top accent and deep charcoal unselected tabs.
 *  - Dark Mode: Rich navy selected tab with neon emerald accent and dark canvas unselected tabs.
 *  - Interactive hover feedback and hand cursor over tab headers.
 *  - Instant adaptation to ThemeManager runtime theme toggling.
 */
public class ThemeTabbedPaneUI extends BasicTabbedPaneUI {

    private int hoveredTabIndex = -1;
    private MouseMotionListener mouseMotionListener;
    private MouseAdapter mouseListener;

    public static ComponentUI createUI(JComponent c) {
        return new ThemeTabbedPaneUI();
    }

    @Override
    protected void installListeners() {
        super.installListeners();

        mouseMotionListener = new MouseAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) {
                if (tabPane == null) return;
                int index = tabForCoordinate(tabPane, e.getX(), e.getY());
                if (index != hoveredTabIndex) {
                    hoveredTabIndex = index;
                    if (index >= 0) {
                        tabPane.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
                    } else {
                        tabPane.setCursor(Cursor.getDefaultCursor());
                    }
                    tabPane.repaint();
                }
            }
        };

        mouseListener = new MouseAdapter() {
            @Override
            public void mouseExited(MouseEvent e) {
                if (tabPane == null) return;
                if (hoveredTabIndex != -1) {
                    hoveredTabIndex = -1;
                    tabPane.setCursor(Cursor.getDefaultCursor());
                    tabPane.repaint();
                }
            }
        };

        if (tabPane != null) {
            tabPane.addMouseMotionListener(mouseMotionListener);
            tabPane.addMouseListener(mouseListener);
        }
    }

    @Override
    protected void uninstallListeners() {
        if (tabPane != null) {
            if (mouseMotionListener != null) {
                tabPane.removeMouseMotionListener(mouseMotionListener);
            }
            if (mouseListener != null) {
                tabPane.removeMouseListener(mouseListener);
            }
        }
        mouseMotionListener = null;
        mouseListener = null;
        super.uninstallListeners();
    }

    @Override
    protected Insets getTabInsets(int tabPlacement, int tabIndex) {
        return new Insets(10, 24, 10, 24);
    }

    @Override
    protected Insets getTabAreaInsets(int tabPlacement) {
        return new Insets(6, 6, 0, 6);
    }

    @Override
    protected void paintTabBackground(Graphics g, int tabPlacement, int tabIndex,
                                      int x, int y, int w, int h, boolean isSelected) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        Theme t = ThemeManager.getTheme();

        boolean isHover = (tabIndex == hoveredTabIndex);

        if (t.isDark()) {
            if (isSelected) {
                g2.setColor(t.getBgCard()); // Rich deep navy #161E36
            } else if (isHover) {
                g2.setColor(new Color(30, 41, 69)); // Subtle lighter hover navy
            } else {
                g2.setColor(t.getBgMain()); // Dark canvas #0D1121
            }
        } else {
            // Light Mode
            if (isSelected) {
                g2.setColor(Color.WHITE); // Crisp white selected card
            } else if (isHover) {
                g2.setColor(new Color(226, 232, 240)); // Soft hover neutral #E2E8F0
            } else {
                g2.setColor(new Color(241, 245, 249)); // Light neutral canvas #F1F5F9
            }
        }

        // Fill rounded top tab shape
        g2.fillRoundRect(x, y + 2, w - 2, h + 8, 8, 8);

        // Draw active top accent indicator for selected tab
        if (isSelected) {
            g2.setColor(t.getAccentGreen());
            g2.fillRoundRect(x + 4, y + 2, w - 10, 3, 2, 2);
        }

        g2.dispose();
    }

    @Override
    protected void paintTabBorder(Graphics g, int tabPlacement, int tabIndex,
                                  int x, int y, int w, int h, boolean isSelected) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        Theme t = ThemeManager.getTheme();

        if (isSelected) {
            g2.setColor(t.getBorder());
            g2.drawRoundRect(x, y + 2, w - 2, h + 8, 8, 8);
        } else {
            g2.setColor(t.getBorderSubtle());
            g2.drawRoundRect(x, y + 2, w - 2, h + 8, 8, 8);
        }

        g2.dispose();
    }

    @Override
    protected void paintText(Graphics g, int tabPlacement, Font font, FontMetrics metrics,
                             int tabIndex, String title, Rectangle textRect, boolean isSelected) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON);
        Theme t = ThemeManager.getTheme();

        // Bold heading font for selected tab, clean bold body for unselected
        Font tabFont = isSelected
                ? StyleConstants.FONT_HEADING
                : StyleConstants.FONT_BODY.deriveFont(Font.BOLD, 13f);
        g2.setFont(tabFont);
        FontMetrics fm = g2.getFontMetrics(tabFont);

        Color textColor;
        if (t.isDark()) {
            if (isSelected) {
                textColor = Color.WHITE;
            } else {
                textColor = (tabIndex == hoveredTabIndex) ? Color.WHITE : t.getTextMuted();
            }
        } else {
            // Light Mode: high-contrast dark text
            if (isSelected) {
                textColor = new Color(17, 24, 39); // Deep dark slate #111827 (16:1 contrast)
            } else {
                textColor = (tabIndex == hoveredTabIndex)
                        ? new Color(17, 24, 39)
                        : new Color(75, 85, 99); // Dark charcoal #4B5563 (7.5:1 contrast)
            }
        }

        g2.setColor(textColor);
        int textY = textRect.y + ((textRect.height - fm.getHeight()) / 2) + fm.getAscent();
        g2.drawString(title, textRect.x, textY);

        g2.dispose();
    }

    @Override
    protected void paintContentBorder(Graphics g, int tabPlacement, int selectedIndex) {
        Graphics2D g2 = (Graphics2D) g.create();
        Theme t = ThemeManager.getTheme();
        int w = tabPane.getWidth();
        int h = tabPane.getHeight();
        Insets insets = getTabAreaInsets(tabPlacement);
        int tabAreaHeight = calculateTabAreaHeight(tabPlacement, runCount, maxTabHeight);

        g2.setColor(t.getBorder());
        g2.drawRect(0, tabAreaHeight + insets.top, w - 1, h - tabAreaHeight - insets.top - 1);

        g2.dispose();
    }

    @Override
    protected void paintFocusIndicator(Graphics g, int tabPlacement, Rectangle[] rects,
                                       int tabIndex, Rectangle iconRect, Rectangle textRect, boolean isSelected) {
        // Clean no-op to remove standard dotted rectangle
    }
}
