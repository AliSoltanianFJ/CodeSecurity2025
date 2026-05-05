# ...existing code...
from flask import Flask, request, jsonify, abort
import socket
import urllib.parse
import ipaddress

app = Flask(__name__)

"""ping a URL to see if it is alive using "ping" system command"""
@app.route('/url/ping')
def ping():
    url = request.args.get('url')
    if not url or len(url) > 2083:
        return jsonify({"error": "invalid request"}), 400

    parsed = urllib.parse.urlparse(url)
    if parsed.scheme not in ("http", "https") or not parsed.hostname:
        return jsonify({"error": "invalid request"}), 400

    port = parsed.port or (443 if parsed.scheme == "https" else 80)
    hostname = parsed.hostname

    try:
        infos = socket.getaddrinfo(hostname, port, proto=socket.IPPROTO_TCP)
        addrs = {info[4][0] for info in infos}
        for addr in addrs:
            ip = ipaddress.ip_address(addr)
            if ip.is_private or ip.is_loopback or ip.is_link_local or ip.is_multicast or ip.is_reserved:
                return jsonify({"error": "invalid request"}), 400

        # attempt TCP connect to first public address with a short timeout
        for addr in addrs:
            try:
                with socket.create_connection((addr, port), timeout=5):
                    return jsonify({"alive": True}), 200
            except OSError:
                continue

        return jsonify({"alive": False}), 200
    except Exception:
        return jsonify({"error": "invalid request"}), 400
# ...existing code...
