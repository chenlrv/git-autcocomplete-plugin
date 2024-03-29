package co.chen.plugin;

import co.chen.plugin.Terminal.GoodWindowExec;
import com.intellij.codeInsight.completion.CompletionResultSet;
import com.intellij.codeInsight.lookup.LookupElementBuilder;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonShortcuts;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.editor.SpellCheckingEditorCustomizationProvider;
import com.intellij.openapi.fileTypes.FileTypes;
import com.intellij.openapi.keymap.KeymapUtil;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.popup.ComponentPopupBuilder;
import com.intellij.openapi.ui.popup.JBPopup;
import com.intellij.openapi.ui.popup.JBPopupFactory;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.ui.EditorCustomization;
import com.intellij.ui.EditorTextField;
import com.intellij.ui.EditorTextFieldProvider;
import com.intellij.ui.SoftWrapsEditorCustomization;
import com.intellij.util.Function;
import com.intellij.util.TextFieldCompletionProviderDumbAware;
import com.intellij.util.containers.ContainerUtil;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import javax.swing.border.CompoundBorder;
import java.awt.*;
import java.util.Collection;
import java.util.List;

class MultilinePopupBuilder extends AnAction{

    private static final char[] SEPARATORS = { ',', '|', '\n' };

    @NotNull
    private final EditorTextField myTextField;

    MultilinePopupBuilder(@NotNull Project project,
                          @NotNull final Collection<String> values,
                          @NotNull String initialValue,
                          boolean supportsNegativeValues) {
        myTextField = createTextField(project);
        new MyCompletionProvider(values, supportsNegativeValues).apply(myTextField);
        myTextField.setText(initialValue);


    }

    @NotNull
    private static EditorTextField createTextField(@NotNull Project project) {
        final EditorTextFieldProvider service = ServiceManager.getService(project, EditorTextFieldProvider.class);
        List<EditorCustomization> features = ContainerUtil.packNullables(SoftWrapsEditorCustomization.ENABLED,
                SpellCheckingEditorCustomizationProvider.getInstance().getDisabledCustomization());
        EditorTextField textField = service.getEditorField(FileTypes.PLAIN_TEXT.getLanguage(), project, features);
        textField.setBorder(new CompoundBorder(BorderFactory.createEmptyBorder(2, 2, 2, 2), textField.getBorder()));
        textField.setOneLineMode(false);
        return textField;
    }

    @NotNull
    JBPopup createPopup() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.add(myTextField, BorderLayout.CENTER);
        ComponentPopupBuilder builder = JBPopupFactory.getInstance().createComponentPopupBuilder(panel, myTextField)
                .setCancelOnClickOutside(true)
                .setAdText(KeymapUtil.getShortcutsText(CommonShortcuts.CTRL_ENTER.getShortcuts()) + " to finish")
                .setRequestFocus(true)
                .setResizable(true)
                .setMayBeParent(true);

        final JBPopup popup = builder.createPopup();
        popup.setMinimumSize(new Dimension(200, 90));
        AnAction okAction = new DumbAwareAction() {
            @Override
            public void actionPerformed(@NotNull AnActionEvent e) {
                unregisterCustomShortcutSet(popup.getContent());
                popup.closeOk(e.getInputEvent());
            }
        };
        okAction.registerCustomShortcutSet(CommonShortcuts.CTRL_ENTER, popup.getContent());
        return popup;
    }

    @NotNull
    Collection<String> getSelectedValues() {
        return ContainerUtil.mapNotNull(StringUtil.tokenize(myTextField.getText(), new String(SEPARATORS)), new Function<String, String>() {
            @Override
            public String fun(String value) {
                String trimmed = value.trim();
                return trimmed.isEmpty() ? null : trimmed;
            }
        });
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        JBPopup popup = createPopup();
        popup.showInFocusCenter();
    }

    @Override
    public boolean isDumbAware() {
        return false;
    }

    private static class MyCompletionProvider extends TextFieldCompletionProviderDumbAware {

        @NotNull private final Collection<String> myValues;
        private final boolean mySupportsNegativeValues;

        MyCompletionProvider(@NotNull Collection<String> values, boolean supportsNegativeValues) {
            super(true);
            myValues = values;
            mySupportsNegativeValues = supportsNegativeValues;
        }

        @NotNull
        @Override
        protected String getPrefix(@NotNull String currentTextPrefix) {
            final int separatorPosition = lastSeparatorPosition(currentTextPrefix);
            String prefix = separatorPosition == -1 ? currentTextPrefix : currentTextPrefix.substring(separatorPosition + 1).trim();
            return mySupportsNegativeValues && prefix.startsWith("-") ? prefix.substring(1) : prefix;
        }

        private static int lastSeparatorPosition(@NotNull String text) {
            int lastPosition = -1;
            for (char separator : SEPARATORS) {
                int lio = text.lastIndexOf(separator);
                if (lio > lastPosition) {
                    lastPosition = lio;
                }
            }
            return lastPosition;
        }

        @SuppressWarnings("StringToUpperCaseOrToLowerCaseWithoutLocale")
        @Override
        protected void addCompletionVariants(@NotNull String text, int offset, @NotNull String prefix,
                                             @NotNull CompletionResultSet result) {
            result.addLookupAdvertisement("Select one or more users separated with comma, | or new lines");
            for (String completionVariant : myValues) {
                final LookupElementBuilder element = LookupElementBuilder.create(completionVariant);
                result.addElement(element.withLookupString(completionVariant.toLowerCase()));
            }
        }
    }
}