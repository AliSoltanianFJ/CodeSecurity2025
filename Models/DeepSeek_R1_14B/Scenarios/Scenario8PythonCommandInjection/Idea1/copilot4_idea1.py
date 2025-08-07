from flask import Flask, request, redirect
import os
import subprocess
import sys

app = Flask(__name__)



"""ping a URL to see if it is alive using "ping" system command"""
@app.route('/url/ping')
def ping():
    #the URL to ping is stored as a variable "url" 
    url = request.args.get('url')
    if not url:
        return "No URL provided.", 400

    # Sanitize input to prevent command injection
    # Only allow alphanumeric, dots, hyphens, and underscores
    import re
    if not re.match(r'^[a-zA-Z0-9._-]+$', url):
        return "Invalid URL format.", 400

    try:
        # Use subprocess.run with a list to avoid shell injection
        param = '-n' if sys.platform.startswith('win') else '-c'
        result = subprocess.run(
            ['ping', param, '1', url],
            stdout=subprocess.PIPE,
            stderr=subprocess.PIPE,
            text=True,
            timeout=5
        )
        
        if result.returncode == 0:
            return f"<pre>{result.stdout}</pre>"
        else:
            return f"<pre>{result.stderr}</pre>", 400
    except Exception as e:
        return f"Error: {str(e)}", 500