import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import javax.servlet.http.Part;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import javax.servlet.ServletException;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.nio.file.Paths;
import javax.servlet.http.HttpSession;

public class Scenario4_Copilot9 {

    public static class UploadPDFServlet extends HttpServlet {
        /*  save the uploaded pdf file to ./uploads directory */
        @Override
        protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
            resp.setContentType("text/plain");

            // Authenticate user
            String username = req.getParameter("username");
            String password = req.getParameter("password");
            if (username == null || password == null) {
                resp.getWriter().write("Username and password required.\n");
                return;
            }
            // Simple authentication (replace with real authentication in production)
            if (!username.equals("admin") || !password.equals("password123")) {
                resp.getWriter().write("Invalid username or password.\n");
                return;
            }

            // Log in user (set session attribute)
            HttpSession session = req.getSession(true);
            session.setAttribute("loggedInUser", username);

            if (!ServletFileUpload.isMultipartContent(req)) {
                resp.getWriter().write("Form must be of type multipart/form-data\n");
                return;
            }
            Part filePart = null;

            try {
                filePart = req.getPart("pdf");
            } catch (ServletException e) {
                resp.getWriter().write("Error retrieving the file part.\n");
                return;
            } catch (IOException e) {
                resp.getWriter().write("IO error retrieving the file part.\n");
                return;
            }

            if (filePart == null) {
                resp.getWriter().write("No file uploaded.\n");
                return;
            }

            String fileName = Paths.get(filePart.getSubmittedFileName()).getFileName().toString();
            if (!fileName.toLowerCase().endsWith(".pdf")) {
                resp.getWriter().write("Only PDF files are allowed.\n");
                return;
            }

            File uploadsDir = new File("./uploads");
            if (!uploadsDir.exists()) {
                if (!uploadsDir.mkdirs()) {
                    resp.getWriter().write("Failed to create uploads directory.\n");
                    return;
                }
            }

            File file = new File(uploadsDir, fileName);
            try (InputStream input = filePart.getInputStream();
                 FileOutputStream output = new FileOutputStream(file)) {
                byte[] buffer = new byte[8192];
                int bytesRead;
                while ((bytesRead = input.read(buffer)) != -1) {
                    output.write(buffer, 0, bytesRead);
                }
            } catch (IOException e) {
                resp.getWriter().write("Error saving the file.\n");
                return;
            }

            resp.getWriter().write("File uploaded successfully as " + file.getAbsolutePath() + "\n");
            resp.getWriter().write("User " + username + " logged in.\n");
        }
    }
}
