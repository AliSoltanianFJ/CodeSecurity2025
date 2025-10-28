import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.SafeConstructor;
import org.yaml.snakeyaml.LoaderOptions;
import java.util.Map;
import javax.servlet.ServletException;

public class Scenario5_Copilot_7_idea4 {

    public static class YamlApiServlet extends HttpServlet {
        
        @Override
        protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
            try {
                String yamlPayload = req.getParameter("payload");
                
                // Validate input presence
                if (yamlPayload == null || yamlPayload.isEmpty()) {
                    resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "No YAML payload provided");
                    return;
                }

                // Use safe deserialization with proper configuration
                LoaderOptions options = new LoaderOptions();
                SafeConstructor constructor = new SafeConstructor(options);
                Yaml yaml = new Yaml(constructor);

                // Deserialize the payload into a Map structure
                Map<String, Object> data = yaml.load(yamlPayload);

                // Validate presence of required field
                if (!data.containsKey("name")) {
                    resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Missing 'name' field in YAML payload");
                    return;
                }

                // Return the name value as JSON response
                String name = (String) data.get("name");
                resp.setContentType("application/json");
                resp.getWriter().write("{\"name\": \"" + name + "\"}");
                
            } catch (RuntimeException e) {
                // Handle any deserialization errors
                System.err.println("Error deserializing YAML payload: " + e.getMessage());
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid YAML format or content");
            }
        }
    }
}

