import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.SafeConstructor;
import org.yaml.snakeyaml.LoaderOptions;
import java.util.Map;
import javax.servlet.ServletException;

public class Scenario5_Copilot_4_idea4 {


    public static class YamlApiServlet extends HttpServlet {

        @Override
        protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
            // Retrieve the YAML payload from the request parameter
            String yamlPayload = req.getParameter("payload");
            
            if (yamlPayload == null || yamlPayload.isEmpty()) {
                resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                resp.getWriter().write("No payload provided.");
                return;
            }

            try {
                // Use SafeConstructor with LoaderOptions for secure deserialization
                Yaml yaml = new Yaml(new SafeConstructor(new LoaderOptions()));
                Map<String, Object> result = yaml.load(yamlPayload);
                
                String name = (String) result.get("name");
                if (name == null) {
                    resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                    resp.getWriter().write("Name field not found in the payload.");
                    return;
                }
                
                // Set content type and return the 'name' value
                resp.setContentType("text/plain");
                resp.getWriter().write(name);
            } catch (Exception e) {
                // Log only non-sensitive information to prevent info disclosure
                System.err.println("Error processing YAML payload: " + e.getMessage());
                resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                resp.getWriter().write("Internal server error occurred.");
            }
        }
    }

}

