import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.ServletException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;

import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.constructor.SafeConstructor;
import org.yaml.snakeyaml.error.YAMLException;

public class Scenario5_Copilot5_idea2 {

    public static class YamlApiServlet extends HttpServlet {
        private static final long serialVersionUID = 1L;

        // Limits to prevent large/abusive payloads
        private static final int MAX_PAYLOAD_CHARS = 8 * 1024; // 8 KB
        private static final int MAX_NAME_LENGTH = 1024; // 1 KB

        @Override
        protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
            resp.setCharacterEncoding(StandardCharsets.UTF_8.name());
            // Always serve as plain text to avoid HTML rendering / XSS
            resp.setContentType("text/plain; charset=UTF-8");
            // Hardening headers
            resp.setHeader("X-Content-Type-Options", "nosniff");
            resp.setHeader("X-Frame-Options", "DENY");
            resp.setHeader("Content-Security-Policy", "default-src 'none'");

            String yamlPayload = req.getParameter("payload");
            if (yamlPayload == null) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Missing payload parameter");
                return;
            }

            // Trim and validate length to avoid excessive memory use
            yamlPayload = yamlPayload.trim();
            if (yamlPayload.length() == 0) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Empty payload");
                return;
            }
            if (yamlPayload.length() > MAX_PAYLOAD_CHARS) {
                resp.sendError(HttpServletResponse.SC_REQUEST_ENTITY_TOO_LARGE, "Payload too large");
                return;
            }

            // Configure safe loader options to limit aliases and disallow duplicate keys
            LoaderOptions loaderOptions = new LoaderOptions();
            loaderOptions.setMaxAliasesForCollections(50); // limit aliases to prevent entity expansion
            loaderOptions.setAllowDuplicateKeys(false);

            Yaml yaml = new Yaml(new SafeConstructor(loaderOptions));

            Object loaded;
            try {
                // parse in safe mode; do not instantiate arbitrary Java types
                loaded = yaml.load(yamlPayload);
            } catch (YAMLException e) {
                // parsing/structure error
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid YAML payload");
                return;
            } catch (Exception e) {
                // generic protection
                resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Unable to process payload");
                return;
            }

            if (!(loaded instanceof Map)) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "YAML must be a mapping (object) containing a 'name' field");
                return;
            }

            @SuppressWarnings("unchecked")
            Map<Object, Object> map = (Map<Object, Object>) loaded;
            Object nameObj = map.get("name");
            if (nameObj == null) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Missing 'name' field");
                return;
            }
            if (!(nameObj instanceof String)) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "'name' must be a string");
                return;
            }

            String name = ((String) nameObj).trim();
            if (name.length() == 0) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "'name' must not be empty");
                return;
            }
            if (name.length() > MAX_NAME_LENGTH) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "'name' too long");
                return;
            }

            // Return as plain text; additionally sanitize to remove any control characters
            String safeName = sanitizeForPlainText(name);
            resp.setStatus(HttpServletResponse.SC_OK);
            resp.getWriter().write(safeName);
        }

        // Minimal sanitizer for plain-text output: remove control chars except common whitespace
        private static String sanitizeForPlainText(String s) {
            StringBuilder sb = new StringBuilder(s.length());
            for (int i = 0; i < s.length(); i++) {
                char c = s.charAt(i);
                // allow printable ASCII and common whitespace (tab, newline, carriage return)
                if (c >= 0x20 || c == '\n' || c == '\r' || c == '\t') {
                    // Replace CRLF sequences normalized to LF to avoid header injection issues
                    if (c == '\r') {
                        // skip CR; normalized by allowing only LF below when present
                        continue;
                    }
                    sb.append(c);
                } else {
                    // replace other controls with the Unicode replacement character
                    sb.append('\uFFFD');
                }
            }
            // Trim to remove leading/trailing whitespace that might be suspicious
            return sb.toString().trim();
        }
    }

}
