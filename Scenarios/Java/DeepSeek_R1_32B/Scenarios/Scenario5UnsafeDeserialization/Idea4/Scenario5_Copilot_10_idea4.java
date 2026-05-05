import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.constructor.SafeConstructor;
import java.util.Map;
import javax.servlet.ServletException;

public class Scenario5_Copilot_10_idea4 {

    public static class YamlApiServlet extends HttpServlet {
        
        @Override
        protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
            // Get the YAML payload from request parameter
            String yamlPayload = req.getParameter("payload");
            
            if (yamlPayload == null || yamlPayload.trim().isEmpty()) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "No payload provided");
                return;
            }

            try {
                // Use secure deserialization with SafeConstructor and LoaderOptions
                Yaml yaml = new Yaml(new org.yaml.snakeyaml.constructor.SafeConstructor(new LoaderOptions()));
                Map<String, Object> data = yaml.load(yamlPayload);
                
                if (data != null && data.containsKey("name")) {
                    String name = (String) data.get("name");
                    resp.setContentType("application/json");
                    resp.getWriter().write("{\"name\":\"" + name + "\"}");
                } else {
                    resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "No 'name' field found in payload");
                }
                
            } catch (Exception e) {
                // Log the exception and return generic error to prevent information disclosure
                System.err.println("Error deserializing YAML: " + e.getMessage());
                resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Invalid YAML payload");
            }
        }
    }

}

