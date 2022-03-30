package com.github.procyonprojects.marker.highlighter;

import com.intellij.openapi.editor.colors.TextAttributesKey;
import com.intellij.openapi.util.Pair;
import com.intellij.openapi.util.TextRange;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.List;

public interface TokenHighlighter {

    List<Pair<TextRange, TextAttributesKey>> getHighlights(String text, int startOffset);

    default List<Pair<TextRange, TextAttributesKey>> getHighlights(HighlightTokenType tokenType, String text, int startOffset) {
        if (getSupportedTokenTypes().contains(tokenType)) {
            return getHighlights(text, startOffset);
        }
        return Collections.emptyList();
    }

    @NotNull
    String getTextAttributeKeyByToken(String token);

    List<HighlightTokenType> getSupportedTokenTypes();
}

