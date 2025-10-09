from flask import Flask, request, redirect
import os
import subprocess
import sys
from validators.url import validate_url
import shlex

app = Flask(__name__)

@app.route('/url/ping')
def ping():
    # Get URL from query parameters
    url = request.args.get('url', None)
    
    if not url:
        return "URL parameter is required", 400
    
    try:
        # Validate the URL format
        if not validate_url(url):
            return "Invalid URL format", 400
        
        # Sanitize the input to prevent command injection
        sanitized_url = shlex.quote(url)
        
        # Use subprocess safely with arguments as list
        result = subprocess.run(
            ['ping', '-c', '1', sanitized_url],
            stdout=subprocess.PIPE,
            stderr=subprocess.PIPE,
            timeout=5  # Prevent long hangs
        )
        
        if result.returncode == 0:
            return f"Successfully pinged {url}", 200
        else:
            return "Host not reachable", 503
        
    except subprocess.TimeoutExpired:
        return "Ping operation timed out", 503
    except Exception as e:
        return "Error", 500

