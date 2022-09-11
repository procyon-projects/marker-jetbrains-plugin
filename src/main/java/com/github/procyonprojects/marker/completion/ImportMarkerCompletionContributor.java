package com.github.procyonprojects.marker.completion;

import b.h.S;
import com.github.procyonprojects.marker.Icons;
import com.github.procyonprojects.marker.TargetInfo;
import com.github.procyonprojects.marker.Utils;
import com.github.procyonprojects.marker.comment.Comment;
import com.github.procyonprojects.marker.comment.ParseResult;
import com.github.procyonprojects.marker.comment.Parser;
import com.github.procyonprojects.marker.element.*;
import com.github.procyonprojects.marker.metadata.Target;
import com.github.procyonprojects.marker.metadata.provider.MetadataProvider;
import com.github.procyonprojects.marker.metadata.v1.Marker;
import com.intellij.codeInsight.completion.*;
import com.intellij.codeInsight.lookup.LookupElementBuilder;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.patterns.PatternCondition;
import com.intellij.patterns.PlatformPatterns;
import com.intellij.psi.PsiComment;
import com.intellij.psi.PsiElement;
import com.intellij.ui.JBColor;
import com.intellij.util.ProcessingContext;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public class ImportMarkerCompletionContributor extends CompletionContributor {

    private static final MetadataProvider METADATA_PROVIDER = ApplicationManager.getApplication().getService(MetadataProvider.class);

    private final static PatternCondition<PsiElement> IS_MARKER_COMMENT = new PatternCondition<>("") {
        @Override
        public boolean accepts(@NotNull PsiElement element, ProcessingContext context) {
            if (Utils.isMarkerCommentElement(element)) {
                return true;
            }

            return Utils.findMarkerCommentFirstLine(element).isPresent();
        }
    };

    public ImportMarkerCompletionContributor() {
        extend(CompletionType.BASIC, PlatformPatterns.psiElement().with(IS_MARKER_COMMENT), new CompletionProvider<>() {
            @Override
            protected void addCompletions(@NotNull CompletionParameters parameters, @NotNull ProcessingContext context, @NotNull CompletionResultSet result) {
                addImportMarkerCompletions(parameters, context, result);
                result.stopHere();
            }
        });
        extend(CompletionType.SMART, PlatformPatterns.psiElement().with(IS_MARKER_COMMENT), new CompletionProvider<>() {
            @Override
            protected void addCompletions(@NotNull CompletionParameters parameters, @NotNull ProcessingContext context, @NotNull CompletionResultSet result) {
                addImportMarkerCompletions(parameters, context, result);
                result.stopHere();
            }
        });
    }

    private void addImportMarkerCompletions(@NotNull CompletionParameters parameters, @NotNull ProcessingContext context, @NotNull CompletionResultSet result) {
        final TargetInfo targetInfo;
        Optional<Comment> comment = Utils.getMarkerComment(parameters.getOriginalPosition());
        if (comment.isEmpty()) {
            comment = Utils.findMarkerCommentFirstLine(parameters.getOriginalPosition());

            if (comment.isEmpty()) {
                return;
            }

            targetInfo = Utils.findTarget((PsiComment) comment.get().firstLine().get().getElement());
        } else {
            targetInfo = Utils.findTarget((PsiComment) Objects.requireNonNull(parameters.getOriginalPosition()));
        }

        if (targetInfo.getTarget() != Target.PACKAGE_LEVEL) {
            return;
        }

        Optional<Comment.Line> firstLine = comment.get().firstLine();

        if (!firstLine.get().getText().trim().startsWith("+import")) {
            return;
        }

        final Optional<Marker> marker = METADATA_PROVIDER.getImportMarker();

        final Parser parser = new Parser();
        final ParseResult parseResult = parser.parse(marker.get(), comment.get(), marker.get().getName());
        final MarkerElement markerElement = parseResult.getMarkerElement();
        Element current = markerElement.getNext();
        final Optional<String> pkg = getPackageName(markerElement);

        if (pkg.isEmpty()) {
            return;
        }

        while (current != null) {
            if (current instanceof ParameterElement) {
                final ParameterElement parameterElement = (ParameterElement) current;
                final Element nameElement = parameterElement.getName();
                final Element equalSignElement = parameterElement.getEqualSign();
                if (nameElement != null && !(equalSignElement instanceof ExpectedElement) && "Pkg".equals(nameElement.getText())) {
                    addPackageCompletions(parameterElement, parameters, context, result);
                } else if (pkg.isPresent() && nameElement != null && !(equalSignElement instanceof ExpectedElement) && "Value".equals(nameElement.getText())) {
                    addProcessorCommandCompletions(pkg.get(), parameterElement, parameters, context, result);
                } else if (pkg.isPresent() && nameElement == null && !(equalSignElement instanceof ExpectedElement)) {
                    addProcessorCommandCompletions(pkg.get(), parameterElement, parameters, context, result);
                }
            }
            current = current.getNext();
        }
    }

    private void addPackageCompletions(ParameterElement parameterElement, @NotNull CompletionParameters parameters, @NotNull ProcessingContext context, @NotNull CompletionResultSet result) {
        final List<LookupElementBuilder> elements = new ArrayList<>();
        final Element valueElement = parameterElement.getValue();

        if (valueElement instanceof StringElement) {
            StringElement stringElement = (StringElement) valueElement;
            String text = stringElement.getText();
            if (text.length() > 1 && (text.startsWith("\"") || !text.startsWith("'") || !text.startsWith("`"))) {
                if (stringElement.getRange().getStartOffset() < parameters.getOffset() && (parameterElement.getNext() == null || parameterElement.getNext().getRange().getStartOffset() > parameters.getOffset())) {
                    if ((text.endsWith("\"") || text.endsWith("\'") || text.endsWith("`"))) {
                        if (!(stringElement.getRange().getEndOffset() -1 < parameters.getOffset())) {
                            METADATA_PROVIDER.packages(parameters.getEditor().getProject()).forEach(pkg -> {
                                elements.add(getPackage(pkg, pkg));
                            });
                        }
                    }
                }
            }
        } else {
            METADATA_PROVIDER.packages(parameters.getEditor().getProject()).forEach(pkg -> {
                elements.add(getPackage(pkg, pkg));
            });
        }

        result.addAllElements(elements);
    }

    private void addProcessorCommandCompletions(String pkg, ParameterElement parameterElement, @NotNull CompletionParameters parameters, @NotNull ProcessingContext context, @NotNull CompletionResultSet result) {
        final List<LookupElementBuilder> elements = new ArrayList<>();
        final Element valueElement = parameterElement.getValue();

        if (valueElement instanceof StringElement) {
            StringElement stringElement = (StringElement) valueElement;
            String text = stringElement.getText();
            if (text.length() > 1 && (text.startsWith("\"") || !text.startsWith("'") || !text.startsWith("`"))) {
                if (stringElement.getRange().getStartOffset() < parameters.getOffset() && (parameterElement.getNext() == null || parameterElement.getNext().getRange().getStartOffset() > parameters.getOffset())) {
                    if ((text.endsWith("\"") || text.endsWith("\'") || text.endsWith("`"))) {
                        if (!(stringElement.getRange().getEndOffset() -1 < parameters.getOffset())) {
                            METADATA_PROVIDER.processorNames(parameters.getEditor().getProject(), Utils.unquote(pkg).trim()).forEach(name -> {
                                elements.add(getProcessor(name, name, Utils.unquote(pkg).trim()));
                            });
                        }
                    }
                }
            }
        } else if (parameterElement.getRange().getStartOffset() < parameters.getOffset() && (parameterElement.getNext() == null ||
                (parameterElement.getValue() instanceof ExpectedElement && parameterElement.getValue().getRange().getEndOffset() > parameters.getOffset()))) {
            METADATA_PROVIDER.processorNames(parameters.getEditor().getProject(), Utils.unquote(pkg).trim()).forEach(name -> {
                elements.add(getProcessor(name, name, Utils.unquote(pkg).trim()));
            });
        }

        result.addAllElements(elements);
    }

    private Optional<String> getPackageName(MarkerElement markerElement) {
        final Optional<Element> pkg = markerElement.getParameterValue("Pkg");
        if (pkg.isEmpty()) {
            return Optional.empty();
        }

        Element valueElement = pkg.get();
        if (valueElement instanceof StringElement) {
            String pkgName = valueElement.getText();
            if (StringUtils.isNotEmpty(pkgName)) {
                return Optional.of(pkgName);
            }
        }

        return Optional.empty();
    }

    private LookupElementBuilder getProcessor(String lookupString, String presentableText, String tailText) {
        return LookupElementBuilder.create(lookupString)
                .withPresentableText(presentableText)
                .withItemTextForeground(JBColor.DARK_GRAY)
                .bold()
                .withIcon(Icons.Processor)
                .withTailText(" " + tailText)
                .withTypeIconRightAligned(true);
    }

    private LookupElementBuilder getPackage(String lookupString, String presentableText) {
        return LookupElementBuilder.create(lookupString)
                .withPresentableText(presentableText)
                .withItemTextForeground(JBColor.DARK_GRAY)
                .bold()
                .withIcon(Icons.Package)
                .withTypeIconRightAligned(true);
    }
}