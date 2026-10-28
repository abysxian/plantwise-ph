package com.agristep.ui;

import com.agristep.domain.Crop;

import javax.swing.BorderFactory;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import java.awt.Color;
import java.util.List;

/** Small building blocks used by more than one screen. */
final class Widgets {
    private Widgets() {}

    /** A drop-down of vegetables with their pictures. */
    static JComboBox<Crop> cropBox(List<Crop> crops) {
        JComboBox<Crop> box = new JComboBox<>(crops.toArray(new Crop[0]));
        box.setRenderer((list, value, index, selected, focus) -> {
            JLabel label = new JLabel(value == null ? "" : value.getName() + "  (" + value.getEnglishName() + ")");
            if (value != null) label.setIcon(Emoji.icon(value.getEmoji(), 22));
            label.setIconTextGap(8);
            label.setOpaque(true);
            label.setFont(Theme.BODY);
            label.setForeground(Theme.INK);
            label.setBackground(selected ? Theme.LEAF_SOFT : Theme.WHITE);
            label.setBorder(BorderFactory.createEmptyBorder(4, 8, 4, 8));
            return label;
        });
        return box;
    }

    /** A drop-down whose rows are drawn from {@code text(item)}. */
    static <T> JComboBox<T> textBox(List<T> items, java.util.function.Function<T, String> text) {
        @SuppressWarnings("unchecked") T[] array = (T[]) items.toArray();
        JComboBox<T> box = new JComboBox<>(array);
        box.setRenderer((list, value, index, selected, focus) -> {
            JLabel label = new JLabel(value == null ? "" : text.apply(value));
            label.setOpaque(true);
            label.setFont(Theme.BODY);
            label.setForeground(Theme.INK);
            label.setBackground(selected ? Theme.LEAF_SOFT : Theme.WHITE);
            label.setBorder(BorderFactory.createEmptyBorder(4, 8, 4, 8));
            return label;
        });
        return box;
    }

    /** A wrapping multi-line text box in the app's style. */
    static JTextArea textArea(int rows) {
        JTextArea area = new JTextArea(rows, 20);
        area.setLineWrap(true);
        area.setWrapStyleWord(true);
        area.setFont(Theme.BODY);
        area.setForeground(Theme.INK);
        area.setCaretColor(Theme.INK);
        area.setBackground(new Color(0xF7FAF8));
        area.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(0xC9D6CD)),
                BorderFactory.createEmptyBorder(9, 12, 9, 12)));
        return area;
    }

    /** A row that keeps a component at its natural size on the left. */
    static JPanel left(java.awt.Component c) {
        JPanel row = new JPanel(new RowLayout(0, false));
        row.setOpaque(false);
        row.add(c, RowLayout.WEST);
        return row;
    }

    static JPanel labelled(String label, java.awt.Component field) {
        JPanel box = new JPanel(new StackLayout(4));
        box.setOpaque(false);
        box.add(new WrapLabel(label, Theme.SMALL_BOLD, Theme.MUTED));
        box.add(field);
        return box;
    }

    static Card notice(String text) {
        Card card = new Card(Theme.LIME_SOFT, Theme.LIME).padding(12);
        card.add(WrapLabel.body(text));
        return card;
    }
}
