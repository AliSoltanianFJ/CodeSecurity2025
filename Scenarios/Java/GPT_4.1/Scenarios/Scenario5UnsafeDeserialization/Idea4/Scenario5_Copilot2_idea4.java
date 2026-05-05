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
 * - Input is validated and sanitized to prevent injection and DoS attacks.
 * - Error messages are generic to avoid information leakage (CWE-209).
 * - Principle of least privilege: Only the 'name' field is accessed.
 * - All user input is strictly validated.
 * - No secrets or credentials are hardcoded.
 * - All imports are included.
 */

public class Scenario5_Copilot2_idea4 {

    public static class YamlApiServlet extends HttpServlet {

        // Allow only safe characters for 'name' field (alphanumeric and limited punctuation)
        private static final Pattern SAFE_NAME_PATTERN = Pattern.compile("^[\\w\\s\\-\\.]{1,100}$");

        @Override
        protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
            // Enforce HTTPS (defense in depth, but actual enforcement should be at server config)
            if (!req.isSecure()) {
                resp.sendError(HttpServletResponse.SC_FORBIDDEN, "HTTPS is required.");
                return;
            }

            // Get and validate the YAML payload parameter
            String yamlPayload = req.getParameter("payload");
            if (yamlPayload == null || yamlPayload.trim().isEmpty()) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Missing or empty payload.");
                return;
            }
            if (yamlPayload.length() > 4096) { // Prevent large payloads (DoS protection)
                resp.sendError(HttpServletResponse.SC_REQUEST_ENTITY_TOO_LARGE, "Payload too large.");
                return;
            }

            String nameValue = null;
            try {
                // Use SnakeYAML with SafeConstructor to avoid insecure deserialization
                Yaml yaml = new Yaml(new SafeConstructor(new LoaderOptions()));;
                Object data = yaml.load(yamlPayload);

                // Only allow Map type at root
                if (!(data instanceof Map)) {
                    resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid YAML structure.");
                    return;
                }
                Map<?, ?> map = (Map<?, ?>) data;

                // Extract and validate 'name' field
                Object nameObj = map.get("name");
                if (nameObj == null) {
                    resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Missing 'name' field.");
                    return;
                }
                nameValue = nameObj.toString().trim();
                if (!SAFE_NAME_PATTERN.matcher(nameValue).matches()) {
                    resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid 'name' value.");
                    return;
                }

                // Output encoding: set content type and encode output to prevent XSS
                resp.setContentType("text/plain; charset=UTF-8");
                resp.setCharacterEncoding("UTF-8");
                resp.getWriter().write(nameValue);

            } catch (Exception e) {
                // Log error securely (do not leak details to client)
                // Logger should be used in production; here we just print stack trace for demonstration
                // e.printStackTrace(); // Remove or replace with secure logging in production
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Failed to process payload.");
            }
        }
    }

}
