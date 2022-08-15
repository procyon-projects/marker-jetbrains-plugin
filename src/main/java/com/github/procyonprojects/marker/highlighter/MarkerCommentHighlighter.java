package com.github.procyonprojects.marker.highlighter;

import com.github.procyonprojects.marker.TargetInfo;
import com.github.procyonprojects.marker.Utils;
import com.github.procyonprojects.marker.comment.Comment;
import com.github.procyonprojects.marker.comment.ParseResult;
import com.github.procyonprojects.marker.comment.Parser;
import com.github.procyonprojects.marker.element.*;
import com.github.procyonprojects.marker.metadata.Definition;
import com.github.procyonprojects.marker.metadata.Enum;
import com.github.procyonprojects.marker.fix.MissingParameterFix;
import com.github.procyonprojects.marker.metadata.Parameter;
import com.github.procyonprojects.marker.metadata.Target;
import com.github.procyonprojects.marker.metadata.provider.DefinitionProvider;
import com.intellij.codeInspection.ProblemHighlightType;
import com.intellij.lang.annotation.AnnotationHolder;
import com.intellij.lang.annotation.HighlightSeverity;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.editor.colors.TextAttributesKey;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiComment;
import com.intellij.psi.PsiElement;
import org.apache.commons.collections.CollectionUtils;
import org.jetbrains.annotations.NotNull;

import java.util.*;

@Service
public class MarkerCommentHighlighter {

    private static final DefinitionProvider definitionProvider = ApplicationManager.getApplication().getService(DefinitionProvider.class);

    private void highlightStringElement(StringElement element, TextRange containerTextRange, @NotNull AnnotationHolder holder, boolean isMapValue) {
        if (element.getParts().size() == 1 && !isMapValue) {
            Element value = element.getParts().get(0);
            if (!value.getText().startsWith("'") && !value.getText().startsWith("\"") && !value.getText().startsWith("`")) {
                if (containerTextRange.contains(value.getRange())) {
                    holder.newSilentAnnotation(HighlightSeverity.INFORMATION)
                            .range(value.getRange())
                            .textAttributes(createTextAttribute("STRING_KEY"))
                            .create();
                }

                return;
            }
        }

        for (Element part : element.getParts()) {
            if (containerTextRange.contains(part.getRange())) {
                holder.newSilentAnnotation(HighlightSeverity.INFORMATION)
                        .range(part.getRange())
                        .textAttributes(createTextAttribute("STRING_VALUE"))
                        .create();
            }
        }

    }

    private void highlightSliceElement(SliceElement element, TextRange containerTextRange, @NotNull AnnotationHolder holder) {
        final Parameter parameter = element.getParameter();
        final List<Enum> enumList = parameter.getEnumValues();

        final List<Element> items = element.getItems();
        final Set<String> usedItems = new HashSet<>();

        if (element.getLeftBrace() != null) {
            highlightElement(element.getLeftBrace(), containerTextRange, holder, false);
        }

        if (element.getRightBrace() != null) {
            highlightElement(element.getRightBrace(), containerTextRange, holder, false);
        }

        for (Element item : items) {
            final String text = item.getText();

            if (!enumList.isEmpty() && !",".equals(text) && !"{".equals(text) && !"}".equals(text) && !";".equals(text) && usedItems.contains(item.getText()) && containerTextRange.contains(item.getRange())) {
                holder.newAnnotation(HighlightSeverity.ERROR, "Duplicate item")
                        .range(item.getRange())
                        .textAttributes(createTextAttribute("DUPLICATE"))
                        .create();
            }  else if (!enumList.isEmpty() && !",".equals(text) && !"{".equals(text) && !"}".equals(text) && !";".equals(text) && enumList.stream().noneMatch(enumItem -> enumItem.getValue().equals(text)) && containerTextRange.contains(item.getRange())) {
                final UnresolvedElement unresolvedElement = new UnresolvedElement(String.format("cannot resolve '%s' enum value", text), text, item.getRange());
                highlightElement(unresolvedElement, containerTextRange, holder, false);
            } else {
                highlightElement(item, containerTextRange, holder, false);
            }

            usedItems.add(item.getText());
        }
    }

    private void highlightMapElement(MapElement element, TextRange containerTextRange, @NotNull AnnotationHolder holder) {
        final List<Element> items = element.getItems();
        final Set<String> usedMapKeys = new HashSet<>();

        if (element.getLeftBrace() != null) {
            highlightElement(element.getLeftBrace(), containerTextRange, holder, false);
        }

        if (element.getRightBrace() != null) {
            highlightElement(element.getRightBrace(), containerTextRange, holder, false);
        }

        for (Element item : items) {
            highlightElement(item, containerTextRange, holder, false);

            if (item instanceof KeyValueElement) {
                KeyValueElement keyValueElement = (KeyValueElement) item;

                if (keyValueElement.getKeyElement() != null) {
                    if (usedMapKeys.contains(keyValueElement.getKeyElement().getText()) && keyValueElement.getKeyElement() instanceof StringElement) {
                        StringElement keyElement = (StringElement) keyValueElement.getKeyElement();
                        for (Element part : keyElement.getParts()) {
                            if (containerTextRange.contains(part.getRange())) {
                                holder.newAnnotation(HighlightSeverity.ERROR, "Duplicate map key")
                                        .range(part.getRange())
                                        .textAttributes(createTextAttribute("DUPLICATE"))
                                        .create();
                            }
                        }
                    }

                    usedMapKeys.add(keyValueElement.getKeyElement().getText());
                }

            }
        }
    }

    private void highlightKeyValueElement(KeyValueElement element, TextRange containerTextRange, @NotNull AnnotationHolder holder) {
        if (element.getKeyElement() != null) {
            highlightElement(element.getKeyElement(), containerTextRange, holder, false);
        }

        if (element.getColonElement() != null) {
            highlightElement(element.getColonElement(), containerTextRange, holder, false);
        }

        if (element.getValueElement() != null) {
            highlightElement(element.getValueElement(), containerTextRange, holder, true);
        }
    }

    private void highlightBooleanElement(BooleanElement element, TextRange containerTextRange, @NotNull AnnotationHolder holder) {
        if (!containerTextRange.contains(element.getRange())) {
            return;
        }

        holder.newSilentAnnotation(HighlightSeverity.INFORMATION)
                .range(element.getRange())
                .textAttributes(createTextAttribute("BOOLEAN_VALUE"))
                .create();
    }

    private void highlightIntegerElement(IntegerElement element, TextRange containerTextRange, @NotNull AnnotationHolder holder) {
        if (!containerTextRange.contains(element.getRange())) {
            return;
        }

        holder.newSilentAnnotation(HighlightSeverity.INFORMATION)
                .range(element.getRange())
                .textAttributes(createTextAttribute("INTEGER_VALUE"))
                .create();
    }

    private void highlightBracesAndOperators(Element element, TextRange containerTextRange, @NotNull AnnotationHolder holder) {
        if (!containerTextRange.contains(element.getRange())) {
            return;
        }

        holder.newSilentAnnotation(HighlightSeverity.INFORMATION)
                .range(element.getRange())
                .textAttributes(createTextAttribute("BRACES_AND_OPERATORS"))
                .create();
    }

    private void highlightExpectedElement(ExpectedElement element, TextRange containerTextRange, @NotNull AnnotationHolder holder) {
        if (!containerTextRange.contains(element.getRange())) {
            return;
        }

        holder.newAnnotation(HighlightSeverity.ERROR, element.getMessage())
                .range(element.getRange())
                .textAttributes(createTextAttribute("EXPECTED"))
                .create();
    }

    private void highlightInvalidElement(InvalidElement element, TextRange containerTextRange, @NotNull AnnotationHolder holder) {
        if (!containerTextRange.contains(element.getRange())) {
            return;
        }

        holder.newAnnotation(HighlightSeverity.ERROR, element.getMessage())
                .range(element.getRange())
                .textAttributes(createTextAttribute("INVALID_PARAMETER_VALUE"))
                .create();
    }

    private void highlightUnresolvedElement(UnresolvedElement element, TextRange containerTextRange, @NotNull AnnotationHolder holder) {
        if (!containerTextRange.contains(element.getRange())) {
            return;
        }

        holder.newAnnotation(HighlightSeverity.ERROR, element.getMessage())
                .range(element.getRange())
                .highlightType(ProblemHighlightType.LIKE_UNKNOWN_SYMBOL)
                .create();
    }

    private void highlightMarkerElement(MarkerElement element, TextRange containerTextRange, @NotNull AnnotationHolder holder) {
        if (containerTextRange.contains(element.getRange())) {
            holder.newSilentAnnotation(HighlightSeverity.INFORMATION)
                    .range(element.getRange())
                    .textAttributes(createTextAttribute("MARKER_NAME"))
                    .create();
        }

        final List<ParameterElement> parameterElements = element.getParameterElements();
        final Set<String> seen = new HashSet<>();

        for (ParameterElement parameterElement : parameterElements) {

            if (parameterElement.getName() != null) {
                final Element nameElement = parameterElement.getName();

                if (nameElement instanceof UnresolvedElement || nameElement instanceof ExpectedElement) {
                    highlightElement(nameElement, containerTextRange, holder, false);
                } else {

                    if (containerTextRange.contains(nameElement.getRange())) {

                        if (!seen.contains(nameElement.getText())) {
                            holder.newSilentAnnotation(HighlightSeverity.INFORMATION)
                                    .range(nameElement.getRange())
                                    .textAttributes(createTextAttribute("MARKER_PARAMETER_NAME"))
                                    .create();
                        } else {
                            holder.newAnnotation(HighlightSeverity.ERROR, "Duplicate parameter")
                                    .range(nameElement.getRange())
                                    .textAttributes(createTextAttribute("MARKER_DUPLICATE_PARAMETER"))
                                    .create();
                        }

                    }
                }


                seen.add(nameElement.getText());
            } else {
                seen.add("Value");
            }

            if (parameterElement.getEqualSign() != null) {
                highlightElement(parameterElement.getEqualSign(), containerTextRange, holder, false);
            }

            if (parameterElement.getValue() != null) {
                highlightElement(parameterElement.getValue(), containerTextRange, holder, false);
            }

            Element nextElement = parameterElement.getNext();
            if (nextElement != null && ",".equals(nextElement.getText())) {
                highlightElement(nextElement, containerTextRange, holder, false);
            }
        }

        final Definition definition = element.getDefinition();
        final List<String> missingParams = new ArrayList<>();
        for (Parameter parameter : definition.getParameters()) {
            if (parameter.isRequired() && !seen.contains(parameter.getName())) {
                missingParams.add(parameter.getName());
            }
        }

        if (CollectionUtils.isNotEmpty(missingParams) && containerTextRange.contains(element.getRange())) {
            holder.newAnnotation(HighlightSeverity.ERROR, "Missing parameters : " + missingParams)
                    .range(element.getRange())
                    .textAttributes(createTextAttribute("MARKER_MISSING_PARAMETERS"))
                    .withFix(new MissingParameterFix(missingParams))
                    .create();
        }
    }

    private TextAttributesKey createTextAttribute(String attributeKey) {
        return TextAttributesKey.createTextAttributesKey(attributeKey);
    }

    private void highlightElement(Element element, TextRange containerTextRange, @NotNull AnnotationHolder holder, boolean isMapValue) {
        if (element instanceof MarkerElement) {
            highlightMarkerElement((MarkerElement) element, containerTextRange, holder);
        } else if (element instanceof InvalidElement) {
            highlightInvalidElement((InvalidElement)element, containerTextRange, holder);
        } else if (element instanceof ExpectedElement) {
            highlightExpectedElement((ExpectedElement) element, containerTextRange, holder);
        } else if (element instanceof UnresolvedElement) {
           highlightUnresolvedElement((UnresolvedElement) element, containerTextRange, holder);
        } else if (element instanceof StringElement) {
            highlightStringElement((StringElement) element, containerTextRange, holder, isMapValue);
        } else if (element instanceof SliceElement) {
            highlightSliceElement((SliceElement) element, containerTextRange, holder);
        } else if (element instanceof MapElement) {
            highlightMapElement((MapElement) element, containerTextRange, holder);
        } else if (element instanceof KeyValueElement) {
            highlightKeyValueElement((KeyValueElement)element, containerTextRange, holder);
        } else if (element instanceof BooleanElement) {
            highlightBooleanElement((BooleanElement) element, containerTextRange, holder);
        } else if (element instanceof IntegerElement) {
            highlightIntegerElement((IntegerElement) element, containerTextRange, holder);
        } else {
            highlightBracesAndOperators(element, containerTextRange, holder);
        }
    }

    private void highlightNewLineElements(List<Element> elements, TextRange containerTextRange, @NotNull AnnotationHolder holder) {
        if (CollectionUtils.isEmpty(elements)) {
            return;
        }

        for (Element newLineElement : elements) {
            if (containerTextRange.contains(newLineElement.getRange())) {
                holder.newSilentAnnotation(HighlightSeverity.INFORMATION)
                        .range(newLineElement.getRange())
                        .textAttributes(createTextAttribute("NEW_LINE_CHARACTER"))
                        .create();
            }
        }
    }

    private void highlightUnresolvedMarker(Element element, TextRange containerTextRange, @NotNull AnnotationHolder holder) {
        if (containerTextRange.contains(element.getRange())) {
            holder.newAnnotation(HighlightSeverity.ERROR, "Unresolved marker")
                    .range(element.getRange())
                    .highlightType(ProblemHighlightType.LIKE_UNKNOWN_SYMBOL)
                    .create();
        }
    }

    public void highlight(Comment comment, PsiElement element, AnnotationHolder holder) {
        final TargetInfo targetInfo = Utils.findTarget((PsiComment) element);
        if (targetInfo.getTarget() == Target.INVALID) {
            return;
        }

        final Comment.Line firstLine = comment.firstLine().get();
        final String firstLineText = firstLine.getText().trim();
        final String anonymousName = Utils.getMarkerAnonymousName(firstLineText);
        final String markerName = Utils.getMarkerName(firstLineText);

        Optional<Definition> definition = definitionProvider.find(targetInfo.getTarget(), anonymousName);

        if (definition.isEmpty()) {
            definition = definitionProvider.find(targetInfo.getTarget(), markerName);
            if (definition.isEmpty()) {
                int startIndex = firstLine.startOffset() + firstLine.getText().indexOf("+" + anonymousName);
                int endIndex = startIndex + anonymousName.length() + 1;
                highlightUnresolvedMarker(new Element("+" + anonymousName, new TextRange(startIndex, endIndex)), element.getTextRange(), holder);
                return;
            }
        }

        final Parser parser = new Parser();
        final ParseResult result = parser.parse(definition.get(), comment);
        if (result != null) {
            highlightElement(result.getMarkerElement(), element.getTextRange(), holder, false);
            highlightNewLineElements(result.getNextLineElements(), element.getTextRange(), holder);
        }
    }
}
