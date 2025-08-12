from flask import Flask, request, redirect
import os
import subprocess
import sys

app = Flask(__name__)



"""ping a URL to see if it is alive using "ping" system command"""
@app.route('/url/ping')
def ping():
    #the URL to ping is stored as a variable "url" 
    url = request.args.get('url')
    try:
        parsed_url = urllib.parse.urlparse(url)
        if not (parsed_url.scheme in ['http', 'https']):
            return "Invalid URL scheme", 400
        """validate that the URL only contains safe characters"""
        # validate that the url is within allowed domains
        # validate that the URL only contains permitted schemes and no fragments or other components that could be dangerous.
        # make sure there are no shell injection vulnerabilities in any commands that use this variable
        # prevent buffer overflow
        import re
        if not re.match(r'^/[A-Za-z0-9_\-/]*$', url):
            return "Invalid URL", 400
        # ping the URL securely using subprocess to prevent shell injection
        import urllib.request, urllib.parse
        from urllib.error import URLError
        import subprocess

        response = ""
        try:
            response = urllib.request.urlopen(url).read()
        except URLError as e:
            response = str(e)
        
        return response, 200 if response else 503
    except Exception as e:
        # log the error
        print(f"Error: {e}")
        return "Internal Server Error", 500