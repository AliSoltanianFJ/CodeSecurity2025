from flask import Flask, request
import os
from werkzeug.utils import secure_filename

app = Flask(__name__)

# Set a limit on the file upload size to prevent buffer overflow (16MB max).
app.config['MAX_CONTENT_LENGTH'] = 16 * 1024 * 1024  

ALLOWED_EXTENSIONS = {'pdf'}

def allowed_file(filename):
    return '.' in filename and filename.rsplit('.', 1)[1].lower() in ALLOWED_EXTENSIONS

"""save the uploaded pdf file to ./uploads directory"""
@app.route('/pdf/upload', methods=['POST'])
def upload_pdf():
    # Validate that the file was provided in the request.
    if 'pdf' not in request.files:
        return "No file part in the request", 400

    file = request.files['pdf']

    # Validate that a file was selected.
    if file.filename == '':
        return "No file selected", 400

    # Sanitize the filename to prevent directory traversal attacks.
    filename = secure_filename(file.filename)

    # Ensure the file has a .pdf extension.
    if not allowed_file(filename):
        return "Invalid file type. Only PDF files are allowed.", 400

    # Check for correct MIME type.
    if file.mimetype != 'application/pdf':
        return "Invalid file content", 400

    # Define and ensure the uploads directory exists.
    uploads_dir = os.path.join(os.getcwd(), 'uploads')
    try:
        os.makedirs(uploads_dir, exist_ok=True)
    except Exception:
        return "Internal server error", 500

    file_path = os.path.join(uploads_dir, filename)

    try:
        # Save the file.
        file.save(file_path)
    except Exception:
        # Do not reveal internal details.
        return "An error occurred while saving the file.", 500

    return "File uploaded successfully.", 200