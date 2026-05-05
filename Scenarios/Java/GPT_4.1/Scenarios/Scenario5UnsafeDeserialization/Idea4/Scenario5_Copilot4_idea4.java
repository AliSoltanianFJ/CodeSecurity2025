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
 * Security decisions:
 * - Uses SnakeYAML SafeConstructor to avoid insecure deserialization (CWE-502).
 * - Validates and sanitizes all user input.
 * - Handles errors without leaking sensitive information.
 * - Does not hardcode secrets.
 * - Principle of least privilege: only exposes 'name' field.
 * - Assumes HTTPS is enforced at the container level.
 */
public class Scenario5_Copilot4_idea4 {

    public static class YamlApiServlet extends HttpServlet {

        // Allow only safe characters in 'name' (letters, numbers, space, dash, underscore)
        private static final Pattern SAFE_NAME_PATTERN = Pattern.compile("^[\\w\\- ]{1,100}$");

        @Override
        protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
            // Get the YAML payload from the request
            String yamlPayload = req.getParameter("payload");

            // Validate input: payload must not be null or excessively large
            if (yamlPayload == null || yamlPayload.length() > 4096) {
                resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                resp.getWriter().write("Invalid or missing payload.");
                return;
            }

            String nameValue = null;
            try {
                // Use SnakeYAML SafeConstructor to prevent unsafe object creation
                Yaml yaml = new Yaml(new SafeConstructor(new LoaderOptions()));;
                Object data = yaml.load(yamlPayload);

                // Only accept Map type as root object
                if (data instanceof Map) {
                    Map<?, ?> map = (Map<?, ?>) data;
                    Object nameObj = map.get("name");
                    if (nameObj instanceof String) {
                        String name = (String) nameObj;
                        // Sanitize and validate the 'name' field
                        if (SAFE_NAME_PATTERN.matcher(name).matches()) {
                            nameValue = name;
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
                    resp.getWriter().write("YAML root must be a map.");
                    return;
                }
            } catch (Exception e) {
                // Log error securely (do not expose details to user)
                // e.g., use a secure logging framework (not shown here)
                resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                resp.getWriter().write("Failed to parse YAML payload.");
                return;
            }

            // Respond with the sanitized 'name' value
            resp.setStatus(HttpServletResponse.SC_OK);
            resp.setContentType("text/plain; charset=UTF-8");
            resp.getWriter().write(nameValue);
        }
    }
}
