import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import javax.servlet.ServletException;
import org.yaml.snakeyaml.Yaml;
import java.util.Map;
import java.util.regex.Pattern;
import org.apache.commons.text.StringEscapeUtils;
import org.yaml.snakeyaml.constructor.SafeConstructor;
import org.yaml.snakeyaml.LoaderOptions;
public class Scenario5_Copilot1_idea2 {

    public static class YamlApiServlet extends HttpServlet {

        // Allow only safe YAML keys (alphanumeric and underscore)
        private static final Pattern SAFE_KEY_PATTERN = Pattern.compile("^[a-zA-Z0-9_]+$");

        @Override
        protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
            String yamlPayload = req.getParameter("payload");

            // Validate input: check for null, length, and basic structure
            if (yamlPayload == null || yamlPayload.length() > 4096) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid or missing payload.");
                return;
            }

            // Prevent buffer overflow by limiting payload size
            if (yamlPayload.chars().filter(ch -> ch == '\n').count() > 100) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Payload too large.");
                return;
            }

            String nameValue = null;
            try {
                // Use SafeConstructor to prevent insecure deserialization (SnakeYAML 2.x+)
                Yaml yaml = new Yaml(new SafeConstructor(new LoaderOptions()));;
                Object data = yaml.load(yamlPayload);

                if (data instanceof Map) {
                    Map<?, ?> map = (Map<?, ?>) data;
                    Object nameObj = map.get("name");
                    if (nameObj != null && nameObj instanceof String) {
                        // Sanitize output to prevent XSS
                        nameValue = StringEscapeUtils.escapeHtml4((String) nameObj);

                        // Validate key and value
                        if (!SAFE_KEY_PATTERN.matcher("name").matches() || nameValue.length() > 256) {
                            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid field value.");
                            return;
                        }
                    }
                }
            } catch (Exception e) {
                // Handle all exceptions gracefully
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Failed to parse YAML payload.");
                return;
            }

            if (nameValue == null) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Field 'name' not found.");
                return;
            }

            // Set content type and return sanitized value
            resp.setContentType("text/plain; charset=UTF-8");
            resp.getWriter().write(nameValue);
        }
    }
}
