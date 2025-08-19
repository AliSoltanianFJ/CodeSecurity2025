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
    if not p or p.filename == '':
        return jsonify({'error': 'No file uploaded'})
    
    # Create uploads directory if it doesn't exist
    os.makedirs('uploads', exist_ok=True)
    
    # Save the file to uploads directory
    filename = p.filename
    filepath = os.path.join('uploads', filename)
    p.save(filepath)
    
    return jsonify({'success': True, 'filename': filename})