package com.github.procyonprojects.marker.highlighter;

import com.github.procyonprojects.marker.TargetInfo;
import com.github.procyonprojects.marker.Utils;
import com.github.procyonprojects.marker.comment.Comment;
import com.github.procyonprojects.marker.comment.ParseResult;
import com.github.procyonprojects.marker.comment.Parser;
import com.github.procyonprojects.marker.element.Element;
import com.github.procyonprojects.marker.element.MarkerElement;
import com.github.procyonprojects.marker.fix.DownloadProcessorPackageFix;
import com.github.procyonprojects.marker.metadata.Target;
import com.github.procyonprojects.marker.metadata.provider.MetadataProvider;
import com.github.procyonprojects.marker.metadata.v1.Marker;
import com.intellij.codeInspection.ProblemHighlightType;
import com.intellij.lang.annotation.AnnotationHolder;
import com.intellij.lang.annotation.HighlightSeverity;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiComment;
import com.intellij.psi.PsiElement;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;

@Service
public class ImportValuesHighlighter {

    private static final MetadataProvider METADATA_PROVIDER = ApplicationManager.getApplication().getService(MetadataProvider.class);

    private void highlightUnresolvedPackage(String pkg, Element element, TextRange containerTextRange, @NotNull AnnotationHolder holder) {
        if (containerTextRange.contains(element.getRange())) {
            holder.newAnnotation(HighlightSeverity.ERROR, "Unresolved package")
                    .range(element.getRange())
                    .highlightType(ProblemHighlightType.LIKE_UNKNOWN_SYMBOL)
                    .withFix(new DownloadProcessorPackageFix(pkg))
                    .create();
        }
    }

    private void highlightUnresolvedProcessor(Element element, TextRange containerTextRange, @NotNull AnnotationHolder holder) {
        if (containerTextRange.contains(element.getRange())) {
            holder.newAnnotation(HighlightSeverity.ERROR, "Unresolved processor")
                    .range(element.getRange())
                    .highlightType(ProblemHighlightType.LIKE_UNKNOWN_SYMBOL)
                    .create();
        }
    }

    public void highlight(Comment comment, PsiElement element, AnnotationHolder holder) {
        final TargetInfo targetInfo = Utils.findTarget((PsiComment) comment);
        if (targetInfo.getTarget() == Target.INVALID || targetInfo.getTarget() != Target.PACKAGE_LEVEL) {
            return;
        }

        final Comment.Line firstLine = comment.firstLine().get();

        if (!firstLine.getText().trim().startsWith("+import")) {
            return;
        }

        Optional<Marker> marker = METADATA_PROVIDER.getImportMarker();
        if (marker.isEmpty()) {
            return;
        }

        final Parser parser = new Parser();
        final ParseResult parseResult = parser.parse(marker.get(), comment, marker.get().getName());
        final MarkerElement markerElement = parseResult.getMarkerElement();
        final Optional<Element> pkg = markerElement.getParameterValue("Pkg");
        boolean packageExists;
        if (pkg.isPresent()) {
            packageExists = METADATA_PROVIDER.packageExists(element.getProject(), Utils.unquote(pkg.get().getText()).trim());
            if (!packageExists) {
                highlightUnresolvedPackage(Utils.unquote(pkg.get().getText()).trim(), pkg.get(), element.getTextRange(), holder);
            }
        } else {
            packageExists = false;
        }

        final Optional<Element> processor = markerElement.getParameterValue("Value");
        if (!packageExists && processor.isPresent()) {
            highlightUnresolvedProcessor(processor.get(), element.getTextRange(), holder);
        } else if(packageExists && StringUtils.isNotEmpty(processor.get().getText())
                && !METADATA_PROVIDER.processorExists(element.getProject(), Utils.unquote(pkg.get().getText()).trim(), Utils.unquote(processor.get().getText()).trim())) {
            highlightUnresolvedProcessor(processor.get(), element.getTextRange(), holder);
        }
    }
}
