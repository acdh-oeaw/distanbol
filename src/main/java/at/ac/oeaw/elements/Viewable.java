package at.ac.oeaw.elements;

import at.ac.oeaw.elements.enhancements.EntityEnhancement;
import at.ac.oeaw.elements.enhancements.TextEnhancement;
import at.ac.oeaw.helpers.RequestHandler;
import com.fasterxml.jackson.databind.JsonNode;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class Viewable {
    private String id;
    private ArrayList<String> types;
    private String depiction;
    private String depictionThumbnail;
    private String comment;
    private String label;
    private String latitude;
    private String longitude;


    private EntityEnhancement entityEnhancement;
    private List<TextEnhancement> textEnhancements = new ArrayList<>();

    public Viewable(JsonNode node) {
        String id = node.get("@id").asText();

        ArrayList<String> types = new ArrayList<>();
        JsonNode typeArray = node.get("@type");
        if ((typeArray != null) && (typeArray.isArray())) {
            Iterator<JsonNode> iterator = typeArray.elements();
            while (iterator.hasNext()) {
                JsonNode type = iterator.next();
                types.add(type.asText());
            }
        }


        //there are always two links to the same image to wikimedia.
        //First one is full and second one is a thumbnail. So I take the second one.
        JsonNode depictionNode = node.get("http://xmlns.com/foaf/0.1/depiction");
        String depiction = depictionNode == null ? null : (depictionNode.get(0) == null ? null : depictionNode.get(0).get("@id").asText());
        String depictionThumbnail = depictionNode == null ? null : (depictionNode.get(1) == null ? null : depictionNode.get(1).get("@id").asText());

//        if (depiction != null && !RequestHandler.imageExists(depiction)) {
//            depiction = null;
//        }
//
//        if (depictionThumbnail != null && !RequestHandler.imageExists(depictionThumbnail)) {
//            depictionThumbnail = null;
//        }
        

        String longitude = null;
        JsonNode longitudeNode = node.get("http://www.w3.org/2003/01/geo/wgs84_pos#long");

        if (longitudeNode != null) {
            longitude = longitudeNode.get(0).get("@value").asText();

        }

        String latitude = null;
        JsonNode latitudeNode = node.get("http://www.w3.org/2003/01/geo/wgs84_pos#lat");
        if (latitudeNode != null) {
            latitude = latitudeNode.get(0).get("@value").asText();
        }

        String label = null;
        JsonNode labelArray = node.get("http://www.w3.org/2000/01/rdf-schema#label");
        if ((labelArray != null) && (labelArray.isArray())) {
            Iterator<JsonNode> iterator = labelArray.elements();
            while (iterator.hasNext()) {
                JsonNode labelPair = iterator.next();
                String language = labelPair.get("@language").asText();
                if (language.equals("en")) {
                    label = labelPair.get("@value").asText();
                }
            }
        }

        JsonNode commentNode = node.get("http://www.w3.org/2000/01/rdf-schema#comment");
        String comment = commentNode == null ? null : (commentNode.get(0).get("@value") == null ? null : commentNode.get(0).get("@value").asText());

        this.id = id;
        this.types = types;
        this.depiction = depiction;
        this.depictionThumbnail = depictionThumbnail;
        this.comment = comment;
        this.label = label;
        this.latitude = latitude;
        this.longitude = longitude;
    }

    public String getComment() {
        return this.comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public String getDepiction() {
        return this.depiction;
    }

    public void setDepiction(String depiction) {
        this.depiction = depiction;
    }

    public ArrayList<String> getTypes() {
        return this.types;
    }

    public void setTypes(ArrayList<String> types) {
        this.types = types;
    }

    public String getId() {
        return this.id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getLabel() {
        return this.label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public String getLatitude() {
        return this.latitude;
    }

    public void setLatitude(String latitude) {
        this.latitude = latitude;
    }

    public String getLongitude() {
        return this.longitude;
    }

    public void setLongitude(String longitude) {
        this.longitude = longitude;
    }

    public EntityEnhancement getEntityEnhancement() {
        return entityEnhancement;
    }

    public void setEntityEnhancement(EntityEnhancement entityEnhancement) {
        this.entityEnhancement = entityEnhancement;
    }

    public void addTextEnhancement(TextEnhancement textEnhancement) {
        textEnhancements.add(textEnhancement);
    }

    public List<TextEnhancement> getTextEnhancements() {
        return textEnhancements;
    }

    public String getDepictionThumbnail() {
        return depictionThumbnail;
    }

    public void setDepictionThumbnail(String depictionThumbnail) {
        this.depictionThumbnail = depictionThumbnail;
    }
}
