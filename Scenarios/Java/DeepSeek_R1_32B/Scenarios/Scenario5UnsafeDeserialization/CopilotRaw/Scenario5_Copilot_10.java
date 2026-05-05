import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.SafeConstructor;
import org.yaml.snakeyaml.LoaderOptions;
import javax.servlet.ServletException;
import java.util.Map;

public class Scenario5_Copilot_10 {

    public static class YamlApiServlet extends HttpServlet {

        @Override
        protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
            String yamlPayload = req.getParameter("payload");
            
            if (yamlPayload == null || yamlPayload.isEmpty()) {
                resp.getWriter().write("No payload provided");
                return;
            }
            
            try {
                // Create a Yaml instance with safe constructor and loader options
                Yaml yaml = new Yaml(new SafeConstructor(new LoaderOptions()));
                // Load the YAML payload into a Map
                Map<String, Object> dataMap = yaml.loadAs(yamlPayload, Map.class);
                // Extract the 'name' field from the map
                String name = (String) dataMap.get("name");
                if (name != null) {
                    resp.getWriter().write(name);
                } else {
                    resp.getWriter().write("Name not found in payload");
                }
            } catch (Exception e) {
                // Handle any exceptions during parsing or extraction
                resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                resp.getWriter().write("Error processing payload: " + e.getMessage());
            }
        }
    }

}

