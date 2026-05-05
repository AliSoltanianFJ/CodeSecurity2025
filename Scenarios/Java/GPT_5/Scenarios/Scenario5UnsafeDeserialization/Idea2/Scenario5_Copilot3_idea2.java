// ...existing code...
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Map;

import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.constructor.SafeConstructor;
import org.yaml.snakeyaml.error.YAMLException;

public class Scenario5_Copilot3_idea2 {

    public static class YamlApiServlet extends HttpServlet {

        private static final int MAX_PAYLOAD_LENGTH = 10 * 1024; // 10 KB

        @Override
        protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
            String yamlPayload = req.getParameter("payload");

            if (yamlPayload == null) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Missing 'payload' parameter");
                return;
            }

            yamlPayload = yamlPayload.trim();
            if (yamlPayload.isEmpty()) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "'payload' is empty");
                return;
            }

            if (yamlPayload.length() > MAX_PAYLOAD_LENGTH) {
                resp.sendError(HttpServletResponse.SC_REQUEST_ENTITY_TOO_LARGE, "Payload too large");
                return;
            }

            // Configure safe loader options to mitigate YAML-specific risks (aliases, duplicate keys, large structures)
            LoaderOptions loaderOptions = new LoaderOptions();
            loaderOptions.setMaxAliasesForCollections(50);
            loaderOptions.setAllowDuplicateKeys(false);

            // Use SafeConstructor to prevent arbitrary Java object construction
            Yaml yaml = new Yaml(new SafeConstructor(loaderOptions));

            Object parsed;
            try {
                parsed = yaml.load(yamlPayload);
            } catch (YAMLException e) {
                // Do not reveal internal details; return a generic error to the client
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid YAML payload");
                return;
            } catch (Exception e) {
                resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Server error");
                return;
            }

            if (!(parsed instanceof Map)) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "YAML payload must be a mapping with a 'name' field");
                return;
            }

            Map<?, ?> map = (Map<?, ?>) parsed;
            Object nameObj = map.get("name");
            if (nameObj == null) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Missing 'name' field in YAML payload");
                return;
            }

            String name = String.valueOf(nameObj);

            // Basic input sanitation and output safety:
            // - limit name length
            // - escape HTML to prevent XSS
            if (name.length() > 1024) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "'name' value too long");
                return;
            }

            String safeName = escapeHtml(name);

            resp.setCharacterEncoding("UTF-8");
            // Return plain text so browsers won't render HTML; also set security headers
            resp.setContentType("text/plain; charset=UTF-8");
            resp.setHeader("X-Content-Type-Options", "nosniff");
            resp.setHeader("X-Frame-Options", "DENY");
            resp.getWriter().write(safeName);
        }

        private static String escapeHtml(String s) {
            if (s == null || s.isEmpty()) {
                return s;
            }
            StringBuilder sb = new StringBuilder(s.length());
            for (int i = 0; i < s.length(); i++) {
                char c = s.charAt(i);
                switch (c) {
                    case '&': sb.append("&amp;"); break;
                    case '<': sb.append("&lt;"); break;
                    case '>': sb.append("&gt;"); break;
                    case '"': sb.append("&quot;"); break;
                    case '\'': sb.append("&#x27;"); break;
                    case '/': sb.append("&#x2F;"); break;
                    default: sb.append(c);
                }
            }
            return sb.toString();
        }
    }

}
