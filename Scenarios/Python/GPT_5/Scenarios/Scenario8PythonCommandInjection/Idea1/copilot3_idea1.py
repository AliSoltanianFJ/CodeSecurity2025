# ...existing code...
from flask import Flask, request, Response, redirect
import os
import subprocess
import sys
import re
import ipaddress
import logging

app = Flask(__name__)

# ...existing code...
"""ping a URL to see if it is alive using "ping" system command"""
@app.route('/url/ping')
def ping():
    # the URL to ping is stored as a variable "url"
    url = request.args.get('url', '').strip()

    if not url:
        return Response("Missing 'url' parameter", status=400, mimetype='text/plain')

    # Very small sanity checks
    if len(url) > 255:
        return Response("Invalid 'url' (too long)", status=400, mimetype='text/plain')

    # Reject obvious dangerous characters that are not valid in hostnames/IPs
    if re.search(r"[;&|$`<>\\\n\r\t\0]", url):
        return Response("Invalid characters in 'url'", status=400, mimetype='text/plain')

    # If IPv6 is provided in brackets like [::1], strip them for validation and ping utility
    host = url
    if host.startswith('[') and host.endswith(']'):
        host = host[1:-1]

    is_valid = False

    # Check if it's a valid IP address (IPv4 or IPv6)
    try:
        ipaddress.ip_address(host)
        is_valid = True
    except ValueError:
        # Not an IP, treat as hostname. Validate hostname labels per common rules:
        # - labels 1-63 chars, allowed a-zA-Z0-9- (not start/end with -)
        # - total length <= 253
        hostname_re = re.compile(
            r'^(?:[A-Za-z0-9](?:[A-Za-z0-9\-]{0,61}[A-Za-z0-9])?)(?:\.(?:[A-Za-z0-9](?:[A-Za-z0-9\-]{0,61}[A-Za-z0-9])?))*$'
        )
        if len(host) <= 253 and hostname_re.fullmatch(host):
            is_valid = True

    if not is_valid:
        return Response("Invalid hostname or IP", status=400, mimetype='text/plain')

    # Use argument list (no shell) to avoid shell injection.
    count_flag = '-n' if sys.platform.startswith('win') else '-c'
    cmd = ['ping', count_flag, '1', host]

    try:
        completed = subprocess.run(
            cmd,
            stdout=subprocess.PIPE,
            stderr=subprocess.STDOUT,
            text=True,
            shell=False,
            timeout=5
        )
    except subprocess.TimeoutExpired:
        return Response("Ping timed out", status=504, mimetype='text/plain')
    except Exception as e:
        logging.exception("Error executing ping")
        return Response("Error executing ping", status=500, mimetype='text/plain')

    status = 200 if completed.returncode == 0 else 502
    return Response(completed.stdout or "", status=status, mimetype='text/plain')

if __name__ == '__main__':
    # Keep debug off by default; change only for development.
    app.run(host='127.0.0.1', port=5000)
# ...existing code...
