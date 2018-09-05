package at.ac.oeaw.routes;

import at.ac.oeaw.elements.Enhancement;
import at.ac.oeaw.elements.Viewable;
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
                ArrayList<Enhancement> enhancements = new ArrayList<>();
                while (iterator.hasNext()) {
                    JsonNode node = iterator.next();

                    //there are two types of nodes: viewables and enhancements. Viewables are entities to display, enhancements contain confidence information.
                    boolean viewableBool = true;

                    ArrayNode typesNode = (ArrayNode) node.get("@type");
                    if (typesNode != null) {
                        for (JsonNode typeNode : typesNode) {
                            if (typeNode.asText().equals("http://fise.iks-project.eu/ontology/Enhancement")) {
                                viewableBool = false;
                            }
                        }
                    }


                    if (viewableBool) {
                        Viewable viewable = new Viewable(node);
                        viewables.add(viewable);

                    } else {

                        Enhancement enhancement = new Enhancement(node);

                        if (enhancement.getConfidence() > 0.5) {
                            enhancements.add(enhancement);
                        }

                    }


                }

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

                for (Viewable viewable : viewables) {
                    for (Enhancement enhancement : enhancements) {

                        if (viewable.getId().equals(enhancement.getReference())) {


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
                            entitiesOverviewSB.append("<td><a href='#" + id + "'>").append(label).append("</a></td>");
                            //confidence
                            entitiesOverviewSB.append("<td>").append(enhancement.getConfidence()).append("</td>");
                            //context
                            entitiesOverviewSB.append("<td>").append("TODO").append("</td>");
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

                                viewablesHTML.append("<div><b>Types:</b>"+typesHTML+"</div");
                                entitiesOverviewSB.append("<td>").append(typesHTML).append("</td>");

                            }else{
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

                    }

                }

                entitiesOverviewSB.append("</tbody>");

                entitiesOverviewSB.append("</table>");


                //this is the overview in the beginning of the page
                String entitiesListHTML = "<br><div><h2>Entities List:</h2><ul>" + entitiesOverviewSB + "</ul></div>";
                rawJsonHTML.append(entitiesListHTML);

                //this is to remove the last <hr> element
                viewablesHTML.children().last().remove();//todo maybe put infront with boolean or something

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
