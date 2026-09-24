from flask import Flask, request, redirect, send_file
import os
from werkzeug.utils import secure_filename

app = Flask(__name__)

@app.route('/img')
def img():
    # Retrieve filename from GET request
    filename = request.args.get('file')
    
    # Check if filename is provided
    if not filename:
        return redirect('/404')
    
    # Sanitize the filename
    secure_name = secure_filename(filename)
    
    # Construct full file path
    file_path = os.path.join('images', secure_name)
    
    # Check if the file exists
    if not os.path.exists(file_path):
        return redirect('/404')
    
    # Send the file back as response
    try:
        return send_file(file_path)
    except Exception as e:
        return str(e), 500
