package com.github.procyonprojects.marker.highlighter;

import com.github.procyonprojects.marker.TargetInfo;
import com.github.procyonprojects.marker.Utils;
import com.github.procyonprojects.marker.comment.Comment;
import com.github.procyonprojects.marker.comment.ParseResult;
import com.github.procyonprojects.marker.comment.Parser;
import com.github.procyonprojects.marker.element.*;
import com.github.procyonprojects.marker.metadata.Target;
import com.github.procyonprojects.marker.metadata.Type;
import com.github.procyonprojects.marker.metadata.provider.MetadataProvider;
import com.github.procyonprojects.marker.metadata.v1.EnumValue;
import com.github.procyonprojects.marker.metadata.v1.Marker;
import com.github.procyonprojects.marker.metadata.v1.Parameter;
import com.github.procyonprojects.marker.metadata.v1.schema.SliceSchema;
import com.github.procyonprojects.marker.metadata.v1.schema.StringSchema;
import com.goide.highlighting.GoSyntaxHighlightingColors;
import com.goide.psi.GoTypeSpec;
import com.goide.psi.impl.GoPsiImplUtil;
import com.intellij.codeInspection.ProblemHighlightType;
import com.intellij.lang.annotation.AnnotationHolder;
import com.intellij.lang.annotation.HighlightSeverity;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.editor.colors.TextAttributesKey;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.TextRange;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiComment;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiWhiteSpace;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;

import java.util.*;

@Service
public class MarkerCommentHighlighter {

    private static final MetadataProvider METADATA_PROVIDER = ApplicationManager.getApplication().getService(MetadataProvider.class);

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
        final List<EnumValue> enumList = parameter.getEnumValues();

        if (element.getLeftBrace() != null) {
            highlightElement(element.getLeftBrace(), containerTextRange, holder, false);
        }

        if (element.getRightBrace() != null) {
            highlightElement(element.getRightBrace(), containerTextRange, holder, false);
        }

        final List<EnumValue> enumValues;
        if (parameter.getSchema() instanceof SliceSchema) {
            SliceSchema sliceSchema = (SliceSchema) parameter.getSchema();
            if (sliceSchema.getItemSchema() instanceof StringSchema) {
                enumValues = ((StringSchema) sliceSchema.getItemSchema()).getEnums();
            } else {
                enumValues = List.of();
            }
        } else {
            enumValues = List.of();
        }

        final List<Element> items = element.getItems();
        final Set<String> usedItems = new HashSet<>();

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
                Parameter parameter = parameterElement.getParameter();
                List<EnumValue> enumValues;

                if (parameter != null && parameter.getType().getActualType() == Type.StringType) {
                    StringSchema stringSchema = (StringSchema) parameter.getSchema();
                    enumValues = stringSchema.getEnums();
                } else {
                    enumValues = List.of();
                }

                if (parameter != null && parameter.getType().getActualType() == Type.StringType && CollectionUtils.isNotEmpty(enumValues)) {
                    String text = parameterElement.getValue().getText();
                    boolean match = enumValues.stream().anyMatch(enumValue -> StringUtils.isNotEmpty(text) && text.equals(enumValue.getValue()));
                    if (StringUtils.isNotEmpty(text) && !match) {
                        final UnresolvedElement unresolvedElement = new UnresolvedElement(String.format("cannot resolve '%s' enum value", text), text, parameterElement.getValue().getRange());
                        highlightElement(unresolvedElement, containerTextRange, holder, false);
                    } else {
                        highlightElement(parameterElement.getValue(), containerTextRange, holder, false);
                    }
                } else {
                    highlightElement(parameterElement.getValue(), containerTextRange, holder, false);
                }
            }

            Element nextElement = parameterElement.getNext();
            if (nextElement != null && ",".equals(nextElement.getText())) {
                highlightElement(nextElement, containerTextRange, holder, false);
            }
        }

        final Marker marker = element.getMarker();
        final List<String> missingParams = new ArrayList<>();
        for (Parameter parameter : marker.getParameters()) {
            if (parameter.isRequired() && !seen.contains(parameter.getName())) {
                missingParams.add(parameter.getName());
            }
        }

        if (CollectionUtils.isNotEmpty(missingParams) && containerTextRange.contains(element.getRange())) {
            holder.newAnnotation(HighlightSeverity.ERROR, "Missing parameters : " + missingParams)
                    .range(element.getRange())
                    .textAttributes(createTextAttribute("MARKER_MISSING_PARAMETERS"))
                    .create();
        }
    }

    private void highlightTypeElement(TypeElement element, TextRange containerTextRange, @NotNull AnnotationHolder holder) {
        if (!containerTextRange.contains(element.getRange())) {
            return;
        }

        String value = element.getText();
        int indexOfTypeKeyword = value.indexOf(".type");

        String[] typeParts = value.split("\\.");
        if (typeParts.length == 3) {

            int startOffset = element.getRange().getStartOffset();
            int endOffset = startOffset + typeParts[0].length();

            holder.newSilentAnnotation(HighlightSeverity.INFORMATION)
                    .range(new TextRange(startOffset, endOffset))
                    .textAttributes(GoSyntaxHighlightingColors.PACKAGE)
                    .create();

            highlightBracesAndOperators(new Element(".", new TextRange(endOffset, endOffset + 1)), containerTextRange, holder);

            startOffset = endOffset + 1;
            endOffset = endOffset + typeParts[1].length() + 1;

            holder.newSilentAnnotation(HighlightSeverity.INFORMATION)
                    .range(new TextRange(startOffset, endOffset))
                    .textAttributes(GoSyntaxHighlightingColors.TYPE_SPEC_REFERENCE)
                    .create();

            highlightBracesAndOperators(new Element(".", new TextRange(endOffset, endOffset + 1)), containerTextRange, holder);

        } else {
            List<String> builtinTypes = List.of("int", "int8", "int16", "int32", "int64",
                    "uint", "uint8", "uint16", "uint32", "uint64",
                    "error", "byte", "rune", "uintptr", "bool", "string");

            int startOffset = element.getRange().getStartOffset();
            int endOffset = startOffset + typeParts[0].length();

            if (builtinTypes.contains(typeParts[0])) {
                holder.newSilentAnnotation(HighlightSeverity.INFORMATION)
                        .range(new TextRange(startOffset, endOffset))
                        .textAttributes(GoSyntaxHighlightingColors.KEYWORD)
                        .create();
            } else {
                holder.newSilentAnnotation(HighlightSeverity.INFORMATION)
                        .range(new TextRange(startOffset, endOffset))
                        .textAttributes(GoSyntaxHighlightingColors.TYPE_SPEC_REFERENCE)
                        .create();
            }

            highlightBracesAndOperators(new Element(".", new TextRange(endOffset, endOffset + 1)), containerTextRange, holder);
        }

        if (indexOfTypeKeyword != -1) {
            holder.newSilentAnnotation(HighlightSeverity.INFORMATION)
                    .range(new TextRange(element.getRange().getStartOffset() + indexOfTypeKeyword + 1, element.getRange().getEndOffset()))
                    .textAttributes(GoSyntaxHighlightingColors.KEYWORD)
                    .create();
        }
    }

    private void highlightFuncElement(FuncElement element, TextRange containerTextRange, @NotNull AnnotationHolder holder) {
        if (!containerTextRange.contains(element.getRange())) {
            return;
        }

        String value = element.getText();
        int indexOfFuncKeyword = value.indexOf(".func");

        String[] typeParts = value.split("\\.");
        if (typeParts.length == 3) {

            int startOffset = element.getRange().getStartOffset();
            int endOffset = startOffset + typeParts[0].length();

            holder.newSilentAnnotation(HighlightSeverity.INFORMATION)
                    .range(new TextRange(startOffset, endOffset))
                    .textAttributes(GoSyntaxHighlightingColors.PACKAGE)
                    .create();

            highlightBracesAndOperators(new Element(".", new TextRange(endOffset, endOffset + 1)), containerTextRange, holder);

            startOffset = endOffset + 1;
            endOffset = endOffset + typeParts[1].length() + 1;

            holder.newSilentAnnotation(HighlightSeverity.INFORMATION)
                    .range(new TextRange(startOffset, endOffset))
                    .textAttributes(GoSyntaxHighlightingColors.EXPORTED_FUNCTION_CALL)
                    .create();

            highlightBracesAndOperators(new Element(".", new TextRange(endOffset, endOffset + 1)), containerTextRange, holder);

        } else {
            int startOffset = element.getRange().getStartOffset();
            int endOffset = startOffset + typeParts[0].length();

            holder.newSilentAnnotation(HighlightSeverity.INFORMATION)
                    .range(new TextRange(startOffset, endOffset))
                    .textAttributes(GoSyntaxHighlightingColors.EXPORTED_FUNCTION_CALL)
                    .create();
            highlightBracesAndOperators(new Element(".", new TextRange(endOffset, endOffset + 1)), containerTextRange, holder);
        }

        if (indexOfFuncKeyword != -1) {
            holder.newSilentAnnotation(HighlightSeverity.INFORMATION)
                    .range(new TextRange(element.getRange().getStartOffset() + indexOfFuncKeyword + 1, element.getRange().getEndOffset()))
                    .textAttributes(GoSyntaxHighlightingColors.KEYWORD)
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
        } else if (element instanceof TypeElement) {
            highlightTypeElement((TypeElement) element, containerTextRange, holder);
        } else if (element instanceof FuncElement)
            highlightFuncElement((FuncElement) element, containerTextRange, holder);
        else {
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

        Project project = element.getProject();
        VirtualFile file = element.getContainingFile().getVirtualFile();

        Optional<Marker> marker = METADATA_PROVIDER.findMarker(project, file, anonymousName, targetInfo.getTarget());
        String aliasName;

        if (marker.isEmpty()) {
            marker = METADATA_PROVIDER.findMarker(project, file, markerName, targetInfo.getTarget());
            if (marker.isEmpty()) {
                int startIndex = firstLine.startOffset() + firstLine.getText().indexOf("+" + anonymousName);
                int endIndex = startIndex + anonymousName.length() + 1;
                highlightUnresolvedMarker(new Element("+" + anonymousName, new TextRange(startIndex, endIndex)), element.getTextRange(), holder);
                return;
            }
            aliasName = markerName;
        } else {
            aliasName = anonymousName;
        }

        final Parser parser = new Parser();
        final ParseResult result = parser.parse(marker.get(), comment, aliasName);
        if (result != null) {
            highlightElement(result.getMarkerElement(), element.getTextRange(), holder, false);
            highlightNewLineElements(result.getNextLineElements(), element.getTextRange(), holder);
            highlightDuplicates(element, targetInfo.getElement(), targetInfo.getTarget(), holder);
        }
    }

    private void highlightDuplicateMarker(Element element, TextRange containerTextRange, @NotNull AnnotationHolder holder) {
        if (containerTextRange.contains(element.getRange())) {
            holder.newAnnotation(HighlightSeverity.ERROR, "Marker cannot be used more than once")
                    .range(element.getRange())
                    .textAttributes(createTextAttribute("MARKER_NON_REPEATABLE_ERROR"))
                    .create();
        }
    }

    private void highlightInvalidStringValue(Element element, TextRange containerTextRange, @NotNull AnnotationHolder holder, String error) {
        if (!element.getText().startsWith("'") && !element.getText().startsWith("\"") && !element.getText().startsWith("`")) {
            if (containerTextRange.contains(element.getRange())) {
                holder.newAnnotation(HighlightSeverity.ERROR, error)
                        .range(element.getRange())
                        .textAttributes(createTextAttribute("STRING_KEY_PROBLEM"))
                        .create();
            }
            return;
        }

        for (Element part : ((StringElement)element).getParts()) {
            if (containerTextRange.contains(part.getRange())) {
                holder.newAnnotation(HighlightSeverity.ERROR, error)
                        .range(part.getRange())
                        .textAttributes(createTextAttribute("STRING_VALUE_PROBLEM"))
                        .create();
            }
        }
    }

    private void highlightConflictPackageVersions(Element element, TextRange containerTextRange, AnnotationHolder holder) {
        highlightInvalidStringValue(element, containerTextRange, holder, "Different versions of the package cannot be used at the same time");
    }

    private void highlightReImportedProcessor(String processorName, Element element, TextRange containerTextRange, AnnotationHolder holder) {
        highlightInvalidStringValue(element, containerTextRange, holder, String.format("'%s' is already imported", processorName));
    }

    private void highlightDuplicates(PsiElement commentElement, PsiElement element, Target target, AnnotationHolder holder) {
        final List<Comment> commentList = getAllComments(element);
        final Map<String, List<MarkerDuplicate>> duplicates = new HashMap<>();

        final Project project = element.getProject();
        final VirtualFile file = element.getContainingFile().getVirtualFile();

        commentList.forEach(comment -> {
            final Comment.Line firstLine = comment.firstLine().get();
            final String firstLineText = firstLine.getText().trim();
            final String anonymousName = Utils.getMarkerAnonymousName(firstLineText);
            final String markerName = Utils.getMarkerName(firstLineText);


            Optional<Marker> marker = METADATA_PROVIDER.findMarker(project, file, anonymousName, target);
            String aliasName;
            if (marker.isEmpty()) {
                marker = METADATA_PROVIDER.findMarker(project, file, markerName, target);
                if (marker.isEmpty()) {
                    return;
                }
                aliasName = markerName;
            } else {
                aliasName = anonymousName;
            }

            if (!duplicates.containsKey(marker.get().getName())) {
                List<MarkerDuplicate> markerDuplicateList = new ArrayList<>();
                markerDuplicateList.add(new MarkerDuplicate(comment, marker.get(), aliasName));
                duplicates.put(marker.get().getName(), markerDuplicateList);
            } else {
                duplicates.get(marker.get().getName()).add(new MarkerDuplicate(comment, marker.get(), aliasName));
            }
        });

        duplicates.forEach((markerName, markerDuplicates) -> {
            if (markerDuplicates.size() > 1 && !markerDuplicates.get(0).getMarker().isRepeatable()) {
                markerDuplicates.forEach(markerDuplicate -> {
                    Comment.Line firstLine = markerDuplicate.getComment().firstLine().get();
                    int startIndex = firstLine.startOffset() + firstLine.getText().indexOf("+" + markerDuplicate.getMarkerName());
                    int endIndex = startIndex + markerDuplicate.getMarkerName().length() + 1;
                    highlightDuplicateMarker(new Element("+" + markerDuplicate.getMarkerName(), new TextRange(startIndex, endIndex)), commentElement.getTextRange(), holder);
                });
            } else if ("import".equals(markerName)) {
                Map<String, Map<String, List<MarkerProblem>>> packageProblems = new HashMap<>();
                Map<String, List<MarkerProblem>> processorProblems = new HashMap<>();

                final Parser parser = new Parser();
                markerDuplicates.forEach(markerDuplicate -> {
                    final ParseResult result = parser.parse(markerDuplicate.getMarker(), markerDuplicate.getComment(), markerDuplicate.getMarker().getName());
                    if (result != null) {
                        MarkerElement markerElement = result.getMarkerElement();
                        Optional<Element> pkg = markerElement.getParameterValue("Pkg");

                        if (pkg.isEmpty() || StringUtils.isEmpty(pkg.get().getText())) {
                            return;
                        }

                        String[] parts = Utils.unquote(pkg.get().getText()).trim().split("@", 2);
                        String pkgName = parts[0].trim();

                        String pkgVersion = "latest";
                        if (parts.length == 2) {
                            pkgVersion = parts[1].trim();
                        }

                        if (pkgName.isEmpty()) {
                            return;
                        }

                        if (!packageProblems.containsKey(pkgName)) {
                            Map<String, List<MarkerProblem>> versionMap = new HashMap<>();
                            packageProblems.put(pkgName, versionMap);

                            List<MarkerProblem> markerProblems = new ArrayList<>();
                            markerProblems.add(new MarkerProblem(pkg.get()));
                            versionMap.put(pkgVersion, markerProblems);
                        } else {
                            List<MarkerProblem> markerProblems = packageProblems.get(pkgName)
                                    .computeIfAbsent(pkgVersion, k -> new ArrayList<>());
                            markerProblems.add(new MarkerProblem(pkg.get()));
                        }

                        Optional<Element> processor = markerElement.getParameterValue("Value");

                        if (processor.isEmpty() || StringUtils.isEmpty(Utils.unquote(processor.get().getText()))) {
                            return;
                        }

                        String processorName = Utils.unquote(processor.get().getText()).trim();
                        String processorKey = parts[0].trim() + "#" + processorName;
                        if (!processorProblems.containsKey(processorKey)) {
                            List<MarkerProblem> markerProblems = new ArrayList<>();
                            markerProblems.add(new MarkerProblem(processor.get(), processorName));
                            processorProblems.put(processorKey, markerProblems);
                        } else {
                            processorProblems.get(processorKey).add(new MarkerProblem(processor.get(), processorName));
                        }
                    }
                });

                packageProblems.forEach((pkg, versionMap) -> {
                    if (versionMap.size() > 1) {
                        versionMap.forEach((version, markerProblems) -> {
                            boolean packageExist = METADATA_PROVIDER.packageExists(element.getProject(), Utils.unquote(pkg + "@" + version));
                            if (!packageExist) {
                                return;
                            }

                            markerProblems.forEach(markerProblem -> {
                                highlightConflictPackageVersions(markerProblem.getElement(), commentElement.getTextRange(), holder);
                            });
                        });
                    }
                });

                processorProblems.forEach((processor, markerProblems) -> {
                    if (markerProblems.size() > 1) {
                        markerProblems.forEach(markerProblem -> {
                            highlightReImportedProcessor(markerProblem.getText(), markerProblem.getElement(), commentElement.getTextRange(), holder);
                        });
                    }
                });
            }
        });
    }

    private List<Comment> getAllComments(PsiElement element) {
        PsiElement current = element;
        if (element instanceof GoTypeSpec || GoPsiImplUtil.isFieldDefinition(element)) {
            current = current.getParent();
        }

        final List<Comment> comments = new ArrayList<>();
        while ((current = current.getPrevSibling()) != null) {
            if (Utils.isMarkerCommentElement(current)) {
                Optional<Comment> comment = Utils.getMarkerComment(current);
                comment.ifPresent(comments::add);
            } else if (!(current instanceof PsiWhiteSpace) && !(current instanceof PsiComment)) {
                break;
            }
        }

        return comments;
    }
}
