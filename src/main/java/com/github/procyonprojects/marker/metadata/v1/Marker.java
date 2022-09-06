package com.github.procyonprojects.marker.metadata.v1;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.github.procyonprojects.marker.metadata.Target;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Marker {

    @JsonProperty(required = true)
    private String name;
    @JsonProperty(required = true)
    private List<Target> targets;
    private List<Parameter> parameters;
    private boolean repeatable;
    private boolean deprecated;
    private boolean syntaxFree;

    public Marker() {
    }

    public Marker(String name, List<Target> targets) {
        this(name, targets, new ArrayList<>());
    }
    public Marker(String name, List<Target> targets, List<Parameter> parameters) {
        this(name, targets, parameters, false, true);
    }

    public Marker(String name, List<Target> targets, boolean syntaxFree, boolean repeatable) {
        this(name, targets, new ArrayList<>(), syntaxFree, repeatable);
    }

    public Marker(String name, List<Target> targets, List<Parameter> parameters, boolean syntaxFree, boolean repeatable) {
        this.name = name;
        this.targets = targets;
        this.parameters = parameters;
        this.syntaxFree = syntaxFree;
        this.repeatable = repeatable;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<Target> getTargets() {
        return targets;
    }

    public void setTargets(List<Target> targets) {
        this.targets = targets;
    }

    public List<Parameter> getParameters() {
        return parameters;
    }

    public void setParameters(List<Parameter> parameters) {
        this.parameters = parameters;
    }

    public boolean isRepeatable() {
        return repeatable;
    }

    public void setRepeatable(boolean repeatable) {
        this.repeatable = repeatable;
    }

    public boolean isDeprecated() {
        return deprecated;
    }

    public void setDeprecated(boolean deprecated) {
        this.deprecated = deprecated;
    }

    public boolean isSyntaxFree() {
        return syntaxFree;
    }

    public void setSyntaxFree(boolean syntaxFree) {
        this.syntaxFree = syntaxFree;
    }

    public Optional<Parameter> getParameter(String name) {
        return parameters.stream()
                .filter(parameter -> StringUtils.isNotEmpty(parameter.getName()) && parameter.getName().equals(name))
                .findFirst();
    }
}
