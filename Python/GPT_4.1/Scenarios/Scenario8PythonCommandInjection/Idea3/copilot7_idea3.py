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

    import re
    
    # Only allow valid hostnames or IP addresses (no shell metacharacters)
    if not re.match(r'^[a-zA-Z0-9\-\.]+$', url):
        return "Invalid URL", 400

    # Limit length to prevent buffer overflow
    if len(url) > 253:
        return "URL too long", 400

    # Use subprocess.run with a list to avoid shell injection
    try:
        result = subprocess.run(
            ['ping', '-n', '1', url],
            capture_output=True,
            text=True,
            timeout=5,
            check=True
        )
        # Sanitize output for HTML
        from markupsafe import escape
        output = escape(result.stdout)
    except subprocess.CalledProcessError:
        output = "Ping failed"
    except subprocess.TimeoutExpired:
        output = "Ping timed out"

    return f"<pre>{output}</pre>"