package at.ac.oeaw.routes;

import at.ac.oeaw.elements.enhancements.EntityEnhancement;
import at.ac.oeaw.elements.Viewable;
import at.ac.oeaw.elements.enhancements.TextEnhancement;
import at.ac.oeaw.helpers.FileReader;
import at.ac.oeaw.helpers.RequestHandler;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import org.apache.log4j.Logger;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import javax.servlet.ServletContext;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;

@Path("/convert")
public class Convert {
    private static final Logger logger = Logger.getLogger(Convert.class);

    //default
    private Double CONFIDENCE_THRESHOLD = 0.7;
    private ObjectMapper mapper = new ObjectMapper();

    @Context
    ServletContext servletContext;


    @GET
    @Path("/")
    public Response convertJson(@QueryParam("URL") String URL, @QueryParam("confidence") String confidence) {

        //todo determine if its a text file or if its a json
        //todo if its a text file, send to stanbol

        //todo add fulltext

        Response response;
        try {
            response = RequestHandler.getJSON(URL);
        } catch (BadRequestException e) {
            return Response.status(400).entity(e.getMessage()).build();
        } catch (ProcessingException e) {
            return Response.status(504).entity("The request to the URL provided exceeded the timout: " + RequestHandler.TIMEOUT).build();
        }

        if (confidence != null) {
            try {
                CONFIDENCE_THRESHOLD = Double.parseDouble(confidence);
            } catch (NumberFormatException e) {
                //it means probably an empty string is provided, do nothing and leave the default value
            }

        }
        if (CONFIDENCE_THRESHOLD < 0.0 || CONFIDENCE_THRESHOLD > 1.0) {
            return Response.status(400).entity("Confidence(double) must be between 0 and 1").build();
        }

        String json = response.readEntity(String.class);
        return processStanbolJSONtoHTML(URL, json);

    }

    private Response processStanbolJSONtoHTML(String URL, String json) {

        Document doc;
        try {
            String html = FileReader.readFile(this.servletContext.getRealPath("/WEB-INF/classes/view/html/view.html"));
            doc = Jsoup.parse(html);
        } catch (IOException e) {
            return Response.serverError().entity("Something went wrong.").build();
        }


        JsonNode jsonNode;
        try {
            jsonNode = mapper.readTree(json);
        } catch (IOException e) {
            return Response.status(400).entity("Given json file is not valid.").build();
        }

        Element urlInput = doc.getElementById("URLInput");
        urlInput.attr("value", URL);

        Element confidenceInput = doc.getElementById("confidenceInput");
        confidenceInput.attr("value", String.valueOf(CONFIDENCE_THRESHOLD));

        Element rawJsonHTML = doc.getElementById("rawJson");
        rawJsonHTML.append("Stanbol JSON input: <a href=\"" + URL + "\">" + URL + "</a>");

        if (jsonNode.isArray()) {
            Iterator<JsonNode> iterator = jsonNode.elements();

            Element viewablesHTML = doc.getElementById("viewables");

            ArrayList<Viewable> viewables = new ArrayList<>();
            ArrayList<EntityEnhancement> entityEnhancements = new ArrayList<>();
            ArrayList<TextEnhancement> textEnhancements = new ArrayList<>();

            //create an object for each node and save them into respective lists
            while (iterator.hasNext()) {
                JsonNode node = iterator.next();

                //there are three types of nodes: viewables, entity enhancements and text enhancements.
                //Viewables are entities to display.
                //Entity enhancements contain confidence information
                //Text enhancements contain context information.

                ArrayNode typesNode = (ArrayNode) node.get("@type");
                if (typesNode != null) {
                    if (typesNode.size() == 2 && typesNode.get(0).asText().equals("http://fise.iks-project.eu/ontology/Enhancement")) {

                        switch (typesNode.get(1).asText()) {
                            case "http://fise.iks-project.eu/ontology/TextAnnotation":
                                TextEnhancement textEnhancement = new TextEnhancement(node);
                                textEnhancements.add(textEnhancement);
                                break;
                            case "http://fise.iks-project.eu/ontology/EntityAnnotation":
                                EntityEnhancement entityEnhancement = new EntityEnhancement(node);
                                //only take entity enhancements that are over the threshold,
                                if (entityEnhancement.getConfidence() >= CONFIDENCE_THRESHOLD) {
                                    entityEnhancements.add(entityEnhancement);
                                }
                                break;
                            default:
                                return Response.status(400).entity("The given Stanbol output is not valid.").build();
                        }

                    } else {
                        Viewable viewable = new Viewable(node);
                        viewables.add(viewable);
                    }

                } else {
                    //node has no type, it means it is a viewable
                    Viewable viewable = new Viewable(node);
                    viewables.add(viewable);
                }

            }


            ArrayList<Viewable> finalViewables = new ArrayList<>();

            //Each viewable has exactly one matching entityEnhancement.
            //Each entityEnhancement can have one or more textEnhancements.

            //entityEnhancements list is already filtered above to take confidence higher than threshold
            for (Viewable viewable : viewables) {

                for (EntityEnhancement entityEnhancement : entityEnhancements) {

                    if (viewable.getId().equals(entityEnhancement.getReference())) {

                        viewable.setEntityEnhancement(entityEnhancement);
                        for (TextEnhancement textEnhancement : textEnhancements) {

                            if (entityEnhancement.getRelations().contains(textEnhancement.getId())) {
                                viewable.addTextEnhancement(textEnhancement);
                            }
                        }

                        finalViewables.add(viewable);

                    }

                }
            }

            if (finalViewables.size() == 0) {
                return Response.status(400).entity("There are no entities above the given threshold: " + CONFIDENCE_THRESHOLD).build();
            } else {
                Element formHTML = doc.getElementById("tableBody");
                boolean firstElement = true;

                for (Viewable viewable : finalViewables) {

                    //to have a small space between elements
                    if (firstElement) {
                        firstElement = false;
                    } else {
                        viewablesHTML.append("<hr>");
                    }

                    try {
                        viewablesHTML.append(viewable.getHTMLDepiction(this.servletContext.getRealPath("/WEB-INF/classes/view/html/viewable/Viewable.html")));


                        formHTML.append(viewable.getHTMLTableRowDepiction());
                    } catch (IOException e) {
                        return Response.serverError().entity("Something went wrong.").build();
                    }

                }

                return Response.accepted().entity(doc.html()).type("text/html").build();
            }


        } else {
            return Response.status(400).entity("The given Stanbol output is not valid.").build();
        }

    }


}
