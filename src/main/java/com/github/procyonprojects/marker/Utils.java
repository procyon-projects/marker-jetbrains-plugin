package com.github.procyonprojects.marker;

import com.github.procyonprojects.marker.comment.Comment;
import com.github.procyonprojects.marker.metadata.Definition;

import java.util.Optional;

public class Utils {

    public Optional<String> getMarkerOptions(Definition definition, Comment comment) {
        if (definition == null || comment == null || comment.getLines().isEmpty()) {
            return Optional.empty();
        }

        final Optional<Comment.Line> firstLine = comment.firstLine();

        if (firstLine.isEmpty()) {
            return Optional.empty();
        }

        final String markerName = definition.getName();
        String firstLineText = firstLine.get().getText();
        firstLineText = firstLineText.replace(markerName, "");

        if (firstLineText.startsWith(":")) {
            return Optional.of(firstLineText.substring(1));
        }

        String[] nameFieldParts = firstLineText.split("=", 2);

        if (nameFieldParts.length == 1) {
            return Optional.empty();
        }

        return Optional.of(nameFieldParts[1]);
    }
}