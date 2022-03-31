package com.github.procyonprojects.marker.provider;

import com.github.procyonprojects.marker.model.Argument;
import com.github.procyonprojects.marker.model.Marker;
import com.github.procyonprojects.marker.model.TargetType;
import com.intellij.openapi.components.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public final class MarkerMetadataProvider {

    public final static Map<String, Marker> packageLevelMap = new HashMap<>();
    public final static Map<String, Marker> interfaceLevelMap = new HashMap<>();
    public final static Map<String, Marker> interfaceMethodLevelMap = new HashMap<>();

    {
        List<Argument> arguments = new ArrayList<>();
        arguments.add(new Argument("Value", "Import Name", "string", false, null));
        arguments.add(new Argument("Alias", "Import Alias Name", "string", true, List.of("GET", "POST", "DELETE", "HEAD", "PATCH")));
        arguments.add(new Argument("PkgId", "Process Package Name", "string", false, null));
        packageLevelMap.put("", new Marker("", "+import", TargetType.PACKAGE_LEVEL, arguments));

        arguments = new ArrayList<>();
        arguments.add(new Argument("Value", "Accessor Name", "string", false, null));
        arguments.add(new Argument("Url", "Endpoint Base URL", "string", true, null));
        interfaceLevelMap.put("", new Marker("github.com/procyon-projects/accessor", "+accessor", TargetType.INTERFACE_LEVEL, arguments));

        arguments = new ArrayList<>();
        arguments.add(new Argument("Value", "URL", "string", false, null));
        arguments.add(new Argument("Method", "HTTP Method", "string", true, List.of("GET", "POST", "DELETE", "HEAD", "PATCH")));
        interfaceMethodLevelMap.put("",new Marker("github.com/procyon-projects/accessor", "import", TargetType.INTERFACE_METHOD_LEVEL, arguments));
    }
}
