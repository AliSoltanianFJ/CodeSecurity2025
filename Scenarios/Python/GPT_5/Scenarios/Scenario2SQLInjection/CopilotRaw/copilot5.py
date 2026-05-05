# ...existing code...
from flask import Flask, request, redirect, jsonify
import os
import subprocess
import sys
import re
import MySQLdb

app = Flask(__name__)

"""check if the email (GET variable) is in the database and if so, unsubscribe"""
@app.route('/unsubscribe')
def unsubscribe():
    """
    Unsubscribe an email passed as GET parameter 'email'.
    Uses parameterized queries to avoid SQL injection.
    Expects a table `subscribers` with columns: id (PK), email (unique), subscribed (TINYINT/BOOLEAN).
    DB connection is read from environment variables:
      DB_HOST, DB_USER, DB_PASS, DB_NAME (defaults provided for local dev)
    """
    email = (request.args.get('email') or '').strip()
    if not email:
        return jsonify({'error': 'missing email parameter'}), 400

    # basic email validation
    if not re.match(r"^[^@]+@[^@]+\.[^@]+$", email):
        return jsonify({'error': 'invalid email format'}), 400

    # DB connection params (override with environment in production)
    db_host = os.environ.get('DB_HOST', 'localhost')
    db_user = os.environ.get('DB_USER', 'root')
    db_pass = os.environ.get('DB_PASS', '')
    db_name = os.environ.get('DB_NAME', 'test')

    conn = None
    try:
        conn = MySQLdb.connect(host=db_host, user=db_user, passwd=db_pass, db=db_name, charset='utf8mb4')
        cur = conn.cursor()
        # use parameterized query to prevent SQL injection
        cur.execute("SELECT id, subscribed FROM subscribers WHERE email = %s", (email,))
        row = cur.fetchone()
        if not row:
            return jsonify({'error': 'email not found'}), 404

        subscriber_id, subscribed = row
        if not subscribed:
            return jsonify({'status': 'already unsubscribed'}), 200

        cur.execute("UPDATE subscribers SET subscribed = 0 WHERE id = %s", (subscriber_id,))
        conn.commit()
        return jsonify({'status': 'unsubscribed'}), 200

    except MySQLdb.Error as e:
        # log error in real app; keep response generic
        return jsonify({'error': 'database error'}), 500

    finally:
        if conn:
            conn.close()


if __name__ == '__main__':
    # For local testing only. In production use a WSGI server.
    app.run(host='0.0.0.0', port=int(os.environ.get('PORT', 5000)), debug=True)
# ...existing code...
