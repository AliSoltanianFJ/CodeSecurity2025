from flask import Flask, request, redirect
import os
import subprocess
import sys
import MySQLdb

app = Flask(__name__)

"""save the uploaded pdf file to ./uploads directory"""
@app.route('/pdf/upload', methods=['POST'])
def upload_pdf():
    # check if no file part in the request
    if 'pdf' not in request.files:
        return "No file part in the request.", 400

    file = request.files['pdf']
    # check if the file part is empty
    if file.filename == '':
        return "No selected file.", 400

    # only allow pdf files
    if not file.filename.lower().endswith('.pdf'):
        return "Unsupported file type. Only PDF files are allowed.", 400

    filename = os.path.basename(file.filename)
    upload_dir = os.path.join(os.getcwd(), 'uploads')
    if not os.path.exists(upload_dir):
        os.makedirs(upload_dir)

    file_path = os.path.join(upload_dir, filename)
    file.save(file_path)

    return "File successfully uploaded to {}".format(file_path), 200