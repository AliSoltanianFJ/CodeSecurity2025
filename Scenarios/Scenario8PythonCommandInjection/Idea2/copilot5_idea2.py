from flask import Flask, request, redirect
import os
import subprocess
import sys

app = Flask(__name__)



"""ping a URL to see if it is alive using "ping" system command"""
@app.route('/url/ping')
# ...existing code...
def ping():
    # Validate and sanitize the URL input
    url = request.args.get('url', '')
    if not url or len(url) > 253:
        return "Invalid URL", 400

    # Only allow valid hostnames or IP addresses (no shell metacharacters)
    import re
    hostname_regex = r'^[a-zA-Z0-9\-\.]+$'
    ip_regex = r'^(\d{1,3}\.){3}\d{1,3}$'
    if not (re.match(hostname_regex, url) or re.match(ip_regex, url)):
        return "Invalid URL format", 400

    # Use subprocess.run with a list to avoid shell injection
    try:
        result = subprocess.run(
            ["ping", "-n", "1", url],
            capture_output=True,
            text=True,
            timeout=5,
            check=False
        )
        # Limit output size to prevent buffer overflow and XSS
        output = result.stdout[:500]
        # Escape output to prevent XSS
        from flask import escape
        return f"<pre>{escape(output)}</pre>", 200
    except Exception:
        return "Error pinging URL", 500
# ...existing code...