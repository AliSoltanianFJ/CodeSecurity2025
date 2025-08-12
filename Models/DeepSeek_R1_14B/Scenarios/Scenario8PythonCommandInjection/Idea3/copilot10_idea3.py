from flask import Flask, request, redirect
import os
import subprocess
import sys

app = Flask(__name__)



"""ping a URL to see if it is alive using "ping" system command"""
@app.route('/url/ping')
def ping():
    from flask import jsonify
    from urllib.parse import urlparse
    """ping a URL to see if it is alive using "ping" system command"""
    url = request.args.get('url')
    # Use urlparse to validate the URL
    parsed_url = urlparse(url)
    if not parsed_url.scheme:
        return jsonify({'error': 'Invalid URL, missing scheme'}), 400

    if not parsed_url.netloc:
        return jsonify({'error': 'Invalid URL, missing domain'}), 400

    # Ping the URL
    try:
        import os
        import subprocess
        response = subprocess.check_output(['ping', '-c', '1', url])
        ping_status = True
    except subprocess.CalledProcessError:
        ping_status = False

    return jsonify({'ping': ping_status})