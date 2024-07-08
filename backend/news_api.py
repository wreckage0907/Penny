from newsapi import NewsApiClient
import json
from datetime import datetime

newsapi = NewsApiClient(api_key='e0fa70d9a02145b9b68c87f8ee2ed62b')

top_headlines = newsapi.get_top_headlines(category='business',
                                          language='en',
                                          country='in')

timestamp = datetime.now().strftime("%Y%m%d_%H%M%S")

filename = f"finance_news_{timestamp}.json"

with open(filename, 'w', encoding='utf-8') as f:
    json.dump(top_headlines, f, ensure_ascii=False, indent=4)

print(f"News data has been saved to {filename}")