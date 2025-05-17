package de.antl.parser.model;

public record PDFIndirectObject(
        int objectNumber,
        int genNumber,
        Object value
) {
    @Override
    public String toString() {
        return "PDFIndirectObject{" +
                "objectNumber=" + objectNumber +
                ", genNumber=" + genNumber +
                ", value=" + value +
                '}';
    }
}
