import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Map;
import java.util.logging.Logger;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.constructor.SafeConstructor;

public class Scenario5_Copilot8_idea4 {

    public static class YamlApiServlet extends HttpServlet {
        private static final Logger LOGGER = Logger.getLogger(YamlApiServlet.class.getName());

        // maximum allowed payload length to mitigate DoS via huge payloads
        private static final int MAX_PAYLOAD_LENGTH = 8 * 1024;

        @Override
        protected void doGet(HttpServletRequest req, HttpServletResponse resp)
                throws ServletException, IOException {
            // Enforce HTTPS in deployment; reject non-secure requests
            if (!req.isSecure()) {
                resp.sendError(HttpServletResponse.SC_FORBIDDEN, "Transport layer security required");
                return;
            }

            String yamlPayload = req.getParameter("payload");
            if (yamlPayload == null || yamlPayload.trim().isEmpty()) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Missing or empty payload");
                return;
            }
            if (yamlPayload.length() > MAX_PAYLOAD_LENGTH) {
                resp.sendError(HttpServletResponse.SC_REQUEST_ENTITY_TOO_LARGE, "Payload too large");
                return;
            }

            try {
                // Use LoaderOptions with SafeConstructor to prevent arbitrary code execution
                LoaderOptions loaderOptions = new LoaderOptions();
                loaderOptions.setAllowDuplicateKeys(false);
                loaderOptions.setMaxAliasesForCollections(0);
                Yaml yaml = new Yaml(new SafeConstructor(loaderOptions));

                Object loaded = yaml.load(yamlPayload);
                if (!(loaded instanceof Map)) {
                    resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid YAML structure");
                    return;
                }

                @SuppressWarnings("unchecked")
                Map<String, Object> data = (Map<String, Object>) loaded;
                Object nameObj = data.get("name");
                if (!(nameObj instanceof String)) {
                    resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Field 'name' missing or not a string");
                    return;
                }

                // Basic sanitization: remove control characters except CR/LF/TAB
                String name = ((String) nameObj)
                        .replaceAll("[\\p{Cntrl}&&[^\r\n\t]]", "")
                        .trim();

                // Return as plain text; charset UTF-8 enforced
                resp.setContentType("text/plain; charset=UTF-8");
                resp.getWriter().write(name);

            } catch (Exception e) {
                // Log minimal info without exposing stack traces to clients
                LOGGER.warning("Failed to parse YAML payload: " + e.getMessage());
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid payload");
            }
        }
    }

}
