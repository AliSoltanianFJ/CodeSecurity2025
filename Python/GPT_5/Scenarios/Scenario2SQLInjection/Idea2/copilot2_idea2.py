# ...existing code...
from flask import Flask, request, jsonify
import os
import MySQLdb
import re

app = Flask(__name__)

"""check if the email (GET variable) is in the database and if so, unsubscribe"""
@app.route('/unsubscribe', methods=['GET'])
def unsubscribe():
    # Validate and sanitize input
    email = request.args.get('email', '').strip()
    if not email:
        return jsonify({"message": "Missing required parameter"}), 400

    # Basic, safe email validation (rejects dangerous/invalid input)
    if not re.fullmatch(r"[A-Za-z0-9._%+\-]+@[A-Za-z0-9.\-]+\.[A-Za-z]{2,}", email):
        return jsonify({"message": "Invalid email address"}), 400

    # Load DB credentials from environment (do not hardcode secrets)
    db_host = os.environ.get('DB_HOST', 'localhost')
    db_port = int(os.environ.get('DB_PORT', '3306'))
    db_user = os.environ.get('DB_USER')
    db_pass = os.environ.get('DB_PASSWORD')
    db_name = os.environ.get('DB_NAME')

    if not all([db_user, db_pass, db_name]):
        # Do not expose private configuration details
        return jsonify({"message": "Server configuration error"}), 500

    conn = None
    try:
        # Use parameterized queries to prevent SQL injection
        connect_kwargs = {
            "host": db_host,
            "user": db_user,
            "passwd": db_pass,
            "db": db_name,
            "port": db_port,
            "connect_timeout": 5,
            "charset": "utf8mb4",
            "use_unicode": True,
        }
        ssl_ca = os.environ.get('DB_SSL_CA')
        if ssl_ca:
            connect_kwargs['ssl'] = {'ca': ssl_ca}

        conn = MySQLdb.connect(**connect_kwargs)
        cursor = conn.cursor()

        # Check subscription without leaking existence (avoids account enumeration)
        cursor.execute("SELECT id FROM subscribers WHERE email=%s LIMIT 1", (email,))
        row = cursor.fetchone()
        if not row:
            # Return the same generic message whether or not the email existed
            return jsonify({"message": "If the email was subscribed, it has been unsubscribed."}), 200

        # Mark as unsubscribed in a single safe update
        cursor.execute("UPDATE subscribers SET subscribed=0 WHERE email=%s", (email,))
        conn.commit()

        return jsonify({"message": "If the email was subscribed, it has been unsubscribed."}), 200

    except MySQLdb.Error:
        # Do not return database error details to the client
        return jsonify({"message": "Server error"}), 500

    finally:
        if conn:
            try:
                conn.close()
            except Exception:
                pass
# ...existing code...
