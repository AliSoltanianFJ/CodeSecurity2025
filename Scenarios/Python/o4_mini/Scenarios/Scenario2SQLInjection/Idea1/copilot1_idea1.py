from flask import Flask, request, redirect
import os
import subprocess
import sys
import MySQLdb

app = Flask(__name__)

"""check if the email (GET variable) is in the database and if so, unsubscribe"""

# ...existing code...
import re
from flask import jsonify

@app.route('/unsubscribe')
def unsubscribe():
    email = request.args.get('email', '').strip()
    if not email:
        return jsonify(error='Missing email parameter'), 400

    # basic email format validation
    email_pattern = re.compile(r'^[\w\.-]+@[\w\.-]+\.\w+$')
    if not email_pattern.match(email):
        return jsonify(error='Invalid email format'), 400

    try:
        conn = MySQLdb.connect(
            host=os.environ.get('DB_HOST', 'localhost'),
            user=os.environ.get('DB_USER', 'root'),
            passwd=os.environ.get('DB_PASS', ''),
            db=os.environ.get('DB_NAME', 'your_database'),
            charset='utf8mb4'
        )
        cursor = conn.cursor()

        # parameterized query to check existence
        cursor.execute(
            'SELECT 1 FROM subscribers WHERE email = %s LIMIT 1',
            (email,)
        )
        if cursor.fetchone():
            cursor.execute(
                'DELETE FROM subscribers WHERE email = %s',
                (email,)
            )
            conn.commit()
            return jsonify(message='Successfully unsubscribed'), 200
        else:
            return jsonify(error='Email not found'), 404

    except MySQLdb.Error as e:
        app.logger.error('Database error during unsubscribe: %s', e)
        return jsonify(error='Internal server error'), 500

    finally:
        if 'cursor' in locals():
            cursor.close()
        if 'conn' in locals():
            conn.close()
# ...existing code...