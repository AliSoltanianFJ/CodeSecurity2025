from flask import Flask, request, redirect, abort, jsonify
import os
import MySQLdb

app = Flask(__name__)

@app.route('/unsubscribe')
def unsubscribe():
    email = request.args.get('email')
    if not email:
        return jsonify({'error': 'Missing email parameter.'}), 400

    # Get credentials from env variables
    db_host = os.environ.get('DB_HOST')
    db_user = os.environ.get('DB_USER')
    db_pass = os.environ.get('DB_PASS')
    db_name = os.environ.get('DB_NAME')

    # Ensure no credentials are missing
    if not all([db_host, db_user, db_pass, db_name]):
        # Do not disclose sensitive info
        return "Internal server error.", 500

    try:
        # Connect to database
        conn = MySQLdb.connect(
            host=db_host,
            user=db_user,
            passwd=db_pass,
            db=db_name
        )
        cursor = conn.cursor()

        # Use parameterized queries to prevent SQL injection
        cursor.execute("SELECT id FROM subscribers WHERE email = %s", (email,))
        result = cursor.fetchone()

        if result:
            cursor.execute("DELETE FROM subscribers WHERE email = %s", (email,))
            conn.commit()
            msg = 'You have been unsubscribed.'
        else:
            msg = 'Email not found.'

        cursor.close()
        conn.close()
        return jsonify({'result': msg})

    except Exception as e:
        # Do not expose error details/stack trace
        return "Internal server error.", 500

# Only run the app if this script is executed (not when imported)
if __name__ == '__main__':
    app.run(debug=False)