package at.ac.oeaw.elements;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;

public class Enhancement {

    private String reference;
    private double confidence;

    public Enhancement(String reference, double confidence) {
        this.reference = reference;
        this.confidence = confidence;
    }

    public Enhancement(JsonNode node){
        String reference;
        double confidence = 0.0;

        ArrayNode referenceNode = (ArrayNode) node.get("http://fise.iks-project.eu/ontology/entity-reference");
        if(referenceNode!=null){
            reference = referenceNode.get(0).get("@id").asText();

            ArrayNode confidenceNode = (ArrayNode) node.get("http://fise.iks-project.eu/ontology/confidence");
            if(confidenceNode!=null){
                confidence = confidenceNode.get(0).get("@value").asDouble();
            }

        }else{
            reference=null;
        }

        this.reference=reference;
        this.confidence=confidence;





    }

    public double getConfidence() {
        return confidence;
    }

    public void setConfidence(double confidence) {
        this.confidence = confidence;
    }

    public String getReference() {
        return reference;
    }

    public void setReference(String reference) {
        this.reference = reference;
    }
}
