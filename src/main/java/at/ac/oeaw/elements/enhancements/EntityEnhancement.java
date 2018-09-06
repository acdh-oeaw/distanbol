package at.ac.oeaw.elements.enhancements;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;

import java.util.ArrayList;
import java.util.List;

public class EntityEnhancement {

    private String reference;
    private Double confidence;
    private List<String> relations = new ArrayList<>();

    public EntityEnhancement(JsonNode node) {
        String reference;
        Double confidence = 0.0;

        ArrayNode referenceNode = (ArrayNode) node.get("http://fise.iks-project.eu/ontology/entity-reference");
        if (referenceNode != null) {
            reference = referenceNode.get(0).get("@id").asText();

        } else {
            reference = null;
        }

        ArrayNode confidenceNode = (ArrayNode) node.get("http://fise.iks-project.eu/ontology/confidence");
        if (confidenceNode != null) {
            confidence = confidenceNode.get(0).get("@value").asDouble();
        }else {
            confidence=0.0;
        }


        ArrayNode relationNode = (ArrayNode) node.get("http://purl.org/dc/terms/relation");
        for (JsonNode relation : relationNode) {
            relations.add(relation.get("@id").asText());
        }


        this.reference = reference;
        this.confidence = confidence;


    }

    public Double getConfidence() {
        return confidence;
    }

    public void setConfidence(Double confidence) {
        this.confidence = confidence;
    }

    public String getReference() {
        return reference;
    }

    public void setReference(String reference) {
        this.reference = reference;
    }

    public List<String> getRelations() {
        return relations;
    }
}
