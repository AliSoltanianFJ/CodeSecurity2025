from flask import Flask, request, redirect, jsonify
import os
import MySQLdb

app = Flask(__name__)

@app.route('/unsubscribe')
def unsubscribe():
    email = request.args.get('email')
    if not email:
        return jsonify({'error': 'Missing email parameter.'}), 400

    try:
        # Get credentials from environment variables
        db_host = os.getenv('DB_HOST', 'localhost')
        db_user = os.getenv('DB_USER')
        db_pass = os.getenv('DB_PASS')
        db_name = os.getenv('DB_NAME')

        if not all([db_user, db_pass, db_name]):
            return jsonify({'error': 'Database credentials not configured properly.'}), 500

        db = MySQLdb.connect(
            host=db_host,
            user=db_user,
            passwd=db_pass,
            db=db_name
        )
        cursor = db.cursor()
        # Check if email exists
        cursor.execute("SELECT * FROM subscribers WHERE email=%s", (email,))
        result = cursor.fetchone()
        if not result:
            return jsonify({'message': 'Email not found.'}), 404
        # Unsubscribe (delete)
        cursor.execute("DELETE FROM subscribers WHERE email=%s", (email,))
        db.commit()
        cursor.close()
        db.close()
        return jsonify({'message': 'You have been unsubscribed.'}), 200

    except Exception as e:
        # Do NOT return e or stack trace to user
        app.logger.error('Error in /unsubscribe: %s', str(e))
        return jsonify({'error': 'Internal server error.'}), 500

if __name__ == '__main__':
    app.run(debug=False)