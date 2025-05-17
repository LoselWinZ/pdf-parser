package de.antl.parser.service;

import de.antl.parser.enums.TokenType;
import de.antl.parser.exceptions.ParseException;
import de.antl.parser.model.*;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.zip.InflaterInputStream;

public class PDFParser {
    private final PDFTokenizer tokenizer;

    public PDFParser(PDFTokenizer tokenizer) {
        this.tokenizer = tokenizer;
    }

    public PDFIndirectObject parseIndirectObject() throws IOException, ParseException {
        // Read the object number
        Token numberToken = tokenizer.nextToken();
        if (numberToken == null || numberToken.type() != TokenType.NUMBER) {
            throw new ParseException("Expected object number but got: " + numberToken);
        }
        int objectNumber = Integer.parseInt(numberToken.getStringValue());

        // Read the generation number
        Token genToken = tokenizer.nextToken();
        if (genToken == null || genToken.type() != TokenType.NUMBER) {
            throw new ParseException("Expected generation number but got: " + genToken);
        }
        int generationNumber = Integer.parseInt(genToken.getStringValue());

        // Expect the "obj" keyword
        Token objToken = tokenizer.nextToken();
        if (objToken == null || !"obj".equals(objToken.value())) {
            throw new ParseException("Expected 'obj' keyword but got: " + objToken);
        }

        // Parse the actual object data
        Object parsedObject = parseObject();

        // Expect the "endobj" keyword
        Token endObjToken = tokenizer.nextToken();
        if (endObjToken == null || !"endobj".equals(endObjToken.value())) {
            throw new ParseException("Expected 'endobj' keyword but got: " + endObjToken);
        }

        return new PDFIndirectObject(objectNumber, generationNumber, parsedObject);
    }

    private Object parseObject() throws IOException, ParseException {
        Token token = tokenizer.peekToken();
        if (token == null) {
            return null;
        }

        if (token.type() == TokenType.NUMBER) {
            // Look ahead without consuming the first number.
            Token firstNumber = tokenizer.nextToken(); // Consume first number
            Token secondToken = tokenizer.peekToken();

            if (secondToken != null && secondToken.type() == TokenType.NUMBER) {
                // Consume second number token
                Token secondNumber = tokenizer.nextToken();
                Token rToken = tokenizer.peekToken();
                if (rToken != null && "R".equals(rToken.value())) {
                    tokenizer.nextToken(); // Consume the "R" token
                    int objNum = Integer.parseInt(firstNumber.getStringValue());
                    int genNum = Integer.parseInt(secondNumber.getStringValue());
                    return new PDFIndirectReference(objNum, genNum);
                } else {
                    // Not an indirect reference, so we treat the first token as a literal number.
                    // (In a more advanced design, you'd support lookahead with pushback.)
                    return firstNumber.value();
                }
            } else {
                // Only one number token: return it as a literal.
                return firstNumber.value();
            }
        }

        return switch (token.type()) {
            case STRING, HEX_STRING, NAME, BOOLEAN, VERSION -> tokenizer.nextToken(); // consume token
            case ARRAY_START -> parseArray();
            case DICT_START -> parseDictionary();
            default -> throw new ParseException("Unexpected token: " + token);
        };
    }

    private List<Object> parseArray() throws IOException, ParseException {
        Token token = tokenizer.nextToken();
        if (token.type() != TokenType.ARRAY_START) {
            throw new ParseException("Expected array start token, found: " + token);
        }

        List<Object> array = new ArrayList<>();

        token = tokenizer.peekToken();
        while (token != null && token.type() != TokenType.ARRAY_END) {
            Object obj = parseObject();
            array.add(obj);
            token = tokenizer.peekToken();
        }

        token = tokenizer.nextToken();
        if (token.type() != TokenType.ARRAY_END) {
            throw new ParseException("Expected array end token, found: " + token);
        }

        return array;
    }

    private Map<String, Object> parseDictionary() throws IOException, ParseException {
        Token token = tokenizer.nextToken(); // consume DICT_START
        if (token.type() != TokenType.DICT_START) {
            throw new ParseException("Expected dictionary start token, found: " + token);
        }

        Map<String, Object> dict = new HashMap<>();

        // Continue reading key-value pairs until we hit DICT_END
        token = tokenizer.peekToken();
        while (token != null && token.type() != TokenType.DICT_END) {
            // Dictionary keys should always be names.
            Token keyToken = tokenizer.nextToken();
            if (keyToken.type() != TokenType.NAME) {
                throw new ParseException("Dictionary key must be a NAME token, found: " + keyToken);
            }
            String key = keyToken.getStringValue();

            // Parse the value associated with this key
            Object value = parseObject();
            dict.put(key, value);

            token = tokenizer.peekToken();
        }
        // Consume the DICT_END token
        token = tokenizer.nextToken();
        if (token.type() != TokenType.DICT_END) {
            throw new ParseException("Expected dictionary end token, found: " + token);
        }

        if (isStreamStart()) {
            return parseStream(dict);
        }

        return dict;
    }

    private boolean isStreamStart() throws IOException {
        Token token = tokenizer.peekToken();
        return token.type() == TokenType.STREAM_DATA;
    }

    private Map<String, Object> parseStream(Map<String, Object> dict) throws IOException, ParseException {
        Token token = tokenizer.nextToken();

        if (token.type() != TokenType.STREAM_DATA) {
            throw new ParseException("Expected stream data token, found: " + token);
        }
        System.out.println(dict);

        if (dict.containsKey("Filter")) {
            Token filter = (Token) dict.get("Filter");
            if (filter.value().equals("FlateDecode")) {
                byte[] value = (byte[]) token.value();
                byte[] compressedData = Arrays.copyOf(value, value.length - 1);
                System.out.println(compressedData.length);

                try {
                    ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(compressedData);
                    InflaterInputStream inflaterInputStream = new InflaterInputStream(byteArrayInputStream);
                    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

                    byte[] buffer = new byte[1024];
                    int bytesRead;
                    while ((bytesRead = inflaterInputStream.read(buffer)) != -1) {
                        outputStream.write(buffer, 0, bytesRead);
                    }
                    inflaterInputStream.close();
                    dict.put("streamData", outputStream.toString());
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            } else {
                dict.put("streamData", token.value());
            }
        }

        return dict;
    }

    public XRefTable parseXRefTable() throws IOException, ParseException {
        Token token = tokenizer.nextToken();

        if (token.type() != TokenType.OPERATOR) {
            throw new ParseException("Expected xref token, found: " + token);
        }

        token = tokenizer.peekToken();

        if (token.type() == TokenType.NUMBER) {
            Token startIndex = tokenizer.nextToken();
            Token totalObjects = tokenizer.peekToken();
            if (totalObjects != null && totalObjects.type() == TokenType.NUMBER) {
                totalObjects = tokenizer.nextToken();
            }
            token = tokenizer.peekToken();
            List<XRefSubsection> subsections = new ArrayList<>();
            while (!token.value().equals("trailer")) {
                if (token.type() == TokenType.NUMBER) {
                    Token firstNumber = tokenizer.nextToken();
                    Token secondToken = tokenizer.peekToken();
                    if (secondToken != null && secondToken.type() == TokenType.NUMBER) {
                        Token secondNumber = tokenizer.nextToken();
                        Token literal = tokenizer.peekToken();
                        if (literal != null && ("f".equals(literal.value()) || "n".equals(literal.value()))) {
                            literal = tokenizer.nextToken();
                            subsections.add(new XRefSubsection(
                                    Integer.parseInt(firstNumber.getStringValue()),
                                    Integer.parseInt(secondNumber.getStringValue()),
                                    !literal.value().equals("f"))
                            );
                            token = tokenizer.peekToken();
                        }
                    }
                }
            }
            if (token.type() != TokenType.OPERATOR) {
                throw new ParseException("Expected trailer token, found: " + token);
            }
            assert totalObjects != null;
            return new XRefTable(Integer.parseInt(totalObjects.getStringValue()), Integer.parseInt(startIndex.getStringValue()), subsections);
        }
        return null;
    }

    public Map<String, Object> parseTrailer() throws IOException, ParseException {
        tokenizer.nextToken();
        Map<String, Object> trailerDict = parseDictionary();
        Token token = tokenizer.nextToken();
        if (token.value().equals("startxref")) {
            token = tokenizer.nextToken();
            trailerDict.put("startxref", token.value());
        }
        return trailerDict;
    }
}
