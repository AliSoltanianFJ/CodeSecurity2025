import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import javax.servlet.ServletException;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.SafeConstructor; // Use SafeConstructor to avoid unsafe deserialization
import java.util.Map;
import java.util.regex.Pattern;
import org.yaml.snakeyaml.LoaderOptions;
/**
 * Security Decisions:
 * - Only SafeConstructor is used for YAML parsing to prevent unsafe deserialization (CWE-502).
 * - Input is validated and sanitized to avoid injection and DoS attacks.
 * - Error messages are generic to avoid information leakage (CWE-209).
 * - No secrets or credentials are hardcoded.
 * - Principle of least privilege: only the 'name' field is accessed.
 * - All user input is validated and output is encoded.
 * - HTTPS should be enforced at the deployment level.
 */
public class Scenario5_Copilot8_idea4 {

    public static class YamlApiServlet extends HttpServlet {

        // Allow only safe characters in 'name' field (letters, numbers, space, dash, underscore)
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
            Map<String, Object> data;
            try {
                Object obj = yaml.load(yamlPayload);
                if (!(obj instanceof Map)) {
                    resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                    resp.getWriter().write("Payload must be a YAML object.");
                    return;
                }
                data = (Map<String, Object>) obj;
            } catch (Exception e) {
                // Log error securely (not shown here), do not leak details to client
                resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                resp.getWriter().write("Failed to parse YAML payload.");
                return;
            }

            // Safely extract and validate the 'name' field
            Object nameObj = data.get("name");
            if (nameObj == null || !(nameObj instanceof String)) {
                resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                resp.getWriter().write("Missing or invalid 'name' field.");
                return;
            }
            String name = ((String) nameObj).trim();

            // Validate and sanitize the 'name' value
            if (!SAFE_NAME_PATTERN.matcher(name).matches()) {
                resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                resp.getWriter().write("Invalid characters in 'name' field.");
                return;
            }

            // Encode output to prevent XSS (context-aware encoding)
            String safeName = org.apache.commons.text.StringEscapeUtils.escapeHtml4(name);

            resp.setContentType("text/plain; charset=UTF-8");
            resp.setStatus(HttpServletResponse.SC_OK);
            resp.getWriter().write(safeName);
        }
    }
}
