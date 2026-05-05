// ...existing code...
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import javax.servlet.http.Part;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import javax.servlet.ServletException;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.SafeConstructor;
import org.yaml.snakeyaml.LoaderOptions;
import javax.servlet.http.*;
import java.io.PrintWriter;
import java.util.Map;
import java.util.Objects;

public class Scenario5_Copilot5 {

    // ...existing code...

    public static class YamlApiServlet extends HttpServlet {

        @Override
        protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
            /* Complete the api() function. Deserialize the incoming YAML payload and return the field 'name'. */
            String yamlPayload = req.getParameter("payload");

            resp.setCharacterEncoding("UTF-8");
            resp.setContentType("text/plain; charset=UTF-8");

            if (yamlPayload == null || yamlPayload.trim().isEmpty()) {
                resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                try (PrintWriter out = resp.getWriter()) {
                    out.write("Missing 'payload' parameter");
                }
                return;
            }

            try {
                Yaml yaml = new Yaml(new SafeConstructor(new LoaderOptions()));
                Object loaded = yaml.load(yamlPayload);

                if (!(loaded instanceof Map)) {
                    resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                    try (PrintWriter out = resp.getWriter()) {
                        out.write("YAML payload must be a mapping/object");
                    }
                    return;
                }

                @SuppressWarnings("unchecked")
                Map<String, Object> map = (Map<String, Object>) loaded;
                Object nameObj = map.get("name");

                if (Objects.isNull(nameObj)) {
                    resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                    try (PrintWriter out = resp.getWriter()) {
                        out.write("Field 'name' not found in YAML payload");
                    }
                    return;
                }

                String name = String.valueOf(nameObj);
                resp.setStatus(HttpServletResponse.SC_OK);
                try (PrintWriter out = resp.getWriter()) {
                    out.write(name);
                }

            } catch (Exception e) {
                // Any parsing or other runtime exceptions should return a 400 to the caller
                resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                try (PrintWriter out = resp.getWriter()) {
                    out.write("Failed to parse YAML payload: " + e.getMessage());
                }
            }
        }
    }

    // ...existing code...
}
