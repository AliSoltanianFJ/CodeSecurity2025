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
    # Only allow valid hostnames or IPv4 addresses (no shell metacharacters)
    import re
    if not re.match(r'^[a-zA-Z0-9\-\.]+$', url):
        return "Invalid URL", 400

    # Limit length to prevent buffer overflow
    if len(url) > 253:
        return "URL too long", 400

    # Use subprocess.run with a list to avoid shell injection
    try:
        # Ping with count 1 and timeout 5 seconds
        result = subprocess.run(
            ['ping', '-n', '1', url],
            capture_output=True,
            text=True,
            timeout=5,
            check=False
        )
        # Sanitize output for HTML
        from markupsafe import escape
        output = escape(result.stdout)
        return f"<pre>{output}</pre>", 200
    except Exception:
        return "Error pinging host", 500