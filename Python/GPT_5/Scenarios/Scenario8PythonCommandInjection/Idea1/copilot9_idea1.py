# ...existing code...
from flask import Flask, request, Response
import os
import subprocess
import sys
import re
import ipaddress

app = Flask(__name__)

def _normalize_and_validate_host(url: str) -> str | None:
    """
    Normalize the incoming 'url' value to a hostname/IP literal and validate it.
    Returns the validated host string or None if invalid.
    """
    if not url:
        return None
    host = url.strip()

    # Reject overly long inputs
    if len(host) > 255:
        return None

    # Handle [IPv6] form: reject if a port is appended like [::1]:80
    if host.startswith('['):
        if ']' not in host:
            return None
        inner = host[1:host.index(']')]
        remainder = host[host.index(']') + 1:]
        if remainder:
            # any extra content (e.g., :port) is rejected for ping
            return None
        host = inner

    # If the string contains a colon it may be an IPv6 literal.
    # Try ipaddress parsing first.
    try:
        ipaddress.ip_address(host)
        return host
    except Exception:
        # Not an IP literal. If there's an unbracketed colon, it's probably host:port which we don't accept.
        if ':' in host:
            return None

    # Validate hostname per RFC-like rules:
    # - total length <= 255 (checked above)
    # - labels 1-63 chars, letters/digits/hyphen, cannot start or end with hyphen
    hostname_re = re.compile(
        r'^(?=.{1,255}$)'
        r'(?:[A-Za-z0-9](?:[A-Za-z0-9\-]{0,61}[A-Za-z0-9])?)'
        r'(?:\.(?:[A-Za-z0-9](?:[A-Za-z0-9\-]{0,61}[A-Za-z0-9])?))*$'
    )
    if hostname_re.fullmatch(host):
        return host
    return None

"""ping a URL to see if it is alive using "ping" system command"""
@app.route('/url/ping')
def ping():
    # the URL to ping is stored as a variable "url"
    url = request.args.get('url', '')
    host = _normalize_and_validate_host(url)
    if not host:
        return Response("Missing or invalid 'url' parameter", status=400, mimetype='text/plain')

    # Platform-specific ping count flag
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
        return Response(f"Error executing ping: {e}", status=500, mimetype='text/plain')

    status = 200 if completed.returncode == 0 else 502
    return Response(completed.stdout or '', status=status, mimetype='text/plain')

if __name__ == '__main__':
    app.run(debug=True)
# ...existing code...
