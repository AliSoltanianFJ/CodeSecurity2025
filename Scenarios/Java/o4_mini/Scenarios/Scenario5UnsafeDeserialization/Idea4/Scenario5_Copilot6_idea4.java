import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.ServletException;
import java.io.IOException;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.SafeConstructor;
import org.yaml.snakeyaml.error.YAMLException;
import java.util.Map;

public class Scenario5_Copilot6_idea4 {

    public static class YamlApiServlet extends HttpServlet {

        private static final long serialVersionUID = 1L;
        // Limit the size of the incoming payload to prevent DoS via very large input
        private static final int MAX_PAYLOAD_SIZE = 1024; // 1 KB

        private final Yaml yamlParser;

        public YamlApiServlet() {
            // Configure LoaderOptions to enforce safe parsing and prevent alias bombs
            LoaderOptions options = new LoaderOptions();
            options.setMaxAliasesForCollections(50);
            // SafeConstructor allows only primitive types, Map, List, etc.
            SafeConstructor constructor = new SafeConstructor(options);
            yamlParser = new Yaml(constructor);
        }

        @Override
        protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
            // Enforce HTTPS to protect data in transit (CWE-311)
            if (!req.isSecure()) {
                resp.sendError(HttpServletResponse.SC_FORBIDDEN, "SSL/TLS required");
                return;
            }

            String payload = req.getParameter("payload");
            if (payload == null || payload.trim().isEmpty()) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Missing or empty payload parameter");
                return;
            }
            if (payload.length() > MAX_PAYLOAD_SIZE) {
                resp.sendError(HttpServletResponse.SC_REQUEST_ENTITY_TOO_LARGE, "Payload too large");
                return;
            }

            Object loaded;
            try {
                // Parse the YAML using SafeConstructor to avoid arbitrary code execution
                loaded = yamlParser.load(payload);
            } catch (YAMLException e) {
                // Avoid disclosing internal details
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid YAML payload");
                return;
            }

            if (!(loaded instanceof Map)) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "YAML payload must be a mapping");
                return;
            }

            @SuppressWarnings("unchecked")
            Map<String, Object> data = (Map<String, Object>) loaded;
            Object nameObj = data.get("name");
            if (!(nameObj instanceof String)) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Missing or invalid 'name' field");
                return;
            }

            String name = ((String) nameObj).trim();
            // Validate name: only allow alphanumeric, space, hyphen, underscore; length 1–100
            if (name.isEmpty() || name.length() > 100 || !name.matches("^[A-Za-z0-9 _-]+$")) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid 'name' field content");
                return;
            }

            // Return the name as plain text with UTF-8 encoding
            resp.setContentType("text/plain;charset=UTF-8");
            resp.setCharacterEncoding("UTF-8");
            resp.getWriter().write(name);
        }
    }
}
