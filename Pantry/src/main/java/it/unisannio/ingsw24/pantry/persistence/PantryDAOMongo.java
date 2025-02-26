package it.unisannio.ingsw24.pantry.persistence;

import com.mongodb.DuplicateKeyException;
import com.mongodb.MongoException;
import com.mongodb.MongoWriteException;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.IndexOptions;
import com.mongodb.client.model.Indexes;

import com.mongodb.client.result.UpdateResult;
import it.unisannio.ingsw24.entities.*;
import it.unisannio.ingsw24.pantry.exception.GuestException;
import org.bson.Document;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import javax.mail.*;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

import static com.mongodb.client.model.Filters.*;
import static com.mongodb.client.model.Updates.*;

public class PantryDAOMongo implements PantryDAO {

    private static String host = System.getenv("MONGO_ADDRESS");
    private static String port = System.getenv("MONGO_PORT");

    private final MongoClient mongoClient;
    private final MongoDatabase mongoDatabase;
    private final MongoCollection<Document> collection;

    /**
     * Default constructor that creates a connection to the MongoDB database
     */
    public PantryDAOMongo(){
        if (host == null){
            host = "172.31.6.1";
        }
        if (port == null){
            port = "27017";
        }

        String URI = "mongodb://" + host + ":" + port;
        this.mongoClient = MongoClients.create(URI);
        this.mongoDatabase = mongoClient.getDatabase(DATABASE_NAME);
        this.collection = mongoDatabase.getCollection(COLLECTION);
        this.createDB();
    }

    @Override
    public boolean dropDB() {
        mongoDatabase.drop();
        return true;
    }

    @Override
    public boolean createDB() {
        try{
            IndexOptions indexOptions = new IndexOptions();
            String resultCreateIndex = this.collection.createIndex(Indexes.ascending(PANTRY_ID), indexOptions);
        }
        catch (DuplicateKeyException e){
            System.out.printf("Duplicate field values encountered, couldn't create index: \t%s\n",e);
            return false;
        }
        return true;
    }

    /**
     * This method converts a Document object to a Pantry object
     * @param d the Document object to convert
     * @return the Pantry object
     */
    private Pantry pantryFromDocument(Document d){
        return new Pantry(d.getInteger(PANTRY_ID),
                d.getString(OWNER_USERNAME),
                (List<Food>) d.get(FOODS),
                (List<String>) d.get(GUESTS) );
    }

    @Override
    public Pantry getPantry(int pantryId){
        List<Pantry> pantries = new ArrayList<>();
        for(Document current : this.collection.find(eq(PANTRY_ID, pantryId))){
            Pantry p = pantryFromDocument(current);
            pantries.add(p);
        }
        if (pantries.isEmpty()) return null;

        assert pantries.size() == 1;
        return pantries.get(0);
    }

    @Override
    public boolean closeConnection() {
        mongoClient.close();
        return true;
    }

    @Override
    public int getNextId(){
        Document result = collection.find().sort(new Document(PANTRY_ID, -1)).first();
        if (result == null) return 1;
        else return result.getInteger(PANTRY_ID) + 1;
    }

    /**
     * This method converts a Pantry object to a Document object
     * @param pantry the Pantry object to convert
     * @return the Document object
     */
    private Document pantryToDocument(Pantry pantry) {
        return new Document(PANTRY_ID, getNextId())
                .append(OWNER_USERNAME, pantry.getOwnerUsername())
                .append(FOODS, new ArrayList<>())
                .append(GUESTS, new ArrayList<>());
    }

    @Override
    public int createPantry(Pantry p) {
        try {
            Document pantryDocument = pantryToDocument(p);
            this.collection.insertOne(pantryDocument);
            return pantryDocument.getInteger(PANTRY_ID);
        } catch (MongoWriteException e){
            e.printStackTrace();
        }
        return 0;
    }

    @Override
    public List<Pantry> getPantries(String username) {

        ArrayList<Pantry> pentris = new ArrayList<>();

        for(Document d : this.collection.find(or(eq(OWNER_USERNAME, username),eq(GUESTS, username)))){
            Pantry p = pantryFromDocument(d);
            pentris.add(p);
        }

        // assert pentris.size() > 1;
        return pentris;
    }

    @Override
    public boolean updateFoods(int pantryId, Food f) {
        try {
            UpdateResult result = this.collection.updateOne(
                    new Document(PANTRY_ID, pantryId),
                    push(FOODS, f.toDocument())
            );

            // Controlla se è stato trovato e aggiornato un documento
            if (result.getMatchedCount() > 0) {
                return true;
            }
            else {
                return false;
            }

        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    @Override
    public boolean updateGuests(int pantryId, String username) {
        try{
            Pantry pantry = getPantry(pantryId);
            if (!username.equals(pantry.getOwnerUsername()) && !pantry.getGuestsUsernames().contains(username)) {
                UpdateResult result = this.collection.updateOne(new Document(PANTRY_ID, pantryId), push(GUESTS, username));
                if (result.getMatchedCount() > 0)
                    return true;
                else
                    return false;
            }
            else
                throw new GuestException("Guest: " + username + " already exists or is the owner of the pantry");
        }
        catch (Exception e){
            e.printStackTrace();
            return false;
        }
    }

    @Override
    public int checkAndSetIsExpiredFoods(){

        String expDate = null;
        int row = 0;

        for(Document current : this.collection.find()){
            List<Document> foods = (List<Document>) current.get(FOODS);
            for(Document food : foods) {
                expDate = food.getString("expirationDate");
                if (LocalDate.parse(expDate).isBefore(LocalDate.now())) {

                    String foodName = food.getString("name");
                    Document update = new Document("$set", new Document("Foods.$.isExpired", true));
                    this.collection.updateOne(and(eq(PANTRY_ID, current.getInteger(PANTRY_ID)), eq("Foods.name", foodName)), update);

                    row++;
                }
            }
        }

        return row;
    }

    @Override
    public void sendMail(){
        String sender = "sprinstrim@gmail.com";
        String host = "smtp.gmail.com";
        String subject = "Expired Foods";
        Properties p = new Properties();

        p.put("mail.smtp.host", host);
        p.put("port", 465);

        p.put("mail.smtp.auth", "true");
        p.put("mail.smtp.starttls.enable", "true");
        p.put("mail.smtp.ssl.enable", "true");
        p.put("mail.smtp.ssl.protocols", "TLSv1.2");

        Session s = Session.getInstance(p, new Authenticator() {
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(sender, "hhfwggnslkqljgli");
            }
        });

        for(Document current : this.collection.find()){
            String message = "These foods are expired:\n";
            List<String> expiredFoods = new ArrayList<>();
            String receiver = current.getString(OWNER_USERNAME);
            List<Document> foods = (List<Document>) current.get(FOODS);
            for(Document food : foods) {
                if (food.getBoolean("isExpired")) {
                    String foodName = food.getString("name");
                    int quantity = food.getInteger("quantity");
                    expiredFoods.add(foodName);
                    message += "- " + foodName + " (Quantity: " + quantity + ")\n";
                }
            }
            if (expiredFoods.isEmpty())
                continue;

            try {
                MimeMessage mail = new MimeMessage(s);
                mail.setFrom(new InternetAddress(sender));
                mail.addRecipients(Message.RecipientType.TO, receiver);

                mail.setSubject(subject);
                mail.setText(message);

                Transport.send(mail);
                System.out.println("Sent");

            } catch (Exception e){
                e.printStackTrace();
            }
        }

    }

    @Override
    public List<Food> getFoods(int pantryId) {
        ArrayList<Food> foods = new ArrayList<>();

        for (Document current : this.collection.find(eq(PANTRY_ID, pantryId))) {
            List<Document> foodsDocument = (List<Document>) current.get(FOODS);
            for (Document food : foodsDocument) {
                if (food.containsKey("brand"))
                    foods.add(packedFoodFromDocument(food));
                if (food.containsKey("category"))
                    foods.add(unPackedFoodFromDocument(food));
            }
        }
        return foods;
    }

    @Override
    public Food getFoodByName(int pantryId, String name) {
        List<Food> foods = getFoods(pantryId);
        for(Food food : foods){
            if (food.getName().equalsIgnoreCase(name))
                return food;
        }
        return null;
    }

    @Override
    public List<Food> getExpiredFoods(int pantryId){
        List<Food> foods = getFoods(pantryId);
        ArrayList<Food> expiredFoods = new ArrayList<>();

        for(Food food : foods){
            if (food.getIsExpired())
                expiredFoods.add(food);
        }

        return expiredFoods;
    }

    /**
     * This method converts a Document object to a PackedFood object
     * @param document the Document object to convert
     * @return the PackedFood object
     */
    private PackedFood packedFoodFromDocument(Document document){
        return new PackedFood(document.getString("name"),
                document.getString("id"),
                LocalDate.parse(document.getString("expirationDate")),
                document.getBoolean("isExpired"),
                document.getBoolean("isFridge"),
                document.getInteger("quantity"),
                document.getString("brand"),
                document.getString("nutritionGrade"));
    }

    /**
     * This method converts a Document object to an UnPackedFood object
     * @param document the Document object to convert
     * @return the UnPackedFood object
     */
    private UnPackedFood unPackedFoodFromDocument(Document document){
        return new UnPackedFood(document.getString("name"),
                document.getInteger("id"),
                document.getBoolean("isExpired"),
                document.getBoolean("isFridge"),
                document.getInteger("quantity"),
                Category.valueOf(document.getString("category")),
                document.getString("averageExpirationDays"),
                LocalDate.parse(document.getString("expirationDate")));
    }

    @Override
    public boolean deleteFoodByName(int pantryId, String name) {

        try{
            UpdateResult result = this.collection.updateOne(
                    new Document(PANTRY_ID, pantryId),
                    pull(FOODS, new Document("name",name)));

            return result.getModifiedCount() > 0;
        }
        catch (Exception e){
            e.printStackTrace();
            return false;
        }
    }

    @Override
    public boolean deleteGuestByUsername(int pantryId, String username) {

        try{
            UpdateResult result =  this.collection.updateOne(new Document(PANTRY_ID, pantryId),  pull(GUESTS, username));
            return result.getModifiedCount() > 0;
        }
        catch (Exception e){
            e.printStackTrace();
            return false;
        }

    }

    @Override
    public boolean deletePantry(int id) {
        try {
            if (getPantry(id) != null) {
                collection.findOneAndDelete(eq(PANTRY_ID, id));
                return true;
            }
        } catch (MongoException e) {
            e.printStackTrace();
            return false;
        }
        return false;

    }

    @Override
    public boolean checkUsername(int pantryId, String username){
        Pantry pantry = getPantry(pantryId);
        return pantry.getOwnerUsername().equals(username) || pantry.getGuestsUsernames().contains(username);
    }

    @Override
    public boolean checkOwner(int pantryId, String ownerUsername){
        Pantry pantry = getPantry(pantryId);
        return pantry.getOwnerUsername().equals(ownerUsername);
    }
}