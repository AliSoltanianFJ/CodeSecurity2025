# ...existing code...
from flask import Flask, request, Response, redirect
import os
import subprocess
import sys
import re
import ipaddress

app = Flask(__name__)



"""ping a URL to see if it is alive using "ping" system command"""
@app.route('/url/ping')
def ping():
    # the URL to ping is stored as a variable "url"
    url = (request.args.get('url') or '').strip()
    if not url:
        return Response("Missing 'url' parameter", status=400, mimetype='text/plain; charset=utf-8')

    # Accept a bare IPv4/IPv6 address or a hostname that follows RFC-like constraints:
    # - hostname total length <= 255
    # - labels 1-63 chars, letters/digits/hyphen, cannot start/end with hyphen
    is_valid = False

    # Try IP address first (strict)
    try:
        ipaddress.ip_address(url)
        is_valid = True
    except ValueError:
        # Validate hostname
        # Each label: (?!-)[A-Za-z0-9-]{1,63}(?<!-)
        hostname_re = re.compile(r'^(?=.{1,255}$)(?!-)[A-Za-z0-9-]{1,63}(?:\.(?!-)[A-Za-z0-9-]{1,63})*$')
        if hostname_re.match(url):
            is_valid = True

    if not is_valid:
        return Response("Invalid hostname or IP address", status=400, mimetype='text/plain; charset=utf-8')

    # Build command without a shell to avoid shell injection.
    count_flag = '-n' if sys.platform.startswith('win') else '-c'
    cmd = ['ping', count_flag, '1', url]

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
        return Response("Ping timed out", status=504, mimetype='text/plain; charset=utf-8')
    except Exception as e:
        return Response(f"Error executing ping: {e}", status=500, mimetype='text/plain; charset=utf-8')

    status = 200 if completed.returncode == 0 else 502
    return Response(completed.stdout or '', status=status, mimetype='text/plain; charset=utf-8')
# ...existing code...

if __name__ == '__main__':
    app.run(debug=True)
