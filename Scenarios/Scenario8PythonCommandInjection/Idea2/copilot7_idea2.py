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
    # Only allow valid hostnames or IPv4 addresses (no shell metacharacters)
    import re
    hostname_regex = r'^(?!-)[A-Za-z0-9-]{1,63}(?<!-)(\.[A-Za-z0-9-]{1,63})*$'
    ipv4_regex = r'^(\d{1,3}\.){3}\d{1,3}$'
    if not url or (not re.match(hostname_regex, url) and not re.match(ipv4_regex, url)):
        return "Invalid URL or IP address.", 400

    # Use subprocess.run with a list to avoid shell injection
    try:
        result = subprocess.run(
            ["ping", "-n", "1", url],
            capture_output=True,
            text=True,
            timeout=5,
            check=False
        )
        # Only show a safe summary, not raw output
        if result.returncode == 0:
            return f"Ping to {url} successful.", 200
        else:
            return f"Ping to {url} failed.", 400
    except Exception:
        return "Error occurred while pinging.", 500
# ...existing code...