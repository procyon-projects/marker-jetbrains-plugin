package com.github.procyonprojects.marker.comment;

import com.github.procyonprojects.marker.Utils;
import com.github.procyonprojects.marker.element.Element;
import com.github.procyonprojects.marker.element.MarkerElement;
import com.github.procyonprojects.marker.metadata.v1.Marker;
import com.goide.GoParserDefinition;
import com.goide.GoTypes;
import com.goide.lexer.GoLexer;
import com.intellij.lexer.LexerPosition;
import com.intellij.openapi.util.Pair;
import com.intellij.psi.tree.IElementType;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;

import java.util.*;

public class ImportParser {

    private final GoLexer myLexer = new GoLexer();

    public @NotNull Set<ImportMarker> parse(Marker marker, @NotNull CharSequence content) {
        if (content == null) {
            return Collections.emptySet();
        }

        this.myLexer.start(content, 0, content.length());
        final Set<ImportMarker> importMarkers = new HashSet<>();
        final Parser parser = new Parser();

        final Map<String, String> pkgVersionMap = new HashMap<>();
        final Map<String, String> pkgAliasMap = new HashMap<>();

        IElementType type;
        while ((type = this.myLexer.getTokenType()) != null) {
            if (type == GoTypes.PACKAGE) {
                break;
            }

            if (isComment(type) && isMarkerCommentElement(type)) {
                LexerPosition currentPosition = myLexer.getCurrentPosition();
                Comment comment = getMarkerComment();
                myLexer.restore(currentPosition);

                Comment.Line firstLine = comment.firstLine().get();
                if (!firstLine.getText().trim().startsWith("+import")) {
                    myLexer.advance();
                    continue;
                }

                ParseResult parseResult = parser.parse(marker, comment, marker.getName());
                if (parseResult == null) {
                    myLexer.advance();
                    continue;
                }

                MarkerElement markerElement = parseResult.getMarkerElement();
                if (markerElement == null) {
                    myLexer.advance();
                    continue;
                }

                Optional<Element> pkg = markerElement.getParameterValue("Pkg");
                if (pkg.isEmpty()
                        || StringUtils.isEmpty(pkg.get().getText())
                        || StringUtils.isBlank(Utils.unquote(pkg.get().getText()).trim())) {
                    myLexer.advance();
                    continue;
                }

                Optional<Element> value = markerElement.getParameterValue("Value");
                if (value.isEmpty()
                        || StringUtils.isEmpty(value.get().getText())
                        || StringUtils.isBlank(Utils.unquote(value.get().getText()).trim())) {
                    myLexer.advance();
                    continue;
                }

                Optional<Element> alias = markerElement.getParameterValue("Alias");
                if (alias.isPresent()
                        && (StringUtils.isEmpty(alias.get().getText())
                        || StringUtils.isBlank(Utils.unquote(alias.get().getText()).trim()))) {
                    myLexer.advance();
                    continue;
                }

                String pkgText = Utils.unquote(pkg.get().getText()).trim();
                String[] pkgParts = pkgText.split("@", 2);
                String pkgPath = pkgParts[0];

                if (!pkgVersionMap.containsKey(pkgPath)) {
                    pkgVersionMap.put(pkgPath, pkgParts[1]);
                } else if (!pkgVersionMap.get(pkgPath).equals(pkgParts[1])) {
                    myLexer.advance();
                    continue;
                }

                String valueText = Utils.unquote(value.get().getText()).trim();

                if (!pkgAliasMap.containsKey(valueText) && alias.isEmpty()) {
                    pkgAliasMap.put(valueText, pkgPath);
                    importMarkers.add(new ImportMarker(pkgText, valueText));
                } else if (alias.isPresent()) {
                    String aliasText = Utils.unquote(alias.get().getText()).trim();
                    if (pkgAliasMap.containsKey(aliasText)) {
                        myLexer.advance();
                        continue;
                    } else {
                        pkgAliasMap.put(aliasText, pkgPath);
                        importMarkers.add(new ImportMarker(pkgText, valueText, aliasText));
                    }
                } else {
                    myLexer.advance();
                    continue;
                }
            }

            this.myLexer.advance();
        }

        return importMarkers;
    }

    private Comment getMarkerComment() {
        final List<Comment.Line> lines = new ArrayList<>();
        final Comment comment = new Comment(lines);

        IElementType currentElement;
        String currentText = myLexer.getTokenText();
        lines.add(new Comment.Line(currentText));

        Pair<IElementType, Pair<LexerPosition, String>> next = nextElement();
        while (currentText.endsWith(" \\") && isWhiteSpace(next.getFirst()) && next.getSecond().getSecond().equals("\n")) {
            myLexer.advance();
            currentElement = myLexer.getTokenType();
            currentText = myLexer.getTokenText();
            while (isWhiteSpace(currentElement) && (StringUtils.countMatches(currentText, '\t') >= 1 || StringUtils.countMatches(currentText, '\n') >= 1)) {
                myLexer.advance();
                currentElement = myLexer.getTokenType();
                currentText = myLexer.getTokenText();
            }

            if (!isComment(currentElement)) {
                break;
            }

            lines.add(new Comment.Line(currentText));
        }

        return comment;
    }

    private boolean isMarkerCommentElement(IElementType type) {
        String commentText = myLexer.getTokenText();
        LexerPosition commentPosition = myLexer.getCurrentPosition();

        int commentIndex = 0;
        IElementType previous = null;
        String previousText = "";
        IElementType current = type;
        String currentText = myLexer.getTokenText();

        while (current != null) {
            if (!isComment(current) && !isWhiteSpace(current)) {
                break;
            }

            if (isWhiteSpace(current) && StringUtils.countMatches(currentText, '\t') >= 1
                    && isWhiteSpace(previous)
                    && StringUtils.countMatches(previousText, '\n') >= 1) {
                final Pair<IElementType, Pair<LexerPosition, String>> next = nextElement();
                if (isWhiteSpace(next.getFirst()) && StringUtils.countMatches(next.getSecond().getSecond(), '\n') >= 1) {
                    Pair<IElementType, Pair<LexerPosition, String>> temp1 = nextElement(next.getSecond().getFirst(), myLexer.getCurrentPosition());
                    if (isWhiteSpace(temp1.getFirst()) && StringUtils.countMatches(temp1.getSecond().getSecond(), '\t') >= 1) {
                        Pair<IElementType, Pair<LexerPosition, String>> temp2 = nextElement(temp1.getSecond().getFirst(), myLexer.getCurrentPosition());
                        if (isWhiteSpace(temp2.getFirst()) && StringUtils.countMatches(temp2.getSecond().getSecond(), '\n') >= 1) {
                            Pair<IElementType, Pair<LexerPosition, String>> temp3 = nextElement(temp2.getSecond().getFirst(), myLexer.getCurrentPosition());
                            if (!isWhiteSpace(temp3.getFirst())) {
                                commentIndex++;
                            }
                        } else if (isComment(temp1.getFirst())) {
                            commentIndex++;

                            previous = current;
                            previousText = currentText;
                            current = temp2.getFirst();
                            currentText = temp2.getSecond().getSecond();
                            continue;
                        }
                    } else if (isWhiteSpace(temp1.getFirst())) {
                        commentIndex++;
                    }
                }
            } else if (isWhiteSpace(current) && StringUtils.countMatches(currentText, '\n') > 1) {
                final Pair<IElementType, Pair<LexerPosition, String>> next = nextElement();
                if (isComment(next.getFirst())) {
                    commentIndex++;
                } else if(isWhiteSpace(next.getFirst()) && StringUtils.countMatches(next.getSecond().getSecond(), '\t') >= 1
                        && isComment(nextElement(next.getSecond().getFirst(), myLexer.getCurrentPosition()).getFirst())) {
                    commentIndex++;
                }
            }

            previous = current;
            previousText = currentText;
            myLexer.advance();
            current = myLexer.getTokenType();
            currentText = myLexer.getTokenText();
        }

        if (commentIndex > 1) {
            return false;
        }

        myLexer.restore(commentPosition);
        return Utils.isMarkerCommentText(commentText);
    }

    private Pair<IElementType, Pair<LexerPosition, String>> nextElement() {
        LexerPosition restorePosition = myLexer.getCurrentPosition();
        myLexer.advance();

        IElementType nextType = myLexer.getTokenType();
        String nextText = myLexer.getTokenText();
        LexerPosition nextPosition = myLexer.getCurrentPosition();

        myLexer.restore(restorePosition);
        return new Pair<>(nextType, new Pair<>(nextPosition, nextText));
    }

    private Pair<IElementType, Pair<LexerPosition, String>> nextElement(LexerPosition position, LexerPosition restorePosition) {
        myLexer.restore(position);
        myLexer.advance();

        IElementType nextType = myLexer.getTokenType();
        String nextText = myLexer.getTokenText();
        LexerPosition nextPosition = myLexer.getCurrentPosition();

        myLexer.restore(restorePosition);
        return new Pair<>(nextType, new Pair<>(nextPosition, nextText));
    }

    private static boolean isWhiteSpace(IElementType type) {
        return GoParserDefinition.Lazy.WHITESPACES.contains(type);
    }

    private static boolean isComment(IElementType type) {
        return GoParserDefinition.Lazy.COMMENTS.contains(type);
    }

    public static class ImportMarker {
        private String pkg;
        private String processor;
        private String alias;


        public ImportMarker(String pkg, String processor) {
            this.pkg = pkg;
            this.processor = processor;
        }

        public ImportMarker(String pkg, String processor, String alias) {
            this.pkg = pkg;
            this.processor = processor;
            this.alias = alias;
        }

        public String getPkg() {
            return pkg;
        }

        public void setPkg(String pkg) {
            this.pkg = pkg;
        }

        public String getProcessor() {
            return processor;
        }

        public void setProcessor(String processor) {
            this.processor = processor;
        }

        public String getAlias() {
            return alias;
        }

        public void setAlias(String alias) {
            this.alias = alias;
        }
    }
}
