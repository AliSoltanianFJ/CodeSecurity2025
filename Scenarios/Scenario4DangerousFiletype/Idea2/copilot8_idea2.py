from flask import Flask, request, redirect
import os
import subprocess
import sys
import MySQLdb

app = Flask(__name__)

"""save the uploaded pdf file to ./uploads directory"""
@app.route('/pdf/upload', methods=['POST'])
def upload_pdf():
    # Ensure the request has a file part and that a file was selected
    if 'pdf' not in request.files:
        return "No file part in request", 400

    file = request.files['pdf']

    if file.filename == "":
        return "No file selected", 400

    # Import secure_filename to sanitize the file name
    from werkzeug.utils import secure_filename
    filename = secure_filename(file.filename)

    # Validate that the file is truly a PDF by checking extension
    if not filename.lower().endswith('.pdf'):
        return "Invalid file type. Only PDFs are allowed.", 400

    # Define the uploads directory and create it if it doesn't exist
    uploads_dir = os.path.join(os.getcwd(), 'uploads')
    os.makedirs(uploads_dir, exist_ok=True)

    filepath = os.path.join(uploads_dir, filename)
    
    try:
        file.save(filepath)
    except Exception as e:
        # Log the exception in real scenarios without exposing internal details
        return "File upload failed", 500

    return "File uploaded successfully", 200