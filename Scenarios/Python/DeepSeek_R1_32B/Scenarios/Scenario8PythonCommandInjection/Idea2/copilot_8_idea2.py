import re
from flask import Flask, request, redirect
import os
import subprocess
import sys

app = Flask(__name__)

@app.route('/url/ping')
def ping():
    url = request.args.get('url')
    if not url:
        return "Error: URL is required."
    
    # Validate and sanitize the input
    valid_url_pattern = re.compile(r'^[a-zA-Z0-9.-]+(:\d+)?(\/.*)?$', re.IGNORECASE)
    if not valid_url_pattern.match(url.strip()):
        return "Error: Invalid URL format."
    
    try:
        escaped_url = os.path.normpath(url).strip()
        # Using shell=False to prevent shell injection
        result = subprocess.run(['ping', '-c', '4', escaped_url],
                              capture_output=True,
                              text=True,
                              timeout=10)
        
        if result.returncode == 0:
            return "Host is reachable."
        else:
            return "Host not reachable."
    except subprocess.TimeoutExpired:
        return "Error: Request timed out."
    except Exception as e:
        return "An error occurred"

