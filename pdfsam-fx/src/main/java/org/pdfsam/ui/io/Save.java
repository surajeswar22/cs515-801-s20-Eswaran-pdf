package org.pdfsam.ui.io;

import javafx.stage.Window;

import java.io.File;

/**
 * @see RememberingLatestFileChooserWrapper.OpenType#SAVE **/
public class Save extends Type {
    public File showDialog(File selected, Window ownerWindow, RememberingLatestFileChooserWrapper rememberingLatestFileChooserWrapper) {
        rememberingLatestFileChooserWrapper.sanitizeInitialDirectory();
        selected = rememberingLatestFileChooserWrapper.getWrapped().showSaveDialog(ownerWindow);
        return selected;
    }
}