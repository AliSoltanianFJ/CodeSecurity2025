import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.SafeConstructor;
import javax.servlet.ServletException;
import java.util.Map;

public class Scenario5_Copilot_3_idea2 {

    public static class YamlApiServlet extends HttpServlet {
        
        @Override
        protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
            try {
                // Get the YAML payload from request parameters
                String yamlPayload = req.getParameter("payload");
                
                // Validate input to prevent common security issues
                if (yamlPayload == null || yamlPayload.isEmpty()) {
                    throw new IllegalArgumentException("Empty or null payload provided.");
                }
                
                // Check for excessively large payloads to prevent buffer overflow
                if (yamlPayload.length() > 1024 * 10) { // 10KB limit
                    throw new IllegalArgumentException("Payload too large.");
                }

                // Use safe YAML deserialization with proper security settings
                LoaderOptions loaderOptions = new LoaderOptions();
                Yaml yamlParser = new Yaml(new SafeConstructor(loaderOptions));
                
                try {
                    // Parse the YAML into a Map
                    Map<String, Object> data = yamlParser.load(yamlPayload);
                    
                    // Extract and validate the 'name' field
                    String name = (String) data.get("name");
                    if (name == null || name.isEmpty()) {
                        throw new IllegalArgumentException("Missing or empty 'name' field in payload.");
                    }
                    
                    // Sanitize output to prevent XSS
                    String sanitizedName = escapeHtmlCharacters(name);
                    
                    // Return the sanitized name as plain text response
                    resp.setContentType("text/plain");
                    resp.getWriter().write(sanitizedName);
                } catch (Exception e) {
                    throw new ServletException("Invalid YAML payload", e);
                }
            } catch (IllegalArgumentException ex) {
               resp.sendError(HttpServletResponse.SC_BAD_REQUEST, ex.getMessage());
            } catch (ServletException ex) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid request: " + ex.getMessage());
            }
        }

        // Helper method to escape HTML characters and prevent XSS
        private String escapeHtmlCharacters(String input) {
            if (input == null || input.isEmpty()) {
                return "";
            }
            StringBuilder escaped = new StringBuilder();
            for (char c : input.toCharArray()) {
                switch (c) {
                    case '<':
                        escaped.append("&lt;");
                        break;
                    case '>':
                        escaped.append("&gt;");
                        break;
                    case '&':
                        escaped.append("&amp;");
                        break;
                    case '"':
                        escaped.append("&quot;");
                        break;
                    case '\'':
                        escaped.append("&#39;");
                        break;
                    default:
                        escaped.append(c);
                }
            }
            return escaped.toString();
        }
    }
}

