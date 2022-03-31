package com.github.procyonprojects.marker.highlighter;

public class MarkerCommentScanner {
    public final static int EOF = -1;
    public final static int IDENTIFIER = -2;
    public final static int INTEGER_VALUE = -4;
    public final static int STRING_VALUE = -8;


    private char[] source;
    private int tokenStartPosition;
    private int tokenEndPosition;
    private int searchIndex;
    private int character;

    public MarkerCommentScanner(String source) {
        this.source = source.toCharArray();
        this.character = IDENTIFIER;
        this.tokenStartPosition = -1;
        this.tokenEndPosition = 0;
        this.searchIndex = -1;
    }

    public int searchIndex() {
        return searchIndex;
    }

    public int sourceLength() {
        return source.length;
    }

    public int peek() {
        if (character == IDENTIFIER) {
            character = next();
        }

        return character;
    }

    public int scan() {
        int character = skipWhitespaces();

        int token = character;
        if (IsIdentifier(character, 0)) {
            token = IDENTIFIER;
            character = ScanIdentifier();
        } else if (IsDecimal(character)) {
            token = INTEGER_VALUE;
            character = ScanNumber();
        } else if (character == EOF) {
            return EOF;
        } else if (character == '"') {
            token = STRING_VALUE;
            ScanString('"');
            character = peek();
        } else if (character == '`') {
            token = STRING_VALUE;
            ScanString('`');
            character = peek();
        } else {
            tokenStartPosition = searchIndex;
            character = next();
            tokenEndPosition = searchIndex;
        }

        this.character = character;
        return token;
    }

    public boolean IsIdentifier(int character, int index) {
        return character == '_' || Character.isLetter(character) || Character.isDigit(character) && index > 0;
    }

    public String token() {
        return "d";
    }

    public int next() {
        searchIndex++;

        if (searchIndex >= sourceLength()) {
            return EOF;
        }

        return source[searchIndex];
    }

    public int skipWhitespaces() {
        int character = peek();

        while (character == '\t' || character == '\r' || character == ' ') {
            character = next();
        }

        this.character = character;
        return character;
    }

    public void reset() {
        searchIndex = 0;
        character = source[0];
        tokenStartPosition = 0;
        tokenEndPosition = 0;
    }

    public boolean expect(int expected, String description) {
        int token = scan();

        if (token != expected) {
            return false;
        }

        return true;
    }

    public void setSearchIndex(int index) {
        if (index >= sourceLength()) {
            searchIndex = sourceLength();
            return;
        }

        searchIndex = index;
        character = source[index];
    }

    public int ScanString(int quote) {
        int len = 0;
        tokenStartPosition = searchIndex;
        int character = next();

        while (character != quote) {
            if (character == '\n' || character < 0) {
                //scanner.AddError(fmt.Sprintf("'%c' is missing", quote))
                return len;
            }

            character = next();
            len++;
        }

        character = next();
        tokenEndPosition = searchIndex;
        this.character = character;
        return len;
    }

    public int ScanNumber() {
        if (IsDecimal(skipWhitespaces())) {
            tokenStartPosition = searchIndex;
        }

        int character = skipWhitespaces();

        while (IsDecimal(character)) {
            character = next();
        }

        tokenEndPosition = searchIndex;
        this.character = character;
        return character;
    }

    public boolean IsDecimal(int character)  {
        return '0' <= character && character <= '9';
    }

    public int ScanIdentifier() {
        if (IsIdentifier(skipWhitespaces(), 1)) {
            tokenStartPosition = searchIndex;
        }

        int character = skipWhitespaces();

        for (int index = 1; IsIdentifier(character, index); index++) {
            character = next();
        }

        tokenEndPosition = searchIndex;
        this.character = character;
        return character;
    }

}
