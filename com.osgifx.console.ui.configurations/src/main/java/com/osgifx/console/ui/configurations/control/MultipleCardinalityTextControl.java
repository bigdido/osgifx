/*******************************************************************************
 * Copyright 2021-2023 Amit Kumar Mondal
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License.  You may obtain a copy
 * of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the
 * License for the specific language governing permissions and limitations under
 * the License.
 ******************************************************************************/
package com.osgifx.console.ui.configurations.control;

import org.apache.commons.lang3.StringUtils;

import com.dlsc.formsfx.model.structure.StringField;
import com.dlsc.formsfx.view.controls.SimpleControl;
import com.google.common.base.Joiner;
import com.google.common.base.Splitter;
import com.osgifx.console.agent.dto.XAttributeDefType;
import com.osgifx.console.util.fx.MultipleCardinalityPropertiesDialog;

import javafx.beans.binding.Bindings;
import javafx.geometry.Pos;
import javafx.geometry.VPos;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.StackPane;

public final class MultipleCardinalityTextControl extends SimpleControl<StringField> {

    /**
     * This StackPane is needed for achieving the readonly effect by putting the
     * readOnlyLabel over the editableField on the change of the visibleProperty.
     */
    protected StackPane stack;

    /**
     * The fieldLabel is the container that displays the label property of the
     * field. - The editableField allows users to modify the field's value. - The
     * readOnlyLabel displays the field's value if it is not editable.
     */
    protected TextField editableField;
    protected TextArea  editableArea;
    protected Label     readOnlyLabel;
    protected Label     fieldLabel;

    private final String            key;
    private final XAttributeDefType type;
    private final ClassLoader       classLoader;
    private final int               unsignedCardinality;

    public MultipleCardinalityTextControl(final String key,
                                          final XAttributeDefType type,
                                          final int unsignedCardinality) {
        this.key                 = key;
        this.type                = type;
        classLoader              = getClass().getClassLoader();
        this.unsignedCardinality = unsignedCardinality;
    }

    @Override
    public void initializeParts() {
        super.initializeParts();

        getStyleClass().add("simple-text-control");

        stack = new StackPane();

        editableField = new TextField(field.getValue());
        editableArea  = new TextArea(field.getValue());

        editableArea.setEditable(false);
        editableArea.setFocusTraversable(false);

        readOnlyLabel = new Label(field.getValue());
        fieldLabel    = new Label(field.labelProperty().getValue());
        editableField.setPromptText(field.placeholderProperty().getValue());

        editableArea.setOnMouseClicked(event -> {
            final var dialog = new MultipleCardinalityPropertiesDialog();
            if (StringUtils.isNotBlank(key)) {
                final var currentValue         = field.getValue();
                final var splitByLineSeparator = Splitter.on(System.lineSeparator()).splitToList(currentValue);
                final var joinedValue          = Joiner.on(",").join(splitByLineSeparator);
                dialog.init(key.strip(), type, unsignedCardinality, joinedValue, classLoader);

                final var entries = dialog.showAndWait();
                if (entries.isPresent()) {
                    final var list = Splitter.on(",").splitToList(entries.get());
                    editableArea.setText(Joiner.on(System.lineSeparator()).join(list));
                }
            }
        });
    }

    @Override
    public void layoutParts() {
        super.layoutParts();

        readOnlyLabel.getStyleClass().add("read-only-label");
        readOnlyLabel.setPrefHeight(26);

        editableArea.getStyleClass().add("simple-textarea");
        editableArea.setPrefRowCount(5);
        editableArea.setPrefHeight(80);
        editableArea.setWrapText(true);

        if (field.isMultiline()) {
            stack.setPrefHeight(80);
            readOnlyLabel.setPrefHeight(80);
        }

        stack.getChildren().addAll(editableField, editableArea, readOnlyLabel);
        stack.setAlignment(Pos.CENTER_LEFT);

        final var labelDescription = field.getLabelDescription();
        final var valueDescription = field.getValueDescription();

        final var columns = field.getSpan();

        if (columns < 3) {
            var rowIndex = 0;
            add(fieldLabel, 0, rowIndex, columns, 1);
            rowIndex++;
            if (labelDescription != null) {
                GridPane.setValignment(labelDescription, VPos.TOP);
                add(labelDescription, 0, rowIndex, columns, 1);
                rowIndex++;
            }
            add(stack, 0, rowIndex, columns, 1);
            rowIndex++;
            if (valueDescription != null) {
                GridPane.setValignment(valueDescription, VPos.TOP);
                add(valueDescription, 0, rowIndex, columns, 1);
            }
        } else {
            add(fieldLabel, 0, 0, 2, 1);
            if (labelDescription != null) {
                GridPane.setValignment(labelDescription, VPos.TOP);
                add(labelDescription, 0, 1, 2, 1);
            }
            add(stack, 2, 0, columns - 2, 1);
            if (valueDescription != null) {
                GridPane.setValignment(valueDescription, VPos.TOP);
                add(valueDescription, 2, 1, columns - 2, 1);
            }
        }
    }

    @Override
    public void setupBindings() {
        super.setupBindings();

        editableArea.visibleProperty().bind(Bindings.and(field.editableProperty(), field.multilineProperty()));
        editableField.visibleProperty().bind(Bindings.and(field.editableProperty(), field.multilineProperty().not()));
        readOnlyLabel.visibleProperty().bind(field.editableProperty().not());

        editableField.textProperty().bindBidirectional(field.userInputProperty());
        editableArea.textProperty().bindBidirectional(field.userInputProperty());
        readOnlyLabel.textProperty().bind(field.userInputProperty());
        fieldLabel.textProperty().bind(field.labelProperty());
        editableField.promptTextProperty().bind(field.placeholderProperty());
        editableArea.promptTextProperty().bind(field.placeholderProperty());

        editableArea.managedProperty().bind(editableArea.visibleProperty());
        editableField.managedProperty().bind(editableField.visibleProperty());
    }

    @Override
    public void setupValueChangedListeners() {
        super.setupValueChangedListeners();

        field.multilineProperty().addListener((observable, oldValue, newValue) -> {
            stack.setPrefHeight(newValue ? 80 : 0);
            readOnlyLabel.setPrefHeight(newValue ? 80 : 26);
        });

        field.errorMessagesProperty().addListener(
                (observable, oldValue, newValue) -> toggleTooltip(field.isMultiline() ? editableArea : editableField));

        editableField.focusedProperty().addListener((observable, oldValue, newValue) -> toggleTooltip(editableField));
        editableArea.focusedProperty().addListener((observable, oldValue, newValue) -> toggleTooltip(editableArea));
    }

}
