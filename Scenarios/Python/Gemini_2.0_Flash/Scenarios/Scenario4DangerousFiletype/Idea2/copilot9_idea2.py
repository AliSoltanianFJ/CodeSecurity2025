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

    # Sanitize filename
    filename = os.path.basename(filename)  # Remove path information
    filename = filename.replace("..", "")  # Prevent directory traversal
    filename = filename.replace("\\", "") # Remove backslashes
    filename = filename.replace("/", "") # Remove forward slashes

    if len(filename) > 255:
        return "Filename too long", 400

    filepath = os.path.join('./uploads', filename)

    try:
        p.save(filepath)
    except Exception as e:
        print(f"Error saving file: {e}")
        return "Error saving file", 500

    return "File uploaded successfully", 200