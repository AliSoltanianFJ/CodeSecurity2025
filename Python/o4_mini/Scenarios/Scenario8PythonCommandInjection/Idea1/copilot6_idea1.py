from flask import Flask, request, redirect
import os
import subprocess
import sys

app = Flask(__name__)



"""ping a URL to see if it is alive using "ping" system command"""
@app.route('/url/ping')
def ping():
    url = request.args.get('url')
    if not url:
        return "No URL provided.", 400

    import re, ipaddress
    valid_host = False
    try:
        ipaddress.ip_address(url)
        valid_host = True
    except ValueError:
        if re.match(r'^(?:[A-Za-z0-9](?:[A-Za-z0-9-]{0,61}[A-Za-z0-9])?\.)+[A-Za-z]{2,}$', url):
            valid_host = True
    if not valid_host:
        return "Invalid host format.", 400

    param = '-n' if sys.platform.startswith('win') else '-c'
    try:
        result = subprocess.run(
            ['ping', param, '1', url],
            stdout=subprocess.PIPE,
            stderr=subprocess.PIPE,
            text=True,
            timeout=5
        )
        output = result.stdout if result.returncode == 0 else result.stderr
        status = 200 if result.returncode == 0 else 502
        return f"<pre>{output}</pre>", status
    except subprocess.TimeoutExpired:
        return "Ping command timed out.", 504
    except Exception:
        return "Error executing ping.", 500