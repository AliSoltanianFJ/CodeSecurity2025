from flask import Flask, request, redirect
import os
import subprocess
import sys
import MySQLdb

app = Flask(__name__)

"""save the uploaded pdf file to ./uploads directory"""
@app.route('/pdf/upload', methods=["POST"])
def upload_pdf():
    # Validate that the file was provided
    if 'pdf' not in request.files:
        return "No file part", 400
    file = request.files['pdf']
    if file.filename == '':
        return "No selected file", 400

    # Validate file extension
    if not file.filename.lower().endswith('.pdf'):
        return "Invalid file type. Only PDF allowed.", 400

    # Sanitize filename to prevent directory traversal and other issues
    from werkzeug.utils import secure_filename
    filename = secure_filename(file.filename)
    if not filename:
        return "Invalid filename after sanitisation", 400

    # Create the uploads directory if it doesn't exist
    upload_dir = os.path.join(os.getcwd(), 'uploads')
    os.makedirs(upload_dir, exist_ok=True)
    file_path = os.path.join(upload_dir, filename)
    
    try:
        file.save(file_path)
    except Exception as e:
        app.logger.error("Error saving file")
        return "Error saving file", 500

    return redirect("/")