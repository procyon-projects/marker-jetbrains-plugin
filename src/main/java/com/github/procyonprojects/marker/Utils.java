package com.github.procyonprojects.marker;

import com.github.procyonprojects.marker.comment.Comment;
import com.github.procyonprojects.marker.metadata.Target;
import com.goide.psi.GoPackageClause;
import com.goide.psi.impl.*;
import com.intellij.formatting.WhiteSpace;
import com.intellij.psi.PsiComment;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiWhiteSpace;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

public class Utils {

    public static boolean isMarkerCommentElement(@NotNull PsiElement element) {
        if (!(element instanceof PsiComment)) {
            return false;
        }

        int commentIndex = 0;
        PsiElement current = element;

        while (current != null) {
            if (current instanceof GoTypeDeclarationImpl
                    || current instanceof GoMethodDeclarationImpl
                    || current instanceof GoFieldDeclarationImpl
                    || current instanceof GoFunctionDeclarationImpl
                    || current instanceof GoPackageClauseImpl) {
                break;
            }

            if (current instanceof WhiteSpace && StringUtils.countMatches(current.getText(), '\t') >= 1
                    && current.getPrevSibling() instanceof PsiWhiteSpace && StringUtils.countMatches(current.getPrevSibling().getText(), '\n') >= 1) {
                final PsiElement next = current.getNextSibling();
                if (next instanceof PsiWhiteSpace && StringUtils.countMatches(next.getText(), '\n') >= 1) {
                    if (next.getNextSibling() instanceof PsiWhiteSpace && StringUtils.countMatches(next.getNextSibling().getText(), '\t') >= 1) {
                        if (next.getNextSibling().getNextSibling() instanceof PsiWhiteSpace && StringUtils.countMatches(next.getNextSibling().getNextSibling().getText(), '\n') >= 1) {
                            if (!(next.getNextSibling().getNextSibling().getNextSibling() instanceof PsiWhiteSpace)) {
                                commentIndex++;
                            }
                        } else if (next.getNextSibling().getNextSibling() instanceof PsiComment) {
                            commentIndex++;
                            current = next.getNextSibling().getNextSibling();
                            continue;
                        }
                    } else if (next.getNextSibling() instanceof PsiComment) {
                        commentIndex++;
                    }
                }
            } else if (current instanceof PsiWhiteSpace && StringUtils.countMatches(current.getText(), '\n') > 1) {
                final PsiElement next = current.getNextSibling();
                if (next instanceof PsiComment) {
                    commentIndex++;
                } else if (next instanceof PsiWhiteSpace && StringUtils.countMatches(next.getText(), '\t') >= 1 && next.getNextSibling() instanceof PsiComment) {
                    commentIndex++;
                }
            }

            current = current.getNextSibling();
        }

        if (commentIndex > 1) {
            return false;
        }

        return isMarkerCommentText(element.getText());
    }

    public static boolean isMarkerCommentText(String text) {
        if (!StringUtils.startsWith(text, "//")) {
            return false;
        }

        String comment = text.substring(2).trim();

        if (comment.length() < 2 || comment.charAt(0) != '+') {
            return false;
        }

        comment = comment.split(" ")[0];
        final String[] nameFieldParts = comment.split("=", 2);

        return !StringUtils.containsWhitespace(nameFieldParts[0].stripTrailing()); // TODO
    }

    public static Optional<Comment> getMarkerComment(PsiElement element) {
        if (isMarkerCommentElement(element)) {
            final List<Comment.Line> lines = new ArrayList<>();
            lines.add(new Comment.Line(element));

            while (element.getText().endsWith(" \\") && element.getNextSibling() instanceof PsiWhiteSpace && element.getNextSibling().getText().equals("\n")) {
                element = element.getNextSibling();

                while (element instanceof PsiWhiteSpace && (StringUtils.countMatches(element.getText(), '\t') >= 1 ||  StringUtils.countMatches(element.getText(), '\n') >= 1)) {
                    element = element.getNextSibling();
                }

                if (!(element instanceof PsiComment)) {
                    break;
                }

                lines.add(new Comment.Line(element));
            }

            return Optional.of(new Comment(lines));
        }

        return Optional.empty();
    }

    public static Optional<Comment> findMarkerCommentFirstLine(PsiElement element) {
        if (!(element instanceof PsiComment)) {
            return Optional.empty();
        }

        while (element instanceof PsiComment || element instanceof PsiWhiteSpace) {
            element = element.getPrevSibling();
            if (element instanceof  PsiComment) {
                if (!element.getText().endsWith(" \\")) {
                    break;
                }

                Optional<Comment> comment = getMarkerComment(element);
                if (comment.isPresent()) {
                    return comment;
                }
            }
        }

        return Optional.empty();
    }

    public static TargetInfo findTarget(PsiComment psiComment) {
        PsiElement element = psiComment.getNextSibling();
        while (element != null) {
            if (!(element instanceof PsiWhiteSpace || element instanceof PsiComment)) {
                break;
            }
            element = element.getNextSibling();
        }

        Target target = Target.INVALID;

        if (element instanceof GoPackageClause) {
            target = Target.PACKAGE_LEVEL;
        } else if (element instanceof GoMethodSpecImpl) {
            target = Target.INTERFACE_METHOD_LEVEL;
        } else if (element instanceof GoTypeDeclarationImpl) {
            target = Target.STRUCT_LEVEL;
        } else if (element instanceof GoFieldDeclarationImpl) {
            target = Target.FIELD_LEVEL;
        } else if (element instanceof GoMethodDeclarationImpl) {
            target = Target.STRUCT_METHOD_LEVEL;
        } else if (element instanceof GoFunctionDeclarationImpl) {
            target = Target.FUNCTION_LEVEL;
        }

        return new TargetInfo(element, target);
    }

    public static String getMarkerName(String markerComment) {
        markerComment = markerComment.substring(1);
        markerComment = markerComment.split(" ")[0];

        String[] nameFieldParts = markerComment.split("=", 2);

        if (nameFieldParts.length == 1) {
            return nameFieldParts[0];
        }

        String markerName = nameFieldParts[0];
        nameFieldParts = markerName.split(":");

        if (nameFieldParts.length > 1) {
            markerName = String.join(":", Arrays.copyOfRange(nameFieldParts, 0, nameFieldParts.length - 1)).stripTrailing();
        }

        return markerName.stripTrailing();
    }

    public static String getMarkerAnonymousName(String markerComment) {
        markerComment = markerComment.substring(1);
        markerComment = markerComment.split(" ")[0];

        String[] nameFieldParts = markerComment.split("=", 2);
        return nameFieldParts[0].stripTrailing();
    }

    public static int countLeadingSpace(String str) {
        int spaceCount = 0;

        for(char c : str.toCharArray()) {
            if (c == ' ') {
                spaceCount++;
            } else {
                break;
            }
        }

        return spaceCount;
    }

    public static int countTrailingSpaces(String str) {
        int spaceCount = 0;
        char[] chars = str.toCharArray();

        for (int i = chars.length - 1; i >= 0; i--) {
            if (chars[i] == ' ') {
                spaceCount++;
            } else {
                break;
            }
        }

        return spaceCount;
    }

    public static String unquote(String value) {
        if (value.startsWith("\'") && value.endsWith("\'")) {
            return value.substring(1, value.length()-1);
        } else if (value.startsWith("`") && value.endsWith("`")) {
            return value.substring(1, value.length()-1);
        } else if (value.startsWith("\"") && value.endsWith("\"")) {
            return value.substring(1, value.length()-1);
        }

        return value;
    }
}