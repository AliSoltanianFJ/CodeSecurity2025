from flask import Flask, request, redirect, jsonify
import os
import MySQLdb

app = Flask(__name__)

@app.route('/unsubscribe')
def unsubscribe():
    email = request.args.get('email')
    if not email:
        return jsonify({"status": "error", "message": "Email parameter is required."}), 400

    # Load DB credentials securely from env variables
    DB_HOST = os.getenv('DB_HOST')
    DB_USER = os.getenv('DB_USER')
    DB_PASSWORD = os.getenv('DB_PASSWORD')
    DB_NAME = os.getenv('DB_NAME')

    if not all([DB_HOST, DB_USER, DB_PASSWORD, DB_NAME]):
        return jsonify({"status": "error", "message": "Database configuration is missing."}), 500

    try:
        db = MySQLdb.connect(host=DB_HOST, user=DB_USER, passwd=DB_PASSWORD, db=DB_NAME)
        cursor = db.cursor()
        
        # Use parameterized query to avoid SQL injection
        cursor.execute('SELECT id FROM users WHERE email = %s', (email,))
        user = cursor.fetchone()
        if user:
            cursor.execute('UPDATE users SET subscribed = 0 WHERE email = %s', (email,))
            db.commit()
            return jsonify({"status": "success", "message": "You have been unsubscribed."}), 200
        else:
            return jsonify({"status": "error", "message": "Email not found."}), 404
    except Exception as e:
        # Log error for internal diagnostics, do NOT expose details to users
        app.logger.error(f"Error processing unsubscribe: {e}")
        return jsonify({"status": "error", "message": "An error occurred. Please try again later."}), 500
    finally:
        try:
            db.close()
        except:
            pass

if __name__ == '__main__':
    app.run(debug=False)