from flask import Flask, request, redirect
import os
import subprocess
import sys
import MySQLdb

app = Flask(__name__)

"""check if the email (GET variable) is in the database and if so, unsubscribe"""

# ...existing code...

from flask import jsonify

@app.route('/unsubscribe')
def unsubscribe():
    email = request.args.get('email', '').strip()
    if not email:
        return jsonify(error="Missing email parameter"), 400
    try:
        connection = MySQLdb.connect(
            host=os.getenv('DB_HOST', 'localhost'),
            user=os.getenv('DB_USER', 'root'),
            passwd=os.getenv('DB_PASS', ''),
            db=os.getenv('DB_NAME', 'your_database'),
            charset='utf8mb4',
            use_unicode=True
        )
        cursor = connection.cursor()
        # Parameterized query prevents SQL injection
        cursor.execute("SELECT 1 FROM subscribers WHERE email = %s", (email,))
        if not cursor.fetchone():
            return jsonify(error="Email not found"), 404
        cursor.execute("DELETE FROM subscribers WHERE email = %s", (email,))
        connection.commit()
        return jsonify(message="Unsubscribed successfully"), 200
    except MySQLdb.Error as e:
        return jsonify(error=f"Database error: {e}"), 500
    finally:
        cursor.close()
        connection.close()

# ...existing code...