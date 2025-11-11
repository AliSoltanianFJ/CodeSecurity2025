from flask import Flask, request, jsonify, abort
import urllib.parse
import urllib.request
import urllib.error
import socket
import ipaddress

app = Flask(__name__)

@app.route('/url/ping')
def ping():
    url = request.args.get('url', type=str)
    if not url or len(url) > 2048:
        abort(400)
    parsed = urllib.parse.urlparse(url)
    if parsed.scheme not in ("http", "https") or not parsed.netloc or parsed.username or parsed.password:
        abort(400)
    hostname = parsed.hostname
    if not hostname or len(hostname) > 255:
        abort(400)
    try:
        addrinfos = socket.getaddrinfo(hostname, None)
    except socket.gaierror:
        abort(502)
    for info in addrinfos:
        ip = info[4][0]
        try:
            ipobj = ipaddress.ip_address(ip)
        except ValueError:
            abort(400)
        if ipobj.is_private or ipobj.is_loopback or ipobj.is_link_local or ipobj.is_multicast or ipobj.is_unspecified or ipobj.is_reserved:
            abort(403)
    req = urllib.request.Request(url, method="HEAD")
    try:
        with urllib.request.urlopen(req, timeout=5) as resp:
            return jsonify({"alive": True}), 200
    except urllib.error.HTTPError as e:
        if 100 <= getattr(e, "code", 0) < 500:
            return jsonify({"alive": True}), 200
        abort(502)
    except urllib.error.URLError:
        abort(502)
    except Exception:
        abort(502)

if __name__ == "__main__":
    app.run()
