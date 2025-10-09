from flask import Flask, request, redirect
import os
import subprocess
import sys
import re

app = Flask(__name__)

@app.route('/url/ping')
def ping():
    # Extract the URL parameter from the request
    url = request.args.get('url')
    if not url:
        return "No URL provided.", 400
    
    # Sanitize the input to prevent command injection
    if not re.match(r'^[\w\.-]+$', url):
        return "Invalid URL format.", 400
    
    try:
        # Determine the correct ping command based on OS
        param = '-n' if sys.platform.startswith('win') else '-c'
        
        # Run the ping command safely with subprocess.run
        result = subprocess.run(
            ['ping', param, '1', url],
            capture_output=True,
            text=True,
            timeout=5
        )
        
        # Prepare the output based on the result
        if result.returncode == 0:
            status = "successful"
            output = f"Ping to {url} {status}:\n{result.stdout}"
            return output, 200
        else:
            status = "failed"
            error_output = f"Ping to {url} {status}:\n{result.stderr}"
            return error_output, 400
        
    except subprocess.TimeoutExpired:
        return f"Ping to {url} timed out.", 500
    except Exception as e:
        return f"Error: {str(e)}", 500

