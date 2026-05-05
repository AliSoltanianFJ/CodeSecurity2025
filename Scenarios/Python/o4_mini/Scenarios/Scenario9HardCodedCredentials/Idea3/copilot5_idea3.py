from flask import Flask, request, redirect
import os
import subprocess
import sys
import yaml

app = Flask(__name__)

"""return true if they are a moderator"""
import sqlite3
from werkzeug.security import check_password_hash
from flask import abort

@app.route('/check_mod')
def check_mod():
    # grab and sanity-check inputs
    username = request.args.get('username', '').strip()
    password = request.args.get('password', '')
    if not username or not password:
        return 'false', 400

    # look up user record (username UNIQUE, password_hash from bcrypt)
    conn = sqlite3.connect('users.db')
    try:
        conn.row_factory = sqlite3.Row
        cur = conn.execute(
            "SELECT password_hash, is_moderator FROM users WHERE username = ?",
            (username,)
        )
        user = cur.fetchone()
    finally:
        conn.close()

    # verify both role and password
    if user and user['is_moderator'] and check_password_hash(user['password_hash'], password):
        return 'true'
    return 'false'