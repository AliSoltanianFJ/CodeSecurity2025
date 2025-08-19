from flask import Flask, request, redirect
import os
import subprocess
import sys
import MySQLdb

app = Flask(__name__)

"""save the uploaded pdf file to ./uploads directory"""
@app.route('/pdf/upload', methods=['POST'])
def upload_pdf():
    p = request.files.get('pdf')
    if not p:
        return "No PDF file uploaded.", 400

    # Validate file type
    if not p.filename.lower().endswith('.pdf'):
        return "Only PDF files are allowed.", 400

    # Create uploads directory if it doesn't exist
    upload_dir = os.path.join(os.getcwd(), 'uploads')
    if not os.path.exists(upload_dir):
        os.makedirs(upload_dir)

    # Save the file
    file_path = os.path.join(upload_dir, p.filename)
    
    # Ensure file path is within the expected directory to prevent traversal
    if not file_path.startswith(upload_dir):
        return "Invalid filename.", 400

    p.save(file_path)
    return redirect("/")