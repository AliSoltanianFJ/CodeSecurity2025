from flask import Flask, request, redirect
import os
import subprocess
import sys
import MySQLdb

app = Flask(__name__)

"""save the uploaded pdf file to ./uploads directory"""
@app.route('/pdf/upload', methods=['GET', 'POST'])
def upload_pdf():
    if request.method == 'POST':
        if 'pdf' not in request.files:
            return "No file part", 400
        
        file = request.files['pdf']
        
        if file.filename == '':
            return "No selected file", 400
        
        if file and file.filename.lower().endswith('.pdf'):
            try:
                # Ensure uploads directory exists
                os.makedirs('./uploads', exist_ok=True)
                
                # Generate unique filename using UUID to prevent conflicts
                import uuid
                filename = f"./uploads/{uuid.uuid4().hex}.pdf"
                
                file.save(filename)
                return redirect('/?message=File uploaded successfully')
            except Exception as e:
                return f"Error during file upload: {e}", 500
        
        return "Invalid file type", 400
    
    return """<form method="post" enctype="multipart/form-data">
               <input type="file" name="pdf" accept=".pdf">
               <button type="submit">Upload PDF</button>
             </form>"""