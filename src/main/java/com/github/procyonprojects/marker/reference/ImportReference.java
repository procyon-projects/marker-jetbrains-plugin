package com.github.procyonprojects.marker.reference;

import com.goide.psi.GoFile;
import com.goide.psi.GoImportSpec;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiReferenceBase;
import com.intellij.util.containers.MultiMap;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

public class ImportReference extends PsiReferenceBase {

    private PsiElement element;
    private TextRange importRange;
    private String pkg;

    public ImportReference(PsiElement element, TextRange importRange, String pkg) {
        super(element, importRange);
        this.element = element;
        this.importRange = importRange;
        this.pkg = pkg;
    }

    @Override
    public @NotNull TextRange getRangeInElement() {
        int startOffset = importRange.getStartOffset() - element.getTextRange().getStartOffset();
        int endOffset = importRange.getEndOffset() - element.getTextRange().getStartOffset();
        return new TextRange(startOffset, endOffset);
    }

    @Override
    public @Nullable PsiElement resolve() {
        GoFile file = (GoFile) element.getContainingFile();
        MultiMap<String, GoImportSpec> importMap = file.getImportMap();

        if (importMap.containsKey(pkg)) {
            return importMap.get(pkg).stream().findFirst().get().getOriginalElement();
        }

        Optional<GoImportSpec> goImportSpec = file.getImportMap().get("_").stream()
                .filter(e -> e.getPath().equals(pkg)).findFirst();

        if (goImportSpec.isEmpty()) {
            return null;
        }

        return goImportSpec.get().getOriginalElement();
    }
}
