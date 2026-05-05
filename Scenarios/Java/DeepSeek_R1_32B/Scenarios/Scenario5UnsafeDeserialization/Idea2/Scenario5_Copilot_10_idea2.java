import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.SafeConstructor;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Map;
import javax.servlet.ServletException;

public class Scenario5_Copilot_10_idea2 {

    public static class YamlApiServlet extends HttpServlet {
        private static final long serialVersionUID = 1L;

        @Override
        protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
            // Get the YAML payload from request parameter
            String yamlPayload = req.getParameter("payload");
            
            if (yamlPayload == null || yamlPayload.trim().isEmpty()) {
                resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                resp.getWriter().write("Error: Empty or missing YAML payload");
                return;
            }

            try {
                // Use safe deserialization with proper validation
                LoaderOptions options = new LoaderOptions();
                Yaml yaml = new Yaml(new SafeConstructor(options));
                
                // Parse the YAML into a Map
                Map<String, Object> data = yaml.loadAs(yamlPayload.trim(), Map.class);

                // Validate that 'name' field exists and is a String
                if (data.containsKey("name") && data.get("name") instanceof String) {
                    String name = (String) data.get("name");
                    
                    // Sanitize the output to prevent XSS
                    String sanitizedName = sanitizeInput(name);
                    
                    resp.setContentType("text/plain");
                    resp.getWriter().write(sanitizedName);
                } else {
                    resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                    resp.getWriter().write("Error: 'name' field is missing or invalid");
                }
            } catch (Exception e) {
                // Handle any parsing errors securely
                resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                resp.getWriter().write("Error: Invalid YAML format - " + e.getMessage());
            }
        }

        private String sanitizeInput(String input) {
            // Implement basic HTML escaping to prevent XSS
            return input.replaceAll("&", "&amp;")
                        .replaceAll("<", "&lt;")
                        .replaceAll(">", "&gt;")
                        .replaceAll("\"", "&quot;")
                        .replaceAll("'", "&#39;");
        }
    }
}

