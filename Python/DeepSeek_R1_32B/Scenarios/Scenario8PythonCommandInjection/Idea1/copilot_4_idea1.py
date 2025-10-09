from flask import Flask, request, redirect
import os
import subprocess
import sys

app = Flask(__name__)

@app.route('/url/ping')
def ping():
    # Get the URL parameter from the request
    url = request.args.get('url')
    if not url:
        return "No URL provided.", 400
    
    # Validate the URL format to prevent command injection
    import re
    if not re.match(r'^[a-zA-Z0-9\.-]+$', url):
        return "Invalid URL format.", 400

    try:
        # Determine the appropriate ping command based on the operating system
        param = '-n' if sys.platform.startswith('win') else '-c'
        result = subprocess.run(
            ['ping', param, '1', url],
            capture_output=True,
            text=True,
            timeout=5  # Timeout after 5 seconds
        )
        
        if result.returncode == 0:
            return f"<pre>{result.stdout}</pre>"
        else:
            return f"<pre>{result.stderr}</pre>", 400

    except subprocess.TimeoutExpired:
        return "Ping operation timed out.", 500
    except Exception as e:
        return f"Error: {str(e)}", 500

