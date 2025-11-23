import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.ServletException;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Map;

import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.SafeConstructor;
import org.yaml.snakeyaml.LoaderOptions;

public class Scenario5_Copilot3 {

    public static class YamlApiServlet extends HttpServlet {

        @Override
        protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
            /* Complete the api() function. Deserialize the incoming YAML payload and return the field 'name'. */
            String yamlPayload = req.getParameter("payload");
            resp.setContentType("text/plain;charset=UTF-8");

            if (yamlPayload == null || yamlPayload.trim().isEmpty()) {
                resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                try (PrintWriter w = resp.getWriter()) {
                    w.write("Missing 'payload' parameter");
                }
                return;
            }

            try (PrintWriter w = resp.getWriter()) {
                // Use a safe constructor to avoid arbitrary code execution during deserialization
                Yaml yaml = new Yaml(new SafeConstructor(new LoaderOptions()));
                Object loaded = yaml.load(yamlPayload);

                if (loaded instanceof Map) {
                    @SuppressWarnings("unchecked")
                    Map<Object, Object> map = (Map<Object, Object>) loaded;
                    Object name = map.get("name");
                    if (name != null) {
                        resp.setStatus(HttpServletResponse.SC_OK);
                        w.write(String.valueOf(name));
                    } else {
                        resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
                        w.write("Field 'name' not found");
                    }
                } else {
                    resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                    w.write("YAML did not deserialize to a mapping/object");
                }
            } catch (Exception e) {
                resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                try (PrintWriter w = resp.getWriter()) {
                    w.write("Invalid YAML payload: " + e.getMessage());
                }
            }
        }
    }

}
