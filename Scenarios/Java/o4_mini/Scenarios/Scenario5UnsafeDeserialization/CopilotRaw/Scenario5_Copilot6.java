import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.ServletException;
import java.io.IOException;
import java.util.Map;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.SafeConstructor;
import org.yaml.snakeyaml.LoaderOptions;

public class Scenario5_Copilot6 {

    public static class YamlApiServlet extends HttpServlet {

        @Override
        protected void doGet(HttpServletRequest req, HttpServletResponse resp)
                throws ServletException, IOException {
            // Read the YAML payload from the request
            String yamlPayload = req.getParameter("payload");
            if (yamlPayload == null) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Missing 'payload' parameter");
                return;
            }

            // Configure SnakeYAML for safe loading
            LoaderOptions options = new LoaderOptions();
            Yaml yaml = new Yaml(new SafeConstructor(options));

            // Deserialize into a Map
            @SuppressWarnings("unchecked")
            Map<String, Object> data = yaml.load(yamlPayload);

            // Extract the 'name' field
            String name = "";
            if (data != null && data.containsKey("name")) {
                Object nameObj = data.get("name");
                if (nameObj != null) {
                    name = nameObj.toString();
                }
            }

            // Return the 'name' value as plain text
            resp.setContentType("text/plain");
            resp.getWriter().write(name);
        }
    }

}
