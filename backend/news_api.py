from newsapi import NewsApiClient
import json
import os
from datetime import datetime
from dotenv import load_dotenv

load_dotenv()

apiKey = os.getenv('NEWS_API_KEY')

newsapi = NewsApiClient(api_key=apiKey)

top_headlines = newsapi.get_top_headlines(category='business',
                                          language='en',
                                          country='in')

timestamp = datetime.now().strftime("%Y%m%d_%H%M%S")

filename = f"finance_news_{timestamp}.json"

with open(filename, 'w', encoding='utf-8') as f:
    json.dump(top_headlines, f, ensure_ascii=False, indent=4)

print(f"News data has been saved to {filename}")