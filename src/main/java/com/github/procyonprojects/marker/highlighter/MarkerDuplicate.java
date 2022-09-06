package com.github.procyonprojects.marker.highlighter;

import com.github.procyonprojects.marker.comment.Comment;
import com.github.procyonprojects.marker.metadata.v1.Marker;

public class MarkerDuplicate {

    private Comment comment;
    private Marker marker;
    private String markerName;

    public MarkerDuplicate(Comment comment, Marker marker, String markerName) {
        this.comment = comment;
        this.marker = marker;
        this.markerName = markerName;
    }

    public Comment getComment() {
        return comment;
    }

    public void setComment(Comment comment) {
        this.comment = comment;
    }

    public Marker getMarker() {
        return marker;
    }

    public void setMarker(Marker marker) {
        this.marker = marker;
    }

    public String getMarkerName() {
        return markerName;
    }

    public void setMarkerName(String markerName) {
        this.markerName = markerName;
    }
}
