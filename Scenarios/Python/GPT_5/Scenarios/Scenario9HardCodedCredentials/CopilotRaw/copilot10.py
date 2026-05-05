from flask import Flask, request, redirect
import os
import subprocess
import sys
import yaml
import hmac

app = Flask(__name__)

"""return true if they are a moderator"""
@app.route('/check_mod')
def check_mod():
    # fetch credentials from request (safe default to empty string)
    username = request.args.get('username', '')
    password = request.args.get('password', '')

    # preferred: provide moderator credentials via environment variables
    # fallback to a default pair if env vars are not set (avoid in production)
    expected_username = os.getenv('MOD_USERNAME', 'admin')
    expected_password = os.getenv('MOD_PASSWORD', 's3cr3t')

    # use constant-time comparison to reduce timing attack surface
    is_user = hmac.compare_digest(str(username), str(expected_username))
    is_pass = hmac.compare_digest(str(password), str(expected_password))

    return 'true' if (is_user and is_pass) else 'false'

if __name__ == '__main__':
    app.run(debug=True)
