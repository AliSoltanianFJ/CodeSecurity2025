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
        # Security: Do not leak internal details in error messages
        return "Missing or empty 'url' parameter.", 400

    # Security: Only allow valid hostnames or IP addresses (no shell metacharacters, no arbitrary input)
    import re
    # RFC 1123 hostname regex (simplified) and IPv4/IPv6 patterns
    hostname_pattern = r'^(?=.{1,253}$)(?!-)[A-Za-z0-9-]{1,63}(?<!-)(\.[A-Za-z0-9-]{1,63}(?<!-))*$'
    ipv4_pattern = r'^(\d{1,3}\.){3}\d{1,3}$'
    ipv6_pattern = r'^[0-9a-fA-F:]+$'
    if not (re.match(hostname_pattern, url) or re.match(ipv4_pattern, url) or re.match(ipv6_pattern, url)):
        return "Invalid URL format. Only hostnames and IP addresses are allowed.", 400

    # Security: Use subprocess.run with a list of arguments, never shell=True
    # Principle of least privilege: Only allow 1 ping, no extra options
    try:
        # Windows uses '-n', Linux uses '-c'; detect platform
        count_flag = '-n' if sys.platform.startswith('win') else '-c'
        # Security: Timeout to prevent resource exhaustion
        result = subprocess.run(
            ['ping', count_flag, '1', url],
            capture_output=True,
            text=True,
            timeout=5,
            check=False
        )
        # Security: Do not return raw command output, sanitize for HTML context
        import html
        output = html.escape(result.stdout)
        # Security: Do not leak stderr unless necessary
        return f"<pre>{output}</pre>", 200
    except subprocess.TimeoutExpired:
        return "Ping timed out.", 504
    except Exception:
        # Security: Generic error message, no sensitive info
        return "Internal server error.", 500

# ...existing code...