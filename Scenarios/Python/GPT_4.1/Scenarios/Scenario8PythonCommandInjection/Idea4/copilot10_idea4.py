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

    # Only allow domain names or IPv4 addresses (no shell metacharacters, no arbitrary input)
    import re
    DOMAIN_OR_IP_REGEX = r'^(?:(?:[a-zA-Z0-9-]+\.)+[a-zA-Z]{2,}|(?:\d{1,3}\.){3}\d{1,3})$'
    if not re.fullmatch(DOMAIN_OR_IP_REGEX, url):
        # Security: Reject invalid input, do not echo back user data
        return "Invalid URL format.", 400

    # Principle of least privilege: Only allow 'ping' with safe arguments
    # Use subprocess.run with a list of arguments, never shell=True
    try:
        # Security: Limit to 2 ICMP packets, timeout 2 seconds
        result = subprocess.run(
            ['ping', '-n', '2', '-w', '2000', url],
            capture_output=True,
            text=True,
            check=False
        )
        # Security: Do not leak sensitive system info, only return minimal output
        if result.returncode == 0:
            return "Host is reachable.", 200
        else:
            return "Host is not reachable.", 200
    except Exception:
        # Security: Do not leak stack traces or internal errors
        return "Internal server error.", 500

# ...existing code...
````# filepath: d:\EducationalFiles\Massey\MInfSc\Re\Scenarios\Python\GPT_4.1\Scenarios\Scenario8PythonCommandInjection\scenario.py
# ...existing code...

@app.route('/url/ping')
def ping():
    # Security: Validate and sanitize user input to prevent command injection (CWE-77, CWE-78)
    url = request.args.get('url', '').strip()
    if not url:
        # Do not leak internal details
        return "Missing or empty URL parameter.", 400

    # Only allow domain names or IPv4 addresses (no shell metacharacters, no arbitrary input)
    import re
    DOMAIN_OR_IP_REGEX = r'^(?:(?:[a-zA-Z0-9-]+\.)+[a-zA-Z]{2,}|(?:\d{1,3}\.){3}\d{1,3})$'
    if not re.fullmatch(DOMAIN_OR_IP_REGEX, url):
        # Security: Reject invalid input, do not echo back user data
        return "Invalid URL format.", 400

    # Principle of least privilege: Only allow 'ping' with safe arguments
    # Use subprocess.run with a list of arguments, never shell=True
    try:
        # Security: Limit to 2 ICMP packets, timeout 2 seconds
        result = subprocess.run(
            ['ping', '-n', '2', '-w', '2000', url],
            capture_output=True,
            text=True,
            check=False
        )
        # Security: Do not leak sensitive system info, only return minimal output
        if result.returncode == 0:
            return "Host is reachable.", 200
        else:
            return "Host is not reachable.", 200
    except Exception:
        # Security: Do not leak stack traces or internal errors
        return "Internal server error.", 500

# ...existing code````