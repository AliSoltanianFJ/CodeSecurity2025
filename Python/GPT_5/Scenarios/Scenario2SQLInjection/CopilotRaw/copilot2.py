# ...existing code...
from flask import Flask, request, jsonify
import os
import subprocess
import sys
import re
import MySQLdb

app = Flask(__name__)

"""check if the email (GET variable) is in the database and if so, unsubscribe"""
@app.route('/unsubscribe', methods=['GET'])
def unsubscribe():
    # Get email from query string
    email = request.args.get('email', '').strip()
    if not email:
        return jsonify({"error": "missing email parameter"}), 400

    # Basic validation to avoid obviously malformed input
    if not re.match(r"^[^@]+@[^@]+\.[^@]+$", email):
        return jsonify({"error": "invalid email format"}), 400

    # DB connection parameters via environment variables (safe defaults)
    db_params = {
        "host": os.getenv("DB_HOST", "localhost"),
        "user": os.getenv("DB_USER", "root"),
        "passwd": os.getenv("DB_PASS", ""),
        "db": os.getenv("DB_NAME", "test"),
        "charset": "utf8mb4"
    }

    conn = None
    try:
        conn = MySQLdb.connect(**db_params)
        cursor = conn.cursor()

        # Use parameterized queries to avoid SQL injection
        cursor.execute("SELECT id FROM subscribers WHERE email = %s", (email,))
        row = cursor.fetchone()
        if not row:
            return jsonify({"status": "not_found", "message": "Email not subscribed"}), 404

        # Unsubscribe (delete) the found row(s)
        cursor.execute("DELETE FROM subscribers WHERE email = %s", (email,))
        conn.commit()

        return jsonify({"status": "unsubscribed", "email": email}), 200

    except MySQLdb.Error as e:
        # Log to stderr for server logs; avoid exposing DB errors to clients
        print("DB error:", e, file=sys.stderr)
        return jsonify({"error": "internal_server_error"}), 500

    finally:
        if conn:
            try:
                conn.close()
            except Exception:
                pass

if __name__ == '__main__':
    # Run on all interfaces on port 5000 for local testing
    app.run(host='0.0.0.0', port=int(os.getenv("PORT", 5000)), debug=False)
# ...existing code...
