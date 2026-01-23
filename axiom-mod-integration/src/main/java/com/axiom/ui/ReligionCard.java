package com.axiom.ui;

import java.util.Collections;
import java.util.List;

public class ReligionCard {
    private String id;
    private String name;
    private String tagline;
    private String symbol;
    private String color;
    private List<String> details;

    public String getId() {
        return id == null ? "" : id;
    }

    public String getName() {
        return name == null ? "" : name;
    }

    public String getTagline() {
        return tagline == null ? "" : tagline;
    }

    public String getSymbol() {
        return symbol == null ? "" : symbol;
    }

    public String getColor() {
        return color == null ? "" : color;
    }

    public List<String> getDetails() {
        return details == null ? Collections.emptyList() : details;
    }
}
