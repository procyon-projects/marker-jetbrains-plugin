package com.github.procyonprojects.marker.handler;

import com.intellij.codeInsight.completion.InsertHandler;
import com.intellij.codeInsight.completion.InsertionContext;
import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.openapi.editor.Document;
import org.jetbrains.annotations.NotNull;

public class TypeInsertHandler implements InsertHandler<LookupElement> {

    public static final InsertHandler<LookupElement> TYPE_INSERT_HANDLER = new TypeInsertHandler();
    @Override
    public void handleInsert(@NotNull InsertionContext context, @NotNull LookupElement item) {
        Document document = context.getDocument();
        int caretOffset = context.getTailOffset() + ".type".length();
        document.insertString(context.getTailOffset(), ".type");
        context.getEditor().getCaretModel().moveToOffset(caretOffset);
    }
}
