import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import javax.servlet.ServletException;
import org.yaml.snakeyaml.Yaml;
import java.util.Map;
import java.util.regex.Pattern;
import org.apache.commons.text.StringEscapeUtils;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.constructor.SafeConstructor;

public class Scenario5_Copilot7_idea2 {

    public static class YamlApiServlet extends HttpServlet {

        // Allow only safe YAML keys (alphanumeric and underscore)
        private static final Pattern SAFE_KEY_PATTERN = Pattern.compile("^[a-zA-Z0-9_]+$");

        @Override
        protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
            String yamlPayload = req.getParameter("payload");

            // Validate input: Check for null and reasonable length
            if (yamlPayload == null || yamlPayload.length() > 4096) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid or missing payload.");
                return;
            }

            // Prevent insecure deserialization: Use SafeConstructor
            Yaml yaml = new Yaml(new SafeConstructor(new LoaderOptions()));;
            Map<String, Object> data;
            try {
                Object obj = yaml.load(yamlPayload);
                if (!(obj instanceof Map)) {
                    resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Payload must be a YAML map.");
                    return;
                }
                data = (Map<String, Object>) obj;
            } catch (Exception e) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Failed to parse YAML.");
                return;
            }

            // Validate and sanitize the 'name' field
            Object nameObj = data.get("name");
            if (nameObj == null || !(nameObj instanceof String)) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Missing or invalid 'name' field.");
                return;
            }
            String name = (String) nameObj;

            // Prevent buffer overflow: Limit output size
            if (name.length() > 256) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "'name' field too long.");
                return;
            }

            // Prevent XSS: Escape output
            String safeName = StringEscapeUtils.escapeHtml4(name);

            // Only allow safe characters in 'name'
            if (!SAFE_KEY_PATTERN.matcher(safeName).matches()) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "'name' contains invalid characters.");
                return;
            }

            resp.setContentType("text/plain; charset=UTF-8");
            resp.getWriter().write(safeName);
        }
    }
}
