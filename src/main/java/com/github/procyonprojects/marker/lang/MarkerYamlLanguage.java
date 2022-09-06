package com.github.procyonprojects.marker.lang;

import com.intellij.lang.Language;
import com.intellij.openapi.util.NlsSafe;
import org.jetbrains.annotations.NotNull;

public class MarkerYamlLanguage extends Language {

    public static final MarkerYamlLanguage INSTANCE = new MarkerYamlLanguage();

    private MarkerYamlLanguage() {
        super("MarkerYaml");
    }


    @Override
    public @NotNull @NlsSafe String getDisplayName() {
        return "MarkerYaml";
    }
}
