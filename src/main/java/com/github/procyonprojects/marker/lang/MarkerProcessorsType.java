package com.github.procyonprojects.marker.lang;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.fileTypes.LanguageFileType;
import com.intellij.openapi.util.NlsContexts;
import com.intellij.openapi.util.NlsSafe;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

public class MarkerProcessorsType extends LanguageFileType {

    public static final MarkerProcessorsType INSTANCE = new MarkerProcessorsType();

    protected MarkerProcessorsType() {
        super(MarkerYamlLanguage.INSTANCE);
    }

    @Override
    public @NonNls @NotNull String getName() {
        return "marker.processors.yaml";
    }

    @Override
    public @NlsContexts.Label @NotNull String getDescription() {
        return "";
    }

    @Override
    public @NlsSafe @NotNull String getDefaultExtension() {
        return "";
    }

    @Override
    public @Nullable Icon getIcon() {
        return AllIcons.FileTypes.Yaml;
    }
}
