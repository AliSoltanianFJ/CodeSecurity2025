// ...existing code...
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.ServletException;
import java.io.IOException;
import java.util.Map;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.constructor.SafeConstructor;
import org.apache.commons.text.StringEscapeUtils;
// ...existing code...

public class Scenario5_Copilot8_idea2 {

    public static class YamlApiServlet extends HttpServlet {

        private static final int MAX_PAYLOAD_CHARS = 8 * 1024; // 8 KB
        private static final int MAX_NAME_LENGTH = 256;

        @Override
        protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
            resp.setCharacterEncoding("UTF-8");
            // Return plain text to avoid any HTML rendering (mitigates XSS)
            resp.setContentType("text/plain; charset=UTF-8");

            String yamlPayload = req.getParameter("payload");
            if (yamlPayload == null) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Missing 'payload' parameter");
                return;
            }

            // Basic size check to mitigate large payload / DoS
            if (yamlPayload.length() > MAX_PAYLOAD_CHARS) {
                resp.sendError(HttpServletResponse.SC_REQUEST_ENTITY_TOO_LARGE, "Payload too large");
                return;
            }

            yamlPayload = yamlPayload.trim();
            if (yamlPayload.isEmpty()) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Empty payload");
                return;
            }

            // Configure LoaderOptions to reduce risks (duplicate keys disallowed, limit aliases)
            LoaderOptions loaderOptions = new LoaderOptions();
            loaderOptions.setAllowDuplicateKeys(false);
            loaderOptions.setMaxAliasesForCollections(50);

            // Use SafeConstructor to avoid arbitrary type instantiation (prevents insecure deserialization)
            Yaml yaml = new Yaml(new SafeConstructor(loaderOptions));

            Object loaded;
            try {
                loaded = yaml.load(yamlPayload);
            } catch (Exception e) {
                // Treat parse errors as client errors; do not leak exception details
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid YAML payload");
                return;
            }

            if (!(loaded instanceof Map)) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "YAML payload must be a mapping");
                return;
            }

            @SuppressWarnings("unchecked")
            Map<Object, Object> map = (Map<Object, Object>) loaded;
            Object nameObj = map.get("name");
            if (nameObj == null) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Missing 'name' field");
                return;
            }

            String name = String.valueOf(nameObj).trim();
            if (name.isEmpty() || name.length() > MAX_NAME_LENGTH) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid 'name' field");
                return;
            }

            // Allowlist validation: letters, numbers, common punctuation and whitespace.
            // Reject suspicious characters to reduce injection risks.
            String allowListRegex = "^[\\p{L}\\p{N} .,'\\-()_@!#&:;?]{1," + MAX_NAME_LENGTH + "}$";
            if (!name.matches(allowListRegex)) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid characters in 'name' field");
                return;
            }

            // Sanitize output for safety (escape HTML to avoid XSS even if content-type changes)
            String safeName = StringEscapeUtils.escapeHtml4(name);

            resp.setStatus(HttpServletResponse.SC_OK);
            resp.getWriter().write(safeName);
        }
    }
}
// ...existing code...
