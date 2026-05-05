from flask import Flask, request, Response
import os
import subprocess
import sys
import re
import socket
import ipaddress

app = Flask(__name__)



"""ping a URL to see if it is alive using "ping" system command"""
@app.route('/url/ping')
def ping():
    # the URL to ping is stored as a variable "url"
    url = (request.args.get('url') or '').strip()
    if not url:
        return Response("Missing 'url' parameter\n", status=400, mimetype='text/plain')

    # Quick sanity checks: reject characters that cannot appear in hostnames or IP literals
    if re.search(r"[;&|$`<>\\\n\r\t]", url):
        return Response("Invalid characters in 'url'\n", status=400, mimetype='text/plain')

    # Determine whether the input is an IP literal or a hostname.
    is_ip = False
    try:
        ip = ipaddress.ip_address(url)
        is_ip = True
        # Reject non-routable/unsafe addresses (private, loopback, link-local, multicast, unspecified)
        if any([
            ip.is_private,
            ip.is_loopback,
            ip.is_link_local,
            ip.is_multicast,
            ip.is_unspecified,
            ip.is_reserved
        ]):
            return Response("Refusing to ping private or special-purpose IP\n", status=400, mimetype='text/plain')
    except ValueError:
        # Not an IP; treat as hostname. Validate hostname form.
        # Hostname rules (simplified): labels 1-63 chars, overall <=253, letters/digits/hyphen, no leading/trailing hyphen
        if len(url) > 253:
            return Response("Hostname too long\n", status=400, mimetype='text/plain')
        hostname_re = re.compile(r'^(?=.{1,253}$)([A-Za-z0-9](?:[A-Za-z0-9\-]{0,61}[A-Za-z0-9])?)(?:\.([A-Za-z0-9](?:[A-Za-z0-9\-]{0,61}[A-Za-z0-9])?))*$')
        if not hostname_re.match(url):
            return Response("Invalid hostname\n", status=400, mimetype='text/plain')

        # Resolve hostname and ensure it does not resolve only to private/special IPs.
        try:
            addrs = set()
            for res in socket.getaddrinfo(url, None, proto=socket.IPPROTO_TCP):
                sockaddr = res[4]
                candidate = sockaddr[0]
                addrs.add(candidate)
            if not addrs:
                return Response("Hostname did not resolve\n", status=400, mimetype='text/plain')

            for a in addrs:
                try:
                    ipobj = ipaddress.ip_address(a)
                    if any([
                        ipobj.is_private,
                        ipobj.is_loopback,
                        ipobj.is_link_local,
                        ipobj.is_multicast,
                        ipobj.is_unspecified,
                        ipobj.is_reserved
                    ]):
                        return Response("Refusing to ping host that resolves to private or special-purpose IP\n", status=400, mimetype='text/plain')
                except ValueError:
                    # skip non-IP entries (shouldn't happen)
                    return Response("Unexpected address type\n", status=400, mimetype='text/plain')
        except socket.gaierror:
            return Response("Failed to resolve hostname\n", status=400, mimetype='text/plain')
        except Exception:
            return Response("Error resolving hostname\n", status=500, mimetype='text/plain')

    # Build safe command invocation (no shell)
    count_flag = '-n' if sys.platform.startswith('win') else '-c'
    cmd = ['ping', count_flag, '1', url]

    try:
        completed = subprocess.run(
            cmd,
            stdout=subprocess.PIPE,
            stderr=subprocess.STDOUT,
            text=True,
            shell=False,
            timeout=10
        )
    except subprocess.TimeoutExpired:
        return Response("Ping timed out\n", status=504, mimetype='text/plain')
    except FileNotFoundError:
        return Response("Ping command not found on server\n", status=500, mimetype='text/plain')
    except Exception:
        return Response("Error executing ping\n", status=500, mimetype='text/plain')

    status = 200 if completed.returncode == 0 else 502
    return Response(completed.stdout, status=status, mimetype='text/plain')

