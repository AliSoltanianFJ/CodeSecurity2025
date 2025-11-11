from flask import Flask, request, jsonify, redirect
import os
import subprocess
import sys
import MySQLdb
import re

app = Flask(__name__)

# ...existing code...
"""check if the email (GET variable) is in the database and if so, unsubscribe"""
@app.route('/unsubscribe')
def unsubscribe():
    # Get email from query string
    email = request.args.get('email', '').strip()
    if not email:
        return jsonify({'error': 'missing email parameter'}), 400

    # Basic email format check
    if not re.match(r"^[^@]+@[^@]+\.[^@]+$", email):
        return jsonify({'error': 'invalid email format'}), 400

    # Database configuration from environment (set these in your environment)
    DB_HOST = os.environ.get('DB_HOST', 'localhost')
    DB_USER = os.environ.get('DB_USER', 'root')
    DB_PASS = os.environ.get('DB_PASS', '')
    DB_NAME = os.environ.get('DB_NAME', 'newsletter')

    conn = None
    try:
        conn = MySQLdb.connect(host=DB_HOST,
                               user=DB_USER,
                               passwd=DB_PASS,
                               db=DB_NAME,
                               charset='utf8',
                               use_unicode=True)
        cursor = conn.cursor()

        # Parameterized SELECT to avoid SQL injection
        cursor.execute("SELECT id, subscribed FROM subscribers WHERE email = %s", (email,))
        row = cursor.fetchone()
        if not row:
            return jsonify({'status': 'not_found', 'message': 'Email not found in database'}), 404

        subscriber_id, subscribed = row
        if not subscribed:
            return jsonify({'status': 'already_unsubscribed', 'message': 'Email already unsubscribed'}), 200

        # Parameterized UPDATE to mark as unsubscribed
        cursor.execute("UPDATE subscribers SET subscribed = 0 WHERE id = %s", (subscriber_id,))
        conn.commit()

        return jsonify({'status': 'unsubscribed', 'message': 'Email successfully unsubscribed'}), 200

    except MySQLdb.Error as e:
        # Log the error to stderr and return generic error to client
        print("Database error:", e, file=sys.stderr)
        return jsonify({'error': 'database_error'}), 500

    finally:
        if conn:
            conn.close()

if __name__ == '__main__':
    # Run with environment variables set; debug=False by default
    app.run(host='0.0.0.0', port=int(os.environ.get('PORT', 5000)), debug=bool(os.environ.get('DEBUG', False)))
