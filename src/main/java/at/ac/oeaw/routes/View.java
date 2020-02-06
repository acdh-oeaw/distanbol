package at.ac.oeaw.routes;

import at.ac.oeaw.helpers.FileReader;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.Logger;

import javax.servlet.ServletContext;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

@Path("/view")
public class View {

    private static final Logger logger = (Logger) LogManager.getLogger(View.class);
    //fundament has these types of files
    private final List<String> extensionsText = Arrays.asList("css", "js");
    private final List<String> extensionsImage = Arrays.asList("jpg", "png");
    @Context
    ServletContext servletContext;

    @GET
    @Path("/{filepath : .+}")
    public Response handleView(@PathParam("filepath") String filePath) {
        try {

            String extension = filePath.split("\\.")[filePath.split("\\.").length - 1];

            if (extensionsText.contains(extension)) {
                String file = FileReader.readFile(this.servletContext.getRealPath("/WEB-INF/classes/view/" + filePath));
                return Response.status(200).entity(file).build();
            } else if (extensionsImage.contains(extension)) {
                BufferedImage file = FileReader.readImage(this.servletContext.getRealPath("/WEB-INF/classes/view/" + filePath));
                return Response.status(200).type("image/" + extension).entity(file).build();
            } else {
                return Response.status(404).entity("Requested file doesn't exist.").build();
            }

        } catch (IOException e) {
            return Response.status(404).entity("Requested file doesn't exist.").build();
        }

    }
}
