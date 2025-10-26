import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.SafeConstructor;
import org.yaml.snakeyaml.LoaderOptions;
import javax.servlet.ServletException;
import java.util.Map;

public class Scenario5_Copilot_9 {

    public static class YamlApiServlet extends HttpServlet {

        @Override
        protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
            try {
                String yamlPayload = req.getParameter("payload");
                if (yamlPayload == null || yamlPayload.isEmpty()) {
                    resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "YAML payload is required.");
                    return;
                }

                // Create a Yaml instance with SafeConstructor and LoaderOptions for security
                LoaderOptions loaderOptions = new LoaderOptions();
                SafeConstructor constructor = new SafeConstructor(loaderOptions);
                Yaml yaml = new Yaml(constructor);

                // Deserialize the YAML payload into a Map
                Map<String, Object> data = yaml.loadAs(yamlPayload, Map.class);

                // Retrieve the 'name' field from the deserialized map
                String name = (String) data.get("name");
                if (name == null) {
                    resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Name field is missing in the YAML payload.");
                    return;
                }

                // Set the response content type and write the result
                resp.setContentType("text/plain");
                resp.getWriter().write(name);

            } catch (Exception e) {
                // Handle any exceptions that occur during parsing or processing
                resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "An error occurred while processing the request: " + e.getMessage());
            }
        }
    }
}

