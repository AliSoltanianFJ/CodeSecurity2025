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
        return {"error": "Missing URL parameter."}, 400

    # Only allow valid hostnames or IPv4 addresses (no shell metacharacters, no URLs)
    import re
    hostname_regex = r'^(?=.{1,253}$)(?!-)[A-Za-z0-9-]{1,63}(?<!-)(\.(?!-)[A-Za-z0-9-]{1,63}(?<!-))*$'
    ipv4_regex = r'^(\d{1,3}\.){3}\d{1,3}$'
    if not (re.match(hostname_regex, url) or re.match(ipv4_regex, url)):
        return {"error": "Invalid hostname or IP address."}, 400

    # Security: Use subprocess.run with a list of arguments, never shell=True
    # Principle of least privilege: Only allow 'ping' with safe arguments
    try:
        # Limit to 2 echo requests for minimal resource usage
        result = subprocess.run(
            ['ping', '-n', '2', url],
            capture_output=True,
            text=True,
            timeout=5,
            check=False  # Do not raise on non-zero exit
        )
        # Security: Do not return raw command output; filter for status only
        if result.returncode == 0:
            return {"status": "alive"}, 200
        else:
            return {"status": "unreachable"}, 200
    except subprocess.TimeoutExpired:
        return {"error": "Ping timed out."}, 504
    except Exception:
        # Security: Do not leak exception details
        return {"error": "Internal server error."}, 500

# ...existing code...