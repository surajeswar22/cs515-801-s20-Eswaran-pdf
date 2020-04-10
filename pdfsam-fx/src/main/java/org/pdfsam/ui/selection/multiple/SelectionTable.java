/*
 * This file is part of the PDF Split And Merge source code
 * Created on 27/nov/2013
 * Copyright 2017 by Sober Lemur S.a.s. di Vacondio Andrea (info@pdfsam.org).
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.pdfsam.ui.selection.multiple;

import static java.util.Arrays.stream;
import static java.util.Optional.of;
import static java.util.Optional.ofNullable;
import static org.apache.commons.lang3.StringUtils.defaultString;
import static org.pdfsam.eventstudio.StaticStudio.eventStudio;
import static org.pdfsam.support.EncryptionUtils.encrypt;
import static org.pdfsam.ui.commons.SetDestinationRequest.requestFallbackDestination;
import static org.pdfsam.ui.selection.multiple.SelectionChangedEvent.clearSelectionEvent;
import static org.pdfsam.ui.selection.multiple.SelectionChangedEvent.select;
//import static org.sejda.eventstudio.StaticStudio.eventStudio;
import org.pdfsam.eventstudio.annotation.EventListener;
import org.pdfsam.eventstudio.annotation.EventStation;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import org.apache.commons.lang3.StringUtils;
import org.pdfsam.context.DefaultUserContext;
import org.pdfsam.i18n.DefaultI18nContext;
import org.pdfsam.module.ModuleOwned;
import org.pdfsam.pdf.PdfDocumentDescriptor;
import org.pdfsam.pdf.PdfLoadRequestEvent;
import org.pdfsam.support.EncryptionUtils;
import org.pdfsam.support.io.FileType;
import org.pdfsam.ui.commons.ClearModuleEvent;
import org.pdfsam.ui.commons.RemoveSelectedEvent;
import org.pdfsam.ui.commons.SetPageRangesRequest;
import org.pdfsam.ui.notification.AddNotificationRequestEvent;
import org.pdfsam.ui.notification.NotificationType;
import org.pdfsam.ui.selection.PasswordFieldPopup;
import org.pdfsam.ui.selection.ShowPasswordFieldPopupRequest;
import org.pdfsam.ui.selection.multiple.move.MoveSelectedEvent;
import org.pdfsam.ui.selection.multiple.move.SelectionAndFocus;
import org.pdfsam.ui.workspace.RestorableView;
//import org.sejda.eventstudio.annotation.EventListener;
//import org.sejda.eventstudio.annotation.EventStation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javafx.collections.ListChangeListener.Change;
import javafx.collections.ObservableList;
import javafx.geometry.Point2D;
import javafx.scene.Scene;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.DataFormat;
import javafx.scene.input.DragEvent;
import javafx.scene.input.Dragboard;
import javafx.scene.input.TransferMode;
import javafx.stage.Window;

/**
 * Table displaying selected pdf documents
 *
 * @author Andrea Vacondio
 *
 */
public class SelectionTable extends TableView<SelectionTableRowData> implements ModuleOwned, RestorableView {

        private static final Logger LOG = LoggerFactory.getLogger(SelectionTable.class);

        private static final DataFormat DND_TABLE_SELECTION_MIME_TYPE = new DataFormat(
                "application/x-java-table-selection-list");

        private String ownerModule = StringUtils.EMPTY;
        private Label placeHolder = new Label(DefaultI18nContext.getInstance().i18n("Drag and drop PDF files here"));
        private PasswordFieldPopup passwordPopup;
        private SelectionChangedConsumer selectionChangedConsumer;

        public SelectionTable(String ownerModule, boolean canDuplicateItems, boolean canMove,
                              TableColumnProvider<?>... columns) {
                this.ownerModule = defaultString(ownerModule);
                setEditable(true);
                getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
                getColumns().add(new IndexColumn());
                Arrays.stream(columns).forEach(c -> getColumns().add(c.getTableColumn()));
                setColumnResizePolicy(CONSTRAINED_RESIZE_POLICY);
                getStyleClass().add("selection-table");
                initDragAndDrop(canMove);
                getSelectionModel().getSelectedIndices().addListener((Change<? extends Integer> c) -> {

                        ObservableList<? extends Integer> selected = c.getList();
                        if (selected.isEmpty()) {
                                eventStudio().broadcast(clearSelectionEvent(), ownerModule);
                                LOG.trace("Selection cleared for {}", ownerModule);
                        } else {
                                SelectionChangedEvent newSelectionEvent = select(selected).ofTotalRows(getItems().size());
                                eventStudio().broadcast(newSelectionEvent, ownerModule);
                                LOG.trace("{} for {}", newSelectionEvent, ownerModule);
                        }

                });
                placeHolder.getStyleClass().add("drag-drop-placeholder");
                placeHolder.setDisable(true);
                setPlaceholder(placeHolder);
                passwordPopup = new PasswordFieldPopup(this.ownerModule);

                ContextMenu contextMenu = new ContextMenu();
                selectionChangedConsumer = new SelectionChangedConsumer(ownerModule, contextMenu, columns, canDuplicateItems, canMove);
                setContextMenu(contextMenu);
                eventStudio().addAnnotatedListeners(this);
                eventStudio().add(SelectionChangedEvent.class, e -> selectionChangedConsumer.getSelectionChangedConsumer().accept(e), ownerModule);
        }

        private void initDragAndDrop(boolean canMove) {
                setOnDragOver(e -> dragConsume(e, this.onDragOverConsumer()));
                setOnDragEntered(e -> dragConsume(e, this.onDragEnteredConsumer()));
                setOnDragExited(this::onDragExited);
                setOnDragDropped(e -> dragConsume(e, this.onDragDropped()));
                if (canMove) {
                        setRowFactory(tv -> {
                                TableRow<SelectionTableRowData> row = new TableRow<>();
                                row.setOnDragDetected(e -> {
                                        ArrayList<Integer> selection = new ArrayList<>(getSelectionModel().getSelectedIndices());
                                        if (!row.isEmpty() && !selection.isEmpty()) {
                                                Dragboard db = row.startDragAndDrop(TransferMode.MOVE);
                                                db.setDragView(row.snapshot(null, null));
                                                ClipboardContent cc = new ClipboardContent();
                                                cc.put(DND_TABLE_SELECTION_MIME_TYPE, selection);
                                                db.setContent(cc);
                                                e.consume();

                                        }
                                });

                                row.setOnDragOver(e -> {
                                        if (e.getGestureSource() != row && e.getDragboard().hasContent(DND_TABLE_SELECTION_MIME_TYPE)) {
                                                if (!((List<Integer>) e.getDragboard().getContent(DND_TABLE_SELECTION_MIME_TYPE))
                                                        .contains(row.getIndex())) {
                                                        e.acceptTransferModes(TransferMode.MOVE);
                                                        e.consume();
                                                }
                                        }
                                });
                                row.setOnDragEntered(e -> {
                                        if (!row.isEmpty() && e.getDragboard().hasContent(DND_TABLE_SELECTION_MIME_TYPE)) {
                                                if (!((List<Integer>) e.getDragboard().getContent(DND_TABLE_SELECTION_MIME_TYPE))
                                                        .contains(row.getIndex())) {
                                                        row.setOpacity(0.6);
                                                }
                                        }
                                });
                                row.setOnDragExited(e -> {
                                        if (!row.isEmpty() && e.getDragboard().hasContent(DND_TABLE_SELECTION_MIME_TYPE)) {
                                                if (!((List<Integer>) e.getDragboard().getContent(DND_TABLE_SELECTION_MIME_TYPE))
                                                        .contains(row.getIndex())) {
                                                        row.setOpacity(1);
                                                }
                                        }
                                });

                                row.setOnDragDropped(e -> {
                                        Dragboard db = e.getDragboard();
                                        if (db.hasContent(DND_TABLE_SELECTION_MIME_TYPE)) {
                                                Optional<SelectionTableRowData> focus = ofNullable(getFocusModel().getFocusedItem());
                                                Optional<SelectionTableRowData> toDrop = of(row).filter(r -> !r.isEmpty())
                                                        .map(TableRow::getIndex).map(getItems()::get);

                                                List<Integer> dragged = (List<Integer>) e.getDragboard()
                                                        .getContent(DND_TABLE_SELECTION_MIME_TYPE);
                                                List<SelectionTableRowData> toMove = dragged.stream().map(getItems()::get)
                                                        .filter(Objects::nonNull).collect(Collectors.toList());
                                                getItems().removeAll(toMove);

                                                int dropIndex = getItems().size();
                                                if (toDrop.isPresent()) {
                                                        int toDropNewIndex = toDrop.map(getItems()::indexOf).get();
                                                        if (toDropNewIndex == row.getIndex()) {
                                                                // we dropped up
                                                                dropIndex = toDropNewIndex;
                                                        } else {
                                                                // we dropped down
                                                                dropIndex = toDropNewIndex + 1;
                                                        }
                                                }

                                                getItems().addAll(dropIndex, toMove);
                                                e.setDropCompleted(true);
                                                getSelectionModel().clearSelection();
                                                getSelectionModel().selectRange(dropIndex, dropIndex + toMove.size());
                                                focus.map(getItems()::indexOf).ifPresent(getFocusModel()::focus);
                                                e.consume();
                                        }
                                });

                                return row;
                        });
                }
        }

        private void dragConsume(DragEvent e, Consumer<DragEvent> c) {
                if (e.getDragboard().hasFiles()) {
                        c.accept(e);
                }
                e.consume();
        }

        private Consumer<DragEvent> onDragOverConsumer() {
                return (DragEvent e) -> e.acceptTransferModes(TransferMode.COPY_OR_MOVE);
        }

        private Consumer<DragEvent> onDragEnteredConsumer() {
                return (DragEvent e) -> placeHolder.setDisable(false);

        }

        private void onDragExited(DragEvent e) {
                placeHolder.setDisable(true);
                e.consume();
        }

        private Consumer<DragEvent> onDragDropped() {
                return (DragEvent e) -> {
                        final PdfLoadRequestEvent loadEvent = new PdfLoadRequestEvent(getOwnerModule());
                        getFilesFromDragboard(e.getDragboard()).filter(f -> FileType.PDF.matches(f.getName()))
                                .map(PdfDocumentDescriptor::newDescriptorNoPassword).forEach(loadEvent::add);
                        if (!loadEvent.getDocuments().isEmpty()) {
                                eventStudio().broadcast(loadEvent, getOwnerModule());
                        } else {
                                eventStudio().broadcast(new AddNotificationRequestEvent(NotificationType.WARN,
                                        DefaultI18nContext.getInstance()
                                                .i18n("Drag and drop PDF files or directories containing PDF files"),
                                        DefaultI18nContext.getInstance().i18n("No PDF found")));
                        }
                        e.setDropCompleted(true);
                };
        }

        private Stream<File> getFilesFromDragboard(Dragboard board) {
                List<File> files = board.getFiles();
                if (files.size() == 1 && files.get(0).isDirectory()) {
                        return stream(files.get(0).listFiles()).sorted();
                }
                return files.stream();
        }

        @Override
        @EventStation
        public String getOwnerModule() {
                return ownerModule;
        }

        @EventListener(priority = Integer.MIN_VALUE)
        public void onLoadDocumentsRequest(PdfLoadRequestEvent loadEvent) {
                getItems()
                        .addAll(loadEvent.getDocuments().stream().map(SelectionTableRowData::new).collect(Collectors.toList()));
                loadEvent.getDocuments().stream().findFirst().ifPresent(f -> eventStudio()
                        .broadcast(requestFallbackDestination(f.getFile(), getOwnerModule()), getOwnerModule()));
                eventStudio().broadcast(loadEvent);
        }

        @EventListener
        public void onDuplicate(final DuplicateSelectedEvent event) {
                LOG.trace("Duplicating selected items");
                getSelectionModel().getSelectedItems().forEach(i -> getItems().add(i.duplicate()));
        }

        @EventListener
        public void onClear(final ClearModuleEvent event) {
                getItems().forEach(d -> d.descriptor().releaseAll());
                getSelectionModel().clearSelection();
                getItems().clear();
        }

        @EventListener
        public void onRemoveSelected(RemoveSelectedEvent event) {
                SortedSet<Integer> indices = new TreeSet<>(Collections.reverseOrder());
                indices.addAll(getSelectionModel().getSelectedIndices());
                LOG.trace("Removing {} items", indices.size());
                indices.forEach(i -> getItems().remove(i.intValue()).invalidate());
                requestFocus();
        }

        @EventListener
        public void onMoveSelected(final MoveSelectedEvent event) {
                getSortOrder().clear();
                ObservableList<Integer> selectedIndices = getSelectionModel().getSelectedIndices();
                Integer[] selected = selectedIndices.toArray(new Integer[selectedIndices.size()]);
                int focus = getFocusModel().getFocusedIndex();
                getSelectionModel().clearSelection();
                SelectionAndFocus newSelection = event.getType().move(selected, getItems(), focus);
                if (!SelectionAndFocus.NULL.equals(newSelection)) {
                        LOG.trace("Changing selection to {}", newSelection);
                        getSelectionModel().selectIndices(newSelection.getRow(), newSelection.getRows());
                        getFocusModel().focus(newSelection.getFocus());
                        scrollTo(newSelection.getFocus());
                }
        }

        @EventListener
        public void onSetPageRanges(SetPageRangesRequest event) {
                getItems().stream().forEach(i -> i.pageSelection.set(event.range));
        }

        @EventListener
        public void showPasswordFieldPopup(ShowPasswordFieldPopupRequest request) {
                Scene scene = this.getScene();
                if (scene != null) {
                        Window owner = scene.getWindow();
                        if (owner != null && owner.isShowing()) {
                                Point2D nodeCoord = request.getRequestingNode().localToScene(request.getRequestingNode().getWidth() / 2,
                                        request.getRequestingNode().getHeight() / 1.5);
                                double anchorX = Math.round(owner.getX() + scene.getX() + nodeCoord.getX() + 2);
                                double anchorY = Math.round(owner.getY() + scene.getY() + nodeCoord.getY() + 2);
                                passwordPopup.showFor(this, request.getPdfDescriptor(), anchorX, anchorY);
                        }
                }
        }

        @Override
        public void saveStateTo(Map<String, String> data) {
                data.put(defaultString(getId()) + "input.size", Integer.toString(getItems().size()));
                IntStream.range(0, getItems().size()).forEach(i -> {
                        SelectionTableRowData current = getItems().get(i);
                        String id = defaultString(getId());
                        data.put(id + "input." + i, current.descriptor().getFile().getAbsolutePath());
                        if (new DefaultUserContext().isSavePwdInWorkspaceFile()) {
                                data.put(id + "input.password.enc" + i, encrypt(current.descriptor().getPassword()));
                        }
                        data.put(id + "input.range." + i, defaultString(current.pageSelection.get()));
                        data.put(id + "input.step." + i, defaultString(current.pace.get()));
                        data.put(id + "input.reverse." + i, Boolean.toString(current.reverse.get()));
                });
        }

        @Override
        public void restoreStateFrom(Map<String, String> data) {
                onClear(null);
                int size = Optional.ofNullable(data.get(defaultString(getId()) + "input.size")).map(Integer::valueOf).orElse(0);
                if (size > 0) {
                        PdfLoadRequestEvent loadEvent = new PdfLoadRequestEvent(getOwnerModule());
                        List<SelectionTableRowData> items = new ArrayList<>();
                        IntStream.range(0, size).forEach(i -> {
                                String id = defaultString(getId());
                                Optional.ofNullable(data.get(id + "input." + i)).ifPresent(f -> {
                                        PdfDocumentDescriptor descriptor = PdfDocumentDescriptor.newDescriptor(new File(f),
                                                ofNullable(data.get(id + "input.password.enc" + i)).map(EncryptionUtils::decrypt)
                                                        .orElseGet(() -> data.get(defaultString(getId()) + "input.password." + i)));
                                        loadEvent.add(descriptor);
                                        SelectionTableRowData row = new SelectionTableRowData(descriptor);
                                        row.pageSelection.set(data.get(id + "input.range." + i));
                                        row.pace.set(data.get(id + "input.step." + i));
                                        row.reverse.set(Boolean.valueOf(data.get(id + "input.reverse." + i)));
                                        items.add(row);
                                });
                        });
                        getItems().addAll(items);
                        eventStudio().broadcast(loadEvent);
                }

        }
}
