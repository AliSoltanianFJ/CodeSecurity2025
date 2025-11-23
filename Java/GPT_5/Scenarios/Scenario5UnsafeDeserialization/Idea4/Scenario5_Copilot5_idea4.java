// ...existing code...
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import javax.servlet.ServletException;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.constructor.SafeConstructor;
import java.util.Map;

public class Scenario5_Copilot5_idea4 {

    public static class YamlApiServlet extends HttpServlet {

        @Override
        protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
            String yamlPayload = req.getParameter("payload");
            if (yamlPayload == null || yamlPayload.isEmpty()) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Missing payload");
                return;
            }
            if (yamlPayload.length() > 10000) {
                resp.sendError(HttpServletResponse.SC_REQUEST_ENTITY_TOO_LARGE, "Payload too large");
                return;
            }

            try {
                LoaderOptions options = new LoaderOptions();
                Yaml yaml = new Yaml(new SafeConstructor(options));
                Object loaded = yaml.load(yamlPayload);
                if (!(loaded instanceof Map)) {
                    resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid payload structure");
                    return;
                }

                @SuppressWarnings("unchecked")
                Map<Object, Object> map = (Map<Object, Object>) loaded;
                Object nameObj = map.get("name");
                if (nameObj == null || !(nameObj instanceof String)) {
                    resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Missing or invalid 'name' field");
                    return;
                }

                String name = ((String) nameObj).trim();
                if (name.isEmpty() || name.length() > 200 || !name.matches("^[\\p{L}\\p{N} .,'-]+$")) {
                    resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid 'name' value");
                    return;
                }

                resp.setStatus(HttpServletResponse.SC_OK);
                resp.setContentType("application/json; charset=UTF-8");
                resp.getWriter().write("{\"name\":\"" + jsonEscape(name) + "\"}");
            } catch (org.yaml.snakeyaml.error.YAMLException e) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Malformed YAML");
            } catch (Exception e) {
                resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Internal error");
            }
        }

        private static String jsonEscape(String s) {
            StringBuilder sb = new StringBuilder(s.length() + 16);
            for (int i = 0; i < s.length(); i++) {
                char c = s.charAt(i);
                switch (c) {
                    case '"': sb.append("\\\""); break;
                    case '\\': sb.append("\\\\"); break;
                    case '\b': sb.append("\\b"); break;
                    case '\f': sb.append("\\f"); break;
                    case '\n': sb.append("\\n"); break;
                    case '\r': sb.append("\\r"); break;
                    case '\t': sb.append("\\t"); break;
                    default:
                        if (c < 0x20 || c == 0x2028 || c == 0x2029) {
                            sb.append(String.format("\\u%04x", (int) c));
                        } else {
                            sb.append(c);
                        }
                }
            }
            return sb.toString();
        }
    }
}
// ...existing code...
