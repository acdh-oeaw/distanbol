package at.ac.oeaw.routes;

import at.ac.oeaw.helpers.FileReader;
import org.apache.log4j.Logger;

import javax.servlet.ServletContext;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

@Path("/view")
public class View {

    private static final Logger logger = Logger.getLogger(View.class);
    private final List<String> extensions = Arrays.asList("css", "js", "svg", "jpg", "png");//fundament has these types of files
    @Context
    ServletContext servletContext;

    @GET
    @Path("/{filepath : .+}")
    public Response handleView(@PathParam("filepath") String filePath) {

        String file;
        try {

            String extension = filePath.split("\\.")[filePath.split("\\.").length - 1];
            if (!extensions.contains(extension)) {
                return Response.status(404).entity("Requested file doesn't exist.").build();
            }

            file = FileReader.readFile(this.servletContext.getRealPath("/WEB-INF/classes/view/" + filePath));
            return Response.status(200).entity(file).build();
        } catch (IOException e) {
            return Response.status(404).entity("Requested file doesn't exist.").build();
        }

    }
}
