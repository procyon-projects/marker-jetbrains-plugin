package com.github.procyonprojects.marker;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.util.IconLoader;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;

public class Icons {

    private static @NotNull Icon load(@NotNull String path) {
        return IconLoader.getIcon(path, Icons.class);
    }

    public static final @NotNull Icon Marker = load("/icons/marker.svg");
    public static final @NotNull Icon PredefinedMarker = load("/icons/predefined_marker.svg");
    public static final @NotNull Icon Package = AllIcons.Nodes.Package;
    public static final @NotNull Icon Processor = load("/icons/processor.svg");
    public static final @NotNull Icon Parameter = load("/icons/parameter.svg");
    public static final @NotNull Icon RequiredParameter = load("/icons/required_parameter.svg");
    public static final @NotNull Icon Map = load("/icons/block.svg");
    public static final @NotNull Icon Slice = load("/icons/block.svg");
}
