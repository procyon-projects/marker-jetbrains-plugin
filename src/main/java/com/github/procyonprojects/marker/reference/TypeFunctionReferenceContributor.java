package com.github.procyonprojects.marker.reference;

import com.github.procyonprojects.marker.TargetInfo;
import com.github.procyonprojects.marker.Utils;
import com.github.procyonprojects.marker.comment.Comment;
import com.github.procyonprojects.marker.comment.ParseResult;
import com.github.procyonprojects.marker.comment.Parser;
import com.github.procyonprojects.marker.element.*;
import com.github.procyonprojects.marker.metadata.Target;
import com.github.procyonprojects.marker.metadata.Type;
import com.github.procyonprojects.marker.metadata.provider.MetadataProvider;
import com.github.procyonprojects.marker.metadata.v1.Marker;
import com.goide.psi.impl.GoPackageClauseImpl;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.TextRange;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.patterns.PatternCondition;
import com.intellij.patterns.PlatformPatterns;
import com.intellij.psi.*;
import com.intellij.util.ArrayUtil;
import com.intellij.util.ProcessingContext;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public class TypeFunctionReferenceContributor extends PsiReferenceContributor {

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


    @Override
    public void registerReferenceProviders(@NotNull PsiReferenceRegistrar registrar) {
        registrar.registerReferenceProvider(PlatformPatterns.psiElement().with(IS_MARKER_COMMENT), new PsiReferenceProvider() {
            @Override
            public PsiReference @NotNull [] getReferencesByElement(@NotNull PsiElement element, @NotNull ProcessingContext context) {
                if (element instanceof PsiComment) {
                    return getReferences(element);
                }
                return new PsiReference[0];
            }
        });
    }

    private PsiReference[] getReferences(PsiElement element) {
        final TargetInfo targetInfo = Utils.findTarget((PsiComment) element);
        if (targetInfo.getTarget() == Target.INVALID) {
            return PsiReference.EMPTY_ARRAY;
        }

        Optional<Comment> comment = Utils.getMarkerComment(element);

        if (comment.isEmpty()) {
            comment = Utils.findMarkerCommentFirstLine(element);

            if (comment.isEmpty()) {
                return PsiReference.EMPTY_ARRAY;
            }
        }

        final Comment.Line firstLine = comment.get().firstLine().get();
        final String firstLineText = firstLine.getText().trim();
        final String anonymousName = Utils.getMarkerAnonymousName(firstLineText);
        final String markerName = Utils.getMarkerName(firstLineText);

        Project project = element.getProject();
        VirtualFile file = element.getContainingFile().getVirtualFile();

        Optional<Marker> marker = METADATA_PROVIDER.findMarker(project, file, anonymousName, targetInfo.getTarget());
        String aliasName;

        if (marker.isEmpty()) {
            marker = METADATA_PROVIDER.findMarker(project, file, markerName, targetInfo.getTarget());
            if (marker.isEmpty()) {
                return PsiReference.EMPTY_ARRAY;
            }
            aliasName = markerName;
        } else {
            aliasName = anonymousName;
        }

        final Parser parser = new Parser();
        final ParseResult parseResult = parser.parse(marker.get(), comment.get(), aliasName);
        List<PsiReference> referenceList = new ArrayList<>();
        if (parseResult != null) {
            MarkerElement markerElement = parseResult.getMarkerElement();
            for (ParameterElement parameterElement : markerElement.getParameterElements()) {
                if (parameterElement.getTypeInfo() != null) {
                    Type type = parameterElement.getTypeInfo().getActualType();
                    if (Type.GoFunction == type || Type.GoType == type) {
                        Element valueElement = parameterElement.getValue();

                        if (valueElement instanceof TypeElement || valueElement instanceof FuncElement) {
                            ImportReference importReference = null;
                            FileReference fileReference = null;
                            String[] parts = valueElement.getText().split("\\.");
                            String pkgName = parts[0];
                            String elementName = parts[0];

                            if (parts.length == 3 && element.getTextRange().contains(valueElement.getRange())) {
                                TextRange importRange = new TextRange(valueElement.getRange().getStartOffset(), valueElement.getRange().getStartOffset() + pkgName.length());
                                importReference = new ImportReference(element, importRange, parts[0]);
                                elementName = parts[1];

                                TextRange elementRange = new TextRange(valueElement.getRange().getStartOffset() + pkgName.length() + 1,
                                        valueElement.getRange().getStartOffset() + pkgName.length() + elementName.length() + 1);
                                fileReference = new FileReference(element, elementRange, pkgName, elementName);
                            } else if (element.getTextRange().contains(valueElement.getRange())) {
                                TextRange elementRange = new TextRange(valueElement.getRange().getStartOffset(), valueElement.getRange().getStartOffset() + pkgName.length());
                                fileReference = new FileReference(element, elementRange, pkgName, elementName);
                            }

                            if (importReference != null) {
                                referenceList.add(importReference);
                            }

                            if (fileReference != null) {
                                referenceList.add(fileReference);
                            }
                        }
                    }
                }
            }
        }

        return referenceList.toArray(PsiReference[]::new);
    }

    private static class MarkerProcessorReference extends PsiReferenceBase {

        private final PsiElement element;
        private final MarkerElement markerElement;

        public MarkerProcessorReference(@NotNull PsiElement element, MarkerElement markerElement) {
            super(element, markerElement.getRange());
            this.element = element;
            this.markerElement = markerElement;
        }

        @Override
        public @NotNull TextRange getRangeInElement() {
            int startOffset = markerElement.getRange().getStartOffset() - element.getTextRange().getStartOffset();
            int endOffset = markerElement.getRange().getEndOffset() - element.getTextRange().getStartOffset();

            String name = markerElement.getMarker().getName();
            int indexOfSemiColon = name.indexOf(":");
            if (indexOfSemiColon != -1) {
                endOffset -= name.substring(indexOfSemiColon).length();
            }

            return new TextRange(startOffset, endOffset);
        }

        @Override
        public @Nullable PsiElement resolve() {
            PsiElement current = element.getContainingFile().getFirstChild();
            List<PsiComment> importComments = new ArrayList<>();
            while (current != null) {
                if (current instanceof GoPackageClauseImpl) {
                    break;
                }

                if (Utils.isMarkerCommentElement(current)) {
                    importComments.add((PsiComment) current);
                }
                current = current.getNextSibling();
            }

            Map<PsiElement, MarkerElement> elements = parseComments(importComments);
            for (Map.Entry<PsiElement, MarkerElement> entry : elements.entrySet()) {
                Optional<Element> pkg = entry.getValue().getParameterValue("Pkg");
                if (pkg.isEmpty()
                        || !(pkg.get() instanceof StringElement)
                        || StringUtils.isBlank(Utils.unquote(pkg.get().getText()).trim())) {
                    continue;
                }

                if (!METADATA_PROVIDER.packageExists(element.getProject(), Utils.unquote(pkg.get().getText()).trim())) {
                    continue;
                }


                Optional<Element> value = entry.getValue().getParameterValue("Value");
                if (value.isEmpty()
                        || !(value.get() instanceof StringElement)
                        || StringUtils.isBlank(Utils.unquote(value.get().getText()).trim())) {
                    continue;
                }


                String name = markerElement.getMarker().getName();
                String[] parts = name.split(":", 2);

                if (parts[0].equals(Utils.unquote(value.get().getText()).trim())) {
                    return entry.getKey();
                }

                Optional<Element> alias = entry.getValue().getParameterValue("Alias");
                if (alias.isEmpty()
                        || !(alias.get() instanceof StringElement)
                        || StringUtils.isBlank(Utils.unquote(alias.get().getText()).trim())) {
                    continue;
                }

                if (parts[0].equals(Utils.unquote(alias.get().getText()).trim())) {
                    return entry.getKey();
                }

            }
            return null;
        }

        private Map<PsiElement, MarkerElement> parseComments(List<PsiComment> comments) {
            final Map<PsiElement, MarkerElement> elements = new HashMap<>();
            final Optional<Marker> marker = METADATA_PROVIDER.getImportMarker();
            if (marker.isEmpty()) {
                return elements;
            }

            comments.forEach(psiComment -> {
                Optional<Comment> comment = Utils.getMarkerComment(psiComment);
                if (comment.isEmpty()) {
                    return;
                }

                final Parser parser = new Parser();
                final ParseResult parseResult = parser.parse(marker.get(), comment.get(), marker.get().getName());
                final MarkerElement markerElement = parseResult.getMarkerElement();
                elements.put(psiComment, markerElement);
            });

            return elements;
        }
    }
}
