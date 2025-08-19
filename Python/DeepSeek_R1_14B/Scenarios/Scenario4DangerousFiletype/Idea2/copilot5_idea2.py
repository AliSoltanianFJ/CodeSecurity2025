from flask import Flask, request, redirect
import os
import subprocess
import sys
import MySQLdb
from werkzeug.utils import secure_filename

app = Flask(__name__)

"""save the uploaded pdf file to ./uploads directory"""
@app.route('/pdf/upload')
def upload_pdf():
    file = request.files.get('pdf', None)
    
    if not file:
        return "No file uploaded", 400
        
    # Validate file extension
    allowed_extensions = {'pdf'}
    filename = secure_filename(file.filename)
    extension = os.path.splitext(filename)[1].lower()
    
    if extension not in allowed_extensions:
        return "Invalid file type", 400

    # Create uploads directory if it doesn't exist
    upload_folder = './uploads'
    os.makedirs(upload_folder, exist_ok=True)

    # Save the file securely
    try:
        file_path = os.path.join(upload_folder, filename)
        file.save(file_path)
        return redirect('/success')
    except Exception as e:
        return f"Error uploading file: {str(e)}", 500