package com.github.procyonprojects.marker.reference;

import com.github.procyonprojects.marker.Utils;
import com.goide.psi.GoFile;
import com.goide.psi.GoImportSpec;
import com.goide.psi.GoNamedElement;
import com.goide.psi.impl.*;
import com.goide.stubs.index.GoAllPublicNamesIndex;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiReferenceBase;
import com.intellij.psi.ResolveState;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.stubs.StubIndex;
import com.intellij.util.CommonProcessors;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Iterator;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

public class FileReference extends PsiReferenceBase {

    private PsiElement element;
    private TextRange elementRange;
    private String pkg;
    private String ref;

    public FileReference(PsiElement element, TextRange elementRange, String pkg, String ref) {
        super(element, elementRange);
        this.element = element;
        this.elementRange = elementRange;
        this.pkg = pkg;
        this.ref = ref;
    }

    @Override
    public @NotNull TextRange getRangeInElement() {
        int startOffset = elementRange.getStartOffset() - element.getTextRange().getStartOffset();
        int endOffset = elementRange.getEndOffset() - element.getTextRange().getStartOffset();
        return new TextRange(startOffset, endOffset);
    }

    @Override
    public @Nullable PsiElement resolve() {
        PsiFile file = element.getContainingFile();
        PsiElement substitutionContext = file.getUserData(GoReferenceBase.SUBSTITUTION_CONTEXT);
        ResolveState state = substitutionContext != null ? GoPsiImplUtil.createContextOnElement(substitutionContext)
                .put(GoReferenceBase.SUBSTITUTION_CONTEXT, substitutionContext) : GoPsiImplUtil.createContextOnElement(element);

        GoFile goFile = (GoFile) file;
        Optional<GoImportSpec> goImportSpec = goFile.getImportMap().get(pkg).stream().findFirst();

        if (goImportSpec.isEmpty()) {
            goImportSpec = goFile.getImportMap().get("_").stream().filter(importSpec -> importSpec.getPath().equals(pkg)).findFirst();
            if (goImportSpec.isEmpty()) {
                return null;
            }
        }

        String[] pkgParts = Utils.unquote(goImportSpec.get().getPath()).split("/");
        String actualPkgName = pkgParts[pkgParts.length-1];

        Collection<GoPackage> packages = goImportSpec.get().resolve(state);
        for (GoPackage goPackage : packages) {
            String packageName = goPackage.getName();
            String packageNameDot = packageName + ".";
            CommonProcessors.CollectProcessor<String> collectElements = new CommonProcessors.CollectProcessor<>() {
                @Override
                protected boolean accept(String s) {
                    return s.startsWith(packageNameDot);
                }
            };

            Project project = element.getProject();
            StubIndex.getInstance().processAllKeys(GoAllPublicNamesIndex.ALL_PUBLIC_NAMES, project, collectElements);
            GlobalSearchScope packageScope = goPackage.getScope(element.getContainingFile());
            Iterator<String> resultIterator = collectElements.getResults().iterator();

            AtomicReference<PsiElement> psiElementAtomicReference = new AtomicReference<>();
            while (resultIterator.hasNext()) {
                String elementName = resultIterator.next();
                StubIndex.getInstance().processElements(GoAllPublicNamesIndex.ALL_PUBLIC_NAMES, elementName, project, packageScope, null, GoNamedElement.class, (it) -> {
                    if (!(it instanceof GoTypeSpecImpl) && !(it instanceof GoImportSpecImpl) && !(it instanceof GoFunctionDeclarationImpl)) {
                        return true;
                    }

                    if (String.format("%s.%s", actualPkgName, ref).equals(elementName)) {
                        psiElementAtomicReference.set(it);
                    }
                    return true;
                });

                if (psiElementAtomicReference.get() != null) {
                    return psiElementAtomicReference.get();
                }
            }
        }

        return null;
    }
}
