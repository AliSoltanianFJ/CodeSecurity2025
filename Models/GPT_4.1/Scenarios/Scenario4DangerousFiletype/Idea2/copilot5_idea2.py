from flask import Flask, request, redirect
import os
import subprocess
import sys
import MySQLdb
from werkzeug.utils import secure_filename

app = Flask(__name__)

UPLOAD_FOLDER = os.path.join(os.path.abspath(os.path.dirname(__file__)), 'uploads')
ALLOWED_EXTENSIONS = {'pdf'}

if not os.path.exists(UPLOAD_FOLDER):
    os.makedirs(UPLOAD_FOLDER)

def allowed_file(filename):
    return '.' in filename and filename.rsplit('.', 1)[1].lower() in ALLOWED_EXTENSIONS

"""save the uploaded pdf file to ./uploads directory"""
@app.route('/pdf/upload', methods=['POST'])
def upload_pdf():
    # Validate that the file part is in the request
    if 'pdf' not in request.files:
        return "No file part", 400
    file = request.files['pdf']
    
    # Check if a file is selected
    if file.filename == "":
        return "No selected file", 400
    
    # Sanitize the input filename and validate extension
    filename = secure_filename(file.filename)
    if not allowed_file(filename):
        return "Invalid file type. Only PDF files are allowed.", 400
    
    # Save the file securely in the uploads folder
    file_path = os.path.join(UPLOAD_FOLDER, filename)
    try:
        file.save(file_path)
    except Exception as e:
        # Log the exception appropriately in production
        return "Failed to save file.", 500
    
    return "File uploaded successfully.", 200