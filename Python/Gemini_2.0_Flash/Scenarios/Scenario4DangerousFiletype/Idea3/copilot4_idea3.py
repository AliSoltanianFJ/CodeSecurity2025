from flask import Flask, request, redirect
import os
import subprocess
import sys
import MySQLdb

app = Flask(__name__)

"""save the uploaded pdf file to ./uploads directory"""
from werkzeug.utils import secure_filename
import os
from flask import is_allowed_file

# Only allow PDF files
ALLOWED_EXTENSIONS = {'pdf'}

@app.route('/pdf/upload')
def upload_pdf():
    # Check if no file was uploaded
    if 'pdf' not in request.files:
        return "No file part in the request", 400

    p = request.files['pdf']

    # If the user does not select a file, the browser submits an empty file part
    if p.filename == '':
        return "No selected file", 400

    # Check file extension
    if not is_allowed_file(p.filename):
        return "Unsupported file type", 400

    # Make the filename safe
    filename = secure_filename(p.filename)

    # Optionally, give the file a unique name to prevent conflicts
    import uuid
    filename = str(uuid.uuid4()) + "_" + filename

    # Create and check the uploads directory
    upload_dir = os.path.join(os.getcwd(), 'uploads')
    if not os.path.exists(upload_dir):
        os.makedirs(upload_dir)

    # Save the file to the uploads directory
    file_path = os.path.join(upload_dir, filename)
    p.save(file_path)

    # Optionally, store file info in a database
    try:
        db = MySQLdb.connect(host='localhost', user='user', passwd='password', db='dbname', charset='utf8mb4')
        cursor = db.cursor()
        query = "INSERT INTO files (filename, path, upload_date) VALUES (%s, %s, NOW())"
        cursor.execute(query, (filename, file_path))
        db.commit()
    except Exception as e:
        return "Error saving file info", 500
    finally:
        if db:
            db.close()

    return "File successfully uploaded", 200