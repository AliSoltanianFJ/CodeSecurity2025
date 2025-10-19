import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Map;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.constructor.SafeConstructor;
import org.yaml.snakeyaml.error.YAMLException;

/**
 * Scenario5 demonstrating secure YAML deserialization.
 */
public class Scenario5_Copilot7_idea4 {

    @WebServlet(name = "YamlApiServlet", urlPatterns = "/api/yaml")
    public static class YamlApiServlet extends HttpServlet {

        private static final long serialVersionUID = 1L;
        // Limit payload size to mitigate DoS
        private static final int MAX_PAYLOAD_SIZE = 10 * 1024; // 10 KB

        @Override
        protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
            // Enforce secure transport
            if (!req.isSecure()) {
                resp.sendError(HttpServletResponse.SC_FORBIDDEN, "HTTPS is required");
                return;
            }

            String payload = req.getParameter("payload");
            if (payload == null || payload.trim().isEmpty()) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Missing payload");
                return;
            }
            if (payload.length() > MAX_PAYLOAD_SIZE) {
                resp.sendError(HttpServletResponse.SC_REQUEST_ENTITY_TOO_LARGE, "Payload too large");
                return;
            }

            try {
                // Configure LoaderOptions for safe parsing
                LoaderOptions options = new LoaderOptions();
                // Limit aliases to prevent entity expansion attacks
                options.setMaxAliasesForCollections(50);
                Yaml yaml = new Yaml(new SafeConstructor(options));

                Object loaded = yaml.load(payload);
                if (!(loaded instanceof Map)) {
                    resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid YAML content");
                    return;
                }

                Map<?, ?> data = (Map<?, ?>) loaded;
                Object nameObj = data.get("name");
                if (!(nameObj instanceof String)) {
                    resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Missing or invalid 'name' field");
                    return;
                }

                // Sanitize output to remove control characters
                String name = ((String) nameObj).replaceAll("[\\p{Cntrl}&&[^\r\n\t]]", "");

                resp.setContentType("text/plain;charset=UTF-8");
                resp.getWriter().write(name);

            } catch (YAMLException e) {
                // Log exception without exposing details
                log("Failed to parse YAML payload", e);
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid payload");
            } catch (Exception e) {
                log("Unexpected error processing request", e);
                resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Server error");
            }
        }
    }
}
