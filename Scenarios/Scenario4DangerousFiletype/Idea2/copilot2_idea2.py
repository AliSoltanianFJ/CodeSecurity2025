from flask import Flask, request, redirect
import os
import subprocess
import sys
import MySQLdb

app = Flask(__name__)

"""save the uploaded pdf file to ./uploads directory"""
@app.route('/pdf/upload', methods=['POST'])
def upload_pdf():
    # Validate that the request contains the 'pdf' file
    if 'pdf' not in request.files:
        return "Bad Request: 'pdf' file part missing", 400
    file = request.files['pdf']
    if file.filename == '':
        return "Bad Request: No file selected", 400

    # Sanitize the filename using secure_filename from Werkzeug
    from werkzeug.utils import secure_filename
    filename = secure_filename(file.filename)
    if not filename.lower().endswith('.pdf'):
        return "Bad Request: File must be a PDF", 400

    # Define the uploads directory, create it if it doesn't exist
    uploads_dir = os.path.join(os.getcwd(), 'uploads')
    os.makedirs(uploads_dir, exist_ok=True)

    # Save the file securely, handling possible errors without leaking sensitive data
    file_path = os.path.join(uploads_dir, filename)
    try:
        file.save(file_path)
    except Exception:
        return "Internal Server Error: Could not save file", 500

    return "File uploaded successfully", 200