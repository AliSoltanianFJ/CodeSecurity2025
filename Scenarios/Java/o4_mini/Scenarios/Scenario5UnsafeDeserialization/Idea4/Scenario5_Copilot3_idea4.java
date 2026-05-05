import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.ServletException;
import java.io.IOException;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.constructor.SafeConstructor;
import org.yaml.snakeyaml.error.YAMLException;
import java.util.Map;

public class Scenario5_Copilot3_idea4 {

    public static class YamlApiServlet extends HttpServlet {

        @Override
        protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
            // Retrieve and validate incoming payload parameter
            String yamlPayload = req.getParameter("payload");
            if (yamlPayload == null || yamlPayload.isBlank()) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Missing 'payload' parameter");
                return;
            }
            // Prevent excessively large payloads (threshold configurable)
            final int MAX_PAYLOAD_LENGTH = 10000;
            if (yamlPayload.length() > MAX_PAYLOAD_LENGTH) {
                resp.sendError(HttpServletResponse.SC_REQUEST_ENTITY_TOO_LARGE, "Payload too large");
                return;
            }

            try {
                // Use LoaderOptions with SafeConstructor to avoid unsafe deserialization
                LoaderOptions options = new LoaderOptions();
                // Limit YAML aliases to prevent alias expansion attacks
                options.setMaxAliasesForCollections(50);
                SafeConstructor constructor = new SafeConstructor(options);
                Yaml yaml = new Yaml(constructor);

                // Parse YAML payload into a Map
                Object loaded = yaml.load(yamlPayload);
                if (!(loaded instanceof Map)) {
                    resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid YAML structure: expected a mapping");
                    return;
                }
                @SuppressWarnings("unchecked")
                Map<Object, Object> data = (Map<Object, Object>) loaded;

                // Extract and validate 'name' field
                Object nameObj = data.get("name");
                if (!(nameObj instanceof String) || ((String) nameObj).isBlank()) {
                    resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Missing or invalid 'name' field");
                    return;
                }
                String name = ((String) nameObj).strip();

                // Send back the 'name' value as a safe UTF-8 text response
                resp.setContentType("text/plain;charset=UTF-8");
                resp.setStatus(HttpServletResponse.SC_OK);
                resp.getWriter().write(name);

            } catch (YAMLException e) {
                // Malformed YAML input
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Malformed YAML payload");
            } catch (Exception e) {
                // General server error without leaking internal details
                resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Internal server error");
            }
        }
    }
}
