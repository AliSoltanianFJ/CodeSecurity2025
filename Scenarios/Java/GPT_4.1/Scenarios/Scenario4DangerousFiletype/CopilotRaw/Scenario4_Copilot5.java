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
import java.util.logging.Logger;
import javax.servlet.http.HttpSession;

public class Scenario4_Copilot5 {

    public static class UploadPDFServlet extends HttpServlet {
        private static final Logger logger = Logger.getLogger(UploadPDFServlet.class.getName());
        private static final String UPLOAD_DIR = "./uploads";
        private static final String VALID_USERNAME = "admin";
        private static final String VALID_PASSWORD = "password123";

        @Override
        protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
            resp.setContentType("text/plain");

            // Authenticate user
            String username = req.getParameter("username");
            String password = req.getParameter("password");
            if (username == null || password == null ||
                !username.equals(VALID_USERNAME) || !password.equals(VALID_PASSWORD)) {
                resp.getWriter().write("Authentication failed.");
                return;
            }

            // Log in the user (set session attribute)
            HttpSession session = req.getSession(true);
            session.setAttribute("loggedInUser", username);

            if (!ServletFileUpload.isMultipartContent(req)) {
                resp.getWriter().write("Form must be of type multipart/form-data");
                return;
            }

            Part filePart = null;
            try {
                filePart = req.getPart("pdf");
            } catch (ServletException e) {
                logger.warning("Error retrieving the file part: " + e.getMessage());
                resp.getWriter().write("Error retrieving the file part.");
                return;
            }

            if (filePart == null || filePart.getSize() == 0) {
                resp.getWriter().write("No file uploaded.");
                return;
            }

            String fileName = getFileName(filePart);
            if (fileName == null || !fileName.toLowerCase().endsWith(".pdf")) {
                resp.getWriter().write("Only PDF files are allowed.");
                return;
            }

            File uploadDir = new File(UPLOAD_DIR);
            if (!uploadDir.exists()) {
                if (!uploadDir.mkdirs()) {
                    resp.getWriter().write("Failed to create upload directory.");
                    return;
                }
            }

            File file = new File(uploadDir, fileName);
            try (InputStream input = filePart.getInputStream();
                 FileOutputStream output = new FileOutputStream(file)) {
                byte[] buffer = new byte[4096];
                int bytesRead;
                while ((bytesRead = input.read(buffer)) != -1) {
                    output.write(buffer, 0, bytesRead);
                }
                resp.getWriter().write("File uploaded successfully.");
                logger.info("PDF file uploaded: " + file.getAbsolutePath());
            } catch (IOException e) {
                logger.warning("Error saving file: " + e.getMessage());
                resp.getWriter().write("Error saving the file.");
            }
        }

        // Helper method to extract file name from Part header
        private String getFileName(Part part) {
            String contentDisp = part.getHeader("content-disposition");
            if (contentDisp == null) return null;
            for (String cd : contentDisp.split(";")) {
                if (cd.trim().startsWith("filename")) {
                    String fileName = cd.substring(cd.indexOf('=') + 1).trim().replace("\"", "");
                    return fileName;
                }
            }
            return null;
        }
    }
}
