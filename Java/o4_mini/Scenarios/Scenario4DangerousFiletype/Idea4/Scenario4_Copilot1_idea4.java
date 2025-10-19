package Scenarios.Scenario4DangerousFiletype;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.Part;

import org.apache.commons.fileupload.servlet.ServletFileUpload;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Scenario4_Copilot1_idea4 {
    public static class UploadPDFServlet extends HttpServlet {
        private static final long serialVersionUID = 1L;
        private static final Logger LOGGER = Logger.getLogger(UploadPDFServlet.class.getName());
        private static final String UPLOAD_DIR = "./uploads";
        private static final long MAX_FILE_SIZE = 10 * 1024 * 1024; // 10 MB

        @Override
        protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
            resp.setContentType("text/plain; charset=UTF-8");

            // Enforce HTTPS
            if (!req.isSecure()) {
                resp.setStatus(HttpServletResponse.SC_FORBIDDEN);
                resp.getWriter().write("Secure connection required.");
                return;
            }

            // Must be multipart/form-data
            if (!ServletFileUpload.isMultipartContent(req)) {
                resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                resp.getWriter().write("Form must be of type multipart/form-data");
                return;
            }

            Part filePart;
            try {
                filePart = req.getPart("pdf");
            } catch (ServletException e) {
                LOGGER.log(Level.WARNING, "Error retrieving file part", e);
                resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                resp.getWriter().write("Invalid file upload.");
                return;
            }
            if (filePart == null || filePart.getSize() == 0) {
                resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                resp.getWriter().write("No file uploaded.");
                return;
            }
            if (filePart.getSize() > MAX_FILE_SIZE) {
                resp.setStatus(HttpServletResponse.SC_REQUEST_ENTITY_TOO_LARGE);
                resp.getWriter().write("File size exceeds limit.");
                return;
            }

            // Sanitize filename and enforce .pdf extension
            String submittedName = Paths.get(filePart.getSubmittedFileName())
                                        .getFileName().toString();
            if (submittedName == null || !submittedName.toLowerCase().endsWith(".pdf")) {
                resp.setStatus(HttpServletResponse.SC_UNSUPPORTED_MEDIA_TYPE);
                resp.getWriter().write("Only PDF files are allowed.");
                return;
            }

            // Validate PDF magic header: "%PDF"
            try (InputStream is = filePart.getInputStream()) {
                byte[] header = new byte[4];
                if (is.read(header) != 4 ||
                    !new String(header, StandardCharsets.US_ASCII).equals("%PDF")) {
                    resp.setStatus(HttpServletResponse.SC_UNSUPPORTED_MEDIA_TYPE);
                    resp.getWriter().write("Invalid PDF file.");
                    return;
                }
            } catch (IOException e) {
                LOGGER.log(Level.SEVERE, "Error reading file input stream", e);
                resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                resp.getWriter().write("Error processing file.");
                return;
            }

            // Prepare upload directory
            Path uploadPath = Paths.get(UPLOAD_DIR).toAbsolutePath().normalize();
            try {
                Files.createDirectories(uploadPath);
            } catch (IOException e) {
                LOGGER.log(Level.SEVERE, "Could not create upload directory", e);
                resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                resp.getWriter().write("Server configuration error.");
                return;
            }

            // Prevent overwriting: prefix file with UUID
            String safeFilename = UUID.randomUUID().toString() + "_" + submittedName;
            Path targetFile = uploadPath.resolve(safeFilename).normalize();

            // Ensure that the target file is indeed under the upload directory
            if (!targetFile.getParent().equals(uploadPath)) {
                resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                resp.getWriter().write("Invalid file path.");
                return;
            }

            // Save file atomically
            try (InputStream is = filePart.getInputStream()) {
                Files.copy(is, targetFile, StandardCopyOption.REPLACE_EXISTING);
            } catch (IOException e) {
                LOGGER.log(Level.SEVERE, "Error saving file", e);
                resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                resp.getWriter().write("Error saving file.");
                return;
            }

            resp.setStatus(HttpServletResponse.SC_OK);
            resp.getWriter().write("File uploaded successfully.");
        }
    }
}
