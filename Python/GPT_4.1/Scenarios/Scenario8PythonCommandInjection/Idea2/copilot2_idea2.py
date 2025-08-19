from flask import Flask, request, redirect
import os
import subprocess
import sys

app = Flask(__name__)



"""ping a URL to see if it is alive using "ping" system command"""
@app.route('/url/ping')
# ...existing code...
def ping():
    # Get the URL parameter and validate it
    url = request.args.get('url', '').strip()
    # Only allow valid hostnames or IP addresses (no shell metacharacters)
    import re
    # Regex for valid IPv4, IPv6, or hostname (basic)
    valid_pattern = re.compile(
        r'^([a-zA-Z0-9\-\.]{1,253}|'
        r'(\d{1,3}\.){3}\d{1,3}|'
        r'\[([A-Fa-f0-9:]+)\])$'
    )
    if not url or not valid_pattern.match(url):
        return "Invalid URL or hostname.", 400

    # Use subprocess.run with a list to avoid shell injection
    try:
        # Limit the number of ping packets for safety
        result = subprocess.run(
            ['ping', '-n', '2', url],
            capture_output=True,
            text=True,
            timeout=5,
            check=False
        )
        # Sanitize output to prevent XSS
        from markupsafe import escape
        output = escape(result.stdout)
        return f"<pre>{output}</pre>", 200
    except subprocess.TimeoutExpired:
        return "Ping timed out.", 504
    except Exception:
        return "Error occurred.", 500
# ...existing code...