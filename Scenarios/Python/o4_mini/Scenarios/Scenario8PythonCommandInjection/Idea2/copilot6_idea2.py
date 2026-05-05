from flask import Flask, request, redirect
import os
import subprocess
import sys
import re
from markupsafe import escape

app = Flask(__name__)

"""ping a URL to see if it is alive using "ping" system command"""
@app.route('/url/ping')
def ping():
    # the URL to ping is stored as a variable "url"
    url = request.args.get('url', '').strip()
    if not url:
        return 'Missing "url" parameter', 400

    # only allow hostnames (letters, digits, hyphens, dots) or IPv6 in brackets
    host_pattern = re.compile(r'^([A-Za-z0-9\-\.]+|\[[0-9A-Fa-f:]+\])$')
    if not host_pattern.match(url):
        return 'Invalid "url" parameter', 400

    # choose ping count flag based on OS
    ping_flag = '-n' if os.name == 'nt' else '-c'

    try:
        completed = subprocess.run(
            ['ping', ping_flag, '4', url],
            stdout=subprocess.PIPE,
            stderr=subprocess.PIPE,
            text=True,
            timeout=10             # avoid hanging
        )
    except subprocess.TimeoutExpired:
        return 'Ping command timed out', 504

    # if ping failed, show stderr; otherwise stdout
    output = completed.stdout if completed.returncode == 0 else completed.stderr

    # escape to prevent XSS and wrap in <pre> for readability
    return f'<pre>{escape(output)}</pre>'