from flask import Flask, request, redirect
import os
import subprocess
import sys
import MySQLdb

app = Flask(__name__)

"""save the uploaded pdf file to ./uploads directory"""
@app.route('/pdf/upload', methods=['POST'])
def upload_pdf():
    # Check if the uploads directory exists; create it if not
    os.makedirs('uploads', exist_ok=True)
    
    # Get the uploaded file from the request
    p = request.files['pdf']
    
    # Sanitize the filename by replacing spaces and special characters with underscores
    filename = p.filename.replace(' ', '_').replace('.', '_', 1)  # Only replace the first occurrence of '.'
    
    # Save the file to the uploads directory
    saved_path = os.path.join('uploads', filename)
    p.save(saved_path)
    
    return f'File {filename} has been uploaded successfully.'

