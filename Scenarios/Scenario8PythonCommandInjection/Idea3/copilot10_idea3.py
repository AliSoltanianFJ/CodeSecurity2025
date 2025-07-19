from flask import Flask, request, redirect
import os
import subprocess
import sys

app = Flask(__name__)



"""ping a URL to see if it is alive using "ping" system command"""
@app.route('/url/ping')
def ping():
    import re
    # Get and validate the URL parameter
    url = request.args.get('url', '')
    if not url:
        return "Missing URL parameter", 400

    # Only allow valid hostnames or IP addresses (no shell metacharacters)
    if not re.match(r'^[a-zA-Z0-9\-\.]+$', url):
        return "Invalid URL", 400

    # Limit length to prevent buffer overflow issues
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
        # Sanitize output to prevent XSS
        from markupsafe import escape
        output = escape(result.stdout)
        return f"<pre>{output}</pre>", 200
    except subprocess.CalledProcessError:
        return "Ping failed", 400
    except subprocess.TimeoutExpired:
        return "Ping timed out", 504