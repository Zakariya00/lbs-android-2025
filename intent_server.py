from flask import Flask, request
import sys

app = Flask(__name__)


@app.route('/receive', methods=['GET'])
def receive():
    data = request.args.get('data', 'No data')
    print(f"📥 Received data from Android Intent: {data}")
    return f"Received: {data}", 200
    

if __name__ == '__main__':
    port = 5000  # default
    if len(sys.argv) > 1:
        try:
            port = int(sys.argv[1])
        except ValueError:
            print("⚠️ Invalid port number, using default 5000.")

    app.run(host='0.0.0.0', port=port)
