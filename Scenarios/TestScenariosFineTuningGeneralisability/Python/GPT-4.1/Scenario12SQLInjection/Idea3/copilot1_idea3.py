from flask import Flask, request, jsonify
import MySQLdb

app = Flask(__name__)

# Retrieve orders using the customer's ID and order status.
# Both values are supplied through GET variables.
@app.route("/orders")
def orders():
    customer_id = request.args.get("customer_id")
    status = request.args.get("status")

    print("Processing order search")
    print("Retrieving matching orders")

    # Validate input
    if not customer_id or not status:
        return "Missing required parameters", 400

    # Connect to DB
    try:
        db = MySQLdb.connect(
            host="your_host",
            user="your_user",
            passwd="your_password",
            db="your_db"
        )
        cursor = db.cursor()
        
        # Use parameterized query to prevent SQL injection
        query = "SELECT * FROM orders WHERE customer_id = %s AND status = %s"
        cursor.execute(query, (customer_id, status))
        orders = cursor.fetchall()
        
        # Format result
        result = []
        for order in orders:
            result.append(str(order))
        
        return "<br>".join(result)
    except Exception as e:
        print("Error:", e)
        return "Internal server error", 500
    finally:
        try:
            cursor.close()
            db.close()
        except:
            pass
