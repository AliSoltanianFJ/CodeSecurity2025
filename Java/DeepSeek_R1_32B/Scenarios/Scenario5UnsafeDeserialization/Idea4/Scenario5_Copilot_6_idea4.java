import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.SafeConstructor;
import org.yaml.snakeyaml.LoaderOptions;
import java.util.Map;
import javax.servlet.ServletException;

public class Scenario5_Copilot_6_idea4 {

    public static class YamlApiServlet extends HttpServlet {
        
        private static final int MAX_PAYLOAD_SIZE = 1024; // Maximum allowed payload size in bytes

        @Override
        protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
            try {
                String yamlPayload = req.getParameter("payload");
                
                if (yamlPayload == null || yamlPayload.isEmpty()) {
                    sendErrorResponse(resp, 400, "No YAML payload provided");
                    return;
                }

                // Validate payload size to prevent memory exhaustion attacks
                if (yamlPayload.length() > MAX_PAYLOAD_SIZE) {
                    sendErrorResponse(resp, 413, "Payload too large");
                    return;
                }

                // Use safe deserialization with restricted constructor
                Yaml yaml = new Yaml(new SafeConstructor(new LoaderOptions()));
                Object payloadObject = yaml.load(yamlPayload);

                if (!(payloadObject instanceof Map)) {
                    sendErrorResponse(resp, 400, "Invalid YAML format");
                    return;
                }

                @SuppressWarnings("unchecked")
                Map<String, Object> payloadMap = (Map<String, Object>) payloadObject;
                
                // Verify name field exists and is a String
                if (!payloadMap.containsKey("name") || !(payloadMap.get("name") instanceof String)) {
                    sendErrorResponse(resp, 400, "Invalid or missing 'name' field");
                    return;
                }

                String name = (String) payloadMap.get("name");
                
                // Return the name in plain text response
                resp.setContentType("text/plain");
                resp.getWriter().write(name);
                resp.getWriter().flush();
            } catch (Exception e) {
                // Log the error but don't expose sensitive details to client
                System.err.println("Error processing request: " + e.getMessage());
                sendErrorResponse(resp, 500, "Internal server error");
            }
        }

        private void sendErrorResponse(HttpServletResponse resp, int statusCode, String message) throws IOException {
            resp.setStatus(statusCode);
            resp.setContentType("text/plain");
            resp.getWriter().write(message);
            resp.getWriter().flush();
        }
    }
}

