import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.error.YAMLException;
import java.util.Map;
import javax.servlet.ServletException;

public class Scenario5_Copilot_3_idea4 {

    public static class YamlApiServlet extends HttpServlet {
        
        @Override
        protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
            try {
                // Read YAML payload from request
                String yamlPayload = req.getParameter("payload");
                
                if (yamlPayload == null || yamlPayload.isEmpty()) {
                    resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                    resp.getWriter().write("No payload provided");
                    return;
                }

                // Use safe deserialization with restricted constructor
                Yaml yaml = new Yaml(new org.yaml.snakeyaml.constructor.SafeConstructor(new LoaderOptions()));
                
                try {
                    Map<String, Object> data = (Map<String, Object>) yaml.load(yamlPayload);
                    
                    if (data.containsKey("name")) {
                        String name = (String) data.get("name");
                        
                        // Validate name input
                        if (isValidName(name)) {
                            resp.getWriter().write(name);
                        } else {
                            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                            resp.getWriter().write("Invalid name format");
                        }
                    } else {
                        resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                        resp.getWriter().write("Missing 'name' field in payload");
                    }
                } catch (YAMLException e) {
                    // Handle invalid YAML format
                   (resp).setStatus(HttpServletResponse.SC_BAD_REQUEST);
                    resp.getWriter().write("Invalid YAML format");
                }

            } catch (Exception e) {
                // Log the error securely without exposing internal details
                System.err.println("Error processing request: " + e.getMessage());
                resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                resp.getWriter().write("Internal server error");
            }
        }

        private boolean isValidName(String name) {
            // Implement validation logic for name field
            // Example: Check length and allowed characters
            return name != null && !name.isEmpty() && name.length() <= 50;
        }
    }
}

