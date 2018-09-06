package at.ac.oeaw.routes;

import at.ac.oeaw.elements.enhancements.EntityEnhancement;
import at.ac.oeaw.elements.Viewable;
import at.ac.oeaw.elements.enhancements.TextEnhancement;
import at.ac.oeaw.helpers.FileReader;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import org.apache.commons.validator.routines.UrlValidator;
import org.apache.log4j.Logger;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import javax.servlet.ServletContext;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.QueryParam;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Invocation;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;

@Path("/")
public class Distanbol {
    private static final Logger logger = Logger.getLogger(Distanbol.class);

    public final double CONFIDENCE_THRESHOLD = 0.7;

    @Context
    ServletContext servletContext;

    @GET
    @Path("/")
    public Response convert(@QueryParam("URL") String URL) {

        if (URL == null) {
            try {
                String html = FileReader.readFile(this.servletContext.getRealPath("/WEB-INF/classes/view/index.html"));
                return Response.status(200).entity(html).type("text/html").build();
            } catch (IOException e) {
                logger.error("Can't read index html file");
                return Response.serverError().build();
            }
        }

        String[] schemes = {"http", "https"};
        UrlValidator urlValidator = new UrlValidator(schemes, 8L);

        String input = URL;
        if ((!URL.startsWith("http://")) && (!URL.startsWith("https://"))) {
            URL = "http://" + URL;
        }

        if (!urlValidator.isValid(URL)) {
            return Response.status(400).entity("The given URL: '" + input + "' is not valid.").build();
        }

        Client client = ClientBuilder.newClient();
        WebTarget webTarget = client.target(URL);

        Invocation.Builder invocationBuilder = webTarget.request("application/json");
        Response response = invocationBuilder.get();

        if (response.getStatus() == 302) {
            URL = response.getHeaderString("Location");
            if (!urlValidator.isValid(URL)) {
                return Response.status(400).entity("The given URL: '" + input + "' is not valid.").build();
            }
            webTarget = client.target(URL);

            invocationBuilder = webTarget.request("application/json");
            response = invocationBuilder.get();
        }
        if (response.getStatus() == 302) {
            return Response.status(400).entity("Distanbol only allows one redirect when accessing the given URL input").build();
        }

        String contentType = response.getHeaderString("Content-Type");
        if ((!contentType.equals("application/json")) && (!contentType.equals("application/ld+json"))) {
            return Response.status(400).entity("The given URL: '" + input + "' doesn't point to a json or jsonld file.").build();
        }

        String json = response.readEntity(String.class);
        try {
            String html = FileReader.readFile(this.servletContext.getRealPath("/WEB-INF/classes/view/view.html"));
            Document doc = Jsoup.parse(html);

            ObjectMapper mapper = new ObjectMapper();

            JsonNode jsonNode = mapper.readTree(json);

            if (jsonNode.isArray()) {
                Iterator<JsonNode> iterator = jsonNode.elements();

                Element rawJsonHTML = doc.getElementById("rawJson");

                rawJsonHTML.append("Stanbol JSON input: <a href=\"" + URL + "\">" + URL + "</a>");

                Element viewablesHTML = doc.getElementById("viewables");

                StringBuilder entitiesOverviewSB = new StringBuilder();

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
                            if (typesNode.get(1).asText().equals("http://fise.iks-project.eu/ontology/TextAnnotation")) {
                                TextEnhancement textEnhancement = new TextEnhancement(node);
                                textEnhancements.add(textEnhancement);
                            } else if (typesNode.get(1).asText().equals("http://fise.iks-project.eu/ontology/EntityAnnotation")) {
                                EntityEnhancement entityEnhancement = new EntityEnhancement(node);
                                //only take entity enhancements that are over the threshold,
                                if (entityEnhancement.getConfidence() > CONFIDENCE_THRESHOLD) {
                                    entityEnhancements.add(entityEnhancement);
                                }
                            } else {
                                //return 400 todo
                            }

                        } else {
                            Viewable viewable = new Viewable(node);
                            viewables.add(viewable);
                        }

                    } else {
                        //return 400 todo
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
                    //todo return 200 or 400 saying "no entities were found with higher than threshold confidence level"
                } else {


                    //todo structure this so it doesnt come up in the middle of everything like right now
                    //this is for the first list with anchors
                    entitiesOverviewSB.append("<table>");
                    entitiesOverviewSB.append("<thead>");
                    entitiesOverviewSB.append("<tr>");

                    entitiesOverviewSB.append("<th>Name</th>");
                    entitiesOverviewSB.append("<th>Confidence</th>");
                    entitiesOverviewSB.append("<th>Context</th>");
                    entitiesOverviewSB.append("<th>Types</th>");

                    entitiesOverviewSB.append("</tr>");

                    entitiesOverviewSB.append("</thead>");
                    entitiesOverviewSB.append("<tbody>");


                    for (Viewable viewable : finalViewables) {

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


                        //todo structure this so it doesnt come up in the middle of everything like right now
                        entitiesOverviewSB.append("<tr>");

                        //name
                        entitiesOverviewSB.append("<td><a href='#").append(id).append("'>").append(label).append("</a></td>");
                        //confidence
                        entitiesOverviewSB.append("<td>").append(viewable.getEntityEnhancement().getConfidence()).append("</td>");
                        //context
                        entitiesOverviewSB.append("<td>").append(viewable.getTextEnhancements().get(0).getContext()).append("</td>");
                        //types
                        //types are in the following loop

                        ArrayList<String> types = viewable.getTypes();
                        if ((types != null) && (!types.isEmpty())) {

                            StringBuilder sb = new StringBuilder();
                            for (String type : types) {
                                //<a href='" + sb.toString() + "'>"+sb.toString()+"<a>
                                sb.append("<li><a href='").append(type).append("'>").append(type).append("<a></li>");
                            }

                            String typesHTML = "<ul>" + sb.toString() + "</ul>";

                            viewablesHTML.append("<div><b>Types:</b>" + typesHTML + "</div");
                            entitiesOverviewSB.append("<td>").append(typesHTML).append("</td>");

                        } else {
                            entitiesOverviewSB.append("This entity has no known types.");
                        }

                        entitiesOverviewSB.append("</tr>");


                        if (viewable.getDepiction() != null) {
                            String depiction = String.format("<div><b>depiction:</b><div><img src=\"%s\"></img></div></div>", viewable.getDepiction());
                            viewablesHTML.append(depiction);
                        }

                        if ((viewable.getLatitude() != null) && (!viewable.getLatitude().equals("")) && (viewable.getLongitude() != null) && (!viewable.getLongitude().equals(""))) {
                            script.appendText("addMarker(" + viewable.getLongitude() + "," + viewable.getLatitude() + ");");
                        }
                        viewablesHTML.append("<hr>");
                    }

                    entitiesOverviewSB.append("</tbody>");

                    entitiesOverviewSB.append("</table>");
                }


                //this is the overview in the beginning of the page
                String entitiesListHTML = "<br><div><h2>Entities List:</h2><ul>" + entitiesOverviewSB + "</ul></div>";
                rawJsonHTML.append(entitiesListHTML);

                //this is to remove the last <hr> element
                viewablesHTML.children().last().remove();//todo maybe put in front of loop with boolean or something

                return Response.accepted().entity(doc.html()).type("text/html").build();
            } else {
                return Response.status(400).entity("The given Stanbol output is not valid.").build();
            }


        } catch (IOException e) {
            logger.error("Can't read input json file: " + e.getMessage());
        }

        return Response.serverError().entity("Can't read input json file.").build();
    }
}
