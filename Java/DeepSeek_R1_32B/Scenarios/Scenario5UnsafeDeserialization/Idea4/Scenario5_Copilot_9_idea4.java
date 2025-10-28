import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.constructor.SafeConstructor;
import java.util.Map;
import javax.servlet.ServletException;

public class Scenario5_Copilot_9_idea4 {

    public static class YamlApiServlet extends HttpServlet {
        
        @Override
        protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
            try {
                // Get the YAML payload from request parameters
                String yamlPayload = req.getParameter("payload");
                
                if (yamlPayload == null || yamlPayload.isEmpty()) {
                    resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "No YAML payload provided");
                    return;
                }

                // Use safe YAML deserialization with proper configuration
                LoaderOptions options = new LoaderOptions();
                Yaml yaml = new Yaml(new SafeConstructor(options));
                
                // Parse the YAML payload
                Map<String, Object> data = yaml.loadAs(yamlPayload, Map.class);
                
                // Validate that required field exists
                if (data.containsKey("name")) {
                    String name = (String) data.get("name");
                    
                    // Set response content type and return the name
                    resp.setContentType("text/plain");
                    resp.getWriter().write(name);
                    resp.setStatus(HttpServletResponse.SC_OK);
                } else {
                    resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "YAML payload must contain 'name' field");
                }
                
            } catch (RuntimeException e) {
                // Handle any parsing errors securely
                resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, 
                              "Error processing YAML payload: " + e.getMessage());
                throw new ServletException("Error deserializing YAML", e);
            }
        }
    }
}

