package de.webthing.binding.http;

import de.webthing.binding.RESTListener;
import de.webthing.binding.ResourceBuilder;
import de.webthing.thing.Content;
import de.webthing.thing.MediaType;
import fi.iki.elonen.NanoHTTPD;
import fi.iki.elonen.NanoHTTPD.Response.Status;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.stream.Collectors;


public class NanoHttpServer extends NanoHTTPD  implements ResourceBuilder {

    private final Map<String,RESTListener> resmap = new HashMap<>();

	public NanoHttpServer() throws IOException {
        super(8080);
    }

    @Override
    public Response serve(IHTTPSession session) {
         String uri = session.getUri();

        //compare uri against resmap
        RESTListener listener = resmap.get(uri);

        //FIXME partial uris and overviews should be added
        
        //if not found return 404
        if(listener== null) {
            String msg = "Resource not found, availiable resources:\n";
			msg += resmap.keySet().stream().collect(Collectors.joining("\n"));

			return new Response(Response.Status.NOT_FOUND,MIME_PLAINTEXT,msg);

        }

        if(!authorize(session)) {
            return new Response(Response.Status.UNAUTHORIZED,MIME_PLAINTEXT,"Unauthor�zed");
        }

        //get result
        try {
			switch (session.getMethod()) {
			    case GET:
			    	Content resp = listener.onGet();
			    	// TODO how to handle accepted mimeTypes
			    	// e.g., accept=text/html,application/xhtml+xml,application/xml;
			    	return new Response(Status.OK, resp.getMediaType().mediaType,  new ByteArrayInputStream(resp.getContent()));
			    case PUT:
			        listener.onPut(getPayload(session));
			        return new Response(null);
			    case POST:
			    	resp = listener.onPost(getPayload(session));
			    	return new Response(Status.OK, MIME_PLAINTEXT, new String(resp.getContent()));
			    case DELETE:
			        listener.onDelete();
			        return new Response(null);
			    default:
			        return new Response(Response.Status.METHOD_NOT_ALLOWED,MIME_PLAINTEXT,"Method not allowed");
			}
		} catch (Exception e) {
			return new Response(Response.Status.INTERNAL_ERROR,MIME_PLAINTEXT,e.getLocalizedMessage());
		}

	}

    private boolean authorize(IHTTPSession session) {
		//to be replaced once tokenverifier is in place
		return true;

		//String jwt = null;
        //String auth = session.getHeaders().get("Authorization");
        //if(auth != null) {
        //    if(auth.startsWith("Bearer ")) {
        //        jwt = auth.substring("Bearer ".length());
        //    }
        //return tokenVerifier.isAuthorized(jwt);
    }

    private static Content getPayload(IHTTPSession session) throws IOException {
    	// Daniel: to get rid of socket timeout 
    	// http://stackoverflow.com/questions/22349772/retrieve-http-body-in-nanohttpd
    	Integer contentLength = Integer.parseInt(session.getHeaders().get("content-length"));
    	byte[] buffer = new byte[contentLength];
    	int len = 0;
    	do {
    		len += session.getInputStream().read(buffer, len, (contentLength-len));
    	} while(len < contentLength);
    	
    	String contentType = session.getHeaders().get("content-type"); // e.g., content-type=text/plain; charset=UTF-8
    	MediaType mt = MediaType.UNDEFINED; // unknown type
    	if(!(contentType == null || contentType.length() == 0)) {
    		StringTokenizer st = new StringTokenizer(contentType, "; \n\r");
    		String t = st.nextToken();
    		mt = MediaType.getMediaType(t); // may throw exception if content-type is unknown
    	}
    	Content c = new Content(buffer, mt);
    	return c;
    }

    @Override
    public void newResource(String url, RESTListener restListener) {
        resmap.put(url,restListener);
    }
}
