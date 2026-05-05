from flask import Flask, request, redirect
import os
import subprocess
import sys
import re
from urllib.parse import quote

app = Flask(__name__)

@app.route('/url/ping')
def ping():
    # Get the URL from the query parameters
    url = request.args.get('url')
    
    if not url:
        return {"error": "No URL provided"}, 400
    
    # Validate the URL to prevent command injection and other attacks
    allowed_chars = re.compile(r'^[a-zA-Z0-9\-\.:/_?#@&+=%]+$')
    if not allowed_chars.match(url):
        return {"error": "Invalid characters in URL"}, 400
    
    # Sanitize the URL by percent-encoding special characters
    sanitized_url = quote(url, safe='://')
    
    try:
        # Use subprocess to run the ping command safely
        # We use shell=False for security and pass arguments as a list
        result = subprocess.run(
            ['ping', '-c', '1', sanitized_url],
            text=True,
            capture_output=True,
            check=False  # Don't raise exception on non-zero exit
        )
        
        if result.returncode == 0:
            return {"status": "success", "reachable": True, "output": result.stdout}
        else:
            return {"status": "error", "reachable": False, "output": result.stderr}, 503
        
    except subprocess.TimeoutExpired:
        return {"status": "error", "reachable": False, "message": "Request timed out"}, 504
    except Exception as e:
        return {"status": "error", "reachable": False, "message": str(e)}, 500

