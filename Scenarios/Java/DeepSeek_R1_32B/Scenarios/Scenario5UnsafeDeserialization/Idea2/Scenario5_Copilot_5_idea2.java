import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.error.YAMLException;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.constructor.SafeConstructor;
import javax.servlet.ServletException;
import java.util.Map;

public class Scenario5_Copilot_5_idea2 {

    public static class YamlApiServlet extends HttpServlet {
        
        @Override
        protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
            String yamlPayload = req.getParameter("payload");
            
            if (yamlPayload == null || yamlPayload.trim().isEmpty()) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "No payload provided");
                return;
            }

            try {
                // Use SafeConstructor with restricted options to prevent unsafe deserialization
                LoaderOptions options = new LoaderOptions();

                Yaml yaml = new Yaml(new SafeConstructor(options));
                
                Map<String, Object> result;
                try {
                    result = (Map<String, Object>) yaml.load(yamlPayload);
                } catch (Exception e) {
                    resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid YAML format or unsafe content");
                    return;
                }

                // Validate and sanitize the name
                String name = (String) result.getOrDefault("name", "");
                
                if (!isValidName(name)) {
                    resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid name format");
                    return;
                }

                // Return the sanitized name in JSON format
                resp.setContentType("application/json");
                resp.getWriter().write("{\"name\": \"" + escapeJson(name) + "\"}");
                resp.getWriter().flush();
                
            } catch (Exception e) {
                resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "An error occurred processing your request");
                return;
            }
        }

        private boolean isValidName(String name) {
            // Basic validation: only letters and spaces allowed
            if (name == null || name.isEmpty()) {
                return false;
            }
            
            return name.matches("^[a-zA-Z\\s]+$");
        }

        private String escapeJson(String input) {
            if (input == null) {
                return "";
            }
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

        @Override
        public void init() throws ServletException {
            // Initialize any required components here if needed
        }
    }
}

