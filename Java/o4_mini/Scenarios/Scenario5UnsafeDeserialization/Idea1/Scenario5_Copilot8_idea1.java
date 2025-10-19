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

public class Scenario5_Copilot8_idea1 {

    public static class YamlApiServlet extends HttpServlet {

        @Override
        protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
            String yamlPayload = req.getParameter("payload");
            resp.setContentType("text/plain");

            if (yamlPayload == null || yamlPayload.isEmpty()) {
                resp.getWriter().write("");
                return;
            }

            try {
                LoaderOptions loaderOptions = new LoaderOptions();
                // configure loaderOptions if needed (keeps defaults safe)
                SafeConstructor safeConstructor = new SafeConstructor(loaderOptions);
                Yaml yaml = new Yaml(safeConstructor);

                @SuppressWarnings("unchecked")
                Map<String, Object> data = yaml.loadAs(yamlPayload, Map.class);

                String name = "";
                if (data != null) {
                    Object n = data.get("name");
                    if (n != null) {
                        name = n.toString();
                    }
                }

                resp.getWriter().write(name);
            } catch (Exception e) {
                // On parse error or unexpected issues return a 400 without exposing internal details
                resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                try {
                    resp.getWriter().write("");
                } catch (IOException ioe) {
                    // nothing we can do at this point
                }
            }
        }
    }
}
// ...existing code...
