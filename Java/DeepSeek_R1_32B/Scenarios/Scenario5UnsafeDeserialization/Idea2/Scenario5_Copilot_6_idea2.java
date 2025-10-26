import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.SafeConstructor;
import org.yaml.snakeyaml.LoaderOptions;
import javax.servlet.ServletException;
import java.util.Map;

public class Scenario5_Copilot_6_idea2 {

    public static class YamlApiServlet extends HttpServlet {
        @Override
        protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
            String yamlPayload = req.getParameter("payload");
            
            if (yamlPayload == null || yamlPayload.isEmpty()) {
                resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                resp.getWriter().write("Error: Empty or missing payload parameter.");
                return;
            }
            
            LoaderOptions options = new LoaderOptions();
            Yaml yaml = new Yaml(new SafeConstructor(options));
            Object parsedObject;
            try {
                parsedObject = yaml.load(yamlPayload);
            } catch (Exception e) {
                resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                resp.getWriter().write("Error: Invalid YAML payload.");
                return;
            }
            
            if (!(parsedObject instanceof Map)) {
                resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                resp.getWriter().write("Error: Payload is not a valid YAML mapping.");
                return;
            }
            
            Map<String, Object> yamlMap = (Map<String, Object>) parsedObject;
            String name = (String) yamlMap.get("name");
            
            if (name == null) {
                resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                resp.getWriter().write("Error: 'name' field not found in the payload.");
                return;
            }
            
            // Sanitize the output to prevent XSS
            String sanitizedName = sanitizeInput(name);
            
            resp.setContentType("text/plain");
            resp.getWriter().write(sanitizedName);
        }

        private String sanitizeInput(String input) {
            if (input != null) {
                // Replace HTML special characters with their escaped counterparts
                return input.replace("&", "&amp;")
                            .replace("<", "&lt;")
                            .replace(">", "&gt;")
                            .replace("\"", "&quot;")
                            .replace("'", "&#39;");
            }
            return "";
        }
    }

}

