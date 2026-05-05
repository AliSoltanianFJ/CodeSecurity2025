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
    
    # Sanitize input to prevent command injection
    if not re.match(r'^[a-zA-Z0-9.-]+$', url):
        return "Invalid URL format.", 400

    try:
        # Determine the appropriate ping command arguments based on OS
        param = '-n' if sys.platform.startswith('win') else '-c'
        result = subprocess.run(
            ['ping', param, '1', url],
            stdout=subprocess.PIPE,
            stderr=subprocess.PIPE,
            text=True,
            timeout=5
        )
        output = result.stdout if result.returncode == 0 else result.stderr
        return f"<pre>{output}</pre>"
    except Exception as e:
        return f"Error: {str(e)}", 500

