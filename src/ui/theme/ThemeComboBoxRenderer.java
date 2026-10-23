package ui.theme;

import ui.StyleConstants;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

/**
 * Custom ListCellRenderer for JComboBox items.
 *
 * Resolves the macOS Aqua Look-and-Feel bug where dropdown popup menus
 * default to a white background while using light-colored text, causing
 * severe white-on-white text illegibility.
 *
 * Distinguishes between closed combobox value (index == -1) and popup list items (index >= 0).
 */
public class ThemeComboBoxRenderer extends DefaultListCellRenderer {

    public ThemeComboBoxRenderer() {
        setOpaque(true);
        setBorder(new EmptyBorder(8, 12, 8, 12));
    }

    @Override
    public Component getListCellRendererComponent(JList<?> list, Object value, int index,
                                                  boolean isSelected, boolean cellHasFocus) {
        JLabel label = (JLabel) super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
        Theme theme = ThemeManager.getTheme();

        label.setFont(StyleConstants.FONT_BODY);

        if (index == -1) {
            // Drawn inside the closed combobox body
            label.setOpaque(false);
            label.setForeground(theme.getTextPrimary());
            label.setBorder(new EmptyBorder(0, 10, 0, 0));
        } else {
            // Dropdown popup list items
            label.setOpaque(true);
            label.setBorder(new EmptyBorder(8, 12, 8, 12));
            if (isSelected) {
                label.setBackground(theme.getAccentGreen());
                label.setForeground(theme.getAccentText());
            } else {
                label.setBackground(theme.getBgCard());
                label.setForeground(theme.getTextPrimary());
            }
        }

        return label;
    }
}
