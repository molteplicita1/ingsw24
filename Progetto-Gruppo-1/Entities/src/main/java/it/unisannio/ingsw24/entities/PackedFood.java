package it.unisannio.ingsw24.entities;

import org.bson.Document;

import java.time.LocalDate;

public class PackedFood extends Food {

    private String brand;
    private String id;
    private String nutritionGrade;

    //private Image image;


    public PackedFood(String name, String id, LocalDate expirationDate, boolean isExpired, boolean isFridge, int quantity, String brand, String nutritionGrade) {
        super(name, expirationDate, isExpired, isFridge, quantity);
        this.id = id;
        this.brand = brand;
        this.nutritionGrade = nutritionGrade;
    }

    @Override
    public void setExpirationDate(String date){
        LocalDate ld = LocalDate.parse(date);
        this.expirationDate = ld;
    }

    public String getId(){
        return this.id;
    }

    public String getBrand(){
        return this.brand;
    }

    public String getNutritionGrade(){
        return this.nutritionGrade;
    }

    public String toString(){
        return super.toString() + "ID: " + this.id + " Brand: " + this.brand + " Nutrition Grade: " + this.nutritionGrade;
    }

    @Override
    public Document toDocument() {
        return super.toDocument()
                .append("id", id)
                .append("Brand", brand)
                .append("NutritionGrade", nutritionGrade);
    }
}
