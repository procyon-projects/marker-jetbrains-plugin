package com.github.procyonprojects.marker.comment;

import com.github.procyonprojects.marker.element.*;
import com.github.procyonprojects.marker.metadata.Definition;
import com.github.procyonprojects.marker.metadata.Parameter;
import com.github.procyonprojects.marker.metadata.TypeInfo;
import com.intellij.openapi.util.TextRange;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class Parser {

    public void parse(Definition definition, Comment comment) {
        final Optional<Comment.Line> firstLine = comment.firstLine();

        if (firstLine.isEmpty()) {
            return;
        }

        final String markerName = "+" + definition.getName();
        final String firstLineText = firstLine.get().getText();
        final int firstLineStartOffset = firstLine.get().startOffset();

        final MarkerElement markerElement = new MarkerElement(definition);
        markerElement.setText(markerName);

        int markerStartIndex = firstLineText.indexOf(markerName) + firstLineStartOffset;
        int markerEndIndex = markerStartIndex + markerName.length();

        if (firstLineText.contains(markerName + ":")) {
            markerElement.setText(markerName + ":");
            markerStartIndex = firstLineText.indexOf(markerName + ":") + firstLineStartOffset;
            markerEndIndex = markerStartIndex + markerName.length() + 1;
        }

        markerElement.setRange(new TextRange(markerStartIndex, markerEndIndex));

        final Scanner scanner = new Scanner();
        final Map<String, Parameter> seenMap = new HashMap<>();

        Element currentElement = markerElement;

        if (scanner.peek() != Scanner.EOF) {
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


                final ParameterElement parameterElement = new ParameterElement();
                currentElement.setNext(parameterElement);
                parameterElement.setPrevious(currentElement);
                currentElement = parameterElement;

                String argumentName = scanner.token();

                character = scanner.skipWhitespaces();

                if (character == Scanner.EOF) {
                    break;
                } else if (character == Scanner.NEW_LINE) {
                    if (!scanner.nextLine()) {
                        break;
                    }
                    character = scanner.skipWhitespaces();
                }

                Optional<Parameter> parameter = definition.getParameter(argumentName);

                if (parameter.isEmpty()) {
                    Parameter anyParameter = new Parameter();
                    anyParameter.setType(TypeInfo.ANY_TYPE_INFO);
                    parameter = Optional.of(anyParameter);
                }

                seenMap.put(argumentName, parameter.get());

                if (scanner.peek() == Scanner.EOF) {

                    break;
                } else if (scanner.peek() == Scanner.NEW_LINE) {
                    if (!scanner.nextLine()) {
                        break;
                    }
                }

                TypeInfo typeInfo = parameter.get().getType();
                parameterElement.setTypeInfo(typeInfo);

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

                    break;
                }
            }
        }
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
                return parseSliceValues(scanner, typeInfo.getItemType());
            case MapType:
                return parseMapValues(scanner);
            case AnyType:
            TypeInfo inferredType = interType(scanner, false);
            return parseValue(scanner, parameter, inferredType);
        }

        return null;
    }

    private Element parseBooleanValue(Scanner scanner) {
        if (!scanner.expect(Scanner.IDENTIFIER, "Boolean (true or false)")) {
            return null;
        }

        final String token = scanner.token();

        if (",".equals(token)) {
            return null;
        }

        if (!"false".equals(token) && !"true".equals(token)) {
            return null;
        }

        return new BooleanElement();
    }


    private Element parseIntegerValue(Scanner scanner) {
        int currentCharacter = scanner.peek();
        boolean isNegative = false;

        if (currentCharacter == '-') {
            isNegative = true;
            scanner.scan();
        }

        if (!scanner.expect(Scanner.INTEGER_VALUE, "Integer")) {
        }

        String valueText = scanner.token();

        if (isNegative) {
            valueText = "-" + valueText;
        }

        try {
            final int integerValue = Integer.parseInt(valueText);
            return new IntegerElement();
        } catch (NumberFormatException exception) {

        }

        return null;
    }

    private Element parseStringValue(Scanner scanner) {
        final int token = scanner.scan();

        if (token == Scanner.STRING_VALUE) {
            return new StringElement();
        }

        final int startPosition = scanner.startPosition();
        final String initialValue = scanner.token();

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

        final int endPosition = scanner.searchIndex();
        String strValue = "";

        try {
            strValue = scanner.currentLineText().substring(startPosition, endPosition);
        } catch (Exception ignored) {

        }

        if (initialValue.equals(strValue) || initialValue.equals("-")) {
            if (token == Scanner.INTEGER_VALUE || (token == Scanner.IDENTIFIER && ("true".equals(strValue) || "false".equals(strValue))) || token == '-') {
                return null;
            }
        }

        if (!strValue.startsWith(",") && !strValue.startsWith(";") && !strValue.startsWith(":") && !strValue.startsWith("{") && !strValue.startsWith("}")) {

        }

        return null;
    }

    private Element parseSliceValues(Scanner scanner, TypeInfo itemTypeInfo) {

        final SliceElement sliceElement = new SliceElement();

        if (scanner.skipWhitespaces() == '{') {

            scanner.scan();
            int character = scanner.skipWhitespaces();

            for (; character != '}' && character != Scanner.EOF; character = scanner.skipWhitespaces()) {
                if (scanner.skipWhitespaces() == Scanner.NEW_LINE) {
                    if (!scanner.nextLine()) {
                        return sliceElement;
                    }
                }

                Element item = parseValue(scanner, null, null);

                if (item == null) {
                    return sliceElement;
                }

                int token = scanner.skipWhitespaces();

                if (token == '}') {
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

                    return sliceElement;
                }
            }


            if (!scanner.expect('}', "Right Curly Bracket '}'")) {

            } else {

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

            Element item = parseValue(scanner, null, null);

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
                return sliceElement;
            }

        }

        return sliceElement;
    }

    private Element parseMapValues(Scanner scanner) {
        final MapElement mapElement = new MapElement();

        if (!scanner.expect('{', "Left Curly Bracket")) {
            return mapElement;
        }

        int character = scanner.skipWhitespaces();

        for (;character != '}' && character != Scanner.EOF; character = scanner.skipWhitespaces()) {
            if (character == Scanner.NEW_LINE) {
                if (!scanner.nextLine()) {
                    return mapElement;
                }

                continue;
            }

            Element keyElement = parseStringValue(scanner);

            if (keyElement == null) {

                return mapElement;
            }

            if (scanner.skipWhitespaces() == Scanner.NEW_LINE) {
                if (!scanner.nextLine()) {
                    return mapElement;
                }
            }

            if (!scanner.expect(':', "Colon ':'")) {

                return mapElement;
            }

            if (scanner.skipWhitespaces() == Scanner.NEW_LINE) {
                if (!scanner.nextLine()) {
                    return mapElement;
                }
            }

            Element valueElement = parseValue(scanner, null, null);

            if (valueElement == null) {
                return mapElement;
            }

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
                return mapElement;
            }

        }

        if (!scanner.expect('}', "Right Curly Bracket '}")) {
            return mapElement;
        }

        return mapElement;
    }

    private TypeInfo interType(Scanner scanner, boolean ignoreLegacySlice) {
        return null;
    }
}
