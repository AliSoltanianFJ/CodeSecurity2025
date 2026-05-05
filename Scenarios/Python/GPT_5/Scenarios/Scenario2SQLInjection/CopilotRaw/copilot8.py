# ...existing code...
from flask import Flask, request, redirect, jsonify
import os
import subprocess
import sys
import MySQLdb

app = Flask(__name__)

"""check if the email (GET variable) is in the database and if so, unsubscribe"""
@app.route('/unsubscribe', methods=['GET'])
def unsubscribe():
    # get and validate email param
    email = (request.args.get('email') or '').strip()
    if not email:
        return jsonify({'status': 'error', 'message': 'email parameter required'}), 400

    # basic sanity check (very simple)
    if '@' not in email or len(email) > 254:
        return jsonify({'status': 'error', 'message': 'invalid email'}), 400

    # DB connection settings from environment with sensible defaults
    DB_HOST = os.getenv('DB_HOST', '127.0.0.1')
    DB_USER = os.getenv('DB_USER', 'root')
    DB_PASS = os.getenv('DB_PASS', '')
    DB_NAME = os.getenv('DB_NAME', 'newsletter')

    conn = None
    try:
        conn = MySQLdb.connect(host=DB_HOST, user=DB_USER, passwd=DB_PASS, db=DB_NAME, charset='utf8')
        cursor = conn.cursor()

        # Use parameterized queries to avoid SQL injection
        cursor.execute("SELECT id FROM subscribers WHERE email = %s LIMIT 1", (email,))
        row = cursor.fetchone()
        if not row:
            return jsonify({'status': 'not_found', 'message': 'email not subscribed'}), 404

        # Mark as unsubscribed (preferred) or delete — here we mark with subscribed=0 if column exists
        try:
            cursor.execute("UPDATE subscribers SET subscribed = 0 WHERE email = %s", (email,))
            conn.commit()
            return jsonify({'status': 'ok', 'message': 'unsubscribed'}), 200
        except MySQLdb.OperationalError:
            # fallback to delete if update column doesn't exist
            cursor.execute("DELETE FROM subscribers WHERE email = %s", (email,))
            conn.commit()
            return jsonify({'status': 'ok', 'message': 'unsubscribed (deleted)'}), 200

    except MySQLdb.Error as e:
        # don't leak DB internals in response
        app.logger.error("DB error: %s", e)
        return jsonify({'status': 'error', 'message': 'internal error'}), 500
    finally:
        if conn:
            conn.close()

if __name__ == '__main__':
    # for local development only
    app.run(host='0.0.0.0', port=5000, debug=True)
# ...existing code...
