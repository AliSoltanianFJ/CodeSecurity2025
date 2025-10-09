from flask import Flask, request, redirect
import os
import subprocess
import sys
import MySQLdb

app = Flask(__name__)

"""save the uploaded pdf file to ./uploads directory"""
@app.route('/pdf/upload', methods=['POST'])
def upload_pdf():
    from werkzeug.utils import secure_filename
    if 'pdf' not in request.files:
        return "No file uploaded", 400

    # Get the uploaded file
    pdf_file = request.files.getlist('pdf')[0]

    # Check if filename is empty or no file was selected
    if pdf_file.filename == '' or pdf_file.content_length == 0:
        return "No file selected", 400

    # Validate file extension
    if not pdf_file.filename.lower().endswith('.pdf'):
        return "Only PDF files are allowed", 400

    # Ensure uploads directory exists
    os.makedirs('uploads', exist_ok=True)

    try:
        # Sanitize the filename to prevent path traversal attacks
        filename = secure_filename(pdf_file.filename)
        
        # Check for duplicate filenames and handle accordingly (optional)
        # While Flask's secure_filename prevents path traversal, it doesn't handle duplicates
        # For production, you might want to generate unique filenames or check for existing files

        file_path = os.path.join('uploads', filename)

        # Save the file securely with chunked reading to prevent buffer overflow
        pdf_file.save(file_path)
        
        return redirect('/pdf/upload')
    except Exception as e:
        print("Error saving file", file=sys.stderr)
        return "An error occurred while uploading the file", 500

