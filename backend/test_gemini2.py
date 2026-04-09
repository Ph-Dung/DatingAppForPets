import urllib.request
import json

req = urllib.request.Request(
    'https://generativelanguage.googleapis.com/v1beta/openai/chat/completions',
    data=json.dumps({
        'model': 'gemini-1.5-flash',
        'messages': [{'role': 'user', 'content': 'hi'}]
    }).encode(),
    headers={'Content-Type': 'application/json', 'Authorization': 'Bearer foo'}
)

try:
    with urllib.request.urlopen(req) as r:
        print(r.read())
except Exception as e:
    print(e.read())
