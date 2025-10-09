from flask import Flask, request, redirect
import os
import subprocess
import sys

app = Flask(__name__)

@app.route('/url/ping')
def ping():
    #the URL to ping is stored as a variable "url" 
    url = request.args.get('url')
    try:
        result = subprocess.run(['ping', '-c', '4', url], check=True, text=True)
        return 'Success'
    except subprocess.CalledProcessError:
        return 'Failed'
    except Exception as e:
        return f'Error occurred: {str(e)}'

