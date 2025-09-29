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
        return "Missing URL parameter.", 400

    # Only allow valid hostnames or IPv4 addresses (no shell metacharacters, no arbitrary input)
    import re
    hostname_regex = r'^(?=.{1,253}$)(?!-)[A-Za-z0-9-]{1,63}(?<!-)(\.(?!-)[A-Za-z0-9-]{1,63}(?<!-))*$'
    ipv4_regex = r'^(\d{1,3}\.){3}\d{1,3}$'
    if not (re.match(hostname_regex, url) or re.match(ipv4_regex, url)):
        return "Invalid URL parameter.", 400

    # Principle of least privilege: Do not use shell=True, use a fixed command and argument list
    # Use subprocess.run with check=True for proper error handling
    try:
        # Security: Limit the number of ping packets and timeout
        result = subprocess.run(
            ["ping", "-n", "2", "-w", "1000", url],  # Windows: -n count, -w timeout(ms)
            capture_output=True,
            text=True,
            check=True
        )
        # Do not expose raw command output; sanitize before returning
        output = result.stdout
        # Optionally, filter output to only show summary lines
        summary_lines = []
        for line in output.splitlines():
            if "Packets:" in line or "Lost =" in line or "Minimum =" in line or "Maximum =" in line or "Average =" in line:
                summary_lines.append(line)
        safe_output = "\n".join(summary_lines) if summary_lines else "Ping completed."
        return safe_output, 200
    except subprocess.CalledProcessError:
        # Do not leak internal errors
        return "Ping failed.", 500
    except Exception:
        # Catch-all for unexpected errors, log securely if needed
        return "Internal server error.", 500

# ...existing code...