package co.chen.plugin;

import com.intellij.codeInsight.completion.CompletionResultSet;
import com.intellij.codeInsight.lookup.LookupElementBuilder;
import com.intellij.dvcs.repo.Repository;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.fileTypes.FileTypes;
import com.intellij.openapi.project.Project;
import com.intellij.spellchecker.ui.SpellCheckingEditorCustomization;
import com.intellij.ui.EditorCustomization;
import com.intellij.ui.EditorTextField;
import com.intellij.ui.EditorTextFieldProvider;
import com.intellij.ui.SoftWrapsEditorCustomization;
import com.intellij.util.TextFieldCompletionProvider;
import git4idea.GitLocalBranch;
import git4idea.GitUtil;
import git4idea.branch.GitBranchesCollection;
import git4idea.repo.GitRepository;
import git4idea.repo.GitRepositoryManager;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

public class MultiValueAutoComplete {
    private static final char[] SEPARATORS = {','};


    public static EditorTextField create(Project project) {
        List<EditorCustomization> customizations =
                Arrays.<EditorCustomization>asList(SoftWrapsEditorCustomization.ENABLED, SpellCheckingEditorCustomization.DISABLED);
        EditorTextField editorField = ServiceManager.getService(project, EditorTextFieldProvider.class)
                .getEditorField(FileTypes.PLAIN_TEXT.getLanguage(), project, customizations);
        new CommaSeparatedTextFieldCompletion(project).apply(editorField);
        return editorField;

    }

    private static class CommaSeparatedTextFieldCompletion extends TextFieldCompletionProvider {

        private Project project;

        CommaSeparatedTextFieldCompletion(Project project) {
            this.project = project;
        }

        @NotNull
        @Override
        protected String getPrefix(@NotNull String currentTextPrefix) {
            final int separatorPosition = lastSeparatorPosition(currentTextPrefix);
            return separatorPosition == -1 ? currentTextPrefix : currentTextPrefix.substring(separatorPosition + 1).trim();
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

        @Override
        protected void addCompletionVariants(@NotNull String text, int offset, @NotNull String prefix,
                                             @NotNull CompletionResultSet result) {

            result.addLookupAdvertisement("Select one or more users separated with comma, | or new lines");

            List<String> values = getAutocompleteValues();
            //   Arrays.asList("git commit -m", "check");

            for (String completionVariant : values) {
                final LookupElementBuilder element = LookupElementBuilder.create(completionVariant);
                result.addElement(element.withLookupString(completionVariant.toLowerCase()));
            }

        }

        private List<String> getAutocompleteValues(){
            List<String> values = new LinkedList<>();

            List<String> localBranches = getLocalBranches(project);
            values.add("git commit -m");
            values.add("git add --all");
            values.addAll(createMergeBranchValues(localBranches));
            values.addAll(createCheckoutValues(localBranches));
            return values;

        }

        private List<String> createMergeBranchValues(List<String> localBranches) {
            List<String> mergeBranchValues = new LinkedList<>();
            String mergeCommand = "git merge branch ";
            localBranches.forEach(branch -> mergeBranchValues.add(mergeCommand.concat(branch)));
            return mergeBranchValues;
        }

        private List<String> createCheckoutValues(List<String> localBranches) {
            List<String> checkoutValues = new LinkedList<>();
            String mergeCommand = "git checkout branch ";
            localBranches.forEach(branch -> checkoutValues.add(mergeCommand.concat(branch)));
            return checkoutValues;
        }

        private List<String> getLocalBranches(Project project) {
            List<String> localBranchesNames = new LinkedList<>();

            GitRepositoryManager manager = GitUtil.getRepositoryManager(project);
            List<GitRepository> repositories = manager.getRepositories();

            for (GitRepository repo : repositories) {
                repo.getBranches().getLocalBranches().forEach(b -> localBranchesNames.add(b.getName()));
            }

            return localBranchesNames;
        }
    }


}