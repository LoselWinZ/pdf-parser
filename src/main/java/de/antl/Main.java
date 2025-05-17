package de.antl;

import de.antl.parser.enums.TokenType;
import de.antl.parser.exceptions.ParseException;
import de.antl.parser.model.PDFIndirectObject;
import de.antl.parser.model.Token;
import de.antl.parser.model.XRefTable;
import de.antl.parser.service.PDFParser;
import de.antl.parser.service.PDFTokenizer;

import java.io.FileReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class Main {
    public static void main(String[] args) throws IOException {
        if (args.length < 1) {
            System.out.println("Usage: PDFParserApp <pdf-file-path>");
            return;
        }

        String pdfFilePath = args[0];
        List<PDFIndirectObject> objects = new ArrayList<>();
        XRefTable xrefTable = null;
        Map<String, Object> trailer = null;
        System.out.println(new String(Files.readAllBytes(Path.of(pdfFilePath))));

        try (FileReader fileReader = new FileReader(pdfFilePath, StandardCharsets.ISO_8859_1)) {
            // Initialize the tokenizer with the file reader
            PDFTokenizer tokenizer = new PDFTokenizer(fileReader);
            // Create the parser using the tokenizer
            PDFParser parser = new PDFParser(tokenizer);

            // Loop through the PDF file to parse its components
            while (true) {
                Token token = tokenizer.peekToken();
                if (token == null) {
                    // End of file reached
                    break;
                }

                // Check for PDF header, e.g. "%PDF-1.7"
                if (token.getStringValue().startsWith("%PDF-")) {
                    System.out.println("PDF Header: " + token.value());
                    tokenizer.nextToken(); // Consume the header token
                    continue;
                }

                if ("xref".equals(token.value())) {
                    xrefTable = parser.parseXRefTable();
                    continue;
                }

                // Check for the trailer marker "trailer"
                if ("trailer".equals(token.value())) {
                    trailer = parser.parseTrailer();
                    continue;
                }

                if (token.type() == TokenType.NUMBER) {
                    try {
                        PDFIndirectObject indirectObject = parser.parseIndirectObject();
                        objects.add(indirectObject);
                    } catch (ParseException e) {
                        System.err.println("Error parsing indirect object: " + e);
                        break;
                    }
                    continue;
                }

                // If none of the above, consume the token and continue.
                tokenizer.nextToken();
            }
        } catch (IOException | ParseException e) {
            throw new RuntimeException(e);
        }

        System.out.println("Parsed " + objects.size() + " objects");
        for (PDFIndirectObject object : objects) {
            System.out.println(object);
        }

        if (xrefTable != null) {
            System.out.println("Parsed Cross-Reference Table:");
            System.out.println(xrefTable);
        }

        if (trailer != null) {
            System.out.println("Parsed Trailer:");
            System.out.println(trailer);
        }
    }
}