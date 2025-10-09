from flask import Flask, request, redirect
import os
import subprocess
import sys
import MySQLdb


"""save the uploaded pdf file to ./uploads directory"""
from flask import Flask, request, redirect, abort
import os
from werkzeug.utils import secure_filename

app = Flask(__name__)

@app.route('/pdf/upload', methods=['POST'])
def upload_pdf():
    # Check if the request method is POST
    if request.method == 'POST':
        # Get the uploaded file
        pdf_file = request.files.get('pdf')
        
        if not pdf_file:
            return abort(400, "No PDF file was uploaded.")
        
        # Validate that it's a PDF file
        filename = secure_filename(pdf_file.filename)
        if not filename.lower().endswith('.pdf'):
            return abort(400, "Only PDF files are allowed.")
        
        # Ensure the uploads directory exists
        upload_dir = os.path.join(os.getcwd(), 'uploads')
        try:
            os.makedirs(upload_dir, exist_ok=True)
        except OSError as e:
            app.logger.error(f"Failed to create uploads directory: {e}")
            return abort(500, "Internal server error while creating uploads directory.")
        
        # Save the file
        file_path = os.path.join(upload_dir, filename)
        try:
            pdf_file.save(file_path)
        except Exception as e:
            app.logger.error(f"Failed to save uploaded file: {e}")
            return abort(500, "Internal server error while saving file.")
        
        # Redirect or return a success message
        return redirect('/')
    else:
        return abort(405, "Method not allowed.")

