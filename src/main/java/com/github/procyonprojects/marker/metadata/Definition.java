package com.github.procyonprojects.marker.metadata;

import com.github.procyonprojects.marker.metadata.Parameter;
import com.github.procyonprojects.marker.metadata.Target;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

public class Definition {

    private String name;
    private String pkgId;
    private boolean syntaxFree;
    private List<Target> targets;
    private List<Parameter> parameters;


    public Definition(String name, String pkgId, List<Target> targets) {
        this(name, pkgId, targets, new ArrayList<>(), false);
    }

    public Definition(String name, String pkgId, List<Target> targets, List<Parameter> parameters) {
        this(name, pkgId, targets, parameters, false);
    }

    public Definition(String name, String pkgId, List<Target> targets, boolean syntaxFree) {
        this(name, pkgId, targets, new ArrayList<>(), syntaxFree);
    }

    private Definition(String name, String pkgId, List<Target> targets, List<Parameter> parameters, boolean syntaxFree) {
        this.name = name;
        this.pkgId = pkgId;
        this.targets = targets;
        this.parameters = parameters;
        this.syntaxFree = syntaxFree;
    }

    public String getName() {
        return name;
    }

    protected void setName(String name) {
        this.name = name;
    }

    public String getPkgId() {
        return pkgId;
    }

    protected void setPkgId(String pkgId) {
        this.pkgId = pkgId;
    }

    public boolean isSyntaxFree() {
        return syntaxFree;
    }

    protected void setSyntaxFree(boolean syntaxFree) {
        this.syntaxFree = syntaxFree;
    }

    public List<Target> getTargets() {
        return targets;
    }

    protected void setTargets(List<Target> targets) {
        this.targets = targets;
    }

    public List<Parameter> getParameters() {
        if (CollectionUtils.isEmpty(parameters)) {
            return Collections.emptyList();
        }

        return parameters;
    }

    protected void setParameters(List<Parameter> parameters) {
        this.parameters = parameters;
    }

    public Optional<Parameter> getParameter(String parameterName) {
        return getParameters().stream()
                .filter(parameter -> StringUtils.isNotEmpty(parameter.getName()) && parameter.getName().equals(parameterName))
                .findFirst();
    }
}
