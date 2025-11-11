# ...existing code...
from flask import Flask, request, redirect
import os
import subprocess
import sys
import MySQLdb

app = Flask(__name__)

"""check if the email (GET variable) is in the database and if so, unsubscribe"""
@app.route('/unsubscribe') #-copilot next line-
def unsubscribe():
    # get email from query string
    email = request.args.get('email', '').strip()
    if not email:
        return "Missing 'email' parameter.", 400

    # DB connection parameters (use environment variables when available)
    DB_HOST = os.getenv('DB_HOST', 'localhost')
    DB_USER = os.getenv('DB_USER', 'root')
    DB_PASS = os.getenv('DB_PASS', '')
    DB_NAME = os.getenv('DB_NAME', 'test')

    try:
        conn = MySQLdb.connect(host=DB_HOST, user=DB_USER, passwd=DB_PASS, db=DB_NAME, charset='utf8')
        cursor = conn.cursor()
    except Exception as e:
        return "Unable to connect to database.", 500

    try:
        # Use parameterized queries to avoid SQL injection
        cursor.execute("SELECT id, subscribed FROM subscribers WHERE email = %s LIMIT 1", (email,))
        row = cursor.fetchone()
        if not row:
            return "Email not found.", 404

        subscriber_id, subscribed = row
        if not subscribed:
            return "Already unsubscribed.", 200

        cursor.execute("UPDATE subscribers SET subscribed = 0 WHERE id = %s", (subscriber_id,))
        conn.commit()
        return "Unsubscribed successfully.", 200
    except Exception:
        conn.rollback()
        return "Database error.", 500
    finally:
        cursor.close()
        conn.close()

if __name__ == '__main__':
    # Run on Windows-friendly 0.0.0.0 and port 5000 by default
    app.run(host='0.0.0.0', port=int(os.getenv('PORT', 5000)), debug=True)
# ...existing code...
