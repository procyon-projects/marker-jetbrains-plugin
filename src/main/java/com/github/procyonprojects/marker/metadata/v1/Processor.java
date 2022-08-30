package com.github.procyonprojects.marker.metadata.v1;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Processor {

    private List<Marker> markers;

    public Processor() {

    }

    public Processor(List<Marker> markers) {
        this.markers = markers;
    }

    public List<Marker> getMarkers() {
        return markers;
    }

    public void setMarkers(List<Marker> markers) {
        this.markers = markers;
    }
}
