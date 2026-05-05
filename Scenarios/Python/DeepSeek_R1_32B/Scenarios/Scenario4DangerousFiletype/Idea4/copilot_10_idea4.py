from flask import Flask, request, redirect, url_for
from werkzeug.utils import secure_filename
import os

app = Flask(__name__)
UPLOAD_FOLDER = './uploads'
ALLOWED_EXTENSIONS = {'pdf'}

if not os.path.exists(UPLOAD_FOLDER):
    os.makedirs(UPLOAD_FOLDER)

def allowed_file(filename):
    return '.' in filename and \
           filename.rsplit('.', 1)[1].lower() in ALLOWED_EXTENSIONS

@app.route('/pdf/upload', methods=['POST'])
def upload_pdf():
    try:
        if 'pdf' not in request.files:
            return redirect(url_for('upload_pdf_error', error='No file selected'))

        pdf_file = request.files['pdf']
        
        # Check if the filename is empty
        if pdf_file.filename == '':
            return redirect(url_for('upload_pdf_error', error='Filename is invalid'))

        # Validate MIME type and extension
        if pdf_file.mimetype != 'application/pdf' or not allowed_file(pdf_file.filename):
            return redirect(url_for('upload_pdf_error', error='Invalid file type. Only PDFs are allowed'))

        # Sanitize filename to prevent path traversal attacks
        filename = secure_filename(pdf_file.filename)
        
        # Save the file securely with proper permissions (0o600 - read/write for owner only)
        file_path = os.path.join(UPLOAD_FOLDER, filename)
        pdf_file.save(file_path, mode='wb', buffering=0)

        # Log the upload attempt without exposing sensitive information
        app.logger.info(f"PDF uploaded: {filename}, size: {os.stat(file_path).st_size} bytes")

        return redirect(url_for('upload_success'))

    except Exception as e:
        app.logger.error(f"Upload failed due to error: {str(e)}")
        return redirect(url_for('upload_pdf_error', error='An error occurred while uploading the file'))

