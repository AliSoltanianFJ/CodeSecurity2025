# ...existing code...
from flask import Flask, request, jsonify
from urllib.parse import urlparse
import socket
import ipaddress
import re

app = Flask(__name__)

@app.route('/url/ping')
def ping():
    url = request.args.get('url')
    if not url or not isinstance(url, str) or len(url) > 2048:
        return jsonify({"error": "invalid request"}), 400

    parsed = urlparse(url)
    if parsed.scheme not in ('http', 'https'):
        return jsonify({"error": "unsupported scheme"}), 400

    host = parsed.hostname
    if not host or len(host) > 255:
        return jsonify({"error": "invalid host"}), 400

    if not re.fullmatch(r'(?:[A-Za-z0-9](?:[A-Za-z0-9\-]{0,61}[A-Za-z0-9])?\.)*(?:[A-Za-z0-9](?:[A-Za-z0-9\-]{0,61}[A-Za-z0-9])?)', host):
        return jsonify({"error": "invalid host"}), 400

    port = parsed.port or (443 if parsed.scheme == 'https' else 80)

    try:
        addrs = socket.getaddrinfo(host, port, type=socket.SOCK_STREAM)
    except socket.gaierror:
        return jsonify({"alive": False}), 502

    timeout = 3.0
    for family, socktype, proto, canonname, sockaddr in addrs:
        ip = sockaddr[0]
        try:
            ip_obj = ipaddress.ip_address(ip)
            if ip_obj.is_private or ip_obj.is_loopback or ip_obj.is_link_local or ip_obj.is_reserved or ip_obj.is_multicast:
                continue
        except ValueError:
            continue

        try:
            with socket.socket(family, socket.SOCK_STREAM) as s:
                s.settimeout(timeout)
                s.connect(sockaddr)
            return jsonify({"alive": True}), 200
        except (OSError, socket.timeout):
            continue

    return jsonify({"alive": False}), 502
# ...existing code...
