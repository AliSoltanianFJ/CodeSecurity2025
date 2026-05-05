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
 * - Uses SafeConstructor to avoid unsafe deserialization (prevents RCE, CWE-502).
 * - Validates and sanitizes all user input.
 * - Handles errors without leaking sensitive information.
 * - Assumes HTTPS is enforced at the server configuration level.
 * - No secrets or credentials are hardcoded.
 * - Principle of least privilege: only reads a single field.
 * - Output encoding is applied to prevent XSS.
 */
public class Scenario5_Copilot10_idea4 {

    public static class YamlApiServlet extends HttpServlet {

        // Allow only safe characters for 'name' field (alphanumeric, space, dash, underscore)
        private static final Pattern SAFE_NAME_PATTERN = Pattern.compile("^[\\w\\- ]{1,100}$");

        @Override
        protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
            // Validate input: 'payload' parameter must not be null or excessively large
            String yamlPayload = req.getParameter("payload");
            if (yamlPayload == null || yamlPayload.length() > 4096) {
                resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                resp.getWriter().write("Invalid or missing payload.");
                return;
            }

            String nameValue = null;
            try {
                // Use SnakeYAML's SafeConstructor to avoid unsafe deserialization
                Yaml yaml = new Yaml(new SafeConstructor(new LoaderOptions()));;
                Object data = yaml.load(yamlPayload);

                // Ensure the deserialized object is a Map
                if (data instanceof Map) {
                    Map<?, ?> map = (Map<?, ?>) data;
                    Object nameObj = map.get("name");
                    if (nameObj instanceof String) {
                        String nameCandidate = (String) nameObj;
                        // Validate and sanitize the 'name' field
                        if (SAFE_NAME_PATTERN.matcher(nameCandidate).matches()) {
                            // Basic HTML encoding to prevent XSS
                            nameValue = encodeForHtml(nameCandidate);
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
                resp.getWriter().write("Failed to process payload.");
                return;
            }

            // Respond with the sanitized 'name' value
            resp.setContentType("text/plain; charset=UTF-8");
            resp.setStatus(HttpServletResponse.SC_OK);
            resp.getWriter().write(nameValue);
        }

        /**
         * Basic HTML encoding to prevent XSS.
         * In production, use a robust encoding library (e.g., OWASP Java Encoder).
         */
        private String encodeForHtml(String input) {
            if (input == null) return "";
            return input.replace("&", "&amp;")
                        .replace("<", "&lt;")
                        .replace(">", "&gt;")
                        .replace("\"", "&quot;")
                        .replace("'", "&#x27;");
        }
    }
}
