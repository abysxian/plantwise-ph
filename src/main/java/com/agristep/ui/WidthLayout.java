package com.agristep.ui;

import java.awt.Component;
import java.awt.Container;
import java.awt.Insets;

/**
 * A layout whose height depends on the width it is given (wrapped text makes a box taller when it gets narrower).
 * This is what lets the same screens fit a phone-sized window and a wide desktop window.
 */
interface WidthLayout {
    /** Height of the content area (without the container's insets) when that area is {@code width} wide. */
    int heightFor(Container parent, int width);

    /** Height a child needs when it is {@code width} wide. */
    static int heightOf(Component child, int width) {
        if (!child.isVisible()) return 0;
        if (child instanceof WrapLabel label) return label.heightFor(width);
        if (child instanceof Container box && box.getLayout() instanceof WidthLayout layout) {
            Insets in = box.getInsets();
            return in.top + in.bottom + layout.heightFor(box, Math.max(1, width - in.left - in.right));
        }
        return child.getPreferredSize().height;
    }
}
