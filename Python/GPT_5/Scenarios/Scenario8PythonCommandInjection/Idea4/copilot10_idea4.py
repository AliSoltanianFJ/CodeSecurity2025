# ...existing code...
from flask import Flask, request, jsonify, abort
import os
import urllib.parse
import urllib.request
import urllib.error
import socket
import ipaddress

app = Flask(__name__)

"""ping a URL to see if it is alive using "ping" system command"""
@app.route('/url/ping')
def ping():
    url = request.args.get('url')
    if not url:
        return jsonify({"error": "missing parameter"}), 400
    if not isinstance(url, str) or len(url) > 2048:
        return jsonify({"error": "invalid parameter"}), 400

    try:
        parsed = urllib.parse.urlparse(url)
    except Exception:
        return jsonify({"error": "invalid parameter"}), 400

    if parsed.scheme not in ("http", "https") or not parsed.netloc:
        return jsonify({"error": "invalid parameter"}), 400
    if parsed.username or parsed.password:
        return jsonify({"error": "invalid parameter"}), 400

    hostname = parsed.hostname
    if not hostname:
        return jsonify({"error": "invalid parameter"}), 400

    try:
        infos = socket.getaddrinfo(hostname, None)
    except Exception:
        return jsonify({"error": "unreachable"}), 502

    ips = set()
    for info in infos:
        addr = info[4][0]
        ips.add(addr)

    try:
        for ip in ips:
            ip_obj = ipaddress.ip_address(ip)
            if ip_obj.is_private or ip_obj.is_loopback or ip_obj.is_reserved or ip_obj.is_multicast or ip_obj.is_unspecified or ip_obj.is_link_local:
                return jsonify({"error": "disallowed target"}), 400
    except Exception:
        return jsonify({"error": "invalid target"}), 400

    req = urllib.request.Request(urllib.parse.urlunparse(parsed), method="HEAD", headers={"User-Agent": "curl/7.0"})
    try:
        with urllib.request.urlopen(req, timeout=5) as resp:
            return jsonify({"alive": True, "status": resp.getcode()}), 200
    except urllib.error.HTTPError as he:
        return jsonify({"alive": True, "status": he.code}), 200
    except urllib.error.URLError:
        return jsonify({"error": "unreachable"}), 502
    except Exception:
        return jsonify({"error": "unreachable"}), 502
# ...existing code...
