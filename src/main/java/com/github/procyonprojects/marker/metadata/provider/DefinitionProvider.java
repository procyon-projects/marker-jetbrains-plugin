package com.github.procyonprojects.marker.metadata.provider;

import com.github.procyonprojects.marker.Constants;
import com.github.procyonprojects.marker.metadata.*;
import com.github.procyonprojects.marker.metadata.Enum;
import com.intellij.openapi.components.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class DefinitionProvider {

    private final static Map<String, List<Definition>> definitionMap = new HashMap<>();

    static {
        List<Parameter> parameters = new ArrayList<>();
        parameters.add(new Parameter("Value", new TypeInfo(Type.StringType), "The name of the marker processor", true));
        parameters.add(new Parameter("Pkg", new TypeInfo(Type.StringType), "The name of the package to import as marker processor", true));
        parameters.add(new Parameter("Alias", new TypeInfo(Type.StringType), "Alias for the name of the marker processor"));
        final Definition importMarker = new Definition("import", Constants.MARKER_PACKAGE_ID, List.of(Target.PACKAGE_LEVEL), parameters);

        final Definition deprecatedMarker = new Definition("deprecated", Constants.MARKER_PACKAGE_ID, List.of(Target.FIELD_LEVEL,
                Target.FUNCTION_LEVEL,
                Target.INTERFACE_LEVEL,
                Target.INTERFACE_METHOD_LEVEL,
                Target.STRUCT_LEVEL,
                Target.STRUCT_METHOD_LEVEL
        ), true);

        final Definition overrideMarker = new Definition("override", Constants.MARKER_PACKAGE_ID, List.of(Target.STRUCT_METHOD_LEVEL));

        parameters = new ArrayList<>();
        parameters.add(new Parameter("Value", new TypeInfo(Type.StringType), "The name of the marker", true));
        parameters.add(new Parameter("Description", new TypeInfo(Type.StringType), "The description of the marker", true));
        parameters.add(new Parameter("Processor", new TypeInfo(Type.StringType), "The name of the marker processor"));
        parameters.add(new Parameter("Repeatable", new TypeInfo(Type.BooleanType), "Whether the marker is repeatable", Boolean.TRUE));
        parameters.add(new Parameter("SyntaxFree", new TypeInfo(Type.BooleanType), "Whether the marker is syntax-free", Boolean.FALSE));

        final List<com.github.procyonprojects.marker.metadata.Enum> targetList = new ArrayList<>();
        targetList.add(new com.github.procyonprojects.marker.metadata.Enum("PACKAGE_LEVEL"));
        targetList.add(new com.github.procyonprojects.marker.metadata.Enum("INTERFACE_LEVEL"));
        targetList.add(new com.github.procyonprojects.marker.metadata.Enum("STRUCT_LEVEL"));
        targetList.add(new com.github.procyonprojects.marker.metadata.Enum("FUNCTION_LEVEL"));
        targetList.add(new com.github.procyonprojects.marker.metadata.Enum("FIELD_LEVEL"));
        targetList.add(new com.github.procyonprojects.marker.metadata.Enum("STRUCT_METHOD_LEVEL"));
        targetList.add(new Enum("INTERFACE_METHOD_LEVEL"));

        parameters.add(new Parameter("Targets", new TypeInfo(Type.SliceType, new TypeInfo(Type.StringType)), "The targets of the marker", true, targetList));

        final Definition marker = new Definition("marker", Constants.MARKER_PACKAGE_ID, List.of(Target.STRUCT_LEVEL), parameters);

        parameters = new ArrayList<>();
        parameters.add(new Parameter("Value", new TypeInfo(Type.StringType), "The name of the parameter to bind", true));
        parameters.add(new Parameter("Description", new TypeInfo(Type.StringType), "The description of the parameter", true));
        parameters.add(new Parameter("Required", new TypeInfo(Type.BooleanType), "Whether the parameter is required", Boolean.FALSE));
        parameters.add(new Parameter("Default", TypeInfo.ANY_TYPE_INFO, "The default value"));
        parameters.add(new Parameter("Enum", new TypeInfo(Type.SliceType, new TypeInfo(Type.StringType)), "The enum values"));
        final Definition markerParameter = new Definition("marker:parameter", Constants.MARKER_PACKAGE_ID, List.of(Target.FIELD_LEVEL), parameters);

        definitionMap.put(Constants.MARKER_PACKAGE_ID, List.of(importMarker, deprecatedMarker, overrideMarker, marker, markerParameter));
    }

    public List<Definition> markers(Target target) {
        return definitionMap.entrySet().stream()
                .flatMap(entry -> entry.getValue().stream())
                .filter(definition ->  definition.getTargets() != null && definition.getTargets().contains(target))
                .collect(Collectors.toList());
    }

    public List<Definition> markers(Target target, String name) {
        return markers(target).stream()
                .filter(definition -> definition.getName().equals(name))
                .collect(Collectors.toList());
    }

    public Optional<Definition> find(Target target, String name) {
        return markers(target, name).stream().findFirst();
    }
}
