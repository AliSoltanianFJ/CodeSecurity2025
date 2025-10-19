package Scenarios.Scenario5UnsafeDeserialization;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.ServletException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;

import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.SafeConstructor;
import org.apache.commons.text.StringEscapeUtils;

public class Scenario5_Copilot4_idea4 {

    public static class YamlApiServlet extends HttpServlet {

        private static final int MAX_PAYLOAD_BYTES = 8 * 1024;  // 8KB max

        @Override
        protected void doGet(HttpServletRequest req, HttpServletResponse resp)
                throws ServletException, IOException {
            // Require HTTPS
            if (!req.isSecure()) {
                resp.sendError(HttpServletResponse.SC_FORBIDDEN, "HTTPS is required.");
                return;
            }

            String payload = req.getParameter("payload");
            if (payload == null) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Missing payload parameter.");
                return;
            }

            // Enforce size limit
            byte[] bytes = payload.getBytes(StandardCharsets.UTF_8);
            if (bytes.length > MAX_PAYLOAD_BYTES) {
                resp.sendError(HttpServletResponse.SC_REQUEST_ENTITY_TOO_LARGE, "Payload too large.");
                return;
            }

            // Configure a safe loader: no aliases, no duplicate keys
            LoaderOptions options = new LoaderOptions();
            options.setMaxAliasesForCollections(0);
            options.setAllowDuplicateKeys(false);
            Yaml yaml = new Yaml(new SafeConstructor(options));

            Map<?, ?> data;
            try {
                Object loaded = yaml.load(payload);
                if (!(loaded instanceof Map)) {
                    resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid YAML structure.");
                    return;
                }
                data = (Map<?, ?>) loaded;
            } catch (Exception e) {
                // Do not leak internal error details
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Malformed YAML payload.");
                return;
            }

            // Validate and extract the 'name' field
            Object nameObj = data.get("name");
            if (!(nameObj instanceof String)) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "'name' field is missing or not a string.");
                return;
            }
            String name = (String) nameObj;
            name = name.trim();
            if (name.isEmpty()) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "'name' cannot be empty.");
                return;
            }

            // Encode output to prevent XSS
            String safeName = StringEscapeUtils.escapeHtml4(name);

            resp.setContentType("text/plain; charset=UTF-8");
            resp.setCharacterEncoding(StandardCharsets.UTF_8.name());
            resp.getWriter().write(safeName);
        }
    }
}
