package at.ac.oeaw.helpers;

import org.apache.commons.validator.routines.UrlValidator;
import org.glassfish.jersey.apache.connector.ApacheConnectorProvider;
import org.glassfish.jersey.client.ClientConfig;
import org.glassfish.jersey.client.ClientProperties;

import javax.imageio.ImageIO;
import javax.ws.rs.BadRequestException;
import javax.ws.rs.ProcessingException;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Invocation;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Response;
import java.awt.image.BufferedImage;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.URL;

public class RequestHandler {

    public static final int TIMEOUT = 2500;

    public static Response getJSON(String URL) throws BadRequestException {
        validateURL(URL);

        Response response = get(URL, "application/json");


        String contentType = response.getHeaderString("Content-Type");

        if (contentType == null) {
            throw new BadRequestException("The given URL: '" + URL + "' doesn't have a content-type field in its response headers. Distanbol expects an application/json response.");
        } else if ((!contentType.equals("application/json")) && (!contentType.equals("application/ld+json"))) {
            throw new BadRequestException("The given URL: '" + URL + "' doesn't point to a json or jsonld file.");
        }


        return response;
    }

    private static Response get(String URL, String accept) throws ProcessingException {
        ClientConfig clientConfig = new ClientConfig();
        clientConfig.connectorProvider(new ApacheConnectorProvider());//this is for allowing redirects
        Client client = ClientBuilder.newClient(clientConfig);
        WebTarget webTarget = client.target(URL).property(ClientProperties.CONNECT_TIMEOUT, TIMEOUT).property(ClientProperties.READ_TIMEOUT, TIMEOUT);
        Invocation.Builder invocationBuilder;
        if(accept==null){
            invocationBuilder = webTarget.request();
        }else{
            invocationBuilder = webTarget.request(accept);
        }

        return invocationBuilder.get();
    }

    private static String validateURL(String URL) throws BadRequestException {
        String[] schemes = {"http", "https"};
        UrlValidator urlValidator = new UrlValidator(schemes, 8L);

        String input = URL;
        if ((!URL.startsWith("http://")) && (!URL.startsWith("https://"))) {
            URL = "http://" + URL;
        }

        if (!urlValidator.isValid(URL)) {
            throw new BadRequestException("The given URL: '" + input + "' is not valid.");
        }
        return URL;
    }

    public static boolean imageExists(String depictionURL) {
        try {
            Response response = get(depictionURL,null);
            int status = response.getStatus();
            if(status==200 || status==304){
                return response.getHeaderString("content-type").startsWith("image/");
            }

            return false;
        } catch (ProcessingException e) {
            return false;
        }
    }
}
