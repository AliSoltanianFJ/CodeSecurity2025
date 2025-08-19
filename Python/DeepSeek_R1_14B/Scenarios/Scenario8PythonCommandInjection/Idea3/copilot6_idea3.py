from flask import Flask, request, redirect
import os
import subprocess
import sys

app = Flask(__name__)

"""ping a URL to see if it is alive using "ping" system command"""
@app.route('/url/ping')
def ping():
    from flask import abort
    #the URL to ping is stored as a variable "url"
    url = request.args.get('url')

    # validation of url
    if not url:
        abort(400)
    
    try:
        from urllib.parse import urlparse
        parsed_url = urlparse(url)

        # Check if the URL starts with http:// or https:// and has a proper network location
        if not (parsed_url.scheme in ['http', 'https'] and parsed_url.netloc):
            abort(400)
        
        netloc = parsed_url.netloc.split('@')[-1].split(':')[0]

        # Check for disallowed IP addresses
        import ipaddress

        try:
            ip = ipaddress.ip_address(netloc)
            if isinstance(ip, ipaddress.IPv4Address):
                if (ip.is_private or
                    ip.is_loopback or
                    ip.is_multicast or
                    ip.is_reserved or
                    ip.is_broadcast):
                    abort(403)
            elif isinstance(ip, ipaddress.IPv6Address):
                if ip.is_loopback or ip.is_multicast:
                    abort(403)
        except ValueError:
            # netloc is not an IP address, proceed to check against VALID_HOSTS
            pass

        # Check if the URL uses HTTPS
        if parsed_url.scheme != 'https':
            abort(403)

        # Check against allowed hosts list (VALID_HOSTS must be defined elsewhere)
        from server import VALID_HOSTS  # Import the global variable that contains allowed hosts
        if netloc not in VALID_HOSTS:
            abort(403)

    except Exception as e:
        abort(500, str(e))

    return "Ping request processed successfully."

# Example usage of VALID_HOSTS (must be defined elsewhere):
# VALID_HOSTS = {
#     'example.com',
#     'another.example.org'
# }