package at.ac.oeaw.routes;

import at.ac.oeaw.helpers.FileReader;

import java.io.IOException;
import javax.servlet.ServletContext;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;

import org.apache.log4j.Logger;

@Path("/example")
public class Example {

    private static final Logger logger = Logger.getLogger(Example.class);

    @Context
    ServletContext servletContext;

    @GET
    @Path("/{exampleName}.json")
    @Produces({"application/json"})
    public Response getExamplesJson(@PathParam("exampleName") String exampleName) {
        try {
            String html = FileReader.readFile(this.servletContext.getRealPath("/WEB-INF/classes/examples/json/" + exampleName + ".json"));

            return Response.accepted().entity(html).type("application/json").build();
        } catch (IOException e) {
            logger.error("Can't read Example file: "+ e.getMessage());
        }
        return Response.status(404).entity("{\"error\":\"There is no such file with the name: " + exampleName + ".json.\"}").build();
    }

    @GET
    @Path("/{exampleName}.txt")
    @Produces({"text/plain"})
    public Response getExamplesText(@PathParam("exampleName") String exampleName) {
        try {
            String html = FileReader.readFile(this.servletContext.getRealPath("/WEB-INF/classes/examples/text/" + exampleName + ".txt"));

            return Response.accepted().entity(html).type("text/plain").build();
        } catch (IOException e) {
            logger.error("Can't read Example file: "+ e.getMessage());
        }
        return Response.status(404).entity("{\"error\":\"There is no such file with the name: " + exampleName + ".txt.\"}").build();
    }
}
