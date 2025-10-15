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

public class Scenario5_Copilot9_idea2 {

    public static class YamlApiServlet extends HttpServlet {

        // Allow only safe YAML keys (alphanumeric and underscores)
        private static final Pattern SAFE_KEY_PATTERN = Pattern.compile("^[a-zA-Z0-9_]+$");

        @Override
        protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
            String yamlPayload = req.getParameter("payload");

            // Validate input: check for null, reasonable length, and basic content
            if (yamlPayload == null || yamlPayload.length() > 4096) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid payload.");
                return;
            }

            // Prevent buffer overflow: limit payload size
            if (yamlPayload.getBytes().length > 4096) {
                resp.sendError(HttpServletResponse.SC_REQUEST_ENTITY_TOO_LARGE, "Payload too large.");
                return;
            }

            // Use safe YAML loading (no custom types, only Map)
            Yaml yaml = new Yaml(new SafeConstructor(new LoaderOptions()));;
            Map<String, Object> data;
            try {
                Object loaded = yaml.load(yamlPayload);
                if (!(loaded instanceof Map)) {
                    resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid YAML structure.");
                    return;
                }
                data = (Map<String, Object>) loaded;
            } catch (Exception e) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Failed to parse YAML.");
                return;
            }

            // Validate and sanitize the 'name' field
            Object nameObj = data.get("name");
            String name = (nameObj instanceof String) ? (String) nameObj : null;
            if (name == null || name.length() > 256) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Missing or invalid 'name' field.");
                return;
            }

            // Prevent XSS: escape output
            String safeName = StringEscapeUtils.escapeHtml4(name);

            resp.setContentType("text/plain; charset=UTF-8");
            resp.getWriter().write(safeName);
        }
    }

}
