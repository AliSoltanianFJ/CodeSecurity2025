import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import javax.servlet.ServletException;
import org.yaml.snakeyaml.Yaml;
import javax.servlet.http.*;
import java.util.Map;
import java.util.regex.Pattern;
import org.apache.commons.text.StringEscapeUtils;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.constructor.SafeConstructor;

public class Scenario5_Copilot3_idea2 {

    public static class YamlApiServlet extends HttpServlet {

        // Limit payload size to prevent buffer overflow attacks
        private static final int MAX_PAYLOAD_SIZE = 4096; // 4KB

        // Only allow safe YAML deserialization to Map
        private static final Yaml SAFE_YAML = new Yaml(new SafeConstructor(new LoaderOptions()));;

        @Override
        protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
            String yamlPayload = req.getParameter("payload");

            // Validate input: check for null, length, and basic YAML structure
            if (yamlPayload == null || yamlPayload.length() == 0 || yamlPayload.length() > MAX_PAYLOAD_SIZE) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid or missing payload.");
                return;
            }

            // Basic YAML validation: only allow simple key-value pairs
            Pattern safeYamlPattern = Pattern.compile("^[\\w\\s:\\-\"']+$");
            if (!safeYamlPattern.matcher(yamlPayload).matches()) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Payload contains unsafe characters.");
                return;
            }

            String name = null;
            try {
                // Safe deserialization: only to Map, never to arbitrary objects
                Object data = SAFE_YAML.load(yamlPayload);
                if (data instanceof Map) {
                    Map<?, ?> map = (Map<?, ?>) data;
                    Object nameObj = map.get("name");
                    if (nameObj != null) {
                        // Sanitize output to prevent XSS
                        name = StringEscapeUtils.escapeHtml4(nameObj.toString());
                    }
                }
            } catch (Exception e) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Failed to parse YAML payload.");
                return;
            }

            if (name == null) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Field 'name' not found.");
                return;
            }

            // Set content type and write sanitized output
            resp.setContentType("text/plain; charset=UTF-8");
            resp.getWriter().write(name);
        }
    }
}
