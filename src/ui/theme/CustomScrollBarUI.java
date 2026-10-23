package ui.theme;

import javax.swing.*;
import javax.swing.plaf.basic.BasicScrollBarUI;
import java.awt.*;

/**
 * Custom scrollbar UI providing rounded thumbs and theme-aligned tracks,
 * preventing Aqua's native white track background from flashing against dark surfaces.
 */
public class CustomScrollBarUI extends BasicScrollBarUI {

    @Override
    protected void configureScrollBarColors() {
        Theme theme = ThemeManager.getTheme();
        this.thumbColor = theme.getScrollThumb();
        this.trackColor = theme.getScrollTrack();
    }

    @Override
    protected JButton createDecreaseButton(int orientation) {
        return createZeroButton();
    }

    @Override
    protected JButton createIncreaseButton(int orientation) {
        return createZeroButton();
    }

    private JButton createZeroButton() {
        JButton btn = new JButton();
        btn.setPreferredSize(new Dimension(0, 0));
        btn.setMinimumSize(new Dimension(0, 0));
        btn.setMaximumSize(new Dimension(0, 0));
        btn.setOpaque(false);
        btn.setFocusable(false);
        return btn;
    }

    @Override
    protected void paintTrack(Graphics g, JComponent c, Rectangle trackBounds) {
        Theme theme = ThemeManager.getTheme();
        g.setColor(theme.getScrollTrack());
        g.fillRect(trackBounds.x, trackBounds.y, trackBounds.width, trackBounds.height);
    }

    @Override
    protected void paintThumb(Graphics g, JComponent c, Rectangle thumbBounds) {
        if (thumbBounds.isEmpty() || !scrollbar.isEnabled()) {
            return;
        }

        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        Theme theme = ThemeManager.getTheme();
        g2.setColor(isThumbRollover() ? theme.getAccentGreen() : theme.getScrollThumb());

        int arc = Math.min(thumbBounds.width, thumbBounds.height);
        int margin = 2;
        g2.fillRoundRect(thumbBounds.x + margin, thumbBounds.y + margin,
                thumbBounds.width - margin * 2, thumbBounds.height - margin * 2, arc, arc);
        g2.dispose();
    }
}
