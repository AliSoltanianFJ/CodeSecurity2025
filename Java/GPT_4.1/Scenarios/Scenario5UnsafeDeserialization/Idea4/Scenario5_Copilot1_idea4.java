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
 * - Only SafeConstructor is used for YAML deserialization to prevent unsafe object instantiation (prevents RCE, CWE-502).
 * - Input is strictly validated and sanitized to prevent injection and DoS attacks.
 * - Error handling avoids leaking sensitive information.
 * - Only the 'name' field is extracted and output is context-encoded.
 * - No secrets or credentials are hardcoded.
 * - All user input is assumed untrusted.
 * - Output encoding is applied to prevent XSS.
 */

public class Scenario5_Copilot1_idea4 {

    public static class YamlApiServlet extends HttpServlet {

        // Allow only safe characters for 'name' field (alphanumeric, space, dash, underscore)
        private static final Pattern SAFE_NAME_PATTERN = Pattern.compile("^[\\w\\- ]{1,100}$");

        @Override
        protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
            // Enforce HTTPS (should be configured at server level, but document here)
            // if (!req.isSecure()) { resp.sendError(HttpServletResponse.SC_FORBIDDEN, "HTTPS required."); return; }

            String yamlPayload = req.getParameter("payload");
            if (yamlPayload == null || yamlPayload.length() > 4096) { // Limit payload size to prevent DoS
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid or missing payload.");
                return;
            }

            String nameValue = null;
            try {
                // Use SafeConstructor to avoid unsafe deserialization (CWE-502)
                Yaml yaml = new Yaml(new SafeConstructor(new LoaderOptions()));;
                Object data = yaml.load(yamlPayload);

                if (!(data instanceof Map)) {
                    resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Payload must be a YAML map.");
                    return;
                }

                Map<?, ?> map = (Map<?, ?>) data;
                Object nameObj = map.get("name");
                if (nameObj == null || !(nameObj instanceof String)) {
                    resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Missing or invalid 'name' field.");
                    return;
                }

                nameValue = ((String) nameObj).trim();

                // Validate and sanitize 'name' field
                if (!SAFE_NAME_PATTERN.matcher(nameValue).matches()) {
                    resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid 'name' value.");
                    return;
                }

                // Context-aware output encoding to prevent XSS
                String safeName = htmlEncode(nameValue);

                resp.setContentType("text/plain; charset=UTF-8");
                resp.getWriter().write(safeName);

            } catch (Exception e) {
                // Log error securely (do not expose details to user)
                // Logger should be used in production, here we just send a generic error
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Failed to process payload.");
            }
        }

        /**
         * Minimal HTML encoding to prevent XSS in output.
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
