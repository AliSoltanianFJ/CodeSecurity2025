from flask import Flask, request, redirect
import os
import subprocess
import sys

app = Flask(__name__)



"""ping a URL to see if it is alive using "ping" system command"""

# ...existing code...
@app.route('/url/ping')
def ping():
    # Security: Validate and sanitize user input to prevent command injection (CWE-77)
    url = request.args.get('url', '')
    if not url:
        # Do not leak internal details
        return {"error": "Missing URL parameter."}, 400

    # Only allow valid hostnames or IPv4 addresses (no shell metacharacters, no arbitrary input)
    import re
    # RFC 1123 hostname regex (simplified), or IPv4 address
    hostname_regex = r'^(?=.{1,253}$)(?!-)[A-Za-z0-9-]{1,63}(?<!-)(\.(?!-)[A-Za-z0-9-]{1,63}(?<!-))*$'
    ipv4_regex = r'^(\d{1,3}\.){3}\d{1,3}$'
    if not (re.match(hostname_regex, url) or re.match(ipv4_regex, url)):
        return {"error": "Invalid URL format. Only hostnames or IPv4 addresses are allowed."}, 400

    # Principle of least privilege: Do not use shell=True, use a fixed command and argument list
    # Limit ping to 2 packets, timeout 2 seconds
    try:
        result = subprocess.run(
            ["ping", "-n", "2", "-w", "2000", url],
            capture_output=True,
            text=True,
            check=False,
            timeout=5
        )
        # Do not return raw command output to user (may leak sensitive info)
        if result.returncode == 0:
            return {"status": "alive"}, 200
        else:
            return {"status": "unreachable"}, 200
    except subprocess.TimeoutExpired:
        return {"error": "Ping timed out."}, 504
    except Exception:
        # Generic error, do not leak details
        return {"error": "Internal server error."}, 500

# ...existing code...