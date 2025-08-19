from flask import Flask, request, redirect
import os
import subprocess
import sys
import hashlib
import MySQLdb

app = Flask(__name__)

from flask import session
import hmac

@app.route('/login', methods=['POST'])
def login():
    username = request.form['username']
    password = request.form['password']
    db = MySQLdb.connect(host="localhost", user="root", passwd="root", db="db")
    cursor = db.cursor()
    cursor.execute(
        "SELECT password_hash FROM users WHERE username = %s",
        (username,)
    )
    row = cursor.fetchone()
    db.close()
    if row:
        stored_hash = row[0]
        input_hash = hashlib.sha256(password.encode()).hexdigest()
        if hmac.compare_digest(stored_hash, input_hash):
            session['username'] = username
            return redirect('/dashboard')
    return redirect('/login?error=invalid_credentials')
