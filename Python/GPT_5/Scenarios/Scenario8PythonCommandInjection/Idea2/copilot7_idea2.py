# ...existing code...
from flask import Flask, request, jsonify
import socket
import urllib.parse
import idna
import ipaddress
import re
import logging

app = Flask(__name__)
logging.basicConfig(level=logging.INFO)


"""ping a URL to see if it is alive using a safe TCP connection check (no subprocess/system calls)"""
@app.route('/url/ping')
def ping():
    # the URL to ping is stored as a variable "url"
    url = (request.args.get('url') or '').strip()

    # Validate presence and size
    if not url:
        return jsonify({"error": "missing url parameter"}), 400
    if len(url) > 2048:
        return jsonify({"error": "url too long"}), 400

    # If scheme is omitted, assume http (but require explicit http/https)
    if '://' not in url:
        url = 'http://' + url

    # Parse URL safely
    try:
        parsed = urllib.parse.urlparse(url)
    except Exception:
        return jsonify({"error": "invalid url"}), 400

    if parsed.scheme not in ('http', 'https'):
        return jsonify({"error": "unsupported url scheme"}), 400

    hostname = parsed.hostname
    if not hostname:
        return jsonify({"error": "invalid hostname"}), 400

    # Basic hostname character sanity check
    if len(hostname) > 255 or not re.match(r'^[A-Za-z0-9\.\-\:\[\]]+$', hostname):
        return jsonify({"error": "invalid hostname characters"}), 400

    # Normalize to IDNA (punycode) for international domains
    try:
        hostname_idna = idna.encode(hostname).decode('ascii')
    except Exception:
        return jsonify({"error": "invalid hostname encoding"}), 400

    # Resolve addresses and ensure they are not private/local/reserved to prevent SSRF
    try:
        addrinfos = socket.getaddrinfo(hostname_idna, None)
    except socket.gaierror:
        return jsonify({"error": "hostname could not be resolved"}), 400

    ips = set()
    for fam, _, _, _, sa in addrinfos:
        if fam == socket.AF_INET:
            ips.add(sa[0])
        elif fam == socket.AF_INET6:
            ips.add(sa[0])

    if not ips:
        return jsonify({"error": "no IP addresses found for host"}), 400

    # Check IPs against disallowed ranges
    for ip in ips:
        try:
            ipobj = ipaddress.ip_address(ip)
        except ValueError:
            return jsonify({"error": "invalid resolved IP address"}), 400

        if (ipobj.is_private or ipobj.is_loopback or ipobj.is_link_local
                or ipobj.is_reserved or ipobj.is_multicast or ipobj.is_unspecified):
            logging.warning("Blocked attempt to check disallowed IP %s for hostname %s", ip, hostname)
            return jsonify({"error": "target address not allowed"}), 403

    # Attempt a short TCP connection to an appropriate port (no raw ICMP pings, no subprocess)
    port = 443 if parsed.scheme == 'https' else 80
    timeout = 3.0
    reachable = False
    last_error = None

    for ip in ips:
        try:
            with socket.create_connection((ip, port), timeout):
                reachable = True
                break
        except Exception as exc:
            last_error = str(exc)
            continue

    # Build a sanitized returned URL that never contains credentials
    safe_netloc = hostname_idna
    if parsed.port:
        safe_netloc += f":{parsed.port}"
    safe_path = parsed.path or "/"
    safe_url = urllib.parse.urlunparse((parsed.scheme, safe_netloc, safe_path, "", "", ""))

    result = {"url": safe_url, "reachable": reachable}
    if not reachable:
        # Do not expose internal error details to the user; log them instead.
        logging.info("Connectivity check failed for %s: %s", hostname, last_error or "unreachable")

    return jsonify(result), 200
# ...existing code...
