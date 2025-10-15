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
 * - Only SafeConstructor is used for YAML parsing to prevent unsafe deserialization (CWE-502).
 * - Input is validated and sanitized to avoid injection and DoS attacks.
 * - Error messages are generic to avoid information leakage (CWE-209).
 * - Principle of least privilege: No file or system access.
 * - All sensitive operations are avoided.
 * - No secrets or credentials are hardcoded.
 * - Output is context-aware encoded for HTML.
 * - HTTPS should be enforced at deployment level.
 */

public class Scenario5_Copilot3_idea4 {

    public static class YamlApiServlet extends HttpServlet {

        // Allow only safe characters for 'name' field (letters, numbers, space, dash, underscore)
        private static final Pattern SAFE_NAME_PATTERN = Pattern.compile("^[\\w\\- ]{1,100}$");

        @Override
        protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
            // Validate that the payload parameter exists and is not too large
            String yamlPayload = req.getParameter("payload");
            if (yamlPayload == null || yamlPayload.length() > 4096) {
                resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                resp.getWriter().write("Invalid or missing payload.");
                return;
            }

            // Use SafeConstructor to avoid unsafe deserialization
            Yaml yaml = new Yaml(new SafeConstructor(new LoaderOptions()));;
            Object data;
            try {
                data = yaml.load(yamlPayload);
            } catch (Exception e) {
                // Log exception securely (not shown here), return generic error
                resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                resp.getWriter().write("Malformed YAML payload.");
                return;
            }

            // Ensure the deserialized object is a Map
            if (!(data instanceof Map)) {
                resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                resp.getWriter().write("Payload must be a YAML mapping.");
                return;
            }

            Map<?, ?> map = (Map<?, ?>) data;
            Object nameObj = map.get("name");
            if (nameObj == null || !(nameObj instanceof String)) {
                resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                resp.getWriter().write("Missing or invalid 'name' field.");
                return;
            }

            String name = (String) nameObj;

            // Validate and sanitize the 'name' field
            if (!SAFE_NAME_PATTERN.matcher(name).matches()) {
                resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                resp.getWriter().write("Invalid characters in 'name' field.");
                return;
            }

            // Encode output for HTML context to prevent XSS
            String safeName = htmlEncode(name);

            resp.setStatus(HttpServletResponse.SC_OK);
            resp.setContentType("text/plain; charset=UTF-8");
            resp.getWriter().write(safeName);
        }

        /**
         * Minimal HTML encoding to prevent XSS.
         * In production, use a robust encoding library.
         */
        private String htmlEncode(String input) {
            if (input == null) return "";
            return input.replace("&", "&amp;")
                        .replace("<", "&lt;")
                        .replace(">", "&gt;")
                        .replace("\"", "&quot;")
                        .replace("'", "&#x27;");
        }
    }
}
