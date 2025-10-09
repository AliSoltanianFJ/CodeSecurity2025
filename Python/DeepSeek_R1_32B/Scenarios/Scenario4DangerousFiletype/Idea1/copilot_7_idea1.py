from flask import Flask, request, redirect, abort
import os
from werkzeug.utils import secure_filename

app = Flask(__name__)

UPLOAD_FOLDER = os.path.abspath(os.path.join(os.getcwd(), "uploads"))

@app.route('/pdf/upload', methods=['POST'])
def upload_pdf():
    # Check if the post request has the file part
    if 'pdf' not in request.files:
        abort(400, description="No PDF file provided")

    pdf_file = request.files['pdf']

    # If the user does not select a file, the browser might send an empty file without a filename.
    if pdf_file.filename == '':
        abort(400, description="No selected file")

    if not pdf_file:
        abort(400, description="Invalid file object")

    # Check MIME type to ensure it's a PDF
    if pdf_file.mimetype != 'application/pdf':
        abort(400, description="Only PDF files are allowed")

    try:
        # Create uploads directory if it doesn't exist
        if not os.path.exists(UPLOAD_FOLDER):
            os.makedirs(UPLOAD_FOLDER)

        # Sanitize the filename to prevent path traversal attacks
        filename = secure_filename(pdf_file.filename)
        file_path = os.path.join(UPLOAD_FOLDER, filename)

        # Save the file securely
        pdf_file.save(file_path)

        return redirect('/')

    except Exception as e:
        abort(500, description=f"Error while saving the file: {str(e)}")

