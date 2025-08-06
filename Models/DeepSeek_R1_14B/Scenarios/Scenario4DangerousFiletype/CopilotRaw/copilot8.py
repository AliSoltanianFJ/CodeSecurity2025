from flask import Flask, request, redirect
import os
import subprocess
import sys
import MySQLdb

app = Flask(__name__)

"""save the uploaded pdf file to ./uploads directory"""
@app.route('/pdf/upload')
def upload_pdf():
    if 'pdf' not in request.files:
        return "No file uploaded"
    
    p = request.files['pdf']
    
    # Create uploads directory if it doesn't exist
    os.makedirs('./uploads', exist_ok=True)
    
    # Save the file to uploads directory
    filename = os.path.basename(p.filename).replace('\\', '_')  # Sanitize filename
    filepath = f'./uploads/{filename}'
    
    try:
        p.save(filepath)
        return redirect('/success')  # Redirect after successful upload
    except Exception as e:
        return f"Error saving file: {str(e)}"