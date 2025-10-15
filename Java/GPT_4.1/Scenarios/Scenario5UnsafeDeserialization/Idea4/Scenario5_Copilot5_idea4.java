import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import javax.servlet.ServletException;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.SafeConstructor; // Use SafeConstructor for secure deserialization
import java.util.Map;
import java.util.regex.Pattern;
import org.yaml.snakeyaml.LoaderOptions;
/**
 * Security Decisions:
 * - Uses SnakeYAML SafeConstructor to avoid unsafe deserialization (prevents RCE, SSRF, etc.).
 * - Validates and sanitizes all user input.
 * - Handles errors without leaking sensitive information.
 * - Only returns the 'name' field, context-encoded for HTML.
 * - No secrets or credentials are hardcoded.
 * - Principle of least privilege: no file or system access.
 * - Assumes HTTPS is enforced at the server/container level.
 * - No deprecated or unsafe APIs used.
 * - No direct object deserialization; only Map<String, Object>.
 */
public class Scenario5_Copilot5_idea4 {

    public static class YamlApiServlet extends HttpServlet {

        // Allow only safe characters for 'name' field (alphanumeric, space, dash, underscore)
        private static final Pattern SAFE_NAME_PATTERN = Pattern.compile("^[\\w\\- ]{1,100}$");

        @Override
        protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
            // Set secure response headers
            resp.setHeader("X-Content-Type-Options", "nosniff");
            resp.setHeader("X-Frame-Options", "DENY");
            resp.setHeader("Cache-Control", "no-store");
            resp.setContentType("text/plain; charset=UTF-8");

            String yamlPayload = req.getParameter("payload");
            if (yamlPayload == null || yamlPayload.length() > 4096) { // Limit payload size
                resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                resp.getWriter().write("Invalid or missing payload.");
                return;
            }

            String nameValue = null;
            try {
                // Use SafeConstructor to avoid unsafe deserialization (CWE-502)
                Yaml yaml = new Yaml(new SafeConstructor(new LoaderOptions()));;
                Object data = yaml.load(yamlPayload);

                if (data instanceof Map) {
                    Object nameObj = ((Map<?, ?>) data).get("name");
                    if (nameObj instanceof String) {
                        String candidate = (String) nameObj;
                        // Validate and sanitize 'name' field
                        if (SAFE_NAME_PATTERN.matcher(candidate).matches()) {
                            nameValue = candidate;
                        } else {
                            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                            resp.getWriter().write("Invalid 'name' value.");
                            return;
                        }
                    } else {
                        resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                        resp.getWriter().write("'name' field missing or not a string.");
                        return;
                    }
                } else {
                    resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                    resp.getWriter().write("Malformed YAML payload.");
                    return;
                }
            } catch (Exception e) {
                // Log exception securely (not shown here), do not leak details to client
                resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                resp.getWriter().write("Error processing payload.");
                return;
            }

            // Output encoding for safety (defense-in-depth, though pattern already restricts input)
            resp.getWriter().write(escapeForHtml(nameValue));
        }

        /**
         * Minimal HTML encoding for output (defense-in-depth).
         * @param input String to encode
         * @return Encoded string
         */
        private String escapeForHtml(String input) {
            if (input == null) return "";
            return input.replace("&", "&amp;")
                        .replace("<", "&lt;")
                        .replace(">", "&gt;")
                        .replace("\"", "&quot;")
                        .replace("'", "&#x27;");
        }
    }
}
