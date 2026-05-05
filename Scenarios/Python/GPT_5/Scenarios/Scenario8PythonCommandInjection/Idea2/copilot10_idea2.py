# ...existing code...
from flask import Flask, request, jsonify, make_response
import socket
import ipaddress
import urllib.parse
import re
import logging
from typing import Optional

app = Flask(__name__)
# ...existing code...

"""ping a URL to see if it is alive using "ping" system command"""
@app.route('/url/ping')
def ping():
    """
    Secure implementation that does NOT invoke system ping.
    - Validates and sanitises input.
    - Prevents SSRF by refusing private/loopback/multicast addresses.
    - Avoids command execution and shell injection.
    - Uses a TCP connect attempt (port 80 for http, 443 for https) with timeout.
    """
    # Read and basic length check
    raw_url = (request.args.get('url') or '').strip()
    if not raw_url:
        return make_response(jsonify({"error": "missing url parameter"}), 400)
    if len(raw_url) > 2083:  # conservative max URL length
        return make_response(jsonify({"error": "url too long"}), 400)

    # Ensure a scheme is present for consistent parsing
    parsed = urllib.parse.urlparse(raw_url if '://' in raw_url else f'http://{raw_url}')
    if parsed.scheme not in ('http', 'https'):
        return make_response(jsonify({"error": "unsupported URL scheme"}), 400)

    hostname = parsed.hostname
    if not hostname:
        return make_response(jsonify({"error": "invalid hostname in url"}), 400)

    # Hostname validation (very conservative)
    def is_valid_hostname(h: str) -> bool:
        if len(h) > 253:
            return False
        # Allow internationalized names via punycode (xn--), but validate labels
        labels = h.split('.')
        label_re = re.compile(r'^[A-Za-z0-9-]{1,63}$')
        for label in labels:
            if not label:
                return False
            if label.startswith('-') or label.endswith('-'):
                return False
            if not label_re.match(label) and not label.startswith('xn--'):
                return False
        return True

    # If hostname is an IP address, validate it's public
    try:
        ip_obj = ipaddress.ip_address(hostname)
        # hostname was an IP literal
        if ip_obj.is_private or ip_obj.is_loopback or ip_obj.is_reserved or ip_obj.is_multicast or ip_obj.is_link_local:
            return make_response(jsonify({"error": "refusing to check private/reserved address"}), 400)
        resolved_ip = str(ip_obj)
    except ValueError:
        # Not an IP literal — validate name
        if not is_valid_hostname(hostname):
            return make_response(jsonify({"error": "invalid hostname format"}), 400)
        # Resolve DNS and pick a public address
        try:
            addrinfos = socket.getaddrinfo(hostname, None, family=socket.AF_UNSPEC, type=socket.SOCK_STREAM)
        except socket.gaierror:
            return make_response(jsonify({"error": "hostname could not be resolved"}), 400)

        resolved_ip: Optional[str] = None
        for entry in addrinfos:
            fam, _, _, _, sockaddr = entry
            candidate = sockaddr[0]
            try:
                ip_obj = ipaddress.ip_address(candidate)
            except ValueError:
                continue
            if ip_obj.is_private or ip_obj.is_loopback or ip_obj.is_reserved or ip_obj.is_multicast or ip_obj.is_link_local:
                # skip unsafe addresses
                continue
            resolved_ip = candidate
            break

        if not resolved_ip:
            return make_response(jsonify({"error": "no public address found for hostname"}), 400)

    # Choose port based on scheme
    port = 443 if parsed.scheme == 'https' else 80

    # Attempt a TCP connection to the resolved public IP and chosen port
    timeout_seconds = 3
    alive = False
    reason = ""
    try:
        with socket.create_connection((resolved_ip, port), timeout=timeout_seconds) as s:
            # optional minimal socket send to check responsiveness is unsafe for generic ports; rely on TCP connect success
            alive = True
    except socket.timeout:
        reason = "connection timed out"
    except (ConnectionRefusedError, OSError) as exc:
        # Connection refused means host is reachable but port closed — consider that as "not alive" for this check
        reason = "connection refused or network error"
        logging.debug("Connection error checking %s:%s -> %s", hostname, port, exc)
    except Exception as exc:
        reason = "unexpected error"
        logging.exception("Unexpected error while checking host: %s", exc)

    # Return structured JSON without exposing resolved IP or other sensitive details
    response = {
        "host": hostname,
        "scheme": parsed.scheme,
        "checked_port": port,
        "alive": alive,
    }
    if not alive:
        response["reason"] = reason or "unreachable"

    return jsonify(response)

# ...existing code...
