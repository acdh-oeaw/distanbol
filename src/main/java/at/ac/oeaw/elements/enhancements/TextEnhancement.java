package at.ac.oeaw.elements.enhancements;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;

public class TextEnhancement {

    private String id;
    private String context;

    public TextEnhancement(JsonNode node) {
        String id = node.get("@id").asText();

        String context;
        ArrayNode contextNode = (ArrayNode) node.get("http://fise.iks-project.eu/ontology/selection-context");
        if (contextNode != null) {
            context = contextNode.get(0).get("@value").asText();
        } else {
            context = null;
        }

        this.id = id;
        this.context = context;
    }

    public String getContext() {
        return context;
    }

    public void setContext(String context) {
        this.context = context;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }
}
