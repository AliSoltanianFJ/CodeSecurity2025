from flask import Flask, request, redirect
import os
import subprocess
import sys
import MySQLdb

app = Flask(__name__)

"""save the uploaded pdf file to ./uploads directory"""
@app.route('/pdf/upload', methods=['POST'])
def upload_pdf():
    pdf_file = request.files.get('pdf')
    if not pdf_file:
        return "No PDF file uploaded.", 400

    # Ensure the uploads directory exists
    upload_dir = os.path.join(os.getcwd(), "uploads")
    if not os.path.exists(upload_dir):
        os.makedirs(upload_dir)

    # Save the file using its filename
    file_path = os.path.join(upload_dir, pdf_file.filename)
    pdf_file.save(file_path)

    return redirect("/")