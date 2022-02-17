/*******************************************************************************
 * Copyright 2022 Amit Kumar Mondal
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
package com.osgifx.console.ui.configurations.dialog;

import static com.osgifx.console.agent.dto.XAttributeDefType.BOOLEAN;
import static com.osgifx.console.constants.FxConstants.STANDARD_CSS;

import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.function.UnaryOperator;
import java.util.regex.Pattern;

import javax.inject.Inject;

import org.controlsfx.control.ToggleSwitch;
import org.controlsfx.control.textfield.CustomPasswordField;
import org.controlsfx.control.textfield.CustomTextField;
import org.controlsfx.control.textfield.TextFields;
import org.controlsfx.dialog.LoginDialog;
import org.controlsfx.glyphfont.FontAwesome;
import org.controlsfx.glyphfont.Glyph;
import org.eclipse.fx.core.Triple;
import org.eclipse.fx.core.log.FluentLogger;
import org.eclipse.fx.core.log.Log;

import com.google.common.base.Strings;
import com.google.common.base.Supplier;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.osgifx.console.agent.dto.ConfigValue;
import com.osgifx.console.agent.dto.XAttributeDefType;
import com.osgifx.console.util.converter.ValueConverter;
import com.osgifx.console.util.fx.FxDialog;
import com.osgifx.console.util.fx.MultipleCardinalityPropertiesDialog;

import javafx.beans.value.ChangeListener;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar.ButtonData;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Dialog;
import javafx.scene.control.DialogPane;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.TextFormatter;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.StageStyle;

public final class ConfigurationCreateDialog extends Dialog<ConfigurationDTO> {

    @Log
    @Inject
    private FluentLogger         logger;
    private final ValueConverter converter = new ValueConverter();

    private final Map<PropertiesForm, Triple<Supplier<String>, Supplier<String>, Supplier<XAttributeDefType>>> configurationEntries = Maps
            .newHashMap();

    public void init() {
        final DialogPane dialogPane = getDialogPane();

        initStyle(StageStyle.UNDECORATED);
        dialogPane.setHeaderText("Create New Configuration for OSGi Configuration Admin");
        dialogPane.getStylesheets().add(LoginDialog.class.getResource("dialogs.css").toExternalForm());
        dialogPane.getStylesheets().add(getClass().getResource(STANDARD_CSS).toExternalForm());
        dialogPane.setGraphic(new ImageView(this.getClass().getResource("/graphic/images/configuration.png").toString()));
        dialogPane.getButtonTypes().addAll(ButtonType.CANCEL);

        final CustomTextField txtPid = (CustomTextField) TextFields.createClearableTextField();
        txtPid.setLeft(new ImageView(getClass().getResource("/graphic/icons/id.png").toExternalForm()));

        final CustomTextField txtFactoryPid = (CustomTextField) TextFields.createClearableTextField();
        txtFactoryPid.setLeft(new ImageView(getClass().getResource("/graphic/icons/id.png").toExternalForm()));

        final Label lbMessage = new Label("");
        lbMessage.getStyleClass().addAll("message-banner");
        lbMessage.setVisible(false);
        lbMessage.setManaged(false);

        final VBox content = new VBox(10);

        content.getChildren().add(lbMessage);
        content.getChildren().add(txtPid);
        content.getChildren().add(txtFactoryPid);
        addFieldPair(content);

        dialogPane.setContent(content);

        final ButtonType createButtonType = new ButtonType("Create", ButtonData.OK_DONE);
        dialogPane.getButtonTypes().addAll(createButtonType);

        final Button createButton = (Button) dialogPane.lookupButton(createButtonType);
        createButton.setOnAction(actionEvent -> {
            try {
                lbMessage.setVisible(false);
                lbMessage.setManaged(false);
                hide();
            } catch (final Exception ex) {
                lbMessage.setVisible(true);
                lbMessage.setManaged(true);
                lbMessage.setText(ex.getMessage());
                FxDialog.showExceptionDialog(ex, getClass().getClassLoader());
            }
        });
        final String pidCaption        = "Configuration PID";
        final String factoryPidCaption = "Factory PID";

        txtPid.setPromptText(pidCaption);
        txtFactoryPid.setPromptText(factoryPidCaption);

        setResultConverter(dialogButton -> {
            final ButtonData data = dialogButton == null ? null : dialogButton.getButtonData();
            try {
                return data == ButtonData.OK_DONE ? getInput(txtPid, txtFactoryPid) : null;
            } catch (final Exception e) {
                logger.atError().withException(e).log("Configuration values cannot be converted");
            }
            return null;
        });
    }

    private ConfigurationDTO getInput(final CustomTextField txtPid, final CustomTextField txtFactoryPid) throws Exception {
        final ConfigurationDTO config = new ConfigurationDTO();

        config.pid        = txtPid.getText();
        config.factoryPid = txtFactoryPid.getText();

        final List<ConfigValue> properties = Lists.newArrayList();
        for (final Entry<PropertiesForm, Triple<Supplier<String>, Supplier<String>, Supplier<XAttributeDefType>>> entry : configurationEntries
                .entrySet()) {
            final Triple<Supplier<String>, Supplier<String>, Supplier<XAttributeDefType>> value       = entry.getValue();
            final String                                                                  configKey   = value.value1.get();
            final String                                                                  configValue = value.value2.get();
            XAttributeDefType                                                             configType  = value.value3.get();
            if (Strings.isNullOrEmpty(configKey) || Strings.isNullOrEmpty(configValue)) {
                continue;
            }
            if (configType == null) {
                configType = XAttributeDefType.STRING;
            }
            final Object convertedValue = converter.convert(configValue, configType);
            properties.add(ConfigValue.create(configKey, convertedValue, configType));
        }
        config.properties = properties;
        return config;
    }

    private class PropertiesForm extends HBox {

        private final CustomTextField txtKey;
        private Node                  node;

        private final Button btnAddField;
        private final Button btnRemoveField;

        public PropertiesForm(final VBox parent) {
            setAlignment(Pos.CENTER_LEFT);
            setSpacing(5);

            final String keyCaption = "Key";

            txtKey = (CustomTextField) TextFields.createClearableTextField();
            txtKey.setLeft(new ImageView(getClass().getResource("/graphic/icons/kv.png").toExternalForm()));

            txtKey.setPromptText(keyCaption);

            btnAddField    = new Button();
            btnRemoveField = new Button();

            final ObservableList<XAttributeDefType> options  = FXCollections.observableArrayList(XAttributeDefType.values());
            final ComboBox<XAttributeDefType>       comboBox = new ComboBox<>(options);

            final XAttributeDefType type = comboBox.getValue();
            node = getFieldByType(type == null ? XAttributeDefType.STRING : type);

            comboBox.getSelectionModel().selectedItemProperty().addListener((opt, oldValue, newValue) -> {
                final Class<?> clazz   = XAttributeDefType.clazz(newValue);
                final Node     newNode = getFieldByType(newValue);
                newNode.setOnMouseClicked(e -> {
                    // multiple cardinality
                    if (clazz == null) {
                        final MultipleCardinalityPropertiesDialog dialog = new MultipleCardinalityPropertiesDialog();
                        final String                              key    = txtKey.getText();
                        final TextField                           field  = (TextField) node;

                        dialog.init(key, newValue, field.getText(), getClass().getClassLoader());
                        final Optional<String> entries = dialog.showAndWait();
                        entries.ifPresent(field::setText);
                    }
                });
                node = newNode;
                final ObservableList<Node> children = getChildren();
                if (!children.isEmpty()) {
                    children.set(1, newNode);
                }
            });

            comboBox.getSelectionModel().select(0); // default STRING type

            btnAddField.setGraphic(new Glyph("FontAwesome", FontAwesome.Glyph.PLUS));
            btnRemoveField.setGraphic(new Glyph("FontAwesome", FontAwesome.Glyph.MINUS));

            btnAddField.setOnAction(e -> addFieldPair(parent));
            btnRemoveField.setOnAction(e -> removeFieldPair(parent, this));

            getChildren().addAll(txtKey, node, comboBox, btnAddField, btnRemoveField);

            final Triple<Supplier<String>, Supplier<String>, Supplier<XAttributeDefType>> tuple = new Triple<>(txtKey::getText,
                    () -> getValue(node), comboBox::getValue);
            configurationEntries.put(this, tuple);
        }

        private Node getFieldByType(final XAttributeDefType type) {
            final CustomTextField txtField;
            if (type != BOOLEAN) {
                txtField = (CustomTextField) TextFields.createClearableTextField();
                txtField.setLeft(new ImageView(getClass().getResource("/graphic/icons/kv.png").toExternalForm()));
            } else {
                txtField = null;
            }
            switch (type) {
                case LONG, INTEGER:
                    final String captionAsInt = switch (type) {
                        case LONG -> "Long Number";
                        case INTEGER -> "Integer Number";
                        default -> null;
                    };
                    txtField.setPromptText(captionAsInt);
                    txtField.textProperty().addListener((ChangeListener<String>) (observable, oldValue, newValue) -> {
                        if (!newValue.matches("\\d*")) {
                            txtField.setText(newValue.replaceAll("[^\\d]", ""));
                        }
                    });
                    break;
                case BOOLEAN:
                    return new ToggleSwitch();
                case DOUBLE, FLOAT:
                    final String captionAsDouble = "Decimal Number";
                    txtField.setPromptText(captionAsDouble);
                    final Pattern pattern = Pattern.compile("\\d*|\\d+\\.\\d*");
                    final TextFormatter<?> doubleFormatter = new TextFormatter<>(
                            (UnaryOperator<TextFormatter.Change>) change -> (pattern.matcher(change.getControlNewText()).matches() ? change
                                    : null));
                    txtField.setTextFormatter(doubleFormatter);
                    break;
                case CHAR:
                    final String valueCaptionAsChar = "Character Value";
                    txtField.setPromptText(valueCaptionAsChar);
                    final TextFormatter<?> charFormatter = new TextFormatter<>(
                            (UnaryOperator<TextFormatter.Change>) change -> (change.getControlNewText().length() == 1 ? change : null));
                    txtField.setTextFormatter(charFormatter);
                    break;
                case STRING:
                    final String valueCaptionAsStr = "String Value";
                    txtField.setPromptText(valueCaptionAsStr);
                    break;
                case PASSWORD:
                    final String valueCaptionAsPwd = "Password Value";
                    final CustomPasswordField pwdField = (CustomPasswordField) TextFields.createClearablePasswordField();
                    pwdField.setLeft(new ImageView(getClass().getResource("/graphic/icons/kv.png").toExternalForm()));
                    pwdField.setPromptText(valueCaptionAsPwd);
                    return pwdField;
                case STRING_ARRAY, STRING_LIST, INTEGER_ARRAY, INTEGER_LIST, BOOLEAN_ARRAY, BOOLEAN_LIST, DOUBLE_ARRAY, DOUBLE_LIST, FLOAT_ARRAY, FLOAT_LIST, CHAR_ARRAY, CHAR_LIST, LONG_ARRAY, LONG_LIST:
                    final String valueCaptionAsMultipleCardinality = "Multiple Cardinality Value";
                    txtField.setPromptText(valueCaptionAsMultipleCardinality);
                    break;
            }
            return txtField;
        }
    }

    private void addFieldPair(final VBox content) {
        content.getChildren().add(new PropertiesForm(content));
        getDialogPane().getScene().getWindow().sizeToScene();
    }

    private void removeFieldPair(final VBox content, final PropertiesForm form) {
        if (content.getChildren().size() > 4) {
            content.getChildren().remove(form);
            getDialogPane().getScene().getWindow().sizeToScene();
        }
        configurationEntries.remove(form);
    }

    private String getValue(final Node node) {
        if (node instanceof final TextField tf) {
            return tf.getText();
        }
        if (node instanceof final ToggleSwitch ts) {
            return String.valueOf(ts.isSelected());
        }
        return null;
    }

}
