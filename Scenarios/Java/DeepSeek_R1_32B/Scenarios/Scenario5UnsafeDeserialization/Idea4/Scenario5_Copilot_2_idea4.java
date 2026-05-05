import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.SafeConstructor;
import org.yaml.snakeyaml.LoaderOptions;
import java.util.Map;
import javax.servlet.ServletException;

public class Scenario5_Copilot_2_idea4 {

    public static class YamlApiServlet extends HttpServlet {

        @Override
        protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
            // Get the YAML payload from request parameter
            String yamlPayload = req.getParameter("payload");
            
            if (yamlPayload == null || yamlPayload.trim().isEmpty()) {
                resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                resp.getWriter().write("Error: Empty or missing payload");
                return;
            }

            try {
                // Configure safe YAML deserialization
                LoaderOptions options = new LoaderOptions();
                options.setAllowDuplicateKeys(false);  // Prevent duplicate keys which could cause issues
                Yaml yaml = new Yaml(new SafeConstructor(options));
                
                // Limit maximum allowed size for the payload to prevent resource exhaustion
                final int MAX_PAYLOAD_SIZE = 1024 * 1024; // 1MB
                if (yamlPayload.length() > MAX_PAYLOAD_SIZE) {
                    throw new IllegalArgumentException("Payload exceeds maximum allowed size");
                }

                // Safely deserialize the YAML content
                Map<String, Object> data = yaml.load(yamlPayload);
                
                // Validate that 'name' field exists and is a String
                if (data.containsKey("name") && data.get("name") instanceof String) {
                    String name = (String) data.get("name");
                    
                    // Basic validation for name content
                    if (!isValidName(name)) {
                        throw new IllegalArgumentException("Invalid name format");
                    }
                    
                    // Return the result as JSON
                    resp.setContentType("application/json");
                    resp.getWriter().write("{\"name\": \"" + escapeJson(name) + "\"}");
                } else {
                    throw new IllegalArgumentException("Missing or invalid 'name' field in payload");
                }
            } catch (IllegalArgumentException e) {
                resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                resp.getWriter().write("{\"error\": \"" + e.getMessage() + "\"}");
            } catch (Exception e) {
                // Log the error for analysis
                System.err.println("Error processing YAML: " + e.getMessage());
                resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                resp.getWriter().write("{\"error\": \"Internal server error\"}");
            }
        }

        private boolean isValidName(String name) {
            // Basic validation to prevent potential XSS or injection attempts
            return name.matches("[a-zA-Z0-9_\\s-]+");
        }

        private String escapeJson(String input) {
            // Escape special characters for JSON output
            return input.replace("\\", "\\\\")
                        .replace("\"", "\\\"")
                        .replace("/", "\\/")
                        .replace("\b", "\\b")
                        .replace("\f", "\\f")
                        .replace("\n", "\\n")
                        .replace("\r", "\\r")
                        .replace("\t", "\\t");
        }
    }
}

