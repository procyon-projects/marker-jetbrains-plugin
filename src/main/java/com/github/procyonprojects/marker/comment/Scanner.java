package com.github.procyonprojects.marker.comment;

import com.github.procyonprojects.marker.element.Element;

import java.util.List;

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
    private List<String> lines;

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
            line = lines.get(lineIndex).toCharArray();
        } else {
            line = lines.get(lineIndex).substring(0, lines.get(lineIndex).length() - 1).toCharArray();
        }

        searchIndex = 0;
        if (line.length != 0) {
            current = line[0];
        }

        startPosition = 0;
        endPosition = 0;
        return true;
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

    public int lineLength() {
        return line.length;
    }

    public int startPosition() {
        return startPosition;
    }

    public int endPosition() {
        return endPosition;
    }

    public int searchIndex() {
        return searchIndex;
    }

    public int originalStartPosition() {
        return 0;
    }

    public int originalEndPosition() {
        return 0;
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

    }

    public int scanString(int quote) {
        int len = 0;
        startPosition = searchIndex;
        int character = next();

        tokenList.clear();

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
