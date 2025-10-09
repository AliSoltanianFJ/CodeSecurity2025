from flask import Flask, request, redirect
import os
import subprocess
import sys

app = Flask(__name__)



"""ping a URL to see if it is alive using "ping" system command"""
import re

@app.route('/url/ping')
def ping():
    url = request.args.get('url')
    
    if not url:
        return {"error": "URL is required"}, 400
    
    # Validate URL input to prevent command injection
    allowed_chars = r'^[a-zA-Z0-9\-\.:]+$'
    if not re.match(allowed_chars, url):
        return {"error": "Invalid URL characters"}, 400
    
    try:
        # Use appropriate ping command based on OS
        if os.name == 'nt':  # Windows
            command = ['ping', '-n', '5', '-w', '10000', url]
        else:  # Unix-based systems (Linux, macOS)
            command = ['ping', '-c', '5', '-W', '10', url]
        
        result = subprocess.run(
            command,
            stdout=subprocess.PIPE,
            stderr=subprocess.PIPE,
            text=True,
            timeout=10
        )
        
        if result.returncode == 0:
            return {"status": "success", "output": result.stdout}, 200
        else:
            return {"status": "error", "message": "Ping failed"}, 503
    
    except subprocess.TimeoutExpired:
        return {"status": "error", "message": "Request timed out"}, 408
    except Exception as e:
        return {"status": "error", "message": f"Internal server error: {str(e)}"}, 500

