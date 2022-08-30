package com.github.procyonprojects.marker.completion;

import b.h.S;
import b.h.T;
import com.github.procyonprojects.marker.Icons;
import com.github.procyonprojects.marker.TargetInfo;
import com.github.procyonprojects.marker.Utils;
import com.github.procyonprojects.marker.comment.Comment;
import com.github.procyonprojects.marker.comment.ParseResult;
import com.github.procyonprojects.marker.comment.Parser;
import com.github.procyonprojects.marker.element.*;
import com.github.procyonprojects.marker.metadata.*;
import com.github.procyonprojects.marker.metadata.provider.MetadataProvider;
import com.github.procyonprojects.marker.metadata.v1.EnumValue;
import com.github.procyonprojects.marker.metadata.v1.Marker;
import com.github.procyonprojects.marker.metadata.v1.Parameter;
import com.github.procyonprojects.marker.metadata.v1.Processor;
import com.github.procyonprojects.marker.metadata.v1.schema.SliceSchema;
import com.github.procyonprojects.marker.metadata.v1.schema.StringSchema;
import com.github.procyonprojects.marker.reference.MarkerReference;
import com.github.procyonprojects.marker.scope.FunctionScopeProcessor;
import com.github.procyonprojects.marker.scope.TypeScopeProcessor;
import com.goide.psi.impl.expectedTypes.GoExpectedTypes;
import com.goide.psi.impl.fake.GoFakeResolvable;
import com.intellij.codeInsight.completion.*;
import com.intellij.codeInsight.lookup.LookupElementBuilder;
import com.intellij.icons.AllIcons;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
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

    public MarkerCompletionContributor() {
        extend(CompletionType.BASIC, PlatformPatterns.psiElement().with(IS_MARKER_COMMENT), new CompletionProvider<>() {
            @Override
            protected void addCompletions(@NotNull CompletionParameters parameters, @NotNull ProcessingContext context, @NotNull CompletionResultSet result) {
                addMarkerCompletions(parameters, result);
                addParameterCompletions(parameters, context, result);
                result.stopHere();
                result.runRemainingContributors(parameters, true);
            }
        });
        extend(CompletionType.SMART, PlatformPatterns.psiElement().with(IS_MARKER_COMMENT), new CompletionProvider<>() {
            @Override
            protected void addCompletions(@NotNull CompletionParameters parameters, @NotNull ProcessingContext context, @NotNull CompletionResultSet result) {
                addMarkerCompletions(parameters, result);
                addParameterCompletions(parameters, context, result);
                result.stopHere();
                result.runRemainingContributors(parameters, true);
            }
        });
    }

    private void addMarkerCompletions(@NotNull CompletionParameters parameters, @NotNull CompletionResultSet result) {
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
        final String lookupMarkerName = markerText.substring(1);

        Project project = parameters.getOriginalPosition().getProject();
        VirtualFile file = parameters.getOriginalFile().getVirtualFile();
        Map<String, String> importMap = METADATA_PROVIDER.getImportMap(project, file);

        importMap.entrySet().stream().filter(entry -> entry.getKey().startsWith("p")).forEach(entry -> {
            String processorName = entry.getKey().substring(1);
            String pkg = entry.getValue();
            String alias = null;
            if (importMap.containsKey("a" + processorName)) {
                alias = processorName;
                processorName = importMap.get("a" + processorName);
            }

            Optional<Processor> processor = METADATA_PROVIDER.getProcessorByName(project, pkg, processorName);
            if (processor.isEmpty()) {
                return;
            }

            for (Marker marker : processor.get().getMarkers()) {
                if (CollectionUtils.isEmpty(marker.getTargets()) || !marker.getTargets().contains(targetInfo.getTarget())) {
                    continue;
                }

                String markerName = marker.getName();
                if (StringUtils.isNotBlank(alias)) {
                    markerName = markerName.replace(processorName, alias);
                }

                if (!markerName.startsWith(lookupMarkerName)) {
                    continue;
                }

                String prefix = lookupMarkerName;
                int lastIndexOfColon = lookupMarkerName.lastIndexOf(":");
                if (lastIndexOfColon != -1) {
                    prefix = prefix.substring(0, lastIndexOfColon + 1);
                    markerName = markerName.replace(prefix, "");
                }

                elements.add(LookupElementBuilder.create(markerName)
                        .withPresentableText(markerName)
                        .withItemTextForeground(JBColor.DARK_GRAY)
                        .bold()
                        .withIcon(StringUtils.isEmpty(pkg) ? Icons.PredefinedMarker : Icons.Marker)
                        .withTailText(" " + (StringUtils.isEmpty(pkg) ? "predefined" : pkg))
                        .withTypeText(marker.isSyntaxFree() ? "Free Syntax" : "")
                        .withTypeIconRightAligned(true));

                Optional<Parameter> defaultParameter = marker.getParameters().stream().filter(parameter -> "Value".equals(parameter.getName())).findFirst();
                String finalMarkerName = markerName;
                defaultParameter.ifPresent(parameter -> {
                    elements.add(LookupElementBuilder.create(finalMarkerName + "=" + getParameterDefaultValue(parameter))
                            .withPresentableText(finalMarkerName)
                            .withItemTextForeground(JBColor.DARK_GRAY)
                            .bold()
                            .withIcon(StringUtils.isEmpty(pkg) ? Icons.PredefinedMarker : Icons.Marker)
                            .withTailText(" " + (StringUtils.isEmpty(pkg) ? "predefined" : pkg))
                            .withTypeText("Value Shorthand Syntax | " + parameter.getType().getPresentableText())
                            .withTypeIconRightAligned(true));
                });
            }
        });

        importMap.entrySet().stream().filter(entry -> entry.getKey().startsWith("p")).forEach(entry -> {
            String processorName = entry.getKey().substring(1);
            String pkg = entry.getValue();
            String alias = null;
            if (importMap.containsKey("a" + processorName)) {
                alias = processorName;
                processorName = importMap.get("a" + processorName);
            }

            Optional<Processor> processor = METADATA_PROVIDER.getProcessorByName(project, pkg, processorName);
            if (processor.isEmpty()) {
                return;
            }

            for (Marker marker : processor.get().getMarkers()) {
                if (CollectionUtils.isEmpty(marker.getTargets()) || !marker.getTargets().contains(targetInfo.getTarget())) {
                    continue;
                }

                String markerName = marker.getName();
                if (StringUtils.isNotBlank(alias)) {
                    markerName = markerName.replace(processorName, alias);
                }

                if (StringUtils.isEmpty(lookupMarkerName)) {
                    return;
                }


                String finalMarkerName = markerName;
                if (!lookupMarkerName.endsWith(":")) {

                    if (markerName.startsWith(lookupMarkerName) || markerName.equals(lookupMarkerName)) {
                        String prefix = markerName;
                        if (prefix.lastIndexOf(":") != -1) {
                            prefix = prefix.substring(prefix.lastIndexOf(":" + 1));
                        }

                        String finalPrefix = prefix;
                        marker.getParameters().forEach(parameter -> {
                            elements.add(getParameter(finalPrefix + ":" + parameter.getName(), finalMarkerName + ":" + parameter.getName(), parameter));
                        });
                    } else {
                        int lastColonIndex = lookupMarkerName.lastIndexOf(":");
                        if (lastColonIndex != -1) {
                            final String markerNameCandidate = lookupMarkerName.substring(0, lastColonIndex);
                            final String argumentNameCandidate = lookupMarkerName.substring(lastColonIndex + 1);

                            if (!markerName.equals(markerNameCandidate)) {
                                return;
                            }

                            final List<Parameter> parameterCandidates = marker.getParameters().stream()
                                    .filter(parameter -> parameter.getName().startsWith(argumentNameCandidate))
                                    .collect(Collectors.toList());

                            if (!parameterCandidates.isEmpty()) {
                                parameterCandidates.forEach(parameter -> {
                                    elements.add(getParameter(parameter.getName(), finalMarkerName + ":" + parameter.getName(), parameter));
                                });
                            }
                        }
                    }
                } else if (finalMarkerName.equals(lookupMarkerName.substring(0, lookupMarkerName.length()-1))) {
                    marker.getParameters().forEach(parameter -> {
                        elements.add(getParameter(parameter.getName(), finalMarkerName + ":" + parameter.getName(), parameter));
                    });
                }
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

        Project project = parameters.getOriginalPosition().getProject();
        VirtualFile file = parameters.getOriginalFile().getVirtualFile();

        Optional<Marker> marker = METADATA_PROVIDER.findMarker(project, file, anonymousName, targetInfo.getTarget());

        String aliasMarkerName;
        if (marker.isEmpty()) {
            marker = METADATA_PROVIDER.findMarker(project, file, markerName, targetInfo.getTarget());
            if (marker.isEmpty()) {
                return;
            }
            aliasMarkerName = markerName;
        } else {
            aliasMarkerName = markerName;
        }


        final List<LookupElementBuilder> elements = new ArrayList<>();
        final Parser parser = new Parser();
        final ParseResult result = parser.parse(marker.get(), comment.get(), aliasMarkerName);
        if (result != null) {
            final MarkerElement markerElement = result.getMarkerElement();
            final List<String> parameterList = markerElement.getParameters();
            final List<Parameter> unusedParameters = new ArrayList<>();
            markerElement.getMarker().getParameters().forEach(parameter -> {
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

                    if (nameElement instanceof ExpectedElement && equalSignElement == null && valueElement == null && (parameterElement.getNext() == null || parameterElement.getNext() instanceof ParameterElement && nameElement.getRange().getStartOffset() >= parameters.getOffset())) {
                        if (parameterElement.getPrevious() != null && parameterElement.getPrevious().getRange().getStartOffset() >= parameters.getOffset()) {
                            break;
                        }
                        unusedParameters.forEach(parameter -> {
                            elements.add(getParameter(parameter.getName(), parameter.getName(), parameter));
                        });
                        break;
                    } else if (nameElement instanceof UnresolvedElement && equalSignElement == null && valueElement == null && parameterElement.getNext() == null || equalSignElement instanceof ExpectedElement) {
                        unusedParameters.forEach(parameter -> {
                            elements.add(getParameter(parameter.getName(), parameter.getName(), parameter));
                        });
                    } else if (equalSignElement != null && equalSignElement.getRange().getStartOffset() <= parameters.getOffset()) {
                        if (parameterElement.getTypeInfo().getActualType() == Type.AnyType || valueElement instanceof ExpectedElement) {
                            if (valueElement != null && (",".equals(valueElement.getText()) || valueElement instanceof ExpectedElement)) {
                                if (valueElement.getRange().getEndOffset() >= parameters.getOffset()) {
                                    elements.addAll(fillParameterValues(parameterElement.getTypeInfo(), parameterElement.getParameter(), parameters, resultSet));
                                }
                            } else if (parameterElement.getNext() == null || parameterElement.getNext().getRange().getStartOffset() >= parameters.getOffset()) {
                                if (valueElement instanceof SliceElement) {
                                    SliceElement sliceElement = (SliceElement) valueElement;
                                    if (sliceElement.getLeftBrace() != null || sliceElement.getRightBrace() != null || sliceElement.getItems().size() != 0) {
                                        current = current.getNext();
                                        continue;
                                    }
                                }
                                elements.addAll(fillParameterValues(parameterElement.getTypeInfo(), parameterElement.getParameter(), parameters, resultSet));
                            }
                        } else if (valueElement instanceof MapElement || typeInfo.getActualType() == Type.MapType) {
                            final MapElement mapElement = (MapElement) valueElement;

                            if (mapElement == null) {
                                elements.add(getEmptyMapValue());
                            } else if (mapElement.getLeftBrace() instanceof ExpectedElement && mapElement.getLeftBrace().getRange().getEndOffset() > parameters.getOffset()) {
                                elements.add(getEmptyMapValue());
                            }

                        } else if (valueElement instanceof SliceElement || typeInfo.getActualType() == Type.SliceType) {
                            final SliceElement sliceElement = (SliceElement) valueElement;

                            final Set<String> usedItems = new HashSet<>();

                            if(sliceElement == null) {
                                elements.addAll(fillSliceItemValues(parameterElement.getParameter(), usedItems, true, parameters, resultSet));
                            } else {
                                sliceElement.getItems().forEach(element -> {
                                    usedItems.add(element.getText());
                                });

                                if (sliceElement.getLeftBrace() == null && sliceElement.getRightBrace() == null
                                        && (parameterElement.getNext() == null || parameterElement.getNext().getRange().getEndOffset() > parameters.getOffset())) {
                                    elements.addAll(fillSliceItemValues(sliceElement.getParameter(), usedItems, usedItems.isEmpty(), parameters, resultSet));
                                } else if (sliceElement.getLeftBrace() != null && sliceElement.getRightBrace() != null
                                        && sliceElement.getLeftBrace().getRange().getStartOffset() < parameters.getOffset()
                                        && sliceElement.getRightBrace().getRange().getStartOffset() >= parameters.getOffset()) {
                                    elements.addAll(fillSliceItemValues(sliceElement.getParameter(), usedItems, false, parameters, resultSet));
                                }
                            }

                        } else if (parameterElement.getNext() == null || parameterElement.getNext().getRange().getStartOffset() >= parameters.getOffset()) {
                            if (valueElement instanceof StringElement && StringUtils.isNotEmpty(valueElement.getText())) {
                                current = current.getNext();
                                continue;
                            }
                            elements.addAll(fillParameterValues(parameterElement.getTypeInfo(), parameterElement.getParameter(), parameters, resultSet));
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
                .withIcon(parameter.isRequired() ? Icons.RequiredParameter : Icons.Parameter)
                .withTailText(tailTextBuilder.toString())
                .withTypeText(parameter.getSchema().getPresentableText())
                .withTypeIconRightAligned(true);
    }

    private String getParameterDefaultValue(Parameter parameter) {
        final Type type = parameter.getType().getActualType();
        if (type == Type.InvalidType) {
            return "";
        }

        if (type == Type.StringType) {
            StringSchema stringSchema = (StringSchema) parameter.getSchema();
            if (CollectionUtils.isNotEmpty(stringSchema.getEnums())) {
                return "";
            }
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

    private List<LookupElementBuilder> fillParameterValues(TypeInfo typeInfo, Parameter parameter, @NotNull CompletionParameters parameters, @NotNull CompletionResultSet result) {
        final Type type = typeInfo.getActualType();
        if (type == Type.InvalidType) {
            return Collections.emptyList();
        }

        final List<LookupElementBuilder> elements = new ArrayList<>();

        if (type == Type.StringType || type == Type.AnyType) {
            if (type == Type.StringType) {
                StringSchema stringSchema = (StringSchema) parameter.getSchema();
                stringSchema.getEnums().forEach(enumValue -> {
                    elements.add(getEnumValue(String.valueOf(enumValue), String.valueOf(enumValue)));
                });
            }
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

        if (type == Type.GoType) {
            addTypeCompletions(parameters, result);
        }

        if (type == Type.GoFunction) {
            addFunctionCompletions(parameters, result);
        }

        return elements;
    }

    private List<LookupElementBuilder> fillSliceItemValues(Parameter parameter, Set<String> usedItems, boolean emptySlice, @NotNull CompletionParameters parameters, @NotNull CompletionResultSet result) {

        final List<LookupElementBuilder> elements = new ArrayList<>();

        if (emptySlice) {
            elements.add(getEmptySliceValue());
        }

        SliceSchema sliceSchema = (SliceSchema) parameter.getSchema();
        if (!(sliceSchema.getItemSchema() instanceof StringSchema)) {
            return elements;
        }

        StringSchema stringSchema = (StringSchema) sliceSchema.getItemSchema();
        final List<EnumValue> enumList = stringSchema.getEnums();

        if (CollectionUtils.isNotEmpty(enumList)) {
            enumList.forEach(enumValue -> {
                if (!usedItems.contains(String.valueOf(enumValue.getValue()))) {
                    elements.add(getEnumValue(String.valueOf(enumValue.getValue()), String.valueOf(enumValue.getValue())));
                }
            });
        } else {
            elements.addAll(fillParameterValues(parameter.getType().getItemType(), parameter, parameters, result));
        }

        return elements;
    }


    private LookupElementBuilder getEmptySliceValue() {
        return LookupElementBuilder.create("{}")
                .withPresentableText("{}")
                .withItemTextForeground(JBColor.DARK_GRAY)
                .bold()
                .withIcon(Icons.Slice)
                .withTypeText("slice")
                .withTypeIconRightAligned(true);
    }

    private LookupElementBuilder getEmptyMapValue() {
        return LookupElementBuilder.create("{key:\"value\"}")
                .withPresentableText("{key:\"value\"}")
                .withItemTextForeground(JBColor.DARK_GRAY)
                .bold()
                .withIcon(Icons.Map)
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

    private void addTypeCompletions(@NotNull CompletionParameters parameters, @NotNull CompletionResultSet resultSet) {
        new MarkerReference(new GoFakeResolvable(parameters.getPosition())).processResolveVariants(new TypeScopeProcessor(resultSet, parameters.getOriginalFile(), false, GoExpectedTypes.EMPTY, parameters));
    }

    private void addFunctionCompletions(@NotNull CompletionParameters parameters, @NotNull CompletionResultSet resultSet) {
        new MarkerReference(new GoFakeResolvable(parameters.getPosition())).processResolveVariants(new FunctionScopeProcessor(resultSet, parameters.getOriginalFile(), false, GoExpectedTypes.EMPTY, parameters));
    }
}
