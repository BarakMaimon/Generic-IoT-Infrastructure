import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.sql.SQLException;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.annotation.WebListener;
import javax.servlet.http.HttpServletRequest;

@WebListener
public class WebsiteContextListener implements ServletContextListener {
	public WebsiteContextListener() {

	}

	@Override
	public void contextInitialized(ServletContextEvent sce) {
		ServletContext context = sce.getServletContext();
		try {
			// Create an instance of WebsiteController and store it as an attribute in the
			// servlet context
			AdminDBManager adminDBManager = new AdminDBManager("jdbc:mysql://localhost:3306", "root", "Bm27102000");
			context.setAttribute("adminDBManager", adminDBManager);
		} catch (SQLException | ClassNotFoundException e) {
			e.printStackTrace(); // Handle the exception appropriately

		}
	}

	@Override
	public void contextDestroyed(ServletContextEvent sce) {
		// Perform cleanup if needed
	}

	 public static JsonObject HttpRecToJson(JsonObject startLine, HttpServletRequest request, String[] keys) throws IOException {
	        // Create a JSON object manually based on the retrieved parameters
	        JsonObjectBuilder jsonObjectBuilder = Json.createObjectBuilder();

	        // Add startLine object to the main JSON object
	        jsonObjectBuilder.add("startLine", startLine);

	        for (String key : keys) {
	            String value = request.getParameter(key);
	            jsonObjectBuilder.add(key, value);
	        }

	        return jsonObjectBuilder.build();
	    }
	
	public static JsonObject createJsonObject(String method, String url) {
        JsonObjectBuilder startLineBuilder = Json.createObjectBuilder()
                .add("method", method)
                .add("URL", url);

        return startLineBuilder.build();
    }


	public static boolean isValid(JsonObject data, String[] keys) {
		for (String key : keys) {
			if (!data.containsKey(key)) {
				return false;
			}
		}

		return true;
	}

	public static int sendToGateway(JsonObject req) {
	    try {
	        // Create a SocketChannel object.
	        try (SocketChannel socketChannel = SocketChannel.open()) {
	            // Connect the SocketChannel to the server.
	            InetSocketAddress serverAddress = new InetSocketAddress("localhost", 8085);
	            
	            // Logging for debugging
	            System.out.println("Connecting to server at: " + serverAddress);

	            if (!socketChannel.connect(serverAddress)) {
	                // Finish the connection if it's not complete
	                while (!socketChannel.finishConnect()) {
	                    System.out.println("Connection in progress...");
	                }
	            }

	            // Send the request as a JSON string
	            socketChannel.write(ByteBuffer.wrap(req.toString().getBytes()));

	            // Receive the response
	            ByteBuffer buffer = ByteBuffer.allocate(1500);
	            socketChannel.read(buffer);
	            buffer.flip();

	            // Convert the response to a JsonObject
	            String jsonResponse = new String(buffer.array(), 0, buffer.limit());

	            return extractStatusCode(jsonResponse);
	        }
	    } catch (IOException e) {
	    	 e.printStackTrace();
	    	    System.err.println("Error connecting to the server: " + e.getMessage());
	        return -1; // or throw an exception if needed
	    }
	   
	}


	public static int extractStatusCode(String statusString) {
	    try {
	        // Assuming statusString is in the format "status_code:XXX"
	        String[] parts = statusString.split(":");
	        return Integer.parseInt(parts[1].trim());
	    } catch (Exception e) {
	        e.printStackTrace();
	        // Handle the exception appropriately
	        return -1; // or throw an exception if needed
	    }
	}
	
	public static String getIndexHtml(String htmlFilePath, String externalHTML) {
		// Read the HTML content from the file
		StringBuilder contentBuilder = new StringBuilder();
		try (BufferedReader br = new BufferedReader(new FileReader(htmlFilePath))) {
			String line;
			while ((line = br.readLine()) != null) {
				contentBuilder.append(line);
			}
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		contentBuilder.append(externalHTML);
		
		return contentBuilder.toString();
	}
	
	
}
