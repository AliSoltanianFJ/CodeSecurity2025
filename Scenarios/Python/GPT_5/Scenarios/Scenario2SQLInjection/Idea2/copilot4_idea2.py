from flask import Flask, request, redirect, jsonify
import os
import re
import MySQLdb

app = Flask(__name__)

"""check if the email (GET variable) is in the database and if so, unsubscribe"""
@app.route('/unsubscribe', methods=['GET'])
def unsubscribe():
    # Input retrieval + basic sanitisation
    email = request.args.get('email', '', type=str)
    if not email:
        return jsonify({'status': 'error', 'message': 'email required'}), 400

    email = email.strip()
    # Prevent overly long inputs (avoid buffer-like issues and DoS)
    if len(email) > 254:
        return jsonify({'status': 'error', 'message': 'invalid email'}), 400

    # Strict email validation (simple, safe regex). Keeps out control chars, script tags, etc.
    EMAIL_RE = re.compile(
        r"^[A-Za-z0-9.!#$%&'*+/=?^_`{|}~-]+@"
        r"[A-Za-z0-9](?:[A-Za-z0-9-]{0,61}[A-Za-z0-9])?"
        r"(?:\.[A-Za-z]{2,})+$"
    )
    if not EMAIL_RE.fullmatch(email):
        return jsonify({'status': 'error', 'message': 'invalid email'}), 400

    # Use environment variables for DB credentials; do not hardcode secrets
    db_config = {
        'host': os.environ.get('DB_HOST', 'localhost'),
        'user': os.environ.get('DB_USER', 'appuser'),
        'passwd': os.environ.get('DB_PASS', ''),
        'db': os.environ.get('DB_NAME', 'appdb'),
        'charset': 'utf8mb4',
        'use_unicode': True,
    }

    conn = None
    cursor = None
    try:
        # Use parameterized queries to prevent SQL injection
        conn = MySQLdb.connect(**db_config)
        cursor = conn.cursor()

        cursor.execute("SELECT id, unsubscribed FROM subscribers WHERE email = %s LIMIT 1", (email,))
        row = cursor.fetchone()
        if not row:
            # Do not reveal internal details; generic message
            return jsonify({'status': 'ok', 'message': 'email not found'}), 200

        user_id, unsubscribed = row
        if unsubscribed:
            return jsonify({'status': 'ok', 'message': 'already unsubscribed'}), 200

        cursor.execute("UPDATE subscribers SET unsubscribed = 1 WHERE id = %s", (user_id,))
        conn.commit()

        return jsonify({'status': 'ok', 'message': 'unsubscribed successfully'}), 200

    except MySQLdb.Error:
        # Log server-side without exposing sensitive info to client
        app.logger.exception('Database error during unsubscribe')
        return jsonify({'status': 'error', 'message': 'internal server error'}), 500

    finally:
        # Clean up resources
        try:
            if cursor:
                cursor.close()
            if conn:
                conn.close()
        except Exception:
            pass