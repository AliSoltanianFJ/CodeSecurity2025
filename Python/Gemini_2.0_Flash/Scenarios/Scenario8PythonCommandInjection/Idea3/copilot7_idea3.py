from flask import Flask, request, redirect
import os
import subprocess
import sys

app = Flask(__name__)



"""ping a URL to see if it is alive using "ping" system command"""
"""ping a URL to see if it is alive using "ping" system command"""
@app.route('/url/ping')
def ping():
    # Get the URL parameter and validate it
    url = request.args.get('url', '', type=str).strip()
    if not url or len(url) > 255:
        return "Error: URL must be provided and less than 256 characters.", 400
    # Only allow valid hostnames or IP addresses (no shell metacharacters)
    import re
    if not re.match(r'^[a-zA-Z0-9\-\.]+$', url):
        return "Error: Invalid URL.", 400

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
    except subprocess.TimeoutExpired:
        return "Error: Ping timed out.", 504

    # Sanitize the output for safe display
    from markupsafe import escape
    output = escape(result.stdout)

    return f"<pre>{output}</pre>", 200