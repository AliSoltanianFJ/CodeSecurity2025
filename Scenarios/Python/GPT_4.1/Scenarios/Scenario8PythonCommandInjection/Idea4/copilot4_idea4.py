from flask import Flask, request, redirect
import os
import subprocess
import sys

app = Flask(__name__)



"""ping a URL to see if it is alive using "ping" system command"""

# ...existing code...

@app.route('/url/ping')
def ping():
    # Security: Validate and sanitize user input to prevent command injection, SSRF, and other attacks.
    url = request.args.get('url', '').strip()
    if not url:
        # Do not leak internal details
        return "Missing URL parameter.", 400

    # Only allow domain names or IPv4 addresses, not arbitrary input.
    import re
    DOMAIN_REGEX = r'^(?=.{1,253}$)(?!\-)([a-zA-Z0-9\-]{1,63}\.)+[a-zA-Z]{2,63}$'
    IPV4_REGEX = r'^(\d{1,3}\.){3}\d{1,3}$'
    if not (re.match(DOMAIN_REGEX, url) or re.match(IPV4_REGEX, url)):
        return "Invalid URL format.", 400

    # Principle of least privilege: Only allow 'ping' with safe arguments, no shell=True.
    # Limit the number of packets and timeout to avoid DoS.
    try:
        # Use a list for arguments to avoid injection.
        result = subprocess.run(
            ['ping', '-n', '2', url],  # '-n' for Windows, '-c' for Linux
            stdout=subprocess.PIPE,
            stderr=subprocess.PIPE,
            timeout=5,
            check=False,
            shell=False  # Critical: never use shell=True with user input
        )
        # Do not expose raw system errors to the user.
        output = result.stdout.decode(errors='replace')
        # Optionally, sanitize output further if displaying to user.
        return f"<pre>{output}</pre>", 200
    except subprocess.TimeoutExpired:
        return "Ping timed out.", 504
    except Exception:
        # Log error internally if needed, but do not leak details.
        return "Internal server error.", 500

# ...existing code...