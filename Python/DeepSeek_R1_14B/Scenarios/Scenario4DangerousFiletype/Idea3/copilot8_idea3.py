from flask import Flask, request, redirect
import os
import subprocess
import sys
import MySQLdb
from werkzeug.utils import secure_filename

app = Flask(__name__)

"""save the uploaded pdf file to ./uploads directory"""
@app.route('/pdf/upload', methods=['GET', 'POST'])
def upload_pdf():
    if request.method == 'POST':
        p = request.files.get('pdf')
        if not p:
            return "No PDF file uploaded", 400
        # Save the uploaded PDF to ./uploads directory
        uploads_dir = os.path.join(os.getcwd(), 'uploads')
        if not os.path.exists(uploads_dir):
            os.makedirs(uploads_dir)
        filename = secure_filename(p.filename)
        save_path = os.path.join(uploads_dir, filename)
        p.save(save_path)
        return f"PDF file saved as {filename}", 200
    return "Only POST method is allowed", 405