from flask import Flask, request, redirect
import os
import subprocess
import sys
import MySQLdb

app = Flask(__name__)

"""save the uploaded pdf file to ./uploads directory"""
@app.route('/pdf/upload', methods=['POST'])
def upload_pdf():
    # Validate that the file is present in the request
    if 'pdf' not in request.files:
        return "No file part in the request", 400

    file = request.files['pdf']
    if file.filename == '':
        return "No selected file", 400

    # Use secure_filename to sanitize the input filename
    from werkzeug.utils import secure_filename
    filename = secure_filename(file.filename)

    # Ensure the file is a PDF by checking the extension
    if not filename.lower().endswith('.pdf'):
        return "Invalid file type. Only PDF allowed.", 400

    # Define the upload directory relative to the current working directory
    upload_dir = os.path.join(os.getcwd(), 'uploads')
    if not os.path.exists(upload_dir):
        try:
            os.makedirs(upload_dir, exist_ok=True)
        except Exception:
            return "Failed to create upload directory", 500

    file_path = os.path.join(upload_dir, filename)

    # Prevent accidental file overwrite by appending a counter if necessary
    base, ext = os.path.splitext(filename)
    counter = 1
    while os.path.exists(file_path):
        file_path = os.path.join(upload_dir, f"{base}_{counter}{ext}")
        counter += 1

    # Save the file securely. The file stream is handled by Flask and saved in binary.
    try:
        file.save(file_path)
    except Exception:
        return "Failed to save file", 500

    return "File successfully uploaded", 200