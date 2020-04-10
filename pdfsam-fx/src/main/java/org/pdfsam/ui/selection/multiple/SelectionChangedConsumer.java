package org.pdfsam.ui.selection.multiple;

import static org.pdfsam.eventstudio.StaticStudio.eventStudio;
import static org.pdfsam.support.io.ObjectCollectionWriter.writeContent;
import static org.pdfsam.ui.commons.SetDestinationRequest.requestDestination;
//import static org.sejda.eventstudio.StaticStudio.eventStudio;

import java.util.Arrays;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.pdfsam.eventstudio.annotation.EventStation;
import org.pdfsam.i18n.DefaultI18nContext;
import org.pdfsam.module.ModuleOwned;
import org.pdfsam.ui.commons.OpenFileRequest;
import org.pdfsam.ui.commons.RemoveSelectedEvent;
import org.pdfsam.ui.commons.SetPageRangesRequest;
import org.pdfsam.ui.commons.ShowPdfDescriptorRequest;
import org.pdfsam.ui.selection.multiple.move.MoveSelectedEvent;
import org.pdfsam.ui.selection.multiple.move.MoveType;
//import org.sejda.eventstudio.annotation.EventStation;

import de.jensd.fx.glyphs.materialdesignicons.MaterialDesignIcon;
import de.jensd.fx.glyphs.materialdesignicons.utils.MaterialDesignIconFactory;
import javafx.application.Platform;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.control.TableView;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;

public class SelectionChangedConsumer extends TableView<SelectionTableRowData> implements ModuleOwned {
    private String ownerModule = StringUtils.EMPTY;

    public Consumer<SelectionChangedEvent> selectionChangedConsumer;

    public SelectionChangedConsumer(String ownerModule) {
        this.ownerModule = ownerModule;
    }

    public SelectionChangedConsumer(String ownerModule, ContextMenu contextMenu, TableColumnProvider<?>[] columns, boolean canDuplicateItems, boolean canMove) {
        this(ownerModule);
        initTopSectionContextMenu(contextMenu, Arrays.stream(columns).anyMatch(PageRangesColumn.class::isInstance));
        initItemsSectionContextMenu(contextMenu, canDuplicateItems, canMove);
        initBottomSectionContextMenu(contextMenu);
    }

    public Consumer<SelectionChangedEvent> getSelectionChangedConsumer() {
        return selectionChangedConsumer;
    }

    private void initTopSectionContextMenu(ContextMenu contextMenu, boolean hasRanges) {
        MenuItem setDestinationItem = createMenuItem(DefaultI18nContext.getInstance().i18n("Set destination"),
                MaterialDesignIcon.AIRPLANE_LANDING);
        setDestinationItem.setOnAction(e -> eventStudio().broadcast(
                requestDestination(getSelectionModel().getSelectedItem().descriptor().getFile(), getOwnerModule()),
                getOwnerModule()));
        setDestinationItem.setAccelerator(new KeyCodeCombination(KeyCode.O, KeyCombination.ALT_DOWN));

        selectionChangedConsumer = e -> setDestinationItem.setDisable(!e.isSingleSelection());
        contextMenu.getItems().add(setDestinationItem);

        if (hasRanges) {
            MenuItem setPageRangesItem = createMenuItem(DefaultI18nContext.getInstance().i18n("Set as range for all"),
                    MaterialDesignIcon.FORMAT_INDENT_INCREASE);
            setPageRangesItem.setOnAction(e -> eventStudio().broadcast(
                    new SetPageRangesRequest(getSelectionModel().getSelectedItem().pageSelection.get()),
                    getOwnerModule()));
            setPageRangesItem.setAccelerator(new KeyCodeCombination(KeyCode.R, KeyCombination.CONTROL_DOWN));
            selectionChangedConsumer = selectionChangedConsumer
                    .andThen(e -> setPageRangesItem.setDisable(!e.isSingleSelection()));
            contextMenu.getItems().add(setPageRangesItem);
        }
        contextMenu.getItems().add(new SeparatorMenuItem());
    }

    private void initItemsSectionContextMenu(ContextMenu contextMenu, boolean canDuplicate, boolean canMove) {

        MenuItem removeSelected = createMenuItem(DefaultI18nContext.getInstance().i18n("Remove"),
                MaterialDesignIcon.MINUS);
        removeSelected.setOnAction(e -> eventStudio().broadcast(new RemoveSelectedEvent(), getOwnerModule()));
        removeSelected.setAccelerator(new KeyCodeCombination(KeyCode.DELETE));
        contextMenu.getItems().add(removeSelected);
        selectionChangedConsumer = selectionChangedConsumer
                .andThen(e -> removeSelected.setDisable(e.isClearSelection()));
        if (canMove) {
            MenuItem moveTopSelected = createMenuItem(DefaultI18nContext.getInstance().i18n("Move to Top"),
                    MaterialDesignIcon.CHEVRON_DOUBLE_UP);
            moveTopSelected
                    .setOnAction(e -> eventStudio().broadcast(new MoveSelectedEvent(MoveType.TOP), getOwnerModule()));

            MenuItem moveUpSelected = createMenuItem(DefaultI18nContext.getInstance().i18n("Move Up"),
                    MaterialDesignIcon.CHEVRON_UP);
            moveUpSelected
                    .setOnAction(e -> eventStudio().broadcast(new MoveSelectedEvent(MoveType.UP), getOwnerModule()));

            MenuItem moveDownSelected = createMenuItem(DefaultI18nContext.getInstance().i18n("Move Down"),
                    MaterialDesignIcon.CHEVRON_DOWN);
            moveDownSelected
                    .setOnAction(e -> eventStudio().broadcast(new MoveSelectedEvent(MoveType.DOWN), getOwnerModule()));

            MenuItem moveBottomSelected = createMenuItem(DefaultI18nContext.getInstance().i18n("Move to Bottom"),
                    MaterialDesignIcon.CHEVRON_DOUBLE_DOWN);
            moveBottomSelected.setOnAction(
                    e -> eventStudio().broadcast(new MoveSelectedEvent(MoveType.BOTTOM), getOwnerModule()));

            contextMenu.getItems().addAll(moveTopSelected, moveUpSelected, moveDownSelected, moveBottomSelected);

            moveBottomSelected.setAccelerator(new KeyCodeCombination(KeyCode.END, KeyCombination.ALT_DOWN));
            moveDownSelected.setAccelerator(new KeyCodeCombination(KeyCode.DOWN, KeyCombination.ALT_DOWN));
            moveUpSelected.setAccelerator(new KeyCodeCombination(KeyCode.UP, KeyCombination.ALT_DOWN));
            moveTopSelected.setAccelerator(new KeyCodeCombination(KeyCode.HOME, KeyCombination.ALT_DOWN));

            selectionChangedConsumer = selectionChangedConsumer.andThen(e -> {
                moveTopSelected.setDisable(!e.canMove(MoveType.TOP));
                moveUpSelected.setDisable(!e.canMove(MoveType.UP));
                moveDownSelected.setDisable(!e.canMove(MoveType.DOWN));
                moveBottomSelected.setDisable(!e.canMove(MoveType.BOTTOM));
            });
        }
        if (canDuplicate) {
            MenuItem duplicateItem = createMenuItem(DefaultI18nContext.getInstance().i18n("Duplicate"),
                    MaterialDesignIcon.CONTENT_DUPLICATE);
            duplicateItem.setOnAction(e -> eventStudio().broadcast(new DuplicateSelectedEvent(), getOwnerModule()));
            duplicateItem.setAccelerator(new KeyCodeCombination(KeyCode.DIGIT2, KeyCombination.ALT_DOWN));

            contextMenu.getItems().add(duplicateItem);

            selectionChangedConsumer = selectionChangedConsumer
                    .andThen(e -> duplicateItem.setDisable(e.isClearSelection()));
        }
    }

    private void initBottomSectionContextMenu(ContextMenu contextMenu) {

        MenuItem copyItem = createMenuItem(DefaultI18nContext.getInstance().i18n("Copy to clipboard"),
                MaterialDesignIcon.CONTENT_COPY);
        copyItem.setOnAction(e -> copySelectedToClipboard());

        MenuItem infoItem = createMenuItem(DefaultI18nContext.getInstance().i18n("Document properties"),
                MaterialDesignIcon.INFORMATION_OUTLINE);
        infoItem.setOnAction(e -> Platform.runLater(() -> eventStudio()
                .broadcast(new ShowPdfDescriptorRequest(getSelectionModel().getSelectedItem().descriptor()))));

        MenuItem openFileItem = createMenuItem(DefaultI18nContext.getInstance().i18n("Open"),
                MaterialDesignIcon.FILE_PDF_BOX);
        openFileItem.setOnAction(e -> eventStudio()
                .broadcast(new OpenFileRequest(getSelectionModel().getSelectedItem().descriptor().getFile())));

        MenuItem openFolderItem = createMenuItem(DefaultI18nContext.getInstance().i18n("Open Folder"),
                MaterialDesignIcon.FOLDER_OUTLINE);
        openFolderItem.setOnAction(e -> eventStudio().broadcast(
                new OpenFileRequest(getSelectionModel().getSelectedItem().descriptor().getFile().getParentFile())));

        copyItem.setAccelerator(new KeyCodeCombination(KeyCode.C, KeyCombination.SHORTCUT_DOWN));
        infoItem.setAccelerator(new KeyCodeCombination(KeyCode.P, KeyCombination.ALT_DOWN));
        openFileItem.setAccelerator(new KeyCodeCombination(KeyCode.O, KeyCombination.SHORTCUT_DOWN));
        openFolderItem.setAccelerator(
                new KeyCodeCombination(KeyCode.O, KeyCombination.SHORTCUT_DOWN, KeyCombination.ALT_DOWN));

        contextMenu.getItems().addAll(new SeparatorMenuItem(), copyItem, infoItem, openFileItem, openFolderItem);

        selectionChangedConsumer = selectionChangedConsumer.andThen(e -> {
            copyItem.setDisable(e.isClearSelection());
            infoItem.setDisable(!e.isSingleSelection());
            openFileItem.setDisable(!e.isSingleSelection());
            openFolderItem.setDisable(!e.isSingleSelection());
        });
    }

    private MenuItem createMenuItem(String text, MaterialDesignIcon icon) {
        MenuItem item = new MenuItem(text);
        MaterialDesignIconFactory.get().setIcon(item, icon, "1.1em");
        item.setDisable(true);
        return item;
    }

    private void copySelectedToClipboard() {
        ClipboardContent content = new ClipboardContent();
        writeContent(getSelectionModel().getSelectedItems().stream().map(item -> {
            return item.descriptor().getFile().getAbsolutePath() + ", " + item.descriptor().getFile().length() + ", "
                    + item.descriptor().pages().getValue();
        }).collect(Collectors.toList())).to(content);
        Clipboard.getSystemClipboard().setContent(content);
    }

    @Override
    @EventStation
    public String getOwnerModule() {
        return ownerModule;
    }

}