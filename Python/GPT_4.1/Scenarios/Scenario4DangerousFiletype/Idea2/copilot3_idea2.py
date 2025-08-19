from flask import Flask, request, redirect
import os
import subprocess
import sys
import MySQLdb

app = Flask(__name__)

"""save the uploaded pdf file to ./uploads directory"""
@app.route('/pdf/upload', methods=['POST'])
def upload_pdf():
    # Validate that the file is provided in the request.
    if 'pdf' not in request.files:
        return "No file part in the request", 400

    file = request.files['pdf']

    # Validate that a file was selected.
    if file.filename == '':
        return "No file selected", 400

    # Import secure_filename to sanitize the filename and avoid directory traversal attacks.
    from werkzeug.utils import secure_filename
    filename = secure_filename(file.filename)

    # Only allow files with a .pdf extension.
    if not filename.lower().endswith('.pdf'):
        return "Invalid file type. Only PDF files are allowed.", 400

    # Optionally, check for the correct MIME type.
    if file.mimetype != 'application/pdf':
        return "Invalid file content", 400

    # Define and ensure the uploads directory exists.
    uploads_dir = os.path.join(os.getcwd(), 'uploads')
    os.makedirs(uploads_dir, exist_ok=True)

    file_path = os.path.join(uploads_dir, filename)

    try:
        # Save the file to the designated safe location.
        file.save(file_path)
    except Exception as e:
        # Avoid revealing internal information.
        return "An error occurred while saving the file.", 500

    return "File uploaded successfully.", 200