package com.btssio.applicationrftg.ui.theme;

public class Film {
    private int id;
    private String title;
    private int releaseYear;
    private String description;

    private int inventoryId;

    // Constructeur avec ID
    public Film(int id, String title, int releaseYear, String description, int inventoryId) {
        this.id = id;
        this.title = title;
        this.releaseYear = releaseYear;
        this.description = description;
        this.inventoryId = inventoryId;
    }

    public Film(int filmId, String filmTitle, int releaseYear, int inventoryId) {
        this.id = filmId;
        this.title = filmTitle;
        this.releaseYear = releaseYear;
        this.description = ""; // Optionnel si tu veux éviter des valeurs null
        this.inventoryId = inventoryId;
    }

    public String getDescription() {
        return description;
    }
    

    // Constructeur sans ID
    public Film(String title, int releaseYear) {
        this.title = title;
        this.releaseYear = releaseYear;
    }

    // Getters et Setters
    public int getInventoryId() {
        return inventoryId;
    }

    public void setInventoryId(int inventoryId) {
        this.inventoryId = inventoryId;
    }

    // Getters et setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public int getReleaseYear() { return releaseYear; } // Correction : retourne un int
    public void setReleaseYear(int releaseYear) { this.releaseYear = releaseYear; } // Correction : accepte un int
}
