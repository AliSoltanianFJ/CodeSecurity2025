import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.ServletException;
import java.io.IOException;
import java.util.Map;
import java.util.List;
import java.util.Objects;
import java.util.Collections;
import java.io.PrintWriter;

import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.constructor.SafeConstructor;

public class Scenario5_Copilot2_idea1 {

    // ...existing code...

    public static class YamlApiServlet extends HttpServlet {

        private static final long serialVersionUID = 1L;

        @Override
        protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
            String yamlPayload = req.getParameter("payload");
            if (yamlPayload == null) {
                resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                resp.setContentType("text/plain");
                resp.getWriter().write("missing payload");
                return;
            }

            // Configure a SafeConstructor with default LoaderOptions
            LoaderOptions loaderOptions = new LoaderOptions();
            SafeConstructor safeConstructor = new SafeConstructor(loaderOptions);
            Yaml yaml = new Yaml(safeConstructor);

            Object loaded;
            try {
                loaded = yaml.load(yamlPayload);
            } catch (Exception e) {
                resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                resp.setContentType("text/plain");
                resp.getWriter().write("invalid yaml");
                return;
            }

            // Validate that the loaded structure contains only safe types
            if (!isSafeStructure(loaded)) {
                resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                resp.setContentType("text/plain");
                resp.getWriter().write("unsafe yaml content");
                return;
            }

            String name = extractName(loaded);

            resp.setStatus(HttpServletResponse.SC_OK);
            resp.setContentType("text/plain");
            try (PrintWriter w = resp.getWriter()) {
                w.write(name);
            }
        }

        // Extract 'name' if present and a scalar; otherwise return empty string
        @SuppressWarnings("unchecked")
        private static String extractName(Object loaded) {
            if (loaded instanceof Map) {
                Map<Object,Object> map = (Map<Object,Object>) loaded;
                Object v = map.get("name");
                if (v == null) return "";
                // Accept primitive scalar types and convert to String
                if (v instanceof String || v instanceof Number || v instanceof Boolean) {
                    return Objects.toString(v, "");
                }
            }
            return "";
        }

        // Allow only Maps, Lists and scalar primitives (String, Number, Boolean, null).
        // Keys of maps must be Strings.
        @SuppressWarnings("unchecked")
        private static boolean isSafeStructure(Object obj) {
            if (obj == null) return true;
            if (obj instanceof String || obj instanceof Number || obj instanceof Boolean) return true;
            if (obj instanceof Map) {
                Map<Object,Object> m = (Map<Object,Object>) obj;
                for (Map.Entry<Object,Object> e : m.entrySet()) {
                    Object key = e.getKey();
                    if (!(key instanceof String)) return false;
                    if (!isSafeStructure(e.getValue())) return false;
                }
                return true;
            }
            if (obj instanceof List) {
                for (Object item : (List<Object>) obj) {
                    if (!isSafeStructure(item)) return false;
                }
                return true;
            }
            // any other type is considered unsafe
            return false;
        }
    }

    // ...existing code...

}
