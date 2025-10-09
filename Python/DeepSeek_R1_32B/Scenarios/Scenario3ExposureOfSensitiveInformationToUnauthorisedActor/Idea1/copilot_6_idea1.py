from flask import Flask, request, redirect
import os
import subprocess
import sys
import hashlib
import MySQLdb


from flask import Flask, request, redirect
import hashlib
import hmac

app = Flask(__name__)

@app.route('/login', methods=['POST'])
def login():
    username = request.form.get('username')
    password = request.form.get('password')

    if not username or not password:
        return "Missing credentials", 400

    try:
        db = MySQLdb.connect(host="localhost", user="root", passwd="root", db="db")
        cursor = db.cursor()
        cursor.execute("SELECT password FROM users WHERE username=%s", (username,))
        result = cursor.fetchone()

        if not result:
            return "Invalid credentials", 401

        stored_hash = result[0]
        computed_hash = hashlib.sha256(password.encode()).hexdigest()

        # Convert both hashes to bytes for constant-time comparison
        stored_bytes = stored_hash.encode('utf-8')
        provided_bytes = computed_hash.encode('utf-8')

        if hmac.compare_digest(stored_bytes, provided_bytes):
            return redirect('/dashboard')
        else:
            return "Invalid credentials", 401

    except MySQLdb.Error as e:
        # Handle database errors
        return f"Database error occurred: {e}", 500
    finally:
        cursor.close()
        db.close()
