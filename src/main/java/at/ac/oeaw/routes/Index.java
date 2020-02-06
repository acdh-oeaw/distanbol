package at.ac.oeaw.routes;

import at.ac.oeaw.helpers.FileReader;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.Logger;

import javax.servlet.ServletContext;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import java.io.IOException;

@Path("/")
public class Index {

    private static final Logger logger = (Logger) LogManager.getLogger(Index.class);

    @Context
    ServletContext servletContext;

    @GET
    @Path("/")
    public Response index() {

        try {
            String html = FileReader.readFile(this.servletContext.getRealPath("/WEB-INF/classes/view/html/index.html"));
            return Response.status(200).entity(html).type("text/html").build();
        } catch (IOException e) {
            logger.error("Can't read index.html file: "+e.getMessage());
            return Response.serverError().build();
        }

    }
}
