package com.legacyvisualfix.ui;

public interface MenuScroll {

    boolean legacyvisualfix$scrollAt(int mouseX, int mouseY, int wheel);

    interface Host {

        void legacyvisualfix$registerScrollList(MenuScroll list);
    }
}
