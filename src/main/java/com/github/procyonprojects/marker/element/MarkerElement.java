package com.github.procyonprojects.marker.element;

import com.github.procyonprojects.marker.metadata.v1.Marker;
import com.github.procyonprojects.marker.metadata.v1.Parameter;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class MarkerElement extends Element {

    private Marker marker;

    public MarkerElement(Marker marker) {
        this.marker = marker;
    }

    public Marker getMarker() {
        return marker;
    }

    public void setMarker(Marker definition) {
        this.marker = marker;
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

    public Optional<Element> getParameterValue(String name) {
        Optional<Parameter> defaultParameter = marker.getParameters().stream()
                .filter(parameter -> "Value".equals(parameter.getName()))
                .findFirst();

        Element current = getNext();
        while (current != null) {
            if (current instanceof ParameterElement) {
                ParameterElement parameterElement = (ParameterElement) current;
                if ("Value".equals(name) && defaultParameter.isPresent() && parameterElement.getName() == null) {
                    return Optional.ofNullable(parameterElement.getValue());
                } else if (parameterElement.getName() == null || !name.equals(parameterElement.getName().getText())) {
                    current = current.getNext();
                    continue;
                }
                return Optional.ofNullable(parameterElement.getValue());
            }

            current = current.getNext();
        }

        return Optional.empty();
    }

    public List<String> getParameters() {
        return getParameterElements()
                .stream()
                .map(parameterElement -> parameterElement.getName() == null ? "Value" : parameterElement.getName().getText())
                .collect(Collectors.toList());
    }
}
