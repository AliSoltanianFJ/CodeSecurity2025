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

    # Only allow valid hostnames or IPv4 addresses (no shell metacharacters, no URLs)
    import re
    # RFC 1123 hostname regex (simplified) and IPv4 regex
    hostname_regex = r'^(?=.{1,253}$)(?!-)[A-Za-z0-9-]{1,63}(?<!-)(\.(?!-)[A-Za-z0-9-]{1,63}(?<!-))*$'
    ipv4_regex = r'^(\d{1,3}\.){3}\d{1,3}$'
    if not (re.match(hostname_regex, url) or re.match(ipv4_regex, url)):
        return "Invalid host format.", 400

    # Principle of least privilege: Do not allow pinging privileged addresses (e.g., localhost, private IPs)
    private_ip_regex = (
        r'^127\.'  # Loopback
        r'|^10\.'
        r'|^192\.168\.'
        r'|^172\.(1[6-9]|2[0-9]|3[0-1])\.'
    )
    if re.match(private_ip_regex, url):
        return "Pinging private or loopback addresses is not allowed.", 403

    # Security: Use subprocess.run with a list of arguments, never shell=True
    # Limit ping to 2 packets, timeout 2 seconds
    try:
        result = subprocess.run(
            ['ping', '-n', '2', '-w', '2000', url],
            capture_output=True,
            text=True,
            check=False,
            timeout=5
        )
        # Security: Do not return raw command output, only a summary
        if result.returncode == 0:
            return f"Host '{url}' is reachable.", 200
        else:
            return f"Host '{url}' is not reachable.", 200
    except subprocess.TimeoutExpired:
        return f"Ping to '{url}' timed out.", 504
    except Exception:
        # Security: Generic error message, no stack trace
        return "Internal server error.", 500

# ...existing code...