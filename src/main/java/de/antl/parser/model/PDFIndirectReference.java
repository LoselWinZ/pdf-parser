package de.antl.parser.model;

public record PDFIndirectReference(
        int objectNumber,
        int genNumber
) {
    @Override
    public String toString() {
        return "PDFIndirectReference{" +
                "objectNumber=" + objectNumber +
                ", genNumber=" + genNumber +
                '}';
    }
}
