package at.ac.oeaw.helpers;

import org.apache.commons.validator.routines.UrlValidator;
import org.glassfish.jersey.apache.connector.ApacheConnectorProvider;
import org.glassfish.jersey.client.ClientConfig;
import org.glassfish.jersey.client.ClientProperties;

import javax.imageio.ImageIO;
import javax.ws.rs.BadRequestException;
import javax.ws.rs.Path;
import javax.ws.rs.ProcessingException;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Invocation;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;

public class RequestHandler {

    public static final int TIMEOUT = 2500;
    private static final String stanbolURLString = "https://enrich.acdh.oeaw.ac.at/enhancer/chain/";
    private static final int bufferSize = 1000000;

    public static Response get(String URL, String accept) throws ProcessingException {
        URL = validateURL(URL);
        ClientConfig clientConfig = new ClientConfig();
        clientConfig.connectorProvider(new ApacheConnectorProvider());//this is for allowing redirects
        Client client = ClientBuilder.newClient(clientConfig);
        WebTarget webTarget = client.target(URL).property(ClientProperties.CONNECT_TIMEOUT, TIMEOUT).property(ClientProperties.READ_TIMEOUT, TIMEOUT);
        Invocation.Builder invocationBuilder;
        if (accept == null) {
            invocationBuilder = webTarget.request();
        } else {
            invocationBuilder = webTarget.request(accept);
        }

        return invocationBuilder.get();
    }

    private static String validateURL(String URL) throws BadRequestException {
        URL = URL.trim();
        String[] schemes = {"http", "https"};
        UrlValidator urlValidator = new UrlValidator(schemes, 8L);

        if ((!URL.startsWith("http://")) && (!URL.startsWith("https://"))) {
            URL = "http://" + URL;
        }

        if (!urlValidator.isValid(URL)) {
            throw new BadRequestException("The given URL: '" + URL + "' is not valid.");
        }
        return URL;
    }


    public static String postToStanbol(String fulltext) throws IOException {

        /*
        //todo find out what the different chains are
        switch(args[1]) {
			case "COUNTRIES":
				stanbolURLString += "geoNames_PCLI";
				break;
			case "CITIES":
				stanbolURLString += "geoNames_PPLC";
				break;
			case "LOCATIONS":
				stanbolURLString += "geoNames_SPAsubset";
				break;
			default:
				stanbolURLString += "dbpedia-fst-linking";
}
        */

        URL stanbolURL = new URL(stanbolURLString+"dbpedia-fst-linking");
        HttpURLConnection stanbolCon = (HttpURLConnection) stanbolURL.openConnection();
        stanbolCon.setRequestMethod("POST");
        stanbolCon.setRequestProperty("Accept", "application/json");
        stanbolCon.setRequestProperty("Content-Type", "text/plain");
        stanbolCon.setUseCaches(false);
        stanbolCon.setDoOutput(true);


        OutputStream stanbolOut = stanbolCon.getOutputStream();

        String truncatedFullText = fulltext.substring(0, Math.min(fulltext.length(), bufferSize));

        stanbolOut.write(truncatedFullText.getBytes());

        stanbolOut.flush();
        stanbolOut.close();


        InputStream stanbolIn = stanbolCon.getInputStream();

        StringBuilder sb = new StringBuilder();

        //adding text as json
        sb.append("[{\"fulltext\":\"");
        sb.append(toJSON(truncatedFullText));
        sb.append("\"},\n");

        String stanbolOutput = convertStreamToString(stanbolIn).substring(1);

        return sb.toString()+stanbolOutput;

    }

    private static String convertStreamToString(java.io.InputStream is) {
        java.util.Scanner s = new java.util.Scanner(is).useDelimiter("\\A");
        return s.hasNext() ? s.next() : "";
    }

    private static String toJSON(String text) {
        return text
                .replace("\\", "\\\\")
                .replace("\b", "\\b")
                .replace("\f", "\\f")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t")
                .replace("\"", "\\\"");
    }

//    public static boolean imageExists(String depictionURL) {
//        try {
//            Response response = get(depictionURL,null);
//            int status = response.getStatus();
//            if(status==200 || status==304){
//                return response.getHeaderString("content-type").startsWith("image/");
//            }
//
//            return false;
//        } catch (ProcessingException e) {
//            return false;
//        }
//    }
}
