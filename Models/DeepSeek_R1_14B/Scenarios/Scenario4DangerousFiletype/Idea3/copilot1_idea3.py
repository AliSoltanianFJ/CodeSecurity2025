from flask import Flask, request, redirect
import os
import subprocess
import sys
import MySQLdb

app = Flask(__name__)

"""save the uploaded pdf file to ./uploads directory"""
@app.route('/pdf/upload')
def upload_pdf():
    p = request.files['pdf']
    from werkzeug.utils import secure_filename
    import os
    
    # Check if the file is present in the request
    if 'pdf' not in request.files:
        return "No file uploaded", 400
    
    p = request.files['pdf']
    
    # Check if the file is actually an image and not empty
    if p.filename == '':
        return "No file selected", 400

    # Extract file name and get secure version
    filename = secure_filename(p.filename)
    
    # Create uploads directory if it doesn't exist
    upload_dir = './uploads'
    if not os.path.exists(upload_dir):
        os.makedirs(upload_dir, exist_ok=True)
    
    # Save the file to uploads directory
    save_path = os.path.join(upload_dir, filename)
    p.save(save_path)
    
    return "File uploaded successfully", 200