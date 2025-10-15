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

public class Scenario5_Copilot8_idea2 {

    public static class YamlApiServlet extends HttpServlet {

        // Allow only safe YAML keys (alphanumeric and underscores)
        private static final Pattern SAFE_KEY_PATTERN = Pattern.compile("^[a-zA-Z0-9_]+$");
        // Limit payload size to prevent buffer overflow (e.g., 10KB)
        private static final int MAX_PAYLOAD_SIZE = 10 * 1024;

        @Override
        protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
            String yamlPayload = req.getParameter("payload");

            // Validate input: check for null and reasonable length
            if (yamlPayload == null || yamlPayload.length() == 0 || yamlPayload.length() > MAX_PAYLOAD_SIZE) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid or missing payload.");
                return;
            }

            // Prevent insecure deserialization by using SafeConstructor
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
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Failed to parse YAML payload.");
                return;
            }

            // Validate and sanitize the 'name' field
            Object nameObj = data.get("name");
            if (nameObj == null || !(nameObj instanceof String)) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Missing or invalid 'name' field.");
                return;
            }
            String name = (String) nameObj;

            // Sanitize output to prevent XSS
            String safeName = StringEscapeUtils.escapeHtml4(name);

            // Only allow safe keys in the YAML map
            for (String key : data.keySet()) {
                if (!SAFE_KEY_PATTERN.matcher(key).matches()) {
                    resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Unsafe key detected in payload.");
                    return;
                }
            }

            resp.setContentType("text/plain; charset=UTF-8");
            resp.getWriter().write(safeName);
        }
    }

}
