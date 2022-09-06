package com.github.procyonprojects.marker.handler;

import com.github.procyonprojects.marker.TargetInfo;
import com.github.procyonprojects.marker.Utils;
import com.github.procyonprojects.marker.comment.Comment;
import com.github.procyonprojects.marker.metadata.Target;
import com.github.procyonprojects.marker.metadata.provider.MetadataProvider;
import com.github.procyonprojects.marker.metadata.v1.EnumValue;
import com.github.procyonprojects.marker.metadata.v1.Marker;
import com.github.procyonprojects.marker.metadata.v1.Parameter;
import com.intellij.lang.parameterInfo.CreateParameterInfoContext;
import com.intellij.lang.parameterInfo.ParameterInfoHandler;
import com.intellij.lang.parameterInfo.ParameterInfoUIContext;
import com.intellij.lang.parameterInfo.UpdateParameterInfoContext;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiComment;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiWhiteSpace;
import com.intellij.util.ArrayUtil;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;
import java.util.Optional;

public class MarkerParameterInfoHandler implements ParameterInfoHandler<PsiComment, Object> {

    private static final MetadataProvider METADATA_PROVIDER = ApplicationManager.getApplication().getService(MetadataProvider.class);

    @Override
    public @Nullable PsiComment findElementForParameterInfo(@NotNull CreateParameterInfoContext context) {
        PsiElement element = context.getFile().findElementAt(context.getOffset());
        if (element instanceof PsiWhiteSpace) {
            element = element.getPrevSibling();
        }

        if (!(element instanceof PsiComment)) {
            return null;
        }

        final TargetInfo targetInfo;
        Optional<Comment> comment = Utils.getMarkerComment(element);
        if (comment.isEmpty()) {
            comment = Utils.findMarkerCommentFirstLine(element);

            if (comment.isEmpty()) {
                return (PsiComment) element;
            }

            targetInfo = Utils.findTarget((PsiComment) comment.get().firstLine().get().getElement());
        } else {
            targetInfo = Utils.findTarget((PsiComment) Objects.requireNonNull(element));
        }

        if (targetInfo.getTarget() == Target.INVALID) {
            return null;
        }

        final Comment.Line firstLine = comment.get().firstLine().get();
        final String firstLineText = firstLine.getText().trim();
        final String anonymousName = Utils.getMarkerAnonymousName(firstLineText);
        final String markerName = Utils.getMarkerName(firstLineText);

        Project project = element.getProject();
        VirtualFile file = element.getContainingFile().getVirtualFile();

        Optional<Marker> marker = METADATA_PROVIDER.findMarker(project, file, anonymousName, targetInfo.getTarget());
        if (marker.isEmpty()) {
            marker = METADATA_PROVIDER.findMarker(project, file, markerName, targetInfo.getTarget());
            if (marker.isEmpty()) {
                return (PsiComment) element;
            }
        }

        if (CollectionUtils.isNotEmpty(marker.get().getParameters())) {
            context.setItemsToShow(new Object[]{"<no parameters>"});
        } else {
            context.setItemsToShow(ArrayUtil.toObjectArray(marker.get().getParameters()));
        }

        return (PsiComment) element;
    }

    @Override
    public void showParameterInfo(@NotNull PsiComment element, @NotNull CreateParameterInfoContext context) {
        context.showHint(element, element.getTextRange().getStartOffset(), this);
    }

    @Override
    public @Nullable PsiComment findElementForUpdatingParameterInfo(@NotNull UpdateParameterInfoContext context) {
        PsiElement element = context.getFile().findElementAt(context.getOffset());
        if (element instanceof PsiWhiteSpace) {
            element = element.getPrevSibling();
        }

        if (!(element instanceof PsiComment)) {
            return null;
        }

        final TargetInfo targetInfo;
        Optional<Comment> comment = Utils.getMarkerComment(element);
        if (comment.isEmpty()) {
            comment = Utils.findMarkerCommentFirstLine(element);

            if (comment.isEmpty()) {
                return (PsiComment) element;
            }

            targetInfo = Utils.findTarget((PsiComment) comment.get().firstLine().get().getElement());
        } else {
            targetInfo = Utils.findTarget((PsiComment) Objects.requireNonNull(element));
        }

        if (targetInfo.getTarget() == Target.INVALID) {
            return null;
        }

        return (PsiComment) element;
    }

    @Override
    public void updateParameterInfo(@NotNull PsiComment psiComment, @NotNull UpdateParameterInfoContext context) {

    }

    @Override
    public void updateUI(Object p, @NotNull ParameterInfoUIContext context) {
        if (p instanceof String) {
            context.setupUIComponentPresentation((String) p, 0, ((String) p).length(),
                    false, false, true, context.getDefaultParameterColor());
            return;
        }

        Parameter parameter = (Parameter) p;
        StringBuilder htmlBuilder = new StringBuilder();
        htmlBuilder.append(parameter.getSchema().getPresentableText());
        htmlBuilder.append(" ").append("<b>").append(parameter.getName()).append("</b>");

        if (parameter.getDefaultValue() != null) {
            htmlBuilder.append(" default ").append(parameter.getDefaultValue());
        }

        if (parameter.isRequired()) {
            htmlBuilder.append(" | ").append("<b>required</b>");
        }

        if (StringUtils.isNotBlank(parameter.getDescription())) {
            htmlBuilder.append(" - <i>").append(parameter.getDescription()).append("</i>");
        }

        if (!CollectionUtils.isEmpty(parameter.getEnumValues())) {
            htmlBuilder.append("<br>");
            htmlBuilder.append("<i>enum values:</i>");
            htmlBuilder.append("<ul>");
            for (EnumValue enumValue : parameter.getEnumValues()) {
                htmlBuilder.append("<li>").append(enumValue.getValue().toString()).append("</li>");
            }
            htmlBuilder.append("</ul>");
        }

        context.setupRawUIComponentPresentation(htmlBuilder.toString());
    }
}
