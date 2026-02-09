package com.axiom.domain.model;

public class City {
    private String id;
    private String name;
    private String nationId;
    private int level; // 1..5
    private int population; // approximate
    private String centerChunk; // world:x:z
    private boolean hasHospital = false;
    private boolean hasSchool = false;
    private boolean hasUniversity = false;
    private double happiness = 50.0; // 0-100

    public City() {}
    public City(String id, String name, String nationId, String centerChunk) {
        this.id = id; this.name = name; this.nationId = nationId; this.centerChunk = centerChunk; this.level = 1; this.population = 10;
    }

    public String getId() { return id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getNationId() { return nationId; }
    public int getLevel() { return level; }
    public void setLevel(int level) { this.level = level; }
    public int getPopulation() { return population; }
    public void setPopulation(int population) { this.population = population; }
    public String getCenterChunk() { return centerChunk; }
    public boolean hasHospital() { return hasHospital; }
    public void setHasHospital(boolean v) { this.hasHospital = v; }
    public boolean hasSchool() { return hasSchool; }
    public void setHasSchool(boolean v) { this.hasSchool = v; }
    public boolean hasUniversity() { return hasUniversity; }
    public void setHasUniversity(boolean v) { this.hasUniversity = v; }
    public double getHappiness() { return happiness; }
    public void setHappiness(double v) { this.happiness = Math.max(0, Math.min(100, v)); }
    public void setNationId(String v) { this.nationId = v; }
}


