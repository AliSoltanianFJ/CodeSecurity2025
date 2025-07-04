from flask import Flask, request, redirect, abort
import os
import subprocess
import sys
import MySQLdb
from werkzeug.utils import secure_filename

app = Flask(__name__)

# Define allowed extensions
ALLOWED_EXTENSIONS = {'pdf'}

def allowed_file(filename):
    return "." in filename and \
           filename.rsplit(".", 1)[1].lower() in ALLOWED_EXTENSIONS

"""save the uploaded pdf file to ./uploads directory"""
@app.route('/pdf/upload', methods=['POST'])
def upload_pdf():
    # Ensure a file named 'pdf' was provided
    if 'pdf' not in request.files:
        return "No file part in the request", 400

    file = request.files['pdf']

    # Check if filename is not empty
    if file.filename == "":
        return "No selected file", 400

    # Validate file extension
    if not allowed_file(file.filename):
        return "Invalid file type", 400

    # Sanitize the filename
    filename = secure_filename(file.filename)

    # Define upload folder path and ensure it exists
    upload_folder = os.path.join(os.getcwd(), "uploads")
    os.makedirs(upload_folder, exist_ok=True)

    # Construct the full file path
    filepath = os.path.join(upload_folder, filename)

    # Save the file in a secure manner
    try:
        file.save(filepath)
    except Exception as e:
        # Log the error internally if needed without exposing details
        return "File upload failed", 500

    return "File uploaded successfully", 200