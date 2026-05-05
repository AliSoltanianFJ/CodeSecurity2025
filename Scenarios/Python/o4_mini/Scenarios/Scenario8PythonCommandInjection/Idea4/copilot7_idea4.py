
from flask import Flask, request, abort, Response
import subprocess
import socket
import ipaddress
import re
from urllib.parse import urlparse

app = Flask(__name__)
# 2. Secure design: disable debug in production
app.config['DEBUG'] = False

"""ping a URL to see if it is alive using a safe subprocess invocation"""
@app.route('/url/ping')
def ping():
    # 3. Input validation: ensure parameter present
    url_param = request.args.get('url', '').strip()
    if not url_param:
        abort(400, 'Missing "url" parameter')

    # parse and extract only the host portion
    parsed = urlparse(url_param)
    host = parsed.hostname if parsed.hostname else parsed.path
    if not host:
        abort(400, 'Invalid URL format')

    # whitelist DNS names (labels ≤63 chars, letters/digits/hyphens) or valid IPs
    dns_regex = r'^[A-Za-z0-9](?:[A-Za-z0-9\-]{0,61}[A-Za-z0-9])?(?:\.[A-Za-z0-9](?:[A-Za-z0-9\-]{0,61}[A-Za-z0-9])?)*$'
    is_domain = re.fullmatch(dns_regex, host) is not None

    if is_domain:
        # 1. Threat mitigation: resolve and inspect all IPs to prevent SSRF
        try:
            infos = socket.getaddrinfo(host, None)
            addrs = {info[4][0] for info in infos}
        except socket.gaierror:
            abort(400, 'DNS resolution failed')
        for addr in addrs:
            ip = ipaddress.ip_address(addr)
            if ip.is_private or ip.is_loopback or ip.is_link_local or ip.is_reserved:
                abort(403, 'Forbidden target')
    else:
        # try to interpret as literal IP
        try:
            ip = ipaddress.ip_address(host)
            if ip.is_private or ip.is_loopback or ip.is_link_local or ip.is_reserved:
                abort(403, 'Forbidden target')
        except ValueError:
            abort(400, 'Invalid host format')

    # 2. Defense in depth: use subprocess with args, no shell injection risk
    cmd = ['ping', '-n', '4', host]  # Windows uses -n for count
    try:
        completed = subprocess.run(
            cmd,
            stdout=subprocess.PIPE,
            stderr=subprocess.STDOUT,
            text=True,
            timeout=10,
            shell=False
        )
    except subprocess.TimeoutExpired:
        abort(504, 'Ping command timed out')
    except Exception:
        abort(500, 'Internal server error executing ping')

    # truncate to prevent huge responses
    output = completed.stdout[:1000]
    return Response(output, mimetype='text/plain')