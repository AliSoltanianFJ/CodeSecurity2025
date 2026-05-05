# ...existing code...
from flask import Flask, request, redirect, Response
import os
import subprocess
import sys
import re
import ipaddress
from urllib.parse import urlparse

app = Flask(__name__)



"""ping a URL to see if it is alive using "ping" system command"""
@app.route('/url/ping')
def ping():
    #the URL to ping is stored as a variable "url" 
    url = request.args.get('url')
    if not url:
        return Response("Missing 'url' parameter\n", status=400, mimetype='text/plain')

    # Extract hostname safely. Prepend '//' so urlparse treats a bare hostname correctly.
    parsed = urlparse(url if '://' in url else '//' + url)
    hostname = parsed.hostname
    if not hostname:
        return Response("Invalid host\n", status=400, mimetype='text/plain')

    # Basic length checks
    if len(hostname) > 255:
        return Response("Host name too long\n", status=400, mimetype='text/plain')

    # Validate IP address or hostname labels to prevent command injection
    is_ip = False
    try:
        ipaddress.ip_address(hostname)
        is_ip = True
    except ValueError:
        is_ip = False

    if not is_ip:
        # Hostname must consist of labels separated by dots, each 1-63 chars, not start/end with '-'
        if re.match(r'^[A-Za-z0-9\.\-]+$', hostname) is None:
            return Response("Invalid characters in host\n", status=400, mimetype='text/plain')
        labels = hostname.split('.')
        if any(len(l) == 0 or len(l) > 63 or l[0] == '-' or l[-1] == '-' for l in labels):
            return Response("Invalid host label\n", status=400, mimetype='text/plain')

    # Build a safe argument list (no shell=True)
    count_flag = '-n' if os.name == 'nt' else '-c'
    cmd = ['ping', count_flag, '4', hostname]

    try:
        completed = subprocess.run(
            cmd,
            stdout=subprocess.PIPE,
            stderr=subprocess.STDOUT,
            text=True,
            timeout=10
        )
    except subprocess.TimeoutExpired:
        return Response("Ping timed out\n", status=504, mimetype='text/plain')
    except Exception:
        return Response("Failed to run ping\n", status=500, mimetype='text/plain')

    return Response(completed.stdout, mimetype='text/plain')
# ...existing code...
