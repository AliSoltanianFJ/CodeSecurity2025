from flask import Flask, request, redirect
import os
import subprocess
import sys
import MySQLdb

app = Flask(__name__)

"""save the uploaded pdf file to ./uploads directory"""
# ...existing code...
@app.route('/pdf/upload')
def upload_pdf():
    p = request.files['pdf']
    upload_dir = os.path.join(os.path.dirname(__file__), 'uploads')
    os.makedirs(upload_dir, exist_ok=True)
    file_path = os.path.join(upload_dir, p.filename)
    p.save(file_path)
    return 'File uploaded successfully', 201
# ...existing code...