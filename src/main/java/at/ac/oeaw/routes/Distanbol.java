package at.ac.oeaw.routes;

import at.ac.oeaw.Viewable.Viewable;
import at.ac.oeaw.helpers.FileReader;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import javax.servlet.ServletContext;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.QueryParam;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Invocation;
import javax.ws.rs.client.Invocation.Builder;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;
import org.apache.commons.validator.routines.UrlValidator;
import org.apache.log4j.Logger;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

@Path("/")
public class Distanbol
{
    static final Logger logger = Logger.getLogger(Distanbol.class);
    @Context
    ServletContext servletContext;

    @GET
    @Path("/")
    public Response convert(@QueryParam("URL") String URL)
    {
        if (URL == null) {
            try
            {
                String html = FileReader.readFile(this.servletContext.getRealPath("/WEB-INF/classes/view/index.html"));
                return Response.status(200).entity(html).type("text/html").build();
            }
            catch (IOException e)
            {
                e.printStackTrace();
                logger.error("Can't read index html file");
                return Response.serverError().build();
            }
        }
        String[] schemes = { "http", "https" };
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

        Invocation.Builder invocationBuilder = webTarget.request(new String[] { "application/json" });
        Response response = invocationBuilder.get();
        if (response.getStatus() == 302)
        {
            URL = response.getHeaderString("Location");
            if (!urlValidator.isValid(URL)) {
                return Response.status(400).entity("The given URL: '" + input + "' is not valid.").build();
            }
            webTarget = client.target(URL);

            invocationBuilder = webTarget.request(new String[] { "application/json" });
            response = invocationBuilder.get();
        }
        if (response.getStatus() == 302) {
            return Response.status(400).entity("Distanbol only allows one redirect when accessing the given URL input").build();
        }
        String contentType = response.getHeaderString("Content-Type");
        if ((!contentType.equals("application/json")) && (!contentType.equals("application/ld+json"))) {
            return Response.status(400).entity("The given URL: '" + input + "' doesn't point to a json or jsonld file.").build();
        }
        String json = (String)response.readEntity(String.class);
        try
        {
            String html = FileReader.readFile(this.servletContext.getRealPath("/WEB-INF/classes/view/view.html"));
            Document doc = Jsoup.parse(html);

            ObjectMapper mapper = new ObjectMapper();
            JsonNode jsonNode = mapper.readTree(json);
            JsonNode graph = jsonNode.get("@graph");
            if (graph == null) {
                return Response.status(400).entity("The provided json-ld is not in a known Stanbol output format. Make sure that it has the '@graph' element.").build();
            }
            if (graph.isArray())
            {
                Iterator<JsonNode> iterator = graph.elements();

                Element rawJsonHTML = doc.getElementById("rawJson");

                rawJsonHTML.append("Stanbol JSON input: <a href=\"" + URL + "\">" + URL + "</a>");

                Element viewablesHTML = doc.getElementById("viewables");

                Element script = doc.getElementById("script");
                while (iterator.hasNext())
                {
                    JsonNode item = (JsonNode)iterator.next();

                    Viewable viewable = createViewableFromItem(item);
                    if ((!viewable.getId().startsWith("urn:content")) && (!viewable.getId().startsWith("urn:enhancement")))
                    {
                        String templateText = "<div><b>%s</b>%s</div>";
                        if (viewable.getId() != null)
                        {
                            String id = String.format(templateText, new Object[] { "id:", viewable.getId() });
                            viewablesHTML.append(id);
                        }
                        if (viewable.getLabel() != null)
                        {
                            String label = String.format(templateText, new Object[] { "label:", viewable.getLabel() });
                            viewablesHTML.append(label);
                        }
                        if (viewable.getComment() != null)
                        {
                            String comment = String.format(templateText, new Object[] { "comment:", viewable.getComment() });
                            viewablesHTML.append(comment);
                        }
                        if ((viewable.getTypes() != null) && (!viewable.getTypes().isEmpty()))
                        {
                            String typesHTML = "";
                            for (String type : viewable.getTypes()) {
                                typesHTML = typesHTML + "<li>" + type + "</li>";
                            }
                            String types = "<div><b>types:</b><ul>" + typesHTML + "</ul></div>";
                            viewablesHTML.append(types);
                        }
                        if (viewable.getDepiction() != null)
                        {
                            String depiction = String.format("<div><b>depiction:</b><div><img src=\"%s\"></img></div></div>", new Object[] { viewable.getDepiction() });
                            viewablesHTML.append(depiction);
                        }
                        if ((viewable.getLatitude() != null) && (!viewable.getLatitude().equals("")) && (viewable.getLongitude() != null) && (!viewable.getLongitude().equals(""))) {
                            script.appendText("addMarker(" + viewable.getLongitude() + "," + viewable.getLatitude() + ");");
                        }
                        viewablesHTML.append("<hr>");
                    }
                }
                viewablesHTML.children().last().remove();
            }
            return Response.accepted().entity(doc.html()).type("text/html").build();
        }
        catch (IOException e)
        {
            logger.error("Can't read input json file: " + e.getMessage());
        }
        return Response.serverError().entity("Can't read input json file").build();
    }

    private Viewable createViewableFromItem(JsonNode item)
    {
        String id = item.get("@id").asText();

        ArrayList<String> types = new ArrayList();
        JsonNode typeArray = item.get("@type");
        if ((typeArray != null) && (typeArray.isArray()))
        {
            Iterator<JsonNode> iterator = typeArray.elements();
            while (iterator.hasNext())
            {
                JsonNode type = (JsonNode)iterator.next();
                types.add(type.asText());
            }
        }
        String depiction = item.get("foaf:depiction") == null ? null : item.get("foaf:depiction").get(0).asText();

        String longitude = null;
        if (item.get("geo:long") != null) {
            if (item.get("geo:long").isArray()) {
                longitude = item.get("geo:long").get(0).asText();
            } else {
                longitude = item.get("geo:long").asText();
            }
        }
        String latitude = null;
        if (item.get("geo:lat") != null) {
            if (item.get("geo:lat").isArray()) {
                latitude = item.get("geo:lat").get(0).asText();
            } else {
                latitude = item.get("geo:lat").asText();
            }
        }
        String label = null;
        JsonNode labelArray = item.get("rdfs:label");
        if ((labelArray != null) && (labelArray.isArray()))
        {
            Iterator<JsonNode> iterator = labelArray.elements();
            while (iterator.hasNext())
            {
                JsonNode labelPair = (JsonNode)iterator.next();
                String language = labelPair.get("@language").asText();
                if (language.equals("en")) {
                    label = labelPair.get("@value").asText();
                }
            }
        }
        String comment = item.get("rdfs:comment") == null ? null : item.get("rdfs:comment").get("@value").asText();

        return new Viewable(id, types, depiction, comment, label, latitude, longitude);
    }
}
