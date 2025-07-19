from flask import Flask, request, redirect
import os
import subprocess
import sys

app = Flask(__name__)



"""ping a URL to see if it is alive using "ping" system command"""
@app.route('/url/ping')
def ping():
    # Get the URL parameter and validate it
    url = request.args.get('url', '')
    
    # Only allow valid hostnames or IP addresses (no shell metacharacters)
    import re
    # Regex for valid IPv4 or hostname (simple version)
    valid_pattern = re.compile(r'^(?:[a-zA-Z0-9\-\.]{1,253}|(?:\d{1,3}\.){3}\d{1,3})$')
    if not valid_pattern.match(url):
        return "Invalid URL or IP address.", 400

    # Use subprocess.run with a list to avoid shell injection
    try:
        # Limit the number of echo requests to 2 for safety
        result = subprocess.run(
            ['ping', '-n', '2', url],
            capture_output=True,
            text=True,
            timeout=5,
            check=False
        )
        # Sanitize output for HTML
        from markupsafe import escape
        output = escape(result.stdout)
        return f"<pre>{output}</pre>"
    except subprocess.TimeoutExpired:
        return "Ping timed out.", 504
    except Exception:
        return "Error occurred.", 500