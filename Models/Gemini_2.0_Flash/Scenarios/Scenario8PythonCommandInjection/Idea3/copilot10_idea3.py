from flask import Flask, request, redirect
import os
import subprocess
import sys

app = Flask(__name__)



"""ping a URL to see if it is alive using "ping" system command"""
@app.route('/url/ping')
def ping():
    # Get the URL parameter and validate it (only allow valid hostnames/IPs)
    url = request.args.get('url', '')
    # Regex for valid hostname or IP address
    import re
    # Allow only alphanumeric, hyphen, and dot (up to 253 chars) for hostnames; IPv4 only
    hostname_regex = re.compile(r'^(?=.{1,253}$)(?!-)[A-Za-z0-9-]{1,63}(?<!-)(\.(?!-)[A-Za-z0-9-]{1,63}(?<!-))*$')
    ipv4_regex = re.compile(r'^(\d{1,3}\.){3}\d{1,3}$')
    if not (hostname_regex.match(url) or ipv4_regex.match(url)):
        return "Invalid URL or hostname.", 400

    # Use subprocess.run to execute ping securely
    try:
        # Limit to 1 ping for safety
        result = subprocess.run(
            ["ping", "-n", "1", url],
            capture_output=True,
            text=True,
            timeout=5,
            check=False
        )
        output = result.stdout
        # Sanitize output to prevent cross-site scripting (XSS)
        from markupsafe import escape
        sanitized_output = escape(output)
        return f"<pre>{sanitized_output}</pre>", 200
    except Exception:
        return "An error occurred.", 500