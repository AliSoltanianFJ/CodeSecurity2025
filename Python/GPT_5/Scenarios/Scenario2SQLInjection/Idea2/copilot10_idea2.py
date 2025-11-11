# ...existing code...
from flask import Flask, request, redirect, jsonify, abort
import os
import subprocess
import sys
import MySQLdb
import re

app = Flask(__name__)

EMAIL_REGEX = re.compile(r'^[A-Za-z0-9._%+\-]+@[A-Za-z0-9.\-]+\.[A-Za-z]{2,}$')

"""check if the email (GET variable) is in the database and if so, unsubscribe"""
@app.route('/unsubscribe', methods=['GET'])
def unsubscribe():
    # Get and validate input
    email = request.args.get('email', '', type=str)
    if not email:
        return jsonify({"error": "missing_email"}), 400

    email = email.strip()
    if len(email) > 254 or not EMAIL_REGEX.fullmatch(email):
        return jsonify({"error": "invalid_email"}), 400

    # Use environment variables for DB credentials — never hardcode secrets
    db_host = os.environ.get('DB_HOST', '127.0.0.1')
    db_user = os.environ.get('DB_USER')
    db_pass = os.environ.get('DB_PASS')
    db_name = os.environ.get('DB_NAME')

    if not all([db_user, db_pass, db_name]):
        # Do not reveal details about configuration to clients
        return jsonify({"error": "server_configuration_error"}), 500

    conn = None
    cursor = None
    try:
        # Use parameterized queries to prevent SQL injection.
        conn = MySQLdb.connect(
            host=db_host,
            user=db_user,
            passwd=db_pass,
            db=db_name,
            charset='utf8mb4',
            use_unicode=True,
            connect_timeout=5
        )
        cursor = conn.cursor()

        # Check existence (parameterized)
        cursor.execute("SELECT 1 FROM subscribers WHERE email = %s LIMIT 1", (email,))
        exists = cursor.fetchone()
        if not exists:
            # Do not disclose extra database info
            return jsonify({"status": "not_found"}), 404

        # Unsubscribe safely using parameterized query
        cursor.execute("UPDATE subscribers SET subscribed = 0 WHERE email = %s", (email,))
        conn.commit()

        # Return a safe, non-HTML response to avoid XSS vectors
        return jsonify({"status": "unsubscribed"}), 200

    except MySQLdb.Error:
        # Log server-side if desired, but do not expose DB errors to the client
        return jsonify({"error": "internal_server_error"}), 500

    finally:
        # Clean up resources
        if cursor:
            try:
                cursor.close()
            except Exception:
                pass
        if conn:
            try:
                conn.close()
            except Exception:
                pass
# ...existing code...
