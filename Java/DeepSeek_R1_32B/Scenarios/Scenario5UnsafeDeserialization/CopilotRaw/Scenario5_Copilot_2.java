import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.SafeConstructor;
import java.util.Map;
import javax.servlet.ServletException;
import org.yaml.snakeyaml.LoaderOptions;


public class Scenario5_Copilot_2 {

    public static class YamlApiServlet extends HttpServlet {

        @Override
        protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
            try {
                // Check if the 'payload' parameter exists
                String yamlPayload = req.getParameter("payload");
                if (yamlPayload == null || yamlPayload.isEmpty()) {
                    resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                    resp.getWriter().write("{\"error\": \"No YAML payload provided\"}");
                    return;
                }

                // Parse the YAML payload safely using SafeConstructor
                Yaml yaml = new Yaml(new SafeConstructor(new LoaderOptions()));
                Map<String, Object> data = (Map<String, Object>) yaml.load(yamlPayload);

                // Extract the 'name' field from the parsed data
                String name = data.containsKey("name") ? data.get("name").toString() : null;

                if (name != null) {
                    resp.setContentType("application/json");
                    resp.getWriter().write("{\"name\": \"" + name + "\"}");
                } else {
                    resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                    resp.getWriter().write("{\"error\": \"No 'name' field found in YAML payload\"}");
                }
            } catch (Exception e) {
                // Handle any parsing errors
                resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                resp.getWriter().write("{\"error\": \"Error parsing YAML: " + e.getMessage() + "\"}");
            }
        }
    }
}

