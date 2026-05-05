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
        # Do not leak internal details
        return {"error": "Missing URL parameter."}, 400

    # Only allow valid hostnames or IPv4 addresses (no shell metacharacters, no arbitrary input)
    import re
    # RFC 1123 hostname regex (simplified), or IPv4
    hostname_regex = r'^(?=.{1,253}$)(?!-)[A-Za-z0-9-]{1,63}(?<!-)(\.(?!-)[A-Za-z0-9-]{1,63}(?<!-))*$'
    ipv4_regex = r'^(\d{1,3}\.){3}\d{1,3}$'
    if not (re.match(hostname_regex, url) or re.match(ipv4_regex, url)):
        return {"error": "Invalid URL format. Only hostnames or IPv4 addresses are allowed."}, 400

    # Principle of least privilege: Do not allow pinging privileged addresses (e.g., localhost, internal IPs)
    forbidden_hosts = {'localhost', '127.0.0.1', '0.0.0.0'}
    if url.lower() in forbidden_hosts or url.startswith('192.168.') or url.startswith('10.') or url.startswith('172.'):
        return {"error": "Pinging internal or reserved addresses is not allowed."}, 403

    # Use subprocess.run with a list of arguments, never shell=True (prevents injection)
    try:
        # Security: Limit the number of ping packets and timeout
        result = subprocess.run(
            ['ping', '-n', '2', url],  # '-n' for Windows, '-c' for Unix; adjust as needed
            capture_output=True,
            text=True,
            timeout=5,
            check=False  # Do not raise on non-zero exit
        )
        # Do not leak sensitive output; only return minimal, context-aware info
        if result.returncode == 0:
            return {"status": "alive", "output": "Ping successful."}, 200
        else:
            return {"status": "unreachable", "output": "Ping failed."}, 200
    except subprocess.TimeoutExpired:
        return {"error": "Ping request timed out."}, 504
    except Exception:
        # Log error internally if needed, but do not expose details
        return {"error": "Internal server error."}, 500
# ...existing code...