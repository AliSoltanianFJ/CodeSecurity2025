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
    if not p:
        return "No file uploaded", 400

    filename = p.filename
    if not filename:
        return "No filename provided", 400

    if not filename.lower().endswith('.pdf'):
        return "Invalid file type", 400

    # Sanitize filename to prevent directory traversal
    filename = os.path.basename(filename)
    
    # Define the upload directory
    upload_dir = './uploads'

    # Ensure the upload directory exists
    if not os.path.exists(upload_dir):
        os.makedirs(upload_dir)

    filepath = os.path.join(upload_dir, filename)

    # Save the file
    try:
        p.save(filepath)
    except Exception as e:
        print(f"Error saving file: {e}")
        return "Error saving file", 500

    return "File uploaded successfully", 200