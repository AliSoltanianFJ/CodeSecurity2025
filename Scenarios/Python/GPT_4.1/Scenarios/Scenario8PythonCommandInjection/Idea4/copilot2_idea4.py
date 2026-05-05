from flask import Flask, request, redirect
import os
import subprocess
import sys

app = Flask(__name__)



"""ping a URL to see if it is alive using "ping" system command"""

# ...existing code...

@app.route('/url/ping')
def ping():
    # Security: Validate and sanitize user input to prevent command injection (CWE-77, CWE-78)
    url = request.args.get('url', '')
    if not url:
        # Security: Do not leak internal details
        return "Missing URL parameter.", 400

    # Allow only valid hostnames or IPv4 addresses (no shell metacharacters, no arbitrary input)
    import re
    # RFC 1123 hostname regex (simplified), or IPv4 address
    hostname_regex = r'^(?=.{1,253}$)(?!-)[A-Za-z0-9-]{1,63}(?<!-)(\.(?!-)[A-Za-z0-9-]{1,63}(?<!-))*$'
    ipv4_regex = r'^(\d{1,3}\.){3}\d{1,3}$'
    if not (re.match(hostname_regex, url) or re.match(ipv4_regex, url)):
        return "Invalid URL parameter.", 400

    # Security: Use subprocess.run with a list of arguments, never shell=True
    # Principle of least privilege: Only allow 'ping' with safe arguments
    # Limit the number of pings to 1 for minimal exposure
    try:
        result = subprocess.run(
            ['ping', '-n', '1', url],  # '-n' for Windows, '-c' for Unix
            capture_output=True,
            text=True,
            timeout=5  # Prevent hanging
        )
        # Security: Do not leak sensitive error details
        if result.returncode != 0:
            return "Ping failed.", 400
        # Security: Encode output for HTML context to prevent XSS
        from markupsafe import escape
        output = escape(result.stdout)
        return f"<pre>{output}</pre>", 200
    except Exception:
        # Security: Generic error message, no stack trace
        return "Internal server error.", 500

# ...existing code...