package de.antl.parser.model;

import java.util.List;

public record XRefTable(
        int totalObjects,
        int startIndex,
        List<XRefSubsection> subsections
) {
}
