package com.github.procyonprojects.marker.inspection;

import com.github.procyonprojects.marker.Utils;
import com.github.procyonprojects.marker.comment.Comment;
import com.goide.inspections.GoDeprecationInspection;
import com.goide.inspections.GoInspectionUtil;
import com.goide.inspections.core.GoInspectionBase;
import com.goide.inspections.core.GoInspectionMessage;
import com.goide.inspections.core.GoProblemsHolder;
import com.goide.psi.*;
import com.goide.psi.impl.GoPsiImplUtil;
import com.intellij.codeInspection.LocalInspectionToolSession;
import com.intellij.codeInspection.LocalQuickFix;
import com.intellij.codeInspection.ProblemHighlightType;
import com.intellij.psi.PsiComment;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiReference;
import com.intellij.psi.PsiWhiteSpace;
import com.intellij.usageView.UsageViewUtil;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;

public class MarkerDeprecationInspection extends GoInspectionBase {
    public MarkerDeprecationInspection() {
    }

    @NotNull
    protected GoVisitor buildGoVisitor(@NotNull final GoProblemsHolder holder, @NotNull LocalInspectionToolSession session) {
        return new GoVisitor() {
            public void visitNamedElement(@NotNull GoNamedElement o) {
                if (!o.isPublic() || GoInspectionUtil.isCheapEnoughToSearch(o)) {
                    PsiElement identifier = o.getIdentifier();

                    if (identifier == null) {
                        return;
                    }

                    if (hasDeprecatedComment(o) && !this.isUnused(o)) {
                        String elementType = UsageViewUtil.getType(o);
                        GoInspectionMessage message = GoDeprecationInspection.message("go.inspection.problem.deprecated.is.still.used", new Object[]{elementType});
                        holder.registerProblem(identifier, message, ProblemHighlightType.GENERIC_ERROR_OR_WARNING, new LocalQuickFix[0]);
                    }

                }
            }

            private boolean hasDeprecatedComment(PsiElement element) {
                PsiElement current = element;
                if (element instanceof GoTypeSpec || GoPsiImplUtil.isFieldDefinition(element)) {
                    current = current.getParent();
                }

                while ((current = current.getPrevSibling()) != null) {
                    if (Utils.isMarkerCommentElement(current)) {
                        Optional<Comment> comment = Utils.getMarkerComment(current);
                        if (comment.isPresent()) {
                            Optional<Comment.Line> firstLine = comment.get().firstLine();
                            if (firstLine.isPresent() && firstLine.get().getText().trim().startsWith("+deprecated")) {
                                return true;
                            }
                        }
                    } else if (!(current instanceof PsiWhiteSpace) && !(current instanceof PsiComment)) {
                        break;
                    }
                }

                return false;
            }

            private boolean isUnused(@NotNull GoNamedElement o) {
                return o instanceof GoFunctionDeclaration ? GoInspectionUtil.isUnused((GoFunctionDeclaration)o) : GoReferencesSearch.search(o).findFirst() == null;
            }

            public void visitKey(@NotNull GoKey o) {
                super.visitKey(o);
                GoFieldName name = o.getFieldName();
                PsiReference reference = name != null ? name.getReference() : null;
                PsiElement element = reference != null ? reference.resolve() : null;
                if (element instanceof GoNamedElement && hasDeprecatedComment(element)) {
                    holder.registerProblem(o, GoDeprecationInspection.message("go.inspection.problem.reference.is.deprecated", new Object[]{GoDeprecationInspection.REF}), ProblemHighlightType.LIKE_DEPRECATED, new LocalQuickFix[0]);
                }

            }

            public void visitReferenceExpression(@NotNull GoReferenceExpression o) {
                this.checkReferenceExpression(o);
            }

            public void visitTypeReferenceExpression(@NotNull GoTypeReferenceExpression o) {
                this.checkReferenceExpression(o);
            }

            private void checkReferenceExpression(@NotNull GoReferenceExpressionBase o) {
                PsiElement resolve = o.resolve();
                if (resolve instanceof GoNamedElement && hasDeprecatedComment(resolve)) {
                    holder.registerProblem(o.getIdentifier(), GoDeprecationInspection.message("go.inspection.problem.reference.is.deprecated", new Object[]{GoDeprecationInspection.REF}), ProblemHighlightType.LIKE_DEPRECATED, new LocalQuickFix[0]);
                }

            }
        };
    }
}
