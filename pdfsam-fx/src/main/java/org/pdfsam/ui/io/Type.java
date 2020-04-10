package org.pdfsam.ui.io;

import javafx.stage.Window;

import java.io.File;

public abstract class Type {
    public abstract File showDialog(File selected, Window ownerWindow, RememberingLatestFileChooserWrapper rememberingLatestFileChooserWrapper);
}