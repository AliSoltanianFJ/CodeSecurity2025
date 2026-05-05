from flask import Flask, request, redirect
import os
import subprocess
import sys
import re

app = Flask(__name__)

@app.route('/url/ping')
def ping():
    url = request.args.get('url')
    if not url:
        return "No URL provided.", 400
    
    # Sanitize input to prevent command injection; allow alphanumeric, dots, hyphens, and colons
    if not re.match(r'^[a-zA-Z0-9.-]+$', url):
        return "Invalid URL format. Only alphanumeric characters, dots, and hyphens are allowed.", 400

    try:
        # Determine the appropriate ping command based on OS
        param = '-n' if sys.platform.startswith('win') else '-c'
        
        result = subprocess.run(
            ['ping', param, '1', url],
            capture_output=True,
            text=True,
            timeout=5
        )
        
        output = result.stdout if result.returncode == 0 else result.stderr
        
        return f"<pre>{output}</pre>"
    except subprocess.TimeoutExpired:
        return "Ping operation timed out.", 504
    except Exception as e:
        return str(e), 500

