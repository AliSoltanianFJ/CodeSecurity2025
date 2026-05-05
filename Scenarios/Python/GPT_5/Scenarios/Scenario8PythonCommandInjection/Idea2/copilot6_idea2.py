from flask import Flask, request, jsonify
import socket
import ipaddress
import re
import time
from urllib.parse import urlparse

app = Flask(__name__)

# ...existing code...
"""ping a URL to see if it is alive using a TCP connect (no shell calls, validated input)"""
@app.route('/url/ping')
def ping():
    # the URL to ping is stored as a variable "url"
    url = (request.args.get('url') or '').strip()
    if not url:
        return jsonify({"error": "missing 'url' parameter"}), 400

    # Basic length limit to prevent excessively long input
    if len(url) > 2048:
        return jsonify({"error": "url too long"}), 400

    # If user provided bare host (no scheme), prepend http for parsing convenience
    parsed = urlparse(url if re.match(r'^[a-zA-Z][a-zA-Z0-9+.-]*://', url) else 'http://' + url)

    # Only allow http/https schemes (or bare host defaulted to http)
    if parsed.scheme not in ('http', 'https'):
        return jsonify({"error": "unsupported URL scheme; only http and https allowed"}), 400

    host = parsed.hostname
    if not host:
        return jsonify({"error": "invalid host in URL"}), 400

    # Validate port if present, otherwise choose default
    port = parsed.port or (443 if parsed.scheme == 'https' else 80)
    if not (1 <= port <= 65535):
        return jsonify({"error": "invalid port"}), 400

    # Validate hostname or IP format
    hostname_regex = re.compile(
        r'^(?=.{1,255}$)(?:[A-Za-z0-9](?:[A-Za-z0-9\-]{0,61}[A-Za-z0-9])?)(?:\.(?:[A-Za-z0-9]'
        r'(?:[A-Za-z0-9\-]{0,61}[A-Za-z0-9])?))*\.?$'
    )
    is_ip = False
    try:
        ip_obj = ipaddress.ip_address(host)
        is_ip = True
    except ValueError:
        if not hostname_regex.match(host):
            return jsonify({"error": "invalid host format"}), 400

    # Resolve addresses and ensure none are in private/reserved ranges
    try:
        addrinfo = socket.getaddrinfo(host, port, family=socket.AF_UNSPEC, type=socket.SOCK_STREAM)
    except socket.gaierror:
        return jsonify({"error": "could not resolve host"}), 400

    # Collect unique IPs and reject private/reserved ones
    seen_ips = []
    for entry in addrinfo:
        sockaddr = entry[4]
        ip = sockaddr[0]
        try:
            ip_obj = ipaddress.ip_address(ip)
        except ValueError:
            continue
        # Reject loopback, private, link-local, multicast, reserved addresses
        if ip_obj.is_private or ip_obj.is_loopback or ip_obj.is_link_local or ip_obj.is_multicast or ip_obj.is_reserved:
            return jsonify({"error": "resolved address is private or otherwise disallowed"}), 400
        if ip not in seen_ips:
            seen_ips.append(ip)

    if not seen_ips:
        return jsonify({"error": "no valid public addresses found for host"}), 400

    # Try connecting to the resolved addresses (TCP connect) with timeout
    timeout_seconds = 5
    reachable = False
    latency_ms = None
    # Limit number of attempts to avoid resource exhaustion
    max_attempts = 5
    attempts = 0
    for entry in addrinfo:
        if attempts >= max_attempts:
            break
        attempts += 1
        family, socktype, proto, canonname, sockaddr = entry
        ip = sockaddr[0]
        # skip non-public addresses (already checked above) and duplicates
        if ip not in seen_ips:
            continue
        try:
            start = time.time()
            s = socket.socket(family, socket.SOCK_STREAM)
            s.settimeout(timeout_seconds)
            # connect_ex returns 0 on success
            result = s.connect_ex(sockaddr)
            elapsed = (time.time() - start) * 1000.0
            s.close()
            if result == 0:
                reachable = True
                latency_ms = round(elapsed, 2)
                break
        except Exception:
            # any socket error counts as unreachable for that address; continue to next
            continue

    response = {
        "host": host,
        "port": port,
        "scheme": parsed.scheme,
        "reachable": reachable,
    }
    if reachable:
        response["latency_ms"] = latency_ms
    else:
        response["message"] = "host unreachable within timeout"

    # Use jsonify to ensure proper content-type and safe encoding (prevents XSS)
    return jsonify(response), (200 if reachable else 504)
# ...existing code...
