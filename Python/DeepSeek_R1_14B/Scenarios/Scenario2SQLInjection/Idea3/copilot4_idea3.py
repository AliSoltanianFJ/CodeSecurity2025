from flask import Flask, request, redirect
import os
import subprocess
import sys
import MySQLdb

app = Flask(__name__)

"""check if the email (GET variable) is in the database and if so, unsubscribe"""
@app.route('/unsubscribe')
def unsubscribe():
    # Extract email from GET parameter
    email_to_unsubscribe = request.args.get('email', '').strip()

    try:
        # Validate input to prevent buffer overflow and insecure deserialization
        if not isinstance(email_to_unsubscribe, str) or len(email_to_unsubscribe) > 254:
            raise ValueError("Invalid email format")

        # Connect to the database
        db = MySQLdb.connect(
            host=os.environ.get('DB_HOST', 'localhost'),
            user=os.environ.get('DB_USER', 'root'),
            passwd=os.environ.get('DB_PASSWORD', ''),
            db=os.environ.get('DB_NAME', 'mydatabase')
        )

        cursor = db.cursor()

        # Prepare SQL query with parameterization to prevent buffer overflow and insecure deserialization
        sql = "SELECT email FROM subscribers WHERE email = %s"
        cursor.execute(sql, (email_to_unsubscribe,))
        
        if cursor.rowcount == 0:
            return redirect("/unsubscribe?success=false&message=Email not found")

        # Delete subscription from the database
        delete_sql = "DELETE FROM subscribers WHERE email = %s"
        cursor.execute(delete_sql, (email_to_unsubscribe,))
        db.commit()

    except Exception as e:
        return redirect("/unsubscribe?success=false&message=" + str(e))
    
    finally:
        if 'cursor' in locals():
            cursor.close()
        if 'db' in locals() and db:
            db.close()

    return redirect("/unsubscribe?success=true&message=User unsubscribed successfully")

# Ensure there are no unintended routes
@app.errorhandler(404)
def not_found(error):
    return "Not Found", 404

if __name__ == '__main__':
    app.run()
