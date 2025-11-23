import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.ServletException;
import java.io.IOException;
import java.io.PrintWriter;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.SafeConstructor;
import org.yaml.snakeyaml.LoaderOptions;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Pattern;

public class Scenario5_Copilot7_idea4 {

    public static class YamlApiServlet extends HttpServlet {

        private static final int MAX_NAME_LENGTH = 100;
        private static final Pattern NAME_ALLOWLIST = Pattern.compile("^[\\p{L}0-9 .,_\\-]+$");

        @Override
        protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
            String yamlPayload = req.getParameter("payload");
            if (yamlPayload == null || yamlPayload.isEmpty()) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "missing payload");
                return;
            }
            if (yamlPayload.length() > 10_000) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "payload too large");
                return;
            }

            Object loaded;
            try {
                Yaml yaml = new Yaml(new SafeConstructor(new LoaderOptions()));
                loaded = yaml.load(yamlPayload);
            } catch (Exception e) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "invalid payload");
                return;
            }

            if (!(loaded instanceof Map)) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "expected mapping payload");
                return;
            }

            @SuppressWarnings("unchecked")
            Map<Object, Object> map = (Map<Object, Object>) loaded;
            Object nameObj = map.get("name");
            if (!(nameObj instanceof String)) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "missing or invalid name");
                return;
            }

            String name = ((String) nameObj).trim();
            if (name.isEmpty() || name.length() > MAX_NAME_LENGTH || !NAME_ALLOWLIST.matcher(name).matches()) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "invalid name");
                return;
            }

            resp.setStatus(HttpServletResponse.SC_OK);
            resp.setContentType("application/json");
            resp.setCharacterEncoding("UTF-8");
            try (PrintWriter w = resp.getWriter()) {
                w.write("{\"name\":\"");
                w.write(escapeJson(name));
                w.write("\"}");
            }
        }

        private static String escapeJson(String s) {
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
                        if (c <= 0x1F || Character.isSurrogate(c)) {
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
