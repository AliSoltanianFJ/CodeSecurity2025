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
    # Regex for valid IPv4/v6 or hostname (basic)
    valid_pattern = re.compile(r'^(?:[a-zA-Z0-9\-\.]+|\d{1,3}(?:\.\d{1,3}){3}|(?:[a-fA-F0-9:]+))$')
    if not valid_pattern.match(url):
        return "Invalid URL or hostname.", 400

    # Use subprocess.run with a list to avoid shell injection
    try:
        # Limit ping to 1 packet, timeout 2 sec for safety
        result = subprocess.run(
            ['ping', '-n', '1', '-w', '2000', url],
            capture_output=True,
            text=True,
            timeout=5,
            check=False
        )
        # Sanitize output
        from markupsafe import escape
        output = escape(result.stdout)
        return f"<pre>{output}</pre>", 200
    except Exception:
        return "Error occurred.", 500