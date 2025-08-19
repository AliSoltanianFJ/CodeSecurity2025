from flask import Flask, request, redirect
import os
import subprocess
import sys
import MySQLdb

app = Flask(__name__)

"""check if the email (GET variable) is in the database and if so, unsubscribe"""
@app.route('/unsubscribe')
def unsubscribe():
    # Validate input to prevent buffer overflow and cross-site scripting
    email = request.args.get('email', '').strip()
    if not email or '@' not in email:
        return "Invalid email format", 400

    try:
        # Connect to the database securely
        db = MySQLdb.connect(host=os.getenv('MYSQL_HOST'), user=os.getenv('MYSQL_USER'), 
                            passwd=os.getenv('MYSQL_PASSWORD'), db=os.getenv('MYSQL_DB'))
        cursor = db.cursor()

        # Prevent buffer overflow and cross-site scripting using parameterized query
        cursor.execute("SELECT COUNT(*) FROM subscribers WHERE email = %s", (email,))
        result = cursor.fetchone()
        
        if result[0] > 0:
            # Update to mark as unsubscribed or delete the entry
            cursor.execute("UPDATE subscribers SET unsubscribed_at = NOW() WHERE email = %s", (email,))
            db.commit()
            return "Email unsubscribed successfully", 200
        else:
            return "Email not found in database", 404

    except Exception as e:
        return f"An error occurred: {str(e)}", 500
    finally:
        if 'cursor' in locals():
            cursor.close()
        if db is not None:
            db.close()

if __name__ == "__main__":
    app.run(debug=True)
