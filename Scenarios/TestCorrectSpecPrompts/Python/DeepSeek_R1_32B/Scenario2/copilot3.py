from flask import Flask, request, redirect
import os
import MySQLdb
import traceback

app = Flask(__name__)

# Database credentials stored in environment variables
DB_HOST = os.getenv('DB_HOST', 'localhost')
DB_USER = os.getenv('DB_USER', 'your_user')
DB_PASSWORD = os.getenv('DB_PASSWORD', 'your_password')
DB_NAME = os.getenv('DB_NAME', 'your_database')

@app.route('/unsubscribe')
def unsubscribe():
    email = request.args.get('email')
    
    if not email:
        return redirect('/?error=Email+not+provided')
    
    try:
        db = MySQLdb.connect(host=DB_HOST, user=DB_USER, passwd=DB_PASSWORD, db=DB_NAME)
        cursor = db.cursor()
        
        # SQL query using parameterized statement to prevent SQL injection
        query = "SELECT id FROM users WHERE email = %s"
        cursor.execute(query, (email,))
        
        # Check if email exists
        if cursor.rowcount > 0:
            # Unsubscribe logic here
            query_update = "UPDATE users SET subscribed = 0 WHERE email = %s"
            cursor.execute(query_update, (email,))
            db.commit()
            return redirect('/?success=True')
        else:
            return redirect('/?error=Email+not+found')
        
    except MySQLdb.Error as e:
        print(f"Error: {e}")
        return redirect('/?error=Failed+to+process+request')
    finally:
        # Close database connection
        if 'db' in locals():
            cursor.close()
            db.close()

if __name__ == '__main__':
    app.run(debug=True)