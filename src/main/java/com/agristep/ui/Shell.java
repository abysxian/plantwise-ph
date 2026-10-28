package com.agristep.ui;

import java.awt.Component;

/** The thing the app draws into: the real window, or a plain panel in tests. */
public interface Shell {
    void showBody(Component body);

    void setWindowTitle(String title);
}
