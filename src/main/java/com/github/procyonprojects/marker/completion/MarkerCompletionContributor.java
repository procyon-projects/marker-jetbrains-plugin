package com.github.procyonprojects.marker.completion;

import com.github.procyonprojects.marker.Icons;
import com.github.procyonprojects.marker.TargetInfo;
import com.github.procyonprojects.marker.Utils;
import com.github.procyonprojects.marker.comment.Comment;
import com.github.procyonprojects.marker.comment.ParseResult;
import com.github.procyonprojects.marker.comment.Parser;
import com.github.procyonprojects.marker.element.*;
import com.github.procyonprojects.marker.metadata.*;
import com.github.procyonprojects.marker.metadata.Enum;
import com.github.procyonprojects.marker.metadata.provider.DefinitionProvider;
import com.intellij.codeInsight.completion.*;
import com.intellij.codeInsight.lookup.LookupElementBuilder;
import com.intellij.icons.AllIcons;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.patterns.PatternCondition;
import com.intellij.patterns.PlatformPatterns;
import com.intellij.psi.PsiComment;
import com.intellij.psi.PsiElement;
import com.intellij.ui.JBColor;
import com.intellij.util.ProcessingContext;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.stream.Collectors;

public class MarkerCompletionContributor extends CompletionContributor {

    private static final DefinitionProvider definitionProvider = ApplicationManager.getApplication().getService(DefinitionProvider.class);

    private final static PatternCondition<PsiElement> IS_MARKER_COMMENT = new PatternCondition<>("") {
        @Override
        public boolean accepts(@NotNull PsiElement element, ProcessingContext context) {
            if (Utils.isMarkerCommentElement(element)) {
                return true;
            }

            return Utils.findMarkerCommentFirstLine(element).isPresent();
        }
    };

    public MarkerCompletionContributor() {
        extend(CompletionType.BASIC, PlatformPatterns.psiElement().with(IS_MARKER_COMMENT), new CompletionProvider<>() {
            @Override
            protected void addCompletions(@NotNull CompletionParameters parameters, @NotNull ProcessingContext context, @NotNull CompletionResultSet result) {
                addMarkerCompletions(parameters, context, result);
                addParameterCompletions(parameters, context, result);
                result.stopHere();
            }
        });
        extend(CompletionType.SMART, PlatformPatterns.psiElement().with(IS_MARKER_COMMENT), new CompletionProvider<>() {
            @Override
            protected void addCompletions(@NotNull CompletionParameters parameters, @NotNull ProcessingContext context, @NotNull CompletionResultSet result) {
                addMarkerCompletions(parameters, context, result);
                addParameterCompletions(parameters, context, result);
                result.stopHere();
            }
        });
    }

    private void addMarkerCompletions(@NotNull CompletionParameters parameters, @NotNull ProcessingContext context, @NotNull CompletionResultSet result) {
        if (!(parameters.getPosition() instanceof PsiComment && parameters.getOriginalPosition() instanceof PsiComment)) {
            return;
        }

        String markerText = parameters.getPosition().getText().substring(2).stripLeading().split("IntellijIdeaRulezzz")[0];

        if (!markerText.startsWith("+") || StringUtils.containsWhitespace(markerText)) {
            return;
        }

        final TargetInfo targetInfo = Utils.findTarget((PsiComment) parameters.getOriginalPosition());
        if (targetInfo.getTarget() == Target.INVALID) {
            return;
        }

        final List<LookupElementBuilder> elements = new ArrayList<>();
        final List<Definition> definitions = definitionProvider.markers(targetInfo.getTarget());

        final String lookupMarkerName = markerText.substring(1);

        definitions.forEach(definition -> {

            if (!definition.getName().startsWith(lookupMarkerName)) {
                return;
            }

            String markerName = definition.getName();
            String prefix = lookupMarkerName;

            int lastIndex = lookupMarkerName.lastIndexOf(":");
            if (lastIndex != -1) {
                prefix = prefix.substring(0, lastIndex + 1);
                markerName = definition.getName().replace(prefix, "");
            }

            elements.add(LookupElementBuilder.create(markerName)
                    .withPresentableText(definition.getName())
                    .withItemTextForeground(JBColor.DARK_GRAY)
                    .bold()
                    .withIcon(Icons.MarkerIcon)
                    .withTailText(" " + definition.getPkgId())
                    .withTypeText(definition.isSyntaxFree() ? "Free Syntax" : "")
                    .withTypeIconRightAligned(true));

            Optional<Parameter> defaultParameter = definition.getParameters().stream().filter(parameter -> "Value".equals(parameter.getName())).findFirst();
            defaultParameter.ifPresent(parameter -> {
                elements.add(LookupElementBuilder.create(definition.getName() + "=" + getParameterDefaultValue(parameter))
                        .withPresentableText(definition.getName())
                        .withItemTextForeground(JBColor.DARK_GRAY)
                        .bold()
                        .withIcon(Icons.MarkerIcon)
                        .withTailText(" " + definition.getPkgId())
                        .withTypeText("Value Shorthand Syntax | " + parameter.getType().getPresentableText())
                        .withTypeIconRightAligned(true));
            });
        });

        definitions.forEach(definition -> {
            if (StringUtils.isEmpty(lookupMarkerName)) {
                return;
            }

            if (!lookupMarkerName.endsWith(":")) {

                if (definition.getName().startsWith(lookupMarkerName) || definition.getName().equals(lookupMarkerName)) {
                    String prefix = definition.getName();
                    if (prefix.lastIndexOf(":") != -1) {
                        prefix = prefix.substring(prefix.lastIndexOf(":" + 1));
                    }

                    String finalPrefix = prefix;
                    definition.getParameters().forEach(parameter -> {
                        elements.add(getParameter(finalPrefix + ":" + parameter.getName(), definition.getName() + ":" + parameter.getName(), parameter));
                    });
                } else {
                    int lastColonIndex = lookupMarkerName.lastIndexOf(":");
                    if (lastColonIndex != -1) {
                        final String markerNameCandidate = lookupMarkerName.substring(0, lastColonIndex);
                        final String argumentNameCandidate = lookupMarkerName.substring(lastColonIndex + 1);

                        if (!definition.getName().equals(markerNameCandidate)) {
                            return;
                        }

                        final List<Parameter> parameterCandidates = definition.getParameters().stream()
                                .filter(parameter -> parameter.getName().startsWith(argumentNameCandidate))
                                .collect(Collectors.toList());

                        if (!parameterCandidates.isEmpty()) {
                            parameterCandidates.forEach(parameter -> {
                                elements.add(getParameter(parameter.getName(), definition.getName() + ":" + parameter.getName(), parameter));
                            });
                        }
                    }
                }
            } else if (definition.getName().equals(lookupMarkerName.substring(0, lookupMarkerName.length()-1))) {
                definition.getParameters().forEach(parameter -> {
                    elements.add(getParameter(parameter.getName(), definition.getName() + ":" + parameter.getName(), parameter));
                });
            }
        });

        result.addAllElements(elements);
    }


    private void addParameterCompletions(@NotNull CompletionParameters parameters, @NotNull ProcessingContext context, @NotNull CompletionResultSet resultSet) {
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

        final Comment.Line firstLine = comment.get().firstLine().get();
        final String firstLineText = firstLine.getText().trim();
        final String anonymousName = Utils.getMarkerAnonymousName(firstLineText);
        final String markerName = Utils.getMarkerName(firstLineText);

        Optional<Definition> definition = definitionProvider.find(targetInfo.getTarget(), anonymousName);

        if (definition.isEmpty()) {
            definition = definitionProvider.find(targetInfo.getTarget(), markerName);
            if (definition.isEmpty()) {
                return;
            }
        }


        final List<LookupElementBuilder> elements = new ArrayList<>();
        final Parser parser = new Parser();
        final ParseResult result = parser.parse(definition.get(), comment.get());
        if (result != null) {
            final MarkerElement markerElement = result.getMarkerElement();
            final List<String> parameterList = markerElement.getParameters();
            final List<Parameter> unusedParameters = new ArrayList<>();
            markerElement.getDefinition().getParameters().forEach(parameter -> {
                if (!parameterList.contains(parameter.getName())) {
                    unusedParameters.add(parameter);
                }
            });

            Element current = markerElement.getNext();

            while (current != null) {

                if (current.getPrevious() != null && ",".equals(current.getText()) && (current.getNext() == null || ",".equals(current.getNext().getText()))) {
                    if (current.getRange().getEndOffset() <= parameters.getOffset() && (current.getNext() == null || current.getNext().getRange().getStartOffset() > parameters.getOffset())) {
                        unusedParameters.forEach(parameter -> {
                            elements.add(getParameter(parameter.getName(), parameter.getName(), parameter));
                        });
                    }
                } else if (current instanceof ParameterElement) {
                    final ParameterElement parameterElement = (ParameterElement) current;
                    final Element nameElement = parameterElement.getName();
                    final Element equalSignElement = parameterElement.getEqualSign();
                    final Element valueElement = parameterElement.getValue();
                    final TypeInfo typeInfo = parameterElement.getTypeInfo();

                    if (nameElement instanceof ExpectedElement && equalSignElement == null && valueElement == null && parameterElement.getNext() instanceof ParameterElement && nameElement.getRange().getStartOffset() >= parameters.getOffset()) {
                        if (parameterElement.getPrevious() != null && parameterElement.getPrevious().getRange().getStartOffset() >= parameters.getOffset()) {
                            break;
                        }
                        unusedParameters.forEach(parameter -> {
                            elements.add(getParameter(parameter.getName(), parameter.getName(), parameter));
                        });
                        break;
                    } else if (equalSignElement != null && equalSignElement.getRange().getStartOffset() <= parameters.getOffset()) {
                        if (parameterElement.getTypeInfo().getActualType() == Type.AnyType || valueElement instanceof ExpectedElement) {
                            if (valueElement != null && ",".equals(valueElement.getText())) {
                                if (valueElement.getRange().getEndOffset() > parameters.getOffset()) {
                                    elements.addAll(fillParameterValues(parameterElement.getTypeInfo()));
                                }
                            } else if (parameterElement.getNext() == null || parameterElement.getNext().getRange().getStartOffset() >= parameters.getOffset()) {
                                if (valueElement instanceof SliceElement) {
                                    SliceElement sliceElement = (SliceElement) valueElement;
                                    if (sliceElement.getLeftBrace() != null || sliceElement.getRightBrace() != null || sliceElement.getItems().size() != 0) {
                                        current = current.getNext();
                                        continue;
                                    }
                                }
                                elements.addAll(fillParameterValues(parameterElement.getTypeInfo()));
                            }
                        } else if (valueElement instanceof MapElement || typeInfo.getActualType() == Type.MapType) {
                            final MapElement mapElement = (MapElement) valueElement;

                            if (mapElement == null) {
                                elements.add(getEmptyMapValue());
                            } else if (mapElement.getLeftBrace() instanceof ExpectedElement && mapElement.getLeftBrace() == null && mapElement.getLeftBrace().getRange().getEndOffset() > parameters.getOffset()) {
                                elements.add(getEmptyMapValue());
                            }

                        } else if (valueElement instanceof SliceElement || typeInfo.getActualType() == Type.SliceType) {
                            final SliceElement sliceElement = (SliceElement) valueElement;

                            final Set<String> usedItems = new HashSet<>();

                            if(sliceElement == null) {
                                elements.addAll(fillSliceItemValues(parameterElement.getParameter(), usedItems, true));
                            } else {
                                sliceElement.getItems().forEach(element -> {
                                    usedItems.add(element.getText());
                                });

                                if (sliceElement.getLeftBrace() == null && sliceElement.getRightBrace() == null
                                        && (parameterElement.getNext() == null || parameterElement.getNext().getRange().getEndOffset() > parameters.getOffset())) {
                                    elements.addAll(fillSliceItemValues(sliceElement.getParameter(), usedItems, usedItems.isEmpty()));
                                } else if (sliceElement.getLeftBrace() != null && sliceElement.getRightBrace() != null
                                        && sliceElement.getLeftBrace().getRange().getStartOffset() < parameters.getOffset()
                                        && sliceElement.getRightBrace().getRange().getStartOffset() >= parameters.getOffset()) {
                                    elements.addAll(fillSliceItemValues(sliceElement.getParameter(), usedItems, false));
                                }
                            }

                        } else if (parameterElement.getNext() == null || parameterElement.getNext().getRange().getStartOffset() >= parameters.getOffset()) {
                            if (valueElement instanceof StringElement && StringUtils.isNotEmpty(valueElement.getText())) {
                                current = current.getNext();
                                continue;
                            }
                            elements.addAll(fillParameterValues(parameterElement.getTypeInfo()));
                        }
                    }
                }

                current = current.getNext();
            }
        }

        resultSet.addAllElements(elements);
    }


    private LookupElementBuilder getParameter(String lookupString, String presentableText, Parameter parameter) {
        final StringBuilder tailTextBuilder = new StringBuilder();
        tailTextBuilder.append(" ").append(parameter.getDescription());

        if (parameter.getDefaultValue() != null) {
            tailTextBuilder.append(" ").append("default").append(" ").append(parameter.getDefaultValue());
        }

        return LookupElementBuilder.create(lookupString + "=" + getParameterDefaultValue(parameter))
                .withPresentableText(presentableText)
                .withItemTextForeground(JBColor.DARK_GRAY)
                .bold()
                .withIcon(parameter.isRequired() ? AllIcons.Nodes.PropertyWrite : AllIcons.Nodes.Property)
                .withTailText(tailTextBuilder.toString())
                .withTypeText(parameter.getType().getPresentableText())
                .withTypeIconRightAligned(true);
    }

    private String getParameterDefaultValue(Parameter parameter) {
        final Type type = parameter.getType().getActualType();
        if (type == Type.InvalidType) {
            return "";
        }

        if (type == Type.StringType) {
            return "\"\"";
        } else if (type == Type.SliceType || type == Type.MapType) {
            return "{}";
        } else if (type == Type.BooleanType) {
            return "false";
        } else if (type == Type.SignedIntegerType || type == Type.UnsignedIntegerType) {
            return "0";
        }

        return "";
    }

    private List<LookupElementBuilder> fillParameterValues(TypeInfo typeInfo) {
        final Type type = typeInfo.getActualType();
        if (type == Type.InvalidType) {
            return Collections.emptyList();
        }

        final List<LookupElementBuilder> elements = new ArrayList<>();

        if (type == Type.StringType || type == Type.AnyType) {
            elements.add(getStringValue("\"\"", "\"\""));
            elements.add(getStringValue("''", "''"));
            elements.add(getStringValue("``", "``"));
        }

        if (type == Type.BooleanType || type == Type.AnyType) {
            elements.add(getConstantValue("true", "true", "bool"));
            elements.add(getConstantValue("false", "false", "bool"));
        }

        if (type == Type.SliceType || type == Type.AnyType) {
            elements.add(getEmptySliceValue());
        }

        if (type == Type.MapType  || type == Type.AnyType) {
            elements.add(getEmptyMapValue());
        }

        return elements;
    }

    private List<LookupElementBuilder> fillSliceItemValues(Parameter parameter, Set<String> usedItems, boolean emptySlice) {

        final List<LookupElementBuilder> elements = new ArrayList<>();

        if (emptySlice) {
            elements.add(getEmptySliceValue());
        }

        final List<Enum> enumList = parameter.getEnumValues();

        if (CollectionUtils.isNotEmpty(enumList)) {
            enumList.forEach(enumValue -> {
                if (!usedItems.contains(String.valueOf(enumValue.getValue()))) {
                    elements.add(getEnumValue(String.valueOf(enumValue.getValue()), String.valueOf(enumValue.getValue())));
                }
            });
        } else {
            elements.addAll(fillParameterValues(parameter.getType().getItemType()));
        }

        return elements;
    }


    private LookupElementBuilder getEmptySliceValue() {
        return LookupElementBuilder.create("{}")
                .withPresentableText("{}")
                .withItemTextForeground(JBColor.DARK_GRAY)
                .bold()
                .withIcon(Icons.SliceIcon)
                .withTypeText("slice")
                .withTypeIconRightAligned(true);
    }

    private LookupElementBuilder getEmptyMapValue() {
        return LookupElementBuilder.create("{key:\"value\"}")
                .withPresentableText("{key:\"value\"}")
                .withItemTextForeground(JBColor.DARK_GRAY)
                .bold()
                .withIcon(Icons.MapIcon)
                .withTypeText("map")
                .withTypeIconRightAligned(true);
    }

    private LookupElementBuilder getConstantValue(String lookupString, String presentableText, String typeText) {
        return LookupElementBuilder.create(lookupString)
                .withPresentableText(presentableText)
                .withItemTextForeground(JBColor.DARK_GRAY)
                .bold()
                .withIcon(AllIcons.Nodes.Constant)
                .withTypeText(typeText)
                .withTypeIconRightAligned(true);
    }

    private LookupElementBuilder getStringValue(String lookupString, String presentableText) {
        return LookupElementBuilder.create(lookupString)
                .withPresentableText(presentableText)
                .withItemTextForeground(JBColor.DARK_GRAY)
                .bold()
                .withIcon(AllIcons.Nodes.Static)
                .withTypeText("string")
                .withTypeIconRightAligned(true);
    }

    private LookupElementBuilder getEnumValue(String lookupString, String presentableText) {
        return LookupElementBuilder.create(lookupString)
                .withPresentableText(presentableText)
                .withItemTextForeground(JBColor.DARK_GRAY)
                .bold()
                .withIcon(AllIcons.Nodes.Enum)
                .withTypeText("enum")
                .withTypeIconRightAligned(true);
    }

    @Override
    public void fillCompletionVariants(@NotNull CompletionParameters parameters, @NotNull CompletionResultSet result) {
        super.fillCompletionVariants(parameters, result);
    }
}
