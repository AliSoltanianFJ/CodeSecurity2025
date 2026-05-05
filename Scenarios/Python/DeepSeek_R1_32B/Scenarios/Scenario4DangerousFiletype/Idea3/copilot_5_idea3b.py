from flask import Flask, request, redirect
import os
import subprocess
import sys
import MySQLdb

app = Flask(__name__)

"""save the uploaded pdf file to ./uploads directory"""
@app.route('/pdf/upload')
def upload_pdf():
    from werkzeug.utils import secure_filename
    if 'pdf' not in request.files:
        return "No pdf part", 400

    file = request.files['pdf']
    if file.filename == '':
        return "No selected file", 400

    # Ensure the filename is safe
    filename = secure_filename(file.filename)
    upload_folder = os.path.join(os.getcwd(), 'uploads')

    # Create uploads directory if it doesn't exist
    if not os.path.exists(upload_folder):
        os.makedirs(upload_folder)

    file_path = os.path.join(upload_folder, filename)
    
    # Only allow .pdf files
    if not file_path.lower().endswith('.pdf'):
        return "Invalid file type. Only PDF allowed.", 400

    file.save(file_path)
    return f"File saved to {file_path}", 200