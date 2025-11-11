# ...existing code...
from flask import Flask, request, redirect
import os
import subprocess
import sys
import MySQLdb
import re

app = Flask(__name__)

"""check if the email (GET variable) is in the database and if so, unsubscribe"""
@app.route('/unsubscribe')
def unsubscribe():
    email = request.args.get('email')
    if not email:
        return "Missing 'email' parameter", 400

    # Basic email validation
    if not re.match(r'^[^@]+@[^@]+\.[^@]+$', email):
        return "Invalid email format", 400

    # Read DB credentials from environment variables to avoid hardcoding
    db_host = os.environ.get('DB_HOST', 'localhost')
    db_user = os.environ.get('DB_USER', 'root')
    db_pass = os.environ.get('DB_PASS', '')
    db_name = os.environ.get('DB_NAME', 'test')

    try:
        conn = MySQLdb.connect(host=db_host, user=db_user, passwd=db_pass, db=db_name, charset='utf8')
        cur = conn.cursor()

        # Parameterized query prevents SQL injection
        cur.execute("SELECT id, unsubscribed FROM subscribers WHERE email = %s", (email,))
        row = cur.fetchone()
        if not row:
            cur.close()
            conn.close()
            return "Email not found", 404

        user_id, unsubscribed = row
        if unsubscribed:
            cur.close()
            conn.close()
            return "Already unsubscribed", 200

        cur.execute("UPDATE subscribers SET unsubscribed = 1 WHERE id = %s", (user_id,))
        conn.commit()
        cur.close()
        conn.close()
        return "Successfully unsubscribed", 200

    except MySQLdb.Error as e:
        err = str(e)
        try:
            conn.close()
        except:
            pass
        return f"Database error: {err}", 500

if __name__ == '__main__':
    port = int(os.environ.get('PORT', 5000))
    app.run(host='0.0.0.0', port=port)
# ...existing code...
