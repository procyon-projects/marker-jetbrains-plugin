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
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.TextRange;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.patterns.PatternCondition;
import com.intellij.patterns.PlatformPatterns;
import com.intellij.psi.*;
import com.intellij.util.ProcessingContext;
import org.jetbrains.annotations.NotNull;

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
        VirtualFile file = element.getContainingFile().getOriginalFile().getVirtualFile();

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
}
