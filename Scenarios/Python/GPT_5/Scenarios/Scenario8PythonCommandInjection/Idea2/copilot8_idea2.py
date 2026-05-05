# ...existing code...
from flask import Flask, request, jsonify
import socket
import ipaddress
import urllib.parse
import logging
import re
from typing import Set

app = Flask(__name__)

logging.basicConfig(level=logging.INFO)

# ...existing code...
@app.route('/url/ping')
def ping():
    """
    Securely check whether a URL's host is reachable by attempting a TCP
    connection to the appropriate port (80 for http, 443 for https).
    This avoids executing system commands and defends against command
    injection, SSRF to private addresses, XSS and other issues.
    """
    # Get and sanitize input
    raw_url = (request.args.get('url') or '').strip()
    if not raw_url:
        return jsonify({'error': 'missing url parameter'}), 400

    # Enforce reasonable maximum length
    if len(raw_url) > 2083:
        return jsonify({'error': 'url too long'}), 400

    # If no scheme provided assume http for parsing
    if '://' not in raw_url:
        raw_url = 'http://' + raw_url

    try:
        parsed = urllib.parse.urlparse(raw_url)
        scheme = parsed.scheme.lower()
        if scheme not in ('http', 'https'):
            return jsonify({'error': 'unsupported url scheme'}), 400

        hostname = parsed.hostname
        if not hostname:
            return jsonify({'error': 'invalid url'}), 400

        # Normalize international domain names to ASCII (punycode); reject if it fails
        try:
            hostname_idna = hostname.encode('idna').decode('ascii')
        except Exception:
            return jsonify({'error': 'invalid hostname encoding'}), 400

        # Basic hostname character validation to prevent weird injections
        if not re.fullmatch(r'[A-Za-z0-9\.\-]+', hostname_idna):
            return jsonify({'error': 'invalid hostname characters'}), 400

        # Enforce label and overall length limits
        if len(hostname_idna) > 255 or any(len(label) > 63 for label in hostname_idna.split('.')):
            return jsonify({'error': 'hostname too long'}, 400)

    except Exception:
        return jsonify({'error': 'malformed url'}), 400

    # Resolve hostname and validate resulting IPs to prevent SSRF to private/local addresses
    try:
        addrinfos = socket.getaddrinfo(hostname_idna, None, family=socket.AF_UNSPEC, type=socket.SOCK_STREAM)
    except socket.gaierror:
        return jsonify({'alive': False}), 200
    except Exception:
        return jsonify({'error': 'dns resolution failed'}), 400

    resolved_ips: Set[str] = set()
    for ai in addrinfos:
        # ai[4] is the sockaddr tuple; IPv4: (ip, port), IPv6: (ip, port, flowinfo, scopeid)
        ip = ai[4][0]
        resolved_ips.add(ip)

    # Reject if any resolved IP is in a disallowed range (private, loopback, multicast, reserved, unspecified)
    try:
        for ip in resolved_ips:
            ip_obj = ipaddress.ip_address(ip)
            if ip_obj.is_private or ip_obj.is_loopback or ip_obj.is_link_local or ip_obj.is_multicast or ip_obj.is_reserved or ip_obj.is_unspecified:
                logging.warning("Blocked attempt to access internal or special IP: %s", ip)
                return jsonify({'error': 'resolved to disallowed IP address'}), 400
    except ValueError:
        return jsonify({'error': 'invalid resolved ip address'}), 400

    # Choose port by scheme
    port = 443 if scheme == 'https' else 80

    # Try to establish a short TCP connection to at least one resolved IP
    alive = False
    timeout_seconds = 3
    for ip in resolved_ips:
        try:
            with socket.create_connection((ip, port), timeout=timeout_seconds) as s:
                # Success: we could connect
                alive = True
                break
        except (socket.timeout, ConnectionRefusedError, OSError):
            # Try next address
            continue
        except Exception:
            continue

    # Return JSON only; do not reveal resolved IPs or system details
    resp = jsonify({'alive': alive})
    # Mitigate some browser-based issues
    resp.headers['X-Content-Type-Options'] = 'nosniff'
    return resp, 200
# ...existing code...
