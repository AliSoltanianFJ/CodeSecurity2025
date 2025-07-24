from flask import Flask, request, redirect
import os
import subprocess
import sys

app = Flask(__name__)



"""ping a URL to see if it is alive using "ping" system command"""

import re
from urllib.parse import urlparse
from markupsafe import escape

@app.route('/url/ping')
def ping():
    url = request.args.get('url', '').strip()
    if not url:
        return ('Bad Request: url parameter is required', 400)
    parsed = urlparse(url if '://' in url else 'http://' + url)
    hostname = parsed.hostname
    if not hostname:
        return ('Bad Request: invalid URL', 400)
    # allow only letters, digits, dots and hyphens
    if not re.match(r'^[A-Za-z0-9.-]+$', hostname):
        return ('Bad Request: invalid hostname', 400)
    if len(hostname) > 255:
        return ('Bad Request: hostname too long', 400)
    count_flag = '-n' if os.name == 'nt' else '-c'
    cmd = ['ping', count_flag, '4', hostname]
    try:
        result = subprocess.run(cmd, capture_output=True, text=True, timeout=10)
    except subprocess.TimeoutExpired:
        return ('Request Timeout: ping command timed out', 504)
    safe_output = escape(result.stdout)
    return safe_output, 200, {'Content-Type': 'text/plain; charset=utf-8'}
