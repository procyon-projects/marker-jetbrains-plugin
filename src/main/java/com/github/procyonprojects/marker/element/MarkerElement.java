package com.github.procyonprojects.marker.element;

import com.github.procyonprojects.marker.metadata.Definition;

import java.util.ArrayList;
import java.util.List;

public class MarkerElement extends Element {

    private Definition definition;

    public MarkerElement(Definition definition) {
        this.definition = definition;
    }

    public Definition getDefinition() {
        return definition;
    }

    public void setDefinition(Definition definition) {
        this.definition = definition;
    }

    public List<ParameterElement> getParameterElements() {
        final List<ParameterElement> parameterElements = new ArrayList<>();
        Element current = getNext();
        while (current != null) {
            if (current instanceof ParameterElement) {
                parameterElements.add((ParameterElement) current);
            }

            current = current.getNext();
        }

        return parameterElements;
    }

    public List<String> getParameters() {
        return null;
    }
}
