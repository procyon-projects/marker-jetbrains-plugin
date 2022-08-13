package com.github.procyonprojects.marker.comment;

import b.e.E;
import b.h.T;
import com.github.procyonprojects.marker.Utils;
import com.github.procyonprojects.marker.element.*;
import com.github.procyonprojects.marker.metadata.Definition;
import com.github.procyonprojects.marker.metadata.Parameter;
import com.github.procyonprojects.marker.metadata.Type;
import com.github.procyonprojects.marker.metadata.TypeInfo;
import com.intellij.openapi.util.TextRange;
import org.apache.commons.collections.CollectionUtils;

import java.util.*;

public class Parser {

    public ParseResult parse(Definition definition, Comment comment) {
        final Optional<Comment.Line> firstLine = comment.firstLine();

        if (firstLine.isEmpty()) {
            return null;
        }

        final String markerName = "+" + definition.getName();
        final String firstLineText = firstLine.get().getText();
        final int firstLineStartOffset = firstLine.get().startOffset();

        final MarkerElement markerElement = new MarkerElement(definition);
        markerElement.setText(markerName);

        int markerStartIndex = firstLineText.indexOf(markerName) + firstLineStartOffset;
        int markerEndIndex = markerStartIndex + markerName.length();
        boolean isValueSyntax = true;

        if (firstLineText.contains(markerName + ":")) {
            markerElement.setText(markerName + ":");
            markerStartIndex = firstLineText.indexOf(markerName + ":") + firstLineStartOffset;
            markerEndIndex = markerStartIndex + markerName.length() + 1;
            isValueSyntax = false;
        }

        markerElement.setRange(new TextRange(markerStartIndex, markerEndIndex));

        final Scanner scanner = new Scanner(definition, comment);
        final Map<String, Parameter> seenMap = new HashMap<>();

        Element currentElement = markerElement;

        if (CollectionUtils.isNotEmpty(definition.getParameters()) && scanner.peek() != Scanner.EOF) {
            while (true) {
                int character = scanner.skipWhitespaces();

                if (character == Scanner.EOF) {
                    break;
                } else if (character == Scanner.NEW_LINE) {
                    if (!scanner.nextLine()) {
                        break;
                    }
                    continue;
                }

                ParameterElement parameterElement = new ParameterElement();
                currentElement.setNext(parameterElement);
                parameterElement.setPrevious(currentElement);
                currentElement = parameterElement;

                String argumentName = "";

                if (!scanner.expect(Scanner.IDENTIFIER, "Value or Argument Name")) {
                    if (isValueSyntax) {
                        if (!"=".equals(scanner.token()) && seenMap.size() == 0) {
                            parameterElement.setEqualSign(new ExpectedElement("Expected equal '='", "=", scanner.originalPosition()));
                            break;
                        } else {
                            parameterElement.setEqualSign(new Element("=", scanner.originalPosition()));
                            argumentName = "Value";
                        }
                    } else {
                        parameterElement.setName(new ExpectedElement("Expected argument name", null, scanner.originalPosition()));
                        break;
                    }
                } else {
                    argumentName = scanner.token();
                    parameterElement.setName(new Element(argumentName, scanner.originalPosition()));

                    character = scanner.skipWhitespaces();

                    if (character == Scanner.EOF) {
                        break;
                    } else if (character == Scanner.NEW_LINE) {
                        if (!scanner.nextLine()) {
                            break;
                        }
                        character = scanner.skipWhitespaces();
                    }

                    if (!scanner.expect('=', "Equal Sign")) {
                        parameterElement.setEqualSign(new ExpectedElement("Expected equal '='", "=", new TextRange(scanner.originalStartPosition(), scanner.originalStartPosition() + 1)));
                        break;
                    } else {
                        parameterElement.setEqualSign(new Element("=", new TextRange(scanner.originalStartPosition(), scanner.originalStartPosition() + 1)));
                    }
                }

                Optional<Parameter> parameter = definition.getParameter(argumentName);

                if (parameter.isEmpty()) {
                    Parameter anyParameter = new Parameter();
                    anyParameter.setType(TypeInfo.ANY_TYPE_INFO);
                    parameter = Optional.of(anyParameter);
                    parameterElement.setName(new UnresolvedElement(String.format("Unresolved parameter %s", argumentName), argumentName, scanner.originalPosition()));
                }

                TypeInfo typeInfo = parameter.get().getType();
                parameterElement.setTypeInfo(typeInfo);
                parameterElement.setParameter(parameter.get());

                seenMap.put(argumentName, parameter.get());

                if (scanner.peek() == Scanner.EOF) {
                    break;
                } else if (scanner.peek() == Scanner.NEW_LINE) {
                    if (!scanner.nextLine()) {
                        break;
                    }
                }
                Element value = parseValue(scanner, parameter.get(), typeInfo);
                parameterElement.setValue(value);

                scanner.skipWhitespaces();

                if (scanner.peek() == Scanner.EOF) {
                    break;
                } else if (scanner.peek() == Scanner.NEW_LINE) {
                    if (!scanner.nextLine()) {
                        break;
                    }
                }

                if (!scanner.expect(',', "Comma")) {
                    ExpectedElement expectedComma = new ExpectedElement("Expected comma ','", ",", scanner.originalPosition());
                    currentElement.setNext(expectedComma);
                    expectedComma.setPrevious(currentElement);
                    break;
                }

                Element commaElement = new Element(",", scanner.originalPosition());
                currentElement.setNext(commaElement);
                commaElement.setPrevious(currentElement);
                currentElement = commaElement;
            }
        }

        final List<Element> nextLineElements = new ArrayList<>();
        for (Comment.Line line : comment.getLines()) {
            if (line.getText().endsWith(" \\")) {
                int startOffset = line.startOffset() + line.getText().length() - 1;
                nextLineElements.add(new Element("\\", new TextRange(startOffset, startOffset + 1)));
            }
        }

        return new ParseResult(markerElement, nextLineElements);
    }

    private Element parseValue(Scanner scanner, Parameter parameter, TypeInfo typeInfo) {
        switch (typeInfo.getActualType()) {
            case BooleanType:
                return parseBooleanValue(scanner);
            case SignedIntegerType:
            case UnsignedIntegerType:
                return parseIntegerValue(scanner);
            case StringType:
                return parseStringValue(scanner);
            case SliceType:
                return parseSliceValues(scanner, parameter, typeInfo.getItemType());
            case MapType:
                return parseMapValues(scanner, parameter, typeInfo.getItemType());
            case AnyType:
                TypeInfo inferredType = inferType(scanner, false);
                return parseValue(scanner, parameter, inferredType);
        }

        return null;
    }

    private Element parseBooleanValue(Scanner scanner) {
        if (!scanner.expect(Scanner.IDENTIFIER, "Boolean (true or false)")) {
            return new ExpectedElement("Expected true or false", scanner.token(), scanner.originalPosition());
        }

        final String token = scanner.token();

        if (",".equals(token)) {
            return new ExpectedElement("Expected true or false", token, scanner.originalPosition());
        }

        if (!"false".equals(token) && !"true".equals(token)) {
            return new ExpectedElement("Expected true or false", token, scanner.originalPosition());
        }

        return new BooleanElement(token, scanner.originalPosition());
    }

    private Element parseIntegerValue(Scanner scanner) {
        int currentCharacter = scanner.peek();
        boolean isNegative = false;

        if (currentCharacter == '-') {
            isNegative = true;
            scanner.scan();
        }

        if (!scanner.expect(Scanner.INTEGER_VALUE, "Integer")) {
            String text = scanner.token();

            if (isNegative) {
                text = "-" + text;
                scanner.setStartPosition(scanner.startPosition() - 1);
            }


            return new ExpectedElement("Expected integer", text, scanner.originalPosition());
        }

        String valueText = scanner.token();

        if (isNegative) {
            valueText = "-" + valueText;
            scanner.setStartPosition(scanner.startPosition() - 1);
        }

        scanner.setEndPosition(scanner.searchIndex());

        try {
            final int integerValue = Integer.parseInt(valueText);
            return new IntegerElement(integerValue, valueText, scanner.originalPosition());
        } catch (NumberFormatException exception) {
            return new ExpectedElement("Expected integer", "", scanner.originalPosition());
        }
    }

    private Element parseStringValue(Scanner scanner) {
        int token = scanner.scan();

        if (token == Scanner.STRING_VALUE) {
            return new StringElement(scanner.tokens());
        }

        int startPosition = scanner.startPosition();
        String initialValue = scanner.token();

        int character = scanner.peek();

        if (character != Scanner.NEW_LINE) {
            for (; character != ',' && character != ';' && character != ':' && character != '}' && character != '{' &&
                    character != Scanner.EOF && !Character.isWhitespace(character); character = scanner.next()) {
                if (character == Scanner.NEW_LINE) {
                    scanner.nextLine();
                    break;
                }

                scanner.scan();
            }
        }

        int endPosition = scanner.endPosition();
        String strValue = "";

        try {
            strValue = scanner.currentLineText().substring(startPosition, endPosition);
        } catch (Exception ignored) {

        }

        if (initialValue.equals(strValue) || initialValue.equals("-")) {
            scanner.setStartPosition(startPosition);
            if (token == Scanner.INTEGER_VALUE || (token == Scanner.IDENTIFIER && ("true".equals(strValue) || "false".equals(strValue))) || token == '-') {
                return new ExpectedElement("Expected string", strValue, scanner.originalPosition());
            }
        }

        int leadingSpaces = Utils.countLeadingSpace(strValue);
        int trailingSpaces = Utils.countTrailingSpaces(strValue);

        try {
            strValue = strValue.substring(leadingSpaces, strValue.length() - trailingSpaces);
        } catch (Exception ignored) {

        }

        if (!strValue.startsWith(",") && !strValue.startsWith(";") && !strValue.startsWith(":") && !strValue.startsWith("{") && !strValue.startsWith("}")) {
            scanner.setStartPosition(startPosition);
            try {
                return new StringElement(List.of(new Element(strValue, scanner.originalPosition())));
            } catch (Exception ignored) {

            }
        }

        return null;
    }

    private Element parseSliceValues(Scanner scanner, Parameter parameter, TypeInfo itemTypeInfo) {

        final SliceElement sliceElement = new SliceElement();
        sliceElement.setParameter(parameter);

        Element current = null;

        if (scanner.skipWhitespaces() == '{') {
            scanner.setStartPosition(scanner.searchIndex());
            sliceElement.setLeftBrace(new Element("{", new TextRange(scanner.originalStartPosition(), scanner.originalStartPosition() + 1)));

            scanner.scan();

            int character = scanner.skipWhitespaces();

            for (; character != '}' && character != Scanner.EOF; character = scanner.skipWhitespaces()) {
                if (scanner.skipWhitespaces() == Scanner.NEW_LINE) {
                    if (!scanner.nextLine()) {
                        return sliceElement;
                    }
                }

                Element item = parseValue(scanner, parameter, itemTypeInfo);

                if (item == null) {
                    Element expectedItem = new ExpectedElement("Expected slice item", "slice item", new TextRange(scanner.originalStartPosition(), scanner.originalStartPosition() + 1));
                    expectedItem.setPrevious(current);

                    if (current != null) {
                        current.setNext(expectedItem);
                    } else {
                        sliceElement.setNext(expectedItem);
                    }

                    return sliceElement;
                }

                if (current != null) {
                    current.setNext(item);
                } else {
                    sliceElement.setNext(item);
                }

                item.setPrevious(current);
                current = item;

                int token = scanner.skipWhitespaces();

                if (token == '}') {
                    scanner.setStartPosition(scanner.searchIndex());
                    sliceElement.setRightBrace(new Element("}", new TextRange(scanner.originalStartPosition(), scanner.originalStartPosition() + 1)));
                    break;
                } else if (token == Scanner.EOF) {
                    break;
                }

                if (scanner.skipWhitespaces() == Scanner.NEW_LINE) {
                    if (!scanner.nextLine()) {
                        return sliceElement;
                    }
                }

                if (!scanner.expect(',', "Comma ','")) {
                    Element expectedComma = new ExpectedElement("Expected comma", ",", new TextRange(scanner.originalStartPosition(), scanner.originalStartPosition() + 1));
                    expectedComma.setPrevious(current);
                    current.setNext(expectedComma);
                    return sliceElement;
                }


                Element comma = new Element(",", new TextRange(scanner.originalStartPosition(), scanner.originalStartPosition() + 1));
                current.setNext(comma);
                comma.setPrevious(current);
                current = comma;
            }

            if (current != null && ",".equals(current.getText())) {
                Element expectedItem = new ExpectedElement("Expected slice item", "slice item", new TextRange(scanner.originalStartPosition(), scanner.originalStartPosition() + 1));
                expectedItem.setPrevious(current);
                current.setNext(expectedItem);
            }

            if (!scanner.expect('}', "Right Curly Bracket '}'")) {
                Element expectedRightBracket = new ExpectedElement("Right Curly Bracket '}'", "}", new TextRange(scanner.originalStartPosition(), scanner.originalStartPosition() + 1));
                sliceElement.setRightBrace(expectedRightBracket);
            } else {
                sliceElement.setRightBrace(new Element("}", new TextRange(scanner.originalStartPosition(), scanner.originalStartPosition() + 1)));
            }

            return sliceElement;
        }

        int character = scanner.skipWhitespaces();

        for (; character != ',' && character != '}' && character != Scanner.EOF; character = scanner.skipWhitespaces()) {
            if (scanner.skipWhitespaces() == Scanner.NEW_LINE) {
                if (!scanner.nextLine()) {
                    return sliceElement;
                }
            }

            Element item = parseValue(scanner, parameter, itemTypeInfo);

            if (item == null) {
                Element expectedItem = new ExpectedElement("Expected slice item", "slice item", new TextRange(scanner.originalStartPosition(), scanner.originalStartPosition() + 1));
                expectedItem.setPrevious(current);

                if (current != null) {
                    current.setNext(expectedItem);
                } else {
                    sliceElement.setNext(expectedItem);
                }

                return sliceElement;
            }

            if (current != null) {
                current.setNext(item);
            } else {
                sliceElement.setNext(item);
            }

            current = item;

            int token = scanner.skipWhitespaces();

            if (token == ',' || token == '{' || token == '}' || token == Scanner.EOF) {
                break;
            } else if (token == Scanner.NEW_LINE) {
                if (!scanner.nextLine()) {
                    return sliceElement;
                }

                token = scanner.skipWhitespaces();
            }

            scanner.scan();

            if (token != ';') {
                Element semicolon = new ExpectedElement("Expected semicolon", ";", new TextRange(scanner.originalStartPosition(), scanner.originalStartPosition() + 1));
                semicolon.setPrevious(item);
                current.setNext(semicolon);
                return sliceElement;
            }


            Element semicolon = new Element(";", new TextRange(scanner.originalStartPosition(), scanner.originalStartPosition() + 1));
            current.setNext(semicolon);
            semicolon.setPrevious(current);
            current = semicolon;
        }

        return sliceElement;
    }

    private Element parseMapValues(Scanner scanner, Parameter parameter, TypeInfo itemType) {
        final MapElement mapElement = new MapElement();

        if (!scanner.expect('{', "Left Curly Bracket")) {
            Element expectedLeftBracket = new ExpectedElement("Left Curly Bracket '{'", "{", new TextRange(scanner.originalStartPosition(), scanner.originalStartPosition() + 1));
            mapElement.setLeftBrace(expectedLeftBracket);
            return mapElement;
        }

        Element curlyBracket = new Element("{", new TextRange(scanner.originalStartPosition(), scanner.originalStartPosition() + 1));
        mapElement.setLeftBrace(curlyBracket);

        int character = scanner.skipWhitespaces();

        Element current = null;

        for (;character != '}' && character != Scanner.EOF; character = scanner.skipWhitespaces()) {
            if (character == Scanner.NEW_LINE) {
                if (!scanner.nextLine()) {
                    return mapElement;
                }

                continue;
            }

            final KeyValueElement keyValueElement = new KeyValueElement();

            if (current != null) {
                current.setNext(keyValueElement);
            } else {
                mapElement.setNext(keyValueElement);
                current = keyValueElement;
            }

            Element keyElement = parseStringValue(scanner);

            if (keyElement == null) {
                Element expectedKeyElement = new ExpectedElement("Expected map key", "map key", new TextRange(scanner.originalStartPosition(), scanner.originalStartPosition() + 1));
                keyValueElement.setKeyElement(expectedKeyElement);
                return mapElement;
            }

            keyValueElement.setKeyElement(keyElement);

            if (scanner.skipWhitespaces() == Scanner.NEW_LINE) {
                if (!scanner.nextLine()) {
                    return mapElement;
                }
            }

            if (!scanner.expect(':', "Colon ':'")) {
                Element expectedColon = new ExpectedElement("Expected colon ':'", ":", new TextRange(scanner.originalStartPosition(), scanner.originalStartPosition() + 1));
                keyValueElement.setColonElement(expectedColon);
                return mapElement;
            }

            Element colonElement = new Element(":", new TextRange(scanner.originalStartPosition(), scanner.originalStartPosition() + 1));
            keyValueElement.setColonElement(colonElement);

            if (scanner.skipWhitespaces() == Scanner.NEW_LINE) {
                if (!scanner.nextLine()) {
                    return mapElement;
                }
            }

            Element valueElement = parseValue(scanner, parameter, itemType);

            if (valueElement == null) {
                Element expectedValueElement = new ExpectedElement("Expected map value", "map value", new TextRange(scanner.originalStartPosition(), scanner.originalStartPosition() + 1));
                keyValueElement.setValueElement(expectedValueElement);
                return mapElement;
            }

            keyValueElement.setValueElement(valueElement);

            if (scanner.skipWhitespaces() == Scanner.NEW_LINE) {
                if (!scanner.nextLine()) {
                    return mapElement;
                }
            }

            if (scanner.skipWhitespaces() == '}') {
                break;
            }

            if (scanner.skipWhitespaces() == Scanner.NEW_LINE) {
                if (!scanner.nextLine()) {
                    return mapElement;
                }
            }

            if (!scanner.expect(',', "Comma ','")) {
                Element expectedComma = new ExpectedElement("Expected comma ','", ",", new TextRange(scanner.originalStartPosition(), scanner.originalStartPosition() + 1));
                current.setNext(expectedComma);
                return mapElement;
            }


            Element comma = new Element(",", new TextRange(scanner.originalStartPosition(), scanner.originalStartPosition() + 1));
            current.setNext(comma);
            comma.setPrevious(current);
            current = comma;
        }

        if (!scanner.expect('}', "Right Curly Bracket '}")) {
            Element expectedRightBracket = new ExpectedElement("Right Curly Bracket '}'", "}", new TextRange(scanner.originalStartPosition(), scanner.originalStartPosition() + 1));
            mapElement.setRightBrace(expectedRightBracket);
            return mapElement;
        }

        Element rightCurlyBracket = new Element("}", new TextRange(scanner.originalStartPosition(), scanner.originalStartPosition() + 1));
        mapElement.setRightBrace(rightCurlyBracket);

        return mapElement;
    }

    private TypeInfo inferType(Scanner scanner, boolean ignoreLegacySlice) {
        int character = scanner.skipWhitespaces();
        int searchIndex = scanner.searchIndex();
        int lineIndex = scanner.lineIndex();

        if (!ignoreLegacySlice) {
            TypeInfo itemType = inferType(scanner, true);

            int token = scanner.scan();

            for (; token != ',' && token != Scanner.EOF && token != ';'; token = scanner.scan()) {
                if (token == Scanner.NEW_LINE) {
                    scanner.nextLine();
                    token = scanner.scan();
                }
            }

            scanner.setLineIndex(lineIndex);
            scanner.setSearchIndex(searchIndex);

            if (token == ';') {
                return new TypeInfo(Type.SliceType, itemType);
            }

            return itemType;
        }

        if (character == '"' || character == '\'' || character == '`') {
            return new TypeInfo(Type.StringType);
        }

        if (character == '{') {
            scanner.scan();

            TypeInfo elementType = inferType(scanner, true);

            // skip left curly bracket character
            scanner.setLineIndex(lineIndex);
            scanner.setSearchIndex(searchIndex+1);

            assert elementType != null;
            if (Type.StringType == elementType.getActualType()) {
                int token = scanner.skipWhitespaces();

                if (token == Scanner.NEW_LINE) {
                    if (!scanner.nextLine()) {
                        scanner.setLineIndex(lineIndex);
                        scanner.setSearchIndex(searchIndex);
                    }

                    return new TypeInfo(Type.SliceType, elementType);
                }

                parseStringValue(scanner);
                token = scanner.scan();

                if (token == Scanner.NEW_LINE) {
                    if (!scanner.nextLine()) {
                        scanner.setLineIndex(lineIndex);
                        scanner.setSearchIndex(searchIndex);

                        return new TypeInfo(Type.SliceType, elementType);
                    } else {
                        token = scanner.scan();
                    }
                }

                if (token == ':') {
                    scanner.setLineIndex(lineIndex);
                    scanner.setSearchIndex(searchIndex);
                    return new TypeInfo(Type.MapType, TypeInfo.ANY_TYPE_INFO);
                }
            }

            scanner.setLineIndex(lineIndex);
            scanner.setSearchIndex(searchIndex);

            return new TypeInfo(Type.SliceType, elementType);
        }

        boolean canBeString = false;

        if (character == 't' || character == 'f') {
            int token = scanner.scan();

            if (token == Scanner.IDENTIFIER) {

                if ("true".equals(scanner.token()) || "false".equals(scanner.token())) {
                    scanner.setLineIndex(lineIndex);
                    scanner.setSearchIndex(searchIndex);
                    return new TypeInfo(Type.BooleanType);
                }

                canBeString = true;
            } else {
                return TypeInfo.INVALID_TYPE_INFO;
            }

        }

        if (!canBeString) {
            int token = scanner.scan();

            if (token == '-') {
                token = scanner.scan();
            }

            if (token == Scanner.INTEGER_VALUE) {
                return new TypeInfo(Type.SignedIntegerType);
            }
        }

        return new TypeInfo(Type.StringType);
    }
}
