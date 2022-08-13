package com.github.procyonprojects.marker.comment;

import com.github.procyonprojects.marker.Utils;
import com.github.procyonprojects.marker.element.Element;
import com.github.procyonprojects.marker.metadata.Definition;
import com.intellij.openapi.util.TextRange;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class Scanner {

    public static final int EOF = -1;
    public static final int NEW_LINE = -2;
    public static final int IDENTIFIER = -4;
    public static final int INTEGER_VALUE = -8;
    public static final int STRING_VALUE = -16;

    private int current;
    private char[] line;
    private int lineIndex;

    private int startPosition;
    private int endPosition;
    private int searchIndex;

    private List<Element> tokenList;
    private List<Comment.Line> lines;

    private String firstLineText;
    private int firstLineStartPosition;

    public Scanner(Definition definition, Comment comment) {
        this.tokenList = new ArrayList<>();
        this.lines = comment.getLines();

        final Optional<Comment.Line> firstLine = comment.firstLine();

        final String markerName = "+" + definition.getName();
        final String firstLineText = firstLine.get().getText();
        final int firstLineStartOffset = firstLine.get().startOffset();

        int markerStartIndex = firstLineText.indexOf(markerName);
        int markerEndIndex = markerStartIndex + markerName.length();

        if (firstLineText.contains(markerName + ":")) {
            markerStartIndex = firstLineText.indexOf(markerName + ":");
            markerEndIndex = markerStartIndex + markerName.length() + 1;
        }

        this.firstLineText = firstLine.get().getText().stripTrailing().substring(markerEndIndex);

        if (comment.getLines().size() > 1) {
            this.firstLineText = this.firstLineText.substring(0, this.firstLineText.length() - 2);
        }

        this.firstLineStartPosition = firstLineStartOffset + markerEndIndex;

        this.line = this.firstLineText.toCharArray();
        this.current = IDENTIFIER;
        this.startPosition = -1;
        this.endPosition = 0;
        this.searchIndex = -1;
    }

    public int scan() {
        int character = skipWhitespaces();

        int token = character;
        if (isIdentifier(character, 0)) {
            token = IDENTIFIER;
            character = scanIdentifier();
        } else if (isDecimal(character)) {
            token = INTEGER_VALUE;
            character = scanNumber();
        } else if (character == EOF) {
            return EOF;
        } else if (character == '"') {
            token = STRING_VALUE;
            scanString('"');
            character = peek();
        } else if (character == '`') {
            token = STRING_VALUE;
            scanString('`');
            character = peek();
        } else if (character == '\'') {
            token = STRING_VALUE;
            scanString('\'');
            character = peek();
        } else {
            startPosition = searchIndex;
            character = next();
            endPosition = searchIndex;
        }

        this.current = character;
        return token;
    }

    public int peek() {
        if (current == IDENTIFIER) {
            current = next();
        }

        return current;
    }

    public int next() {
        searchIndex++;

        if (searchIndex >= lineLength()) {

            if (lineIndex + 1 >= lines.size()) {
                return EOF;
            }

            return NEW_LINE;
        }

        return line[searchIndex];
    }

    public boolean nextLine() {
        if (lineIndex + 1 >= lines.size()) {
            return false;
        }

        lineIndex++;

        if (lineIndex == lines.size() - 1) {
            line = lines.get(lineIndex).getText().toCharArray();
        } else {
            line = lines.get(lineIndex).getText().substring(0, lines.get(lineIndex).getText().length() - 2).toCharArray();
        }

        searchIndex = 0;
        if (line.length != 0) {
            current = line[0];
        }

        startPosition = 0;
        endPosition = 0;
        return true;
    }

    public void setLineIndex(int lineIndex) {
        this.lineIndex = lineIndex;

        if (lineIndex == 0) {
            this.line = firstLineText.toCharArray();
        } else if (lineIndex == lines.size() - 1) {
            this.line = lines.get(lineIndex).getText().toCharArray();
        } else {
            this.line = lines.get(lineIndex).getText().substring(0, lines.get(lineIndex).getText().length() - 2).toCharArray();
        }
    }

    public boolean expect(int expected, String description) {
        int token = scan();

        if (token != expected) {
            return false;
        }

        return true;
    }

    public String token() {
        if (startPosition < 0) {
            return "";
        }

        try {
            return String.valueOf(line).substring(startPosition, endPosition);
        } catch (Exception ignored) {

        }

        return "";
    }

    public List<Element> tokens() {
        return tokenList;
    }

    public int skipWhitespaces() {
        int character = peek();
        while (character == '\t' || character == '\r' || character == ' ') {
            character = next();
        }

        this.current = character;
        return character;
    }

    public void resetLine() {
        searchIndex = 0;
        current = line[0];
        startPosition = 0;
        endPosition = 0;
    }

    public String currentLineText() {
        return String.valueOf(line);
    }

    public int lineIndex() {
        return lineIndex;
    }

    public int lineLength() {
        return line.length;
    }

    public int startPosition() {
        return startPosition;
    }

    public void  setStartPosition(int startPosition) {
        this.startPosition = startPosition;
    }

    public int endPosition() {
        return endPosition;
    }

    public void setEndPosition(int endPosition) {
        this.endPosition = endPosition;
    }

    public int searchIndex() {
        return searchIndex;
    }

    public void setSearchIndex(int index) {
        if (index >= lineLength()) {
            this.searchIndex = lineLength();
            return;
        }

        this.searchIndex = index;
        current = line[index];
    }

    public TextRange originalPosition() {
        return new TextRange(originalStartPosition(), originalStartPosition() + token().length());
    }

    public int originalStartPosition() {
        if (lineIndex == 0) {
            return startPosition + firstLineStartPosition;
        }

        return startPosition + lines.get(lineIndex).startOffset();
    }

    public int originalEndPosition() {
        if (lineIndex == 0) {
            return endPosition + firstLineStartPosition;
        }

        return endPosition + lines.get(lineIndex).startOffset();
    }

    public boolean isIdentifier(int character, int index) {
        return character == '_' || Character.isLetter(character) || Character.isDigit(character) && index > 0;
    }

    public boolean isDecimal(int character) {
        return '0' <= character && character <= '9';
    }

    private void addStringToken() {
        String value = token();
        Element element;

        if (tokenList.isEmpty()) {
            element = new Element(value, originalPosition());
        } else {
            int spaceCount = Utils.countLeadingSpace(value);
            element = new Element(value.stripLeading(), new TextRange(originalStartPosition() + spaceCount, originalEndPosition()));
            final Element previous = tokenList.get(tokenList.size()-1);
            previous.setNext(element);
            element.setPrevious(previous);
        }

        tokenList.add(element);
    }

    public int scanString(int quote) {
        int len = 0;
        startPosition = searchIndex;
        int character = next();

        tokenList = new ArrayList<>();

        while (character != quote) {
            if (character < 0) {
                // error
                return len;
            }

            character = next();

            if (character == NEW_LINE) {
                endPosition = searchIndex;
                addStringToken();
                if (!nextLine()) {
                    break;
                }
                character = peek();
            } else if (character == EOF) {
                break;
            }

            len++;
        }

        character = next();
        endPosition = searchIndex;
        addStringToken();
        current = character;
        return len;
    }

    public int scanNumber() {
        if (isDecimal(skipWhitespaces())) {
            startPosition = searchIndex;
        }

        int character = skipWhitespaces();

        while (isDecimal(character)) {
            character = next();
        }

        endPosition = searchIndex;
        this.current = character;
        return character;
    }

    public int scanIdentifier() {
        if (isIdentifier(skipWhitespaces(), 1)) {
            startPosition = searchIndex;
        }

        int character = skipWhitespaces();

        for (int index = 1; isIdentifier(character, index); index++) {
            character = next();
        }

        endPosition = searchIndex;
        this.current = character;
        return character;
    }

}
