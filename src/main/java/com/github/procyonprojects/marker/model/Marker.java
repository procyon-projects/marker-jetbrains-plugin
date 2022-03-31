package com.github.procyonprojects.marker.model;

import java.util.List;

public class Marker {
    private String pkgId;
    private String name;
    private TargetType targets;
    private List<Argument> arguments;

    public Marker(String pkgId, String name, TargetType targets, List<Argument> arguments) {
        this.pkgId = pkgId;
        this.name = name;
        this.targets = targets;
        this.arguments = arguments;
    }

    public String getPkgId() {
        return pkgId;
    }

    public void setPkgId(String pkgId) {
        this.pkgId = pkgId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public TargetType getTargets() {
        return targets;
    }

    public void setTargets(TargetType targets) {
        this.targets = targets;
    }

    public List<Argument> getArguments() {
        return arguments;
    }

    public void setArguments(List<Argument> arguments) {
        this.arguments = arguments;
    }
}
