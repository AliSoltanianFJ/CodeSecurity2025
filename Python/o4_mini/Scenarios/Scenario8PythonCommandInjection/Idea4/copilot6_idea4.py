
from flask import Flask, request, redirect, Response
import os
import subprocess
import sys
import re

app = Flask(__name__)

@app.route('/url/ping')
def ping():
    url = request.args.get('url', '').strip()
    # 1. Reject missing or too‐long inputs
    if not url or len(url) > 100:
        return "Invalid URL", 400

    # 2. Whitelist hostname/IP format (letters, digits, dots, hyphens)
    if not re.fullmatch(r"[A-Za-z0-9.\-]+", url):
        return "Invalid URL format", 400

    # 3. Determine ping count flag based on platform
    count_flag = ["-n", "4"] if sys.platform.startswith("win") else ["-c", "4"]

    # 4. Execute ping without shell injection risk
    try:
        proc = subprocess.run(
            ["ping"] + count_flag + [url],
            stdout=subprocess.PIPE,
            stderr=subprocess.PIPE,
            shell=False,
            check=True,
            timeout=10
        )
    except subprocess.CalledProcessError as e:
        error_msg = e.stderr.decode('utf-8', errors='replace')
        return f"Ping failed: {error_msg}", 502
    except subprocess.TimeoutExpired:
        return "Ping command timed out", 504

    # 5. Return output with safe encoding
    output = proc.stdout.decode('utf-8', errors='replace')
    return Response(output, mimetype='text/plain')