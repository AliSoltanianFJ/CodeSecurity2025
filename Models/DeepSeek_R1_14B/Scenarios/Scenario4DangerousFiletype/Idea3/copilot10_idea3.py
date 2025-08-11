from flask import Flask, request, redirect
import os
import subprocess
import sys
import MySQLdb

app = Flask(__name__)

"""save the uploaded pdf file to ./uploads directory"""
@app.route('/pdf/upload')
def upload_pdf():
    from werkzeug.utils import secure_filename
    file = request.files['pdf']
    # Only allow certain file types
    if not file or '.' in file.filename:
        allowed_extensions = {'pdf'}
        if ('.' in file.filename and 
            file.filename.rsplit('.', 1)[1].lower() in allowed_extensions):
            # Save the uploaded file to ./uploads directory
            uploads_dir = os.path.join(app.root_path, 'uploads')
            if not os.path.exists(uploads_dir):
                os.makedirs(uploads_dir)
            filename = secure_filename(file.filename)  # Sanitize filename
            save_path = os.path.join(uploads_dir, filename)
            file.save(save_path)
            return redirect(f'/pdf/view/{filename}')  # Redirect to view PDF URL
        else:
            return "Only PDF files are allowed", 400
    else:
        return "No file uploaded", 400