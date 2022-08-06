package com.github.procyonprojects.marker;

import com.github.procyonprojects.marker.metadata.Target;
import com.intellij.psi.PsiElement;

public class TargetInfo {

    private PsiElement element;
    private Target target;

    public TargetInfo(PsiElement element, Target target) {
        this.element = element;
        this.target = target;
    }

    public PsiElement getElement() {
        return element;
    }

    public void setElement(PsiElement element) {
        this.element = element;
    }

    public Target getTarget() {
        return target;
    }

    public void setTarget(Target target) {
        this.target = target;
    }
}
