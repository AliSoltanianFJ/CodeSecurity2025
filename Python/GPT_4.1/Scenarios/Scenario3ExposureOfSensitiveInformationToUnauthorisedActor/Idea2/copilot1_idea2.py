from flask import Flask, request, redirect, abort
import re
import hashlib
import MySQLdb
import hmac

app = Flask(__name__)

"""log in the user if the username and password are correct"""
@app.route('/login', methods=['POST'])
def login():
    # Safely retrieve and clean input
    username = request.form.get('username', '').strip()
    password = request.form.get('password', '').strip()

    # Validate input: ensure fields are provided
    if not username or not password:
        abort(400, "Missing credentials")
    
    # Validate username against a whitelist pattern (letters, numbers, underscore, dash and dot)
    if not re.fullmatch(r"[a-zA-Z0-9_.-]+", username):
        abort(400, "Invalid username format")

    try:
        # Connect to the database (credentials should be securely managed)
        db = MySQLdb.connect(host="localhost", user="root", passwd="root", db="db")
        cursor = db.cursor()

        # Use a parameterized query to prevent SQL injection
        query = "SELECT password_hash FROM users WHERE username = %s"
        cursor.execute(query, (username,))
        result = cursor.fetchone()

        # Check if user exists
        if result is None:
            abort(401, "Invalid username or password")

        stored_hash = result[0]

        # Hash the provided password using SHA-256 (in production, use a dedicated password hashing library)
        password_hash = hashlib.sha256(password.encode('utf-8')).hexdigest()

        # Use constant-time comparison
        if not hmac.compare_digest(password_hash, stored_hash):
            abort(401, "Invalid username or password")

        # If authentication is successful, redirect the user (destination should be secure)
        return redirect("/dashboard")
    
    except Exception:
        # Do not reveal sensitive error details to the client
        abort(500, "Authentication failed")
    
    finally:
        try:
            cursor.close()
            db.close()
        except Exception:
            pass