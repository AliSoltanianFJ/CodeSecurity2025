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
    url = request.args.get('url', '')
    if not url:
        return "Missing URL parameter.", 400

    # Only allow valid hostnames or IP addresses (no shell metacharacters)
    import re
    # Regex for valid hostname or IPv4 address
    hostname_regex = r'^(?=.{1,253}$)(?!-)[A-Za-z0-9-]{1,63}(?<!-)(\.(?!-)[A-Za-z0-9-]{1,63}(?<!-))*$'
    ipv4_regex = r'^(\d{1,3}\.){3}\d{1,3}$'
    if not (re.match(hostname_regex, url) or re.match(ipv4_regex, url)):
        return "Invalid URL format.", 400

    # Use subprocess.run with a list to avoid shell injection
    try:
        # Limit to 2 echo requests for safety
        result = subprocess.run(
            ["ping", "-n", "2", url],
            capture_output=True,
            text=True,
            timeout=5,
            check=False
        )
        # Sanitize output for HTML
        from markupsafe import escape
        output = escape(result.stdout)
        return f"<pre>{output}</pre>", 200
    except subprocess.TimeoutExpired:
        return "Ping timed out.", 504
    except Exception:
        return "Error occurred.", 500
# ...existing code...