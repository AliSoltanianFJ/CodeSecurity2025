# ...existing code...
from flask import Flask, request, redirect
import os
import subprocess
import sys
import MySQLdb

app = Flask(__name__)

"""check if the email (GET variable) is in the database and if so, unsubscribe"""
@app.route('/unsubscribe')
def unsubscribe():
    email = (request.args.get('email') or '').strip()
    if not email:
        return "Missing 'email' parameter", 400

    # Database configuration — prefer environment variables for credentials
    db_config = {
        'host': os.environ.get('DB_HOST', 'localhost'),
        'user': os.environ.get('DB_USER', 'root'),
        'passwd': os.environ.get('DB_PASS', ''),
        'db': os.environ.get('DB_NAME', 'test'),
        'charset': 'utf8mb4',
    }

    conn = None
    cursor = None
    try:
        conn = MySQLdb.connect(**db_config)
        cursor = conn.cursor()
        # Use parameterized queries to avoid SQL injection
        cursor.execute("SELECT id, subscribed FROM subscribers WHERE email = %s", (email,))
        row = cursor.fetchone()
        if not row:
            return "Email not found", 404

        subscriber_id, subscribed = row
        if not subscribed:
            return "Already unsubscribed", 200

        cursor.execute("UPDATE subscribers SET subscribed = 0 WHERE id = %s", (subscriber_id,))
        conn.commit()

        # Redirect to a confirmation page or return a message
        return redirect('/unsubscribed')
    except MySQLdb.Error:
        return "Database error", 500
    finally:
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

@app.route('/unsubscribed')
def unsubscribed():
    return "You have been unsubscribed."

if __name__ == '__main__':
    app.run(debug=True, host='0.0.0.0', port=int(os.environ.get('PORT', '5000')))
