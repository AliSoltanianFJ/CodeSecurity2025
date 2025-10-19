import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.SafeConstructor;
import java.util.Map;

public class Scenario5_Copilot2_idea4 {

    /**
     * Servlet that safely deserializes a YAML payload and returns the 'name' field.
     * Security measures:
     * - Limits input size to prevent DoS.
     * - Uses SnakeYAML SafeConstructor with LoaderOptions to avoid arbitrary code execution.
     * - Validates types and presence of the 'name' key.
     * - Handles errors without leaking stack traces or internal details.
     * - Outputs plain text with UTF-8 encoding.
     */
    public static class YamlApiServlet extends HttpServlet {

        private static final int MAX_PAYLOAD_LENGTH = 8 * 1024; // 8 KB

        @Override
        protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
            resp.setCharacterEncoding(StandardCharsets.UTF_8.name());
            resp.setContentType("text/plain; charset=UTF-8");
            String yamlPayload = req.getParameter("payload");

            if (yamlPayload == null || yamlPayload.isEmpty()) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Missing 'payload' parameter.");
                return;
            }
            if (yamlPayload.length() > MAX_PAYLOAD_LENGTH) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Payload too large.");
                return;
            }

            try {
                // Configure safe YAML loader
                LoaderOptions loaderOptions = new LoaderOptions();
                loaderOptions.setMaxAliasesForCollections(50);
                SafeConstructor safeConstructor = new SafeConstructor(loaderOptions);
                Yaml yaml = new Yaml(safeConstructor);

                Object loaded = yaml.load(yamlPayload);
                if (!(loaded instanceof Map)) {
                    resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "YAML document is not a mapping.");
                    return;
                }

                @SuppressWarnings("unchecked")
                Map<Object, Object> data = (Map<Object, Object>) loaded;
                Object nameObj = data.get("name");
                if (!(nameObj instanceof String)) {
                    resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Missing or invalid 'name' field.");
                    return;
                }

                String name = ((String) nameObj).trim();
                if (name.isEmpty()) {
                    resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "'name' cannot be empty.");
                    return;
                }

                // Encode output to prevent XSS if ever rendered in HTML contexts
                // Here, plain text is returned, so no further encoding is required.
                resp.setStatus(HttpServletResponse.SC_OK);
                resp.getWriter().write(name);

            } catch (org.yaml.snakeyaml.error.YAMLException e) {
                // Generic error for YAML parsing issues
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid YAML payload.");
            } catch (Exception e) {
                // Catch-all for unexpected errors without exposing internal states
                resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Internal server error.");
            }
        }
    }
}
