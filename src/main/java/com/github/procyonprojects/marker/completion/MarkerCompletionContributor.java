package com.github.procyonprojects.marker.completion;

import com.github.procyonprojects.marker.model.Marker;
import com.github.procyonprojects.marker.provider.MarkerMetadataProvider;
import com.goide.GoParserDefinition;
import com.goide.psi.GoPackageClause;
import com.intellij.codeInsight.completion.*;
import com.intellij.codeInsight.lookup.LookupElementBuilder;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.patterns.PatternCondition;
import com.intellij.patterns.PlatformPatterns;
import com.intellij.psi.PsiComment;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiWhiteSpace;
import com.intellij.ui.JBColor;
import com.intellij.util.ProcessingContext;
import org.jetbrains.annotations.NotNull;

import java.util.*;

public class MarkerCompletionContributor extends CompletionContributor {

    private static final MarkerMetadataProvider markerMetadataProvider = ApplicationManager.getApplication().getService(MarkerMetadataProvider.class);

    private final static PatternCondition<PsiElement> IS_MARKER_COMMENT = new PatternCondition<>("") {
        @Override
        public boolean accepts(@NotNull PsiElement psiElement, ProcessingContext context) {
            if (psiElement instanceof PsiComment) {
                return isMarkerComment((PsiComment) psiElement);
            }
            return false;
        }
    };

    private final static Map<String, String> markers = Map.of("+accessor", "+accessor", "+accessor:mapping", "+accessor:mapping");

    public MarkerCompletionContributor() {
        extend(CompletionType.BASIC, PlatformPatterns.psiElement().with(IS_MARKER_COMMENT), new CompletionProvider<>() {
            @Override
            protected void addCompletions(@NotNull CompletionParameters parameters, @NotNull ProcessingContext context, @NotNull CompletionResultSet result) {
                PsiElement associatedElement = findAssociatedElement(Objects.requireNonNull(parameters.getOriginalPosition()));

                Collection<Marker> values = new HashSet<>();
                if (associatedElement instanceof GoPackageClause) {
                    values = markerMetadataProvider.packageLevelMap.values();
                }
                List<LookupElementBuilder> elements = new ArrayList<>();

                String markerComment = parameters.getOriginalPosition().getText().substring(2).trim();

                // +accessor:mappingdIntellijIdeaRulezzz
                values.forEach(marker -> {

                    if (marker.getName().startsWith(markerComment)) {
                        if (!markerComment.contains(":")) {
                            elements.add(LookupElementBuilder.create(marker.getName().substring(1))
                                    .withPresentableText(marker.getName())
                                    .withItemTextForeground(JBColor.DARK_GRAY)
                                    .bold()
                                    //.withIcon(PlatformIcons.ADD_ICON)
                                    //.withTailText(" Tail Text")
                                    //.withTypeText("Type Name")
                                    .withTypeIconRightAligned(true));
                        } else {
                            int lastIndex = markerComment.lastIndexOf(":");
                            elements.add(LookupElementBuilder.create(marker.getName().substring(lastIndex + 1))
                                    .withPresentableText(marker.getName())
                                    .withItemTextForeground(JBColor.DARK_GRAY)
                                    .bold()
                                    //.withIcon(PlatformIcons.ADD_ICON)
                                    //.withTailText(" Tail Text")
                                    //.withTypeText("Type Name")
                                    .withTypeIconRightAligned(true));
                        }
                    }
                });
                result.addAllElements(elements);
            }
        });
    }

    @Override
    public void fillCompletionVariants(@NotNull CompletionParameters parameters, @NotNull CompletionResultSet result) {
        super.fillCompletionVariants(parameters, result);
    }

    public static boolean isMarkerComment(PsiComment comment) {
        if (comment == null) {
            return false;
        }

        if (comment.getTokenType() != GoParserDefinition.Lazy.LINE_COMMENT) {
            return false;
        }

        String markerComment = comment.getText().substring(2).trim();

        if (markerComment.length() < 1 || markerComment.charAt(0) != '+') {
            return false;
        }

        int commentIndex = 0;
        boolean seenWhiteSpace = false;

        PsiElement goElement = comment.getNextSibling();
        while (goElement != null) {
            if (commentIndex > 1) {
                break;
            }

            if (goElement instanceof PsiWhiteSpace && !goElement.getText().equals("\n")) {
                seenWhiteSpace = true;
            } else if(seenWhiteSpace && goElement instanceof PsiComment) {
                seenWhiteSpace = false;
                commentIndex++;
            } else if (!(goElement instanceof PsiWhiteSpace || goElement instanceof PsiComment)) {
                break;
            }
            goElement = goElement.getNextSibling();
        }

        return commentIndex <= 1;
    }

    public PsiElement findAssociatedElement(PsiElement comment) {
        PsiElement goElement = comment.getNextSibling();
        while (goElement != null) {
            if (!(goElement instanceof PsiWhiteSpace || goElement instanceof PsiComment)) {
                break;
            }
            goElement = goElement.getNextSibling();
        }

        return goElement;
    }
}
