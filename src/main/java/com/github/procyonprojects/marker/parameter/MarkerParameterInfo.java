package com.github.procyonprojects.marker.parameter;

import com.github.procyonprojects.marker.model.Argument;
import com.github.procyonprojects.marker.provider.MarkerMetadataProvider;
import com.goide.GoParserDefinition;
import com.intellij.lang.parameterInfo.CreateParameterInfoContext;
import com.intellij.lang.parameterInfo.ParameterInfoHandler;
import com.intellij.lang.parameterInfo.ParameterInfoUIContext;
import com.intellij.lang.parameterInfo.UpdateParameterInfoContext;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.psi.PsiComment;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiWhiteSpace;
import com.intellij.util.ArrayUtil;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class MarkerParameterInfo implements ParameterInfoHandler<PsiComment, Object> {

    private static final MarkerMetadataProvider markerMetadataProvider = ApplicationManager.getApplication().getService(MarkerMetadataProvider.class);

    @Override
    public @Nullable PsiComment findElementForParameterInfo(@NotNull CreateParameterInfoContext context) {
        PsiElement element = context.getFile().findElementAt(context.getOffset());
        if (element instanceof PsiWhiteSpace) {
            final PsiElement previousElement = element.getPrevSibling();
            if (previousElement instanceof PsiComment) {
                if (isMarkerComment((PsiComment) previousElement)) {
                    final String commentText = previousElement.getText().substring(2).trim();
                    String markerName = getMarkerAnonymousName(commentText);
                    List<Argument> arguments = markerMetadataProvider.packageLevelMap.values().stream().filter(x -> x.getName().equals("+" + markerName)).findFirst().get().getArguments();
                    context.setItemsToShow(ArrayUtil.toObjectArray( arguments));
                    return (PsiComment) previousElement;
                }
            }
        } else if (element instanceof PsiComment) {
            if (isMarkerComment((PsiComment) element)) {

                final String commentText = element.getText().substring(2).trim();
                String markerName = getMarkerAnonymousName(commentText);
                List<Argument> arguments = markerMetadataProvider.packageLevelMap.values().stream().filter(x -> x.getName().equals("+" + markerName)).findFirst().get().getArguments();
                context.setItemsToShow(ArrayUtil.toObjectArray( arguments));
                return (PsiComment) element;
            }
        }
        return null;
    }

    @Override
    public @Nullable PsiComment findElementForUpdatingParameterInfo(@NotNull UpdateParameterInfoContext context) {
        PsiElement element = context.getFile().findElementAt(context.getOffset());
        if (element instanceof PsiWhiteSpace) {
            final PsiElement previousElement = element.getPrevSibling();
            if (previousElement instanceof PsiComment) {
                if (isMarkerComment((PsiComment) previousElement)) {
                    return (PsiComment) previousElement;
                }
            }
        } else if (element instanceof PsiComment) {
            if (isMarkerComment((PsiComment) element)) {
                return (PsiComment) element;
            }
        }
        return null;
    }

    @Override
    public void updateUI(Object p, @NotNull ParameterInfoUIContext context) {
        Argument argument = (Argument)p;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(argument.getName());
        stringBuilder.append(" ").append(argument.getType());
        if (argument.isOptional()) {
            stringBuilder.append(" | ").append("Optional");
        }
        if (StringUtils.isNotBlank(argument.getDescription())) {
            stringBuilder.append(", ").append("Description: ").append(argument.getDescription());
        }
        if (!CollectionUtils.isEmpty(argument.getValues())) {
            stringBuilder.append(", ").append("Values: ").append(argument.getValues().toString());
        }
        context.setupUIComponentPresentation(stringBuilder.toString(), 0, argument.getName().length(),
                false, false, true,
                context.getDefaultParameterColor());
    }

    @Override
    public void updateParameterInfo(@NotNull PsiComment o, @NotNull UpdateParameterInfoContext context) {
    }

    @Override
    public void showParameterInfo(@NotNull PsiComment element, @NotNull CreateParameterInfoContext context) {
       // addItemsToShow(element, context);
        context.showHint(element, element.getTextRange().getStartOffset(), this);
    }

    private void addItemsToShow(PsiComment psiComment, CreateParameterInfoContext context) {
        context.setItemsToShow(ArrayUtil.toObjectArray( List.of("Test1", "Test2")));
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

        return true;
    }

    private class ParameterPresentation {
        String text;
        int start;
        int end;
        boolean disabled = false;

        public ParameterPresentation(String text, int start, int end, boolean disabled) {
            if (text.length() == 0) {
                this.text = "<no parameters>";
            } else {
                this.text = text;
            }
            this.start = start;
            this.end = end;
            this.disabled = disabled;
        }
    }

    private String getMarkerAnonymousName(String markerComment) {
        markerComment = markerComment.substring(1);

        String[] nameFieldParts = markerComment.split("=", 2);

        return nameFieldParts[0];
    }
}
