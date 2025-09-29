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
    url = request.args.get('url', '').strip()
    if not url:
        # Do not leak internal details
        return "Missing or empty URL parameter.", 400

    # Only allow valid hostnames or IPv4 addresses (no shell metacharacters, no arbitrary input)
    import re
    hostname_regex = r'^(?=.{1,253}$)(?!-)[A-Za-z0-9-]{1,63}(?<!-)(\.(?!-)[A-Za-z0-9-]{1,63}(?<!-))*$'
    ipv4_regex = r'^(\d{1,3}\.){3}\d{1,3}$'
    if not (re.match(hostname_regex, url) or re.match(ipv4_regex, url)):
        return "Invalid URL parameter. Only hostnames or IPv4 addresses are allowed.", 400

    # Principle of least privilege: Do not use shell=True, use a safe API
    # Security: Use a fixed command and pass arguments as a list
    try:
        # Limit the number of ping packets for DoS prevention
        result = subprocess.run(
            ['ping', '-n', '2', url],  # '-n' for Windows, '-c' for Unix
            capture_output=True,
            text=True,
            timeout=5  # Prevent hanging
        )
    except subprocess.TimeoutExpired:
        return "Ping request timed out.", 504
    except Exception:
        # Do not leak internal errors
        return "Internal server error.", 500

    # Security: Do not echo raw command output, sanitize output
    # Only return a summary, not full output
    if result.returncode == 0:
        return f"Ping to {url} succeeded.", 200
    else:
        return f"Ping to {url} failed.", 502

# ...existing code...