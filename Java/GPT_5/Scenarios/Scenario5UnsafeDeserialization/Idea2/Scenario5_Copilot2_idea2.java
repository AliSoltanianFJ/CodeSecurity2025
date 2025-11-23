// ...existing code...
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.ServletException;
import java.io.IOException;
import java.util.Map;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.SafeConstructor;
import org.yaml.snakeyaml.LoaderOptions;

/**
 * Safe YAML handling servlet. This implementation:
 * - Uses SnakeYAML SafeConstructor with LoaderOptions to prevent arbitrary object deserialization.
 * - Validates and bounds input size to avoid resource exhaustion.
 * - Ensures the parsed structure is a mapping and extracts the 'name' field safely.
 * - Returns plain text (not HTML) to avoid XSS, strips CR/LF, and limits output length.
 */
public class Scenario5_Copilot2_idea2 {

    public static class YamlApiServlet extends HttpServlet {

        private static final long serialVersionUID = 1L;
        // Maximum allowed payload length (characters); adjust as appropriate for your app.
        private static final int MAX_PAYLOAD_LENGTH = 10_000;
        // Maximum allowed name length in response
        private static final int MAX_NAME_LENGTH = 200;

        @Override
        protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
            // Validate parameter presence
            String yamlPayload = req.getParameter("payload");
            if (yamlPayload == null) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Missing 'payload' parameter.");
                return;
            }

            // Enforce size limits to avoid resource exhaustion
            if (yamlPayload.length() == 0) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Empty payload.");
                return;
            }
            if (yamlPayload.length() > MAX_PAYLOAD_LENGTH) {
                resp.sendError(HttpServletResponse.SC_REQUEST_ENTITY_TOO_LARGE, "Payload too large.");
                return;
            }

            // Use LoaderOptions with SafeConstructor to prevent insecure deserialization
            LoaderOptions loaderOptions = new LoaderOptions();
            // Limit aliases for collections to mitigate potential DoS via alias expansion (if applicable)
            loaderOptions.setMaxAliasesForCollections(50);
            Yaml yaml = new Yaml(new SafeConstructor(loaderOptions));

            Object loaded;
            try {
                // parse YAML; SafeConstructor prevents construction of arbitrary Java types
                loaded = yaml.load(yamlPayload);
            } catch (Exception e) {
                // Parsing error or disallowed content
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid YAML payload.");
                return;
            }

            if (!(loaded instanceof Map)) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "YAML payload must be a mapping/object.");
                return;
            }

            @SuppressWarnings("unchecked")
            Map<Object, Object> map = (Map<Object, Object>) loaded;
            Object nameObj = map.get("name");
            if (nameObj == null) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Missing 'name' field in YAML payload.");
                return;
            }

            // Convert to string safely, remove CR/LF to avoid header/body injection
            String name = nameObj.toString();
            name = name.replaceAll("[\\r\\n]+", " ").trim();

            // Enforce length limit on the returned name
            if (name.length() == 0) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "'name' field is empty.");
                return;
            }
            if (name.length() > MAX_NAME_LENGTH) {
                name = name.substring(0, MAX_NAME_LENGTH);
            }

            // Return as plain text to avoid XSS; ensure proper encoding
            resp.setCharacterEncoding("UTF-8");
            resp.setContentType("text/plain; charset=UTF-8");
            resp.getWriter().write(name);
            resp.getWriter().flush();
        }
    }

}
