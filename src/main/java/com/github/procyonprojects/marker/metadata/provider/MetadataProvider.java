package com.github.procyonprojects.marker.metadata.provider;
import b.e.O;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.github.procyonprojects.marker.index.MarkerImportIndex;
import com.github.procyonprojects.marker.index.MarkerProcessorPackagesIndex;
import com.github.procyonprojects.marker.metadata.Target;
import com.github.procyonprojects.marker.metadata.Type;
import com.github.procyonprojects.marker.metadata.TypeInfo;
import com.github.procyonprojects.marker.metadata.v1.*;
import com.github.procyonprojects.marker.metadata.v1.schema.AnySchema;
import com.github.procyonprojects.marker.metadata.v1.schema.BoolSchema;
import com.github.procyonprojects.marker.metadata.v1.schema.SliceSchema;
import com.github.procyonprojects.marker.metadata.v1.schema.StringSchema;
import com.github.procyonprojects.marker.scope.MarkerScope;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.util.indexing.FileBasedIndex;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class MetadataProvider {

    private final static ConcurrentHashMap<String, ProcessorList> cache = new ConcurrentHashMap<>();
    private final static ObjectMapper objectMapper = new ObjectMapper(new YAMLFactory());

    static {
        List<Parameter> parameters = new ArrayList<>();
        parameters.add(new Parameter("Value", new StringSchema(), "The name of the marker processor", true));
        parameters.add(new Parameter("Pkg", new StringSchema(), "The name of the package to import as marker processor", true));
        parameters.add(new Parameter("Alias", new StringSchema(), "Alias for the name of the marker processor"));
        final Marker importMarker = new Marker("import", List.of(Target.PACKAGE_LEVEL), parameters);

        final Marker deprecatedMarker = new Marker("deprecated", List.of(Target.FIELD_LEVEL,
                Target.FUNCTION_LEVEL,
                Target.INTERFACE_LEVEL,
                Target.INTERFACE_METHOD_LEVEL,
                Target.STRUCT_LEVEL,
                Target.STRUCT_METHOD_LEVEL
        ), true,false);

        final Marker overrideMarker = new Marker("override", List.of(Target.STRUCT_METHOD_LEVEL), true, false);

        parameters = new ArrayList<>();
        parameters.add(new Parameter("Value", new StringSchema(), "The name of the marker", true));
        parameters.add(new Parameter("Description", new StringSchema(), "The description of the marker", true));
        parameters.add(new Parameter("Repeatable", new BoolSchema(), "Whether the marker is repeatable", Boolean.TRUE));
        parameters.add(new Parameter("SyntaxFree",  new BoolSchema(), "Whether the marker is syntax-free", Boolean.FALSE));
        //parameters.add(new Parameter("Test", new TypeInfo(Type.MapType, new TypeInfo(Type.AnyType)), "Whether the marker is syntax-free", Boolean.FALSE));

        final List<EnumValue> targetList = new ArrayList<>();
        targetList.add(new EnumValue("PACKAGE_LEVEL"));
        targetList.add(new EnumValue("INTERFACE_LEVEL"));
        targetList.add(new EnumValue("STRUCT_LEVEL"));
        targetList.add(new EnumValue("FUNCTION_LEVEL"));
        targetList.add(new EnumValue("FIELD_LEVEL"));
        targetList.add(new EnumValue("STRUCT_METHOD_LEVEL"));
        targetList.add(new EnumValue("INTERFACE_METHOD_LEVEL"));

        parameters.add(new Parameter("Targets", new SliceSchema(new StringSchema(targetList)), "The targets of the marker", true, targetList));

        final Marker marker = new Marker("marker", List.of(Target.STRUCT_LEVEL), parameters, false, false);

        parameters = new ArrayList<>();
        parameters.add(new Parameter("Value", new StringSchema(), "The name of the parameter to bind", true));
        parameters.add(new Parameter("Description", new StringSchema(), "The description of the parameter", true));
        parameters.add(new Parameter("Required",  new BoolSchema(), "Whether the parameter is required", Boolean.FALSE));
        parameters.add(new Parameter("Default", new AnySchema(), "The default value"));
        parameters.add(new Parameter("Enum", new SliceSchema(new StringSchema()), "The enum values"));
        final Marker markerParameter = new Marker("marker:parameter", List.of(Target.FIELD_LEVEL), parameters, false, false);


        final Marker buildMarker = new Marker("build", List.of(Target.PACKAGE_LEVEL), true, true);
        cache.put("", new ProcessorList(
                Map.of(
                        "build", new Processor(List.of(buildMarker)),
                        "import", new Processor(List.of(importMarker)),
                        "deprecated", new Processor(List.of(deprecatedMarker)),
                        "override", new Processor(List.of(overrideMarker)),
                        "marker", new Processor(List.of(marker, markerParameter))
                )
        ));
    }

    public Optional<Marker> findMarker(Project project, VirtualFile file, String markerName, Target target) {
        Map<String, String> importMap = getImportMap(project, file);
        for (Map.Entry<String, String> entry : importMap.entrySet()) {
            if (!entry.getKey().startsWith("p")) {
                continue;
            }

            String processorName = entry.getKey().substring(1);
            String pkg = entry.getValue();
            String alias = null;
            if (importMap.containsKey("a" + processorName)) {
                alias = processorName;
                processorName = importMap.get("a" + processorName);
            }

            Optional<Processor> processor = getProcessorByName(project, pkg, processorName);
            if (processor.isEmpty()) {
                continue;
            }

            for (Marker marker : processor.get().getMarkers()) {
                if (CollectionUtils.isEmpty(marker.getTargets()) || !marker.getTargets().contains(target)) {
                    continue;
                }

                String name = marker.getName();
                if (StringUtils.isNotEmpty(alias)) {
                    name = name.replace(processorName, alias);
                }

                if (name.equals(markerName)) {
                    return Optional.of(marker);
                }
            }
        }

        return Optional.empty();
    }

    public Optional<Marker> getImportMarker() {
        return cache.get("").getProcessors()
                .values().stream().flatMap(processor -> processor.getMarkers().stream())
                .filter(marker -> "import".equals(marker.getName()))
                .findFirst();
    }

    public Map<String, String> getImportMap(Project project, VirtualFile file) {
        Map<String, String> indexMap = new HashMap<>();
        indexMap.put("pimport", "");
        indexMap.put("pbuild", "");
        indexMap.put("pdeprecated", "");
        indexMap.put("poverride", "");
        indexMap.put("pmarker", "");
        indexMap.putAll(FileBasedIndex.getInstance().getFileData(MarkerImportIndex.KEY, file, project));
        return indexMap;
    }

    public Collection<String> packages(Project project) {
        return FileBasedIndex.getInstance().getAllKeys(MarkerProcessorPackagesIndex.KEY, project);
    }

    public boolean processorExists(Project project, String pkg, String processor) {
        return processorNames(project, pkg).contains(processor);
    }

    public boolean packageExists(Project project, String pkg) {
        return FileBasedIndex.getInstance().getAllKeys(MarkerProcessorPackagesIndex.KEY, project)
                .stream().anyMatch(pkg::equals);
    }

    public boolean packageExistsInCache(String pkg) {
        return cache.containsKey(pkg);
    }

    public void updateCache(String pkg, String content) {
        ProcessorList processorList = null;
        try {
            processorList = objectMapper.readValue(content, ProcessorList.class);
            cache.put(pkg, processorList);
        } catch (JsonProcessingException ignored) {
        }
    }

    public Optional<Processor> getProcessorByName(Project project, String pkg, String processor) {
        Set<String> processors = processorNames(project, pkg);
        if (CollectionUtils.isEmpty(processors) || !processors.contains(processor)) {
            return Optional.empty();
        }

        return Optional.ofNullable(cache.get(pkg).getProcessors().get(processor));
    }

    public Set<String> processorNames(Project project, String pkg) {
        if (cache.containsKey(pkg)) {
            return cache.get(pkg).getProcessors().keySet();
        }

        Set<String> result = new HashSet<>();
        FileBasedIndex.getInstance()
                .processValues(MarkerProcessorPackagesIndex.KEY, pkg, null, (file, value) -> {
                    result.add(value);
                    return true;
                }, new MarkerScope(project));

        Set<String> processorNames = new HashSet<>();
        result.forEach(content -> {
            try {
                ProcessorList processorList = objectMapper.readValue(content, ProcessorList.class);
                cache.put(pkg, processorList);
                processorNames.addAll(processorList.getProcessors().keySet());
            } catch (JsonProcessingException ignored) {

            }
        });

        return processorNames;
    }

}
