package de.antl.parser.model;

public record XRefSubsection(
        int byteOffset,
        int genNumber,
        boolean isInUse
) {
}
