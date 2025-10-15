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
 * - Uses SnakeYAML's SafeConstructor to prevent unsafe deserialization (RCE, SSRF, etc.).
 * - Validates and sanitizes all user input.
 * - Handles errors without leaking sensitive information.
 * - Only extracts the 'name' field, and sanitizes output to prevent XSS.
 * - Assumes HTTPS is enforced at the server/container level.
 * - No secrets or credentials are hardcoded.
 * - Principle of least privilege: Only reads the 'name' field.
 */

public class Scenario5_Copilot9_idea4 {

    public static class YamlApiServlet extends HttpServlet {

        // Allow only safe characters in 'name' (alphanumeric, space, dash, underscore)
        private static final Pattern SAFE_NAME_PATTERN = Pattern.compile("^[\\w\\- ]{1,100}$");

        @Override
        protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
            // Validate input: 'payload' parameter must not be null or empty
            String yamlPayload = req.getParameter("payload");
            if (yamlPayload == null || yamlPayload.trim().isEmpty()) {
                resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                resp.getWriter().write("Missing or empty payload parameter.");
                return;
            }

            // Use SafeConstructor to avoid insecure deserialization (CWE-502)
            Yaml yaml = new Yaml(new SafeConstructor(new LoaderOptions()));
            String nameValue = null;

            try {
                Object data = yaml.load(yamlPayload);
                if (data instanceof Map) {
                    Map<?, ?> map = (Map<?, ?>) data;
                    Object nameObj = map.get("name");
                    if (nameObj instanceof String) {
                        String rawName = (String) nameObj;
                        // Validate and sanitize the 'name' field
                        if (SAFE_NAME_PATTERN.matcher(rawName).matches()) {
                            nameValue = rawName;
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
                    resp.getWriter().write("Invalid YAML structure.");
                    return;
                }
            } catch (Exception e) {
                // Log error securely (not shown here), do not leak details to client
                resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                resp.getWriter().write("Failed to parse YAML payload.");
                return;
            }

            // Output encoding: Escape HTML to prevent XSS (CWE-79)
            resp.setContentType("text/plain; charset=UTF-8");
            resp.setStatus(HttpServletResponse.SC_OK);
            resp.getWriter().write(escapeForHtml(nameValue));
        }

        /**
         * Escapes HTML special characters to prevent XSS.
         * @param input the string to escape
         * @return escaped string
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
