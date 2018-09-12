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

@Path("/")
public class Distanbol {
    private static final Logger logger = Logger.getLogger(Distanbol.class);

    //default
    public Double CONFIDENCE_THRESHOLD = 0.7;
    private ObjectMapper mapper = new ObjectMapper();

    //todo put examples showing the difference of different confidence levels

    @Context
    ServletContext servletContext;

    @GET
    @Path("/view/{filepath}")
    public Response handleView(@PathParam("filepath") String filePath) {


        try {
            if (!filePath.endsWith(".css") && !filePath.endsWith(".js")) {
                return Response.status(404).entity("Requested file doesn't exist.").build();
            }

            String file = null;
            if (filePath.endsWith(".js")) {
                file = FileReader.readFile(this.servletContext.getRealPath("/WEB-INF/classes/view/javascript/" + filePath));
                return Response.status(200).entity(file).type("application/javascript").build();
            } else if (filePath.endsWith(".css")){
                file = FileReader.readFile(this.servletContext.getRealPath("/WEB-INF/classes/view/css/" + filePath));
                return Response.status(200).entity(file).type("text/css").build();
            }else {
                return Response.status(404).entity("Requested file doesn't exist.").build();
            }

        } catch (IOException e) {
            logger.error(e.getMessage());
            return Response.status(404).entity("Requested file doesn't exist.").build();
        }

    }

    @GET
    @Path("/")
    public Response convert(@QueryParam("URL") String URL, @QueryParam("confidence") String confidence) {

        if (URL == null) {
            try {
                String html = FileReader.readFile(this.servletContext.getRealPath("/WEB-INF/classes/view/html/index.html"));
                return Response.status(200).entity(html).type("text/html").build();
            } catch (IOException e) {
                logger.error("Can't read index html file");
                return Response.serverError().build();
            }
        }

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
                //it means an empty string is provided, do nothing and leave the default value
            }

        }
        if (CONFIDENCE_THRESHOLD < 0.0 || CONFIDENCE_THRESHOLD > 1.0) {
            return Response.status(400).entity("Confidence(double) must be between 0 and 1").build();
        }


        String json = response.readEntity(String.class);
        return processStanbolJSONtoHTML(URL, CONFIDENCE_THRESHOLD, json);

    }

    private Response processStanbolJSONtoHTML(String URL, Double confidence, String json) {

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


        if (jsonNode.isArray()) {
            Iterator<JsonNode> iterator = jsonNode.elements();

            Element viewablesHTML = doc.getElementById("viewables");

            Element script = doc.getElementById("script");

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


                StringBuilder entitiesTableSb = new StringBuilder();

                appendTableInit(entitiesTableSb);

                boolean firstElement = true;

                for (Viewable viewable : finalViewables) {

                    //to have a small space between elements
                    if (firstElement) {
                        firstElement = false;
                    } else {
                        viewablesHTML.append("<hr>");
                    }


                    String templateIDText = "<div><A name='%s'><b>%s</b>%s</A></div>";
                    String id = viewable.getId();
                    if (id != null) {
                        String idHTML = String.format(templateIDText, id, "Id:", id);
                        viewablesHTML.append(idHTML);
                    }


                    String templateText = "<div><b>%s</b>%s</div>";

                    String label = viewable.getLabel();
                    if (label != null) {
                        String labelHTML = String.format(templateText, "Label:", label);
                        viewablesHTML.append(labelHTML);
                    }

                    String comment = viewable.getComment();
                    if (comment != null) {
                        String commentHTML = String.format(templateText, "Comment:", comment);
                        viewablesHTML.append(commentHTML);
                    }

                    Double entityConfidence = viewable.getEntityEnhancement().getConfidence();
                    if (entityConfidence != null) {
                        String confidenceHTML = String.format(templateText, "Confidence:", entityConfidence);
                        viewablesHTML.append(confidenceHTML);
                    }

                    String context = viewable.getTextEnhancements().get(0).getContext();
                    if (context != null) {
                        String contextHTML = String.format(templateText, "Context:", context);
                        viewablesHTML.append(contextHTML);
                    }

                    String typesHTML;
                    ArrayList<String> types = viewable.getTypes();
                    if ((types != null) && (!types.isEmpty())) {

                        StringBuilder sb = new StringBuilder();
                        for (String type : types) {
                            sb.append("<li><a href='").append(type).append("'>").append(type).append("<a></li>");
                        }

                        typesHTML = "<ul>" + sb.toString() + "</ul>";

                        viewablesHTML.append("<div><b>Types:</b>" + typesHTML + "</div");

                    } else {
                        typesHTML = "This entity has no known types.";
                    }

                    if (viewable.getDepictionThumbnail() != null) {
                        String depictionFormat = "<div><b>depiction(<a href='%s'>full image<a>):</b><div><img src='%s'></img></div></div>";
                        String depictionThumbnail = String.format(depictionFormat, viewable.getDepiction(), viewable.getDepictionThumbnail());
                        viewablesHTML.append(depictionThumbnail);
                    }


//                    //show thumbnail on the page and link to full image
//                    String depictionThumbnail;
//                    if (viewable.getDepictionThumbnail() != null) {
//                        if (viewable.getDepiction() != null) {
//                            String depictionFormat = "<div><b>depiction(<a href='%s'>full image<a>):</b><div><img src='%s'></img></div></div>";
//                            depictionThumbnail = String.format(depictionFormat, viewable.getDepiction(), viewable.getDepictionThumbnail());
//                        } else {
//                            String depictionFormat = "<div><b>depiction:</b><div><img src='%s'></img></div></div>";
//                            depictionThumbnail = String.format(depictionFormat, viewable.getDepictionThumbnail());
//                        }
//
//                    } else {
//                        if (viewable.getDepiction() != null) {
//                            depictionThumbnail = "No thumbnail available: <a href='" + viewable.getDepiction() + "'>Full Image</a>";
//                        } else {
//                            depictionThumbnail = "No thumbnail available";
//                        }
//
//                    }
//                    viewablesHTML.append(depictionThumbnail);


                    if ((viewable.getLatitude() != null) && (!viewable.getLatitude().equals("")) && (viewable.getLongitude() != null) && (!viewable.getLongitude().equals(""))) {
                        script.appendText("addMarker(" + viewable.getLongitude() + "," + viewable.getLatitude() + ");");
                    }

                    appendTableElement(entitiesTableSb, id, label, entityConfidence, context, typesHTML);

                }

                Element rawJsonHTML = doc.getElementById("rawJson");
                rawJsonHTML.append("Stanbol JSON input: <a href=\"" + URL + "\">" + URL + "</a>");

                //start of the page:
                //1)stanbol json output(our input) link
                //2)overview table
                //3)each element in a list
                appendTableEnd(entitiesTableSb);
                Element formHTML = doc.getElementById("form");
                formHTML.append(entitiesTableSb.toString());

                return Response.accepted().entity(doc.html()).type("text/html").build();
            }


        } else {
            return Response.status(400).entity("The given Stanbol output is not valid.").build();
        }

    }

    private void appendTableInit(StringBuilder entitiesTableSb) {

        entitiesTableSb.append("<table>");
        entitiesTableSb.append("<thead>");
        entitiesTableSb.append("<tr>");

        entitiesTableSb.append("<th>Name</th>");
        entitiesTableSb.append("<th>Confidence</th>");
        entitiesTableSb.append("<th>Context</th>");
        entitiesTableSb.append("<th>Types</th>");

        entitiesTableSb.append("</tr>");

        entitiesTableSb.append("</thead>");
        entitiesTableSb.append("<tbody>");
    }

    private void appendTableElement(StringBuilder entitiesTableSb, String id, String label, Double confidence, String context, String types) {
        entitiesTableSb.append("<tr>");

        //name
        entitiesTableSb.append("<td><a href='#").append(id).append("'>").append(label).append("</a></td>");
        //confidence
        entitiesTableSb.append("<td>").append(confidence).append("</td>");
        //context
        entitiesTableSb.append("<td>").append(context).append("</td>");
        //types
        entitiesTableSb.append("<td>").append(types).append("</td>");


        entitiesTableSb.append("</tr>");

    }

    private void appendTableEnd(StringBuilder entitiesTableSb) {
        entitiesTableSb.append("</tbody>");
        entitiesTableSb.append("</table>");

        entitiesTableSb.insert(0, "<br><div><h2>Entities List:</h2>");
        entitiesTableSb.append("</div>");
    }
}
