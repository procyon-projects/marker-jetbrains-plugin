package com.github.procyonprojects.marker.comment;

import com.github.procyonprojects.marker.element.Element;
import com.github.procyonprojects.marker.element.MarkerElement;

import java.util.List;

public class ParseResult {

    private final MarkerElement markerElement;
    private final List<Element> nextLineElements;

    public ParseResult(MarkerElement markerElement, List<Element> nextLineElements) {
        this.markerElement = markerElement;
        this.nextLineElements = nextLineElements;
    }

    public MarkerElement getMarkerElement() {
        return markerElement;
    }

    public List<Element> getNextLineElements() {
        return nextLineElements;
    }
}
