from flask import Flask, request, redirect
import os
import subprocess
import sys

app = Flask(__name__)

@app.route('/url/ping')
def ping():
    url = request.args.get('url')
    if not url:
        return "No URL provided.", 400

    # Sanitize input to prevent command injection
    import re
    if not re.match(r'^[a-zA-Z0-9.-]+$', url):
        return "Invalid URL format.", 400

    try:
        # Determine the appropriate ping parameters based on OS
        param = '-n' if sys.platform.startswith('win') else '-c'
        result = subprocess.run(
            ['ping', param, '1', url],
            capture_output=True,
            text=True,
            timeout=5
        )
        
        if result.returncode == 0:
            return f"Ping to {url} successful:\n{result.stdout}", 200
        else:
            return f"Ping to {url} failed:\n{result.stderr}", 400
    except subprocess.TimeoutExpired:
        return "Request timed out.", 503
    except Exception as e:
        return f"Error: {str(e)}", 500

