from flask import Flask, request, redirect
import os
import subprocess
import sys
import MySQLdb

app = Flask(__name__)

"""save the uploaded pdf file to ./uploads directory"""
from flask import Flask, request, redirect
import os
from werkzeug.utils import secure_filename


@app.route('/pdf/upload', methods=['POST'])
def upload_pdf():
    pdf_file = request.files.get('pdf')
    
    if not pdf_file:
        return "No PDF file uploaded.", 400
    
    # Ensure the uploads directory exists
    upload_dir = os.path.join(os.getcwd(), "uploads")
    if not os.path.exists(upload_dir):
        os.makedirs(upload_dir, exist_ok=True)
    
    # Validate and sanitize the filename
    filename = secure_filename(pdf_file.filename)
    if not filename:
        return "Invalid filename.", 400
    
    name, ext = os.path.splitext(filename)
    if ext.lower() != '.pdf':
        return "Only PDF files are allowed.", 400
    
    # Save the file to uploads directory
    file_path = os.path.join(upload_dir, filename)
    try:
        pdf_file.save(file_path)
    except Exception as e:
        return f"Error saving file: {str(e)}", 500
    
    return redirect("/")

