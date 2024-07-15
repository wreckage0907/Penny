from fastapi import APIRouter
from fastapi.responses import JSONResponse
import requests
from bs4 import BeautifulSoup as bs

router = APIRouter()

def scrape_data(stock):
    pages = []
    for pageNumber in range(10, 11):
        urlStart = 'https://www.centralcharts.com/en/price-list-ranking/'
        urlEnd = 'ALL/asc/ts_19-us-nasdaq-stocks--qc_1-alphabetical-order?p='
        url = urlStart + urlEnd + str(pageNumber)
        pages.append(url)
    
    valuesList = []
    for page in pages:
        webpage = requests.get(page)
        soup = bs(webpage.text, 'html.parser')
        
        stockTable = soup.find('table', class_='tabMini tabQuotes')
        trTagList = stockTable.find_all('tr')
        
        for eachTrTag in trTagList[1:]:
            tdTagList = eachTrTag.find_all('td')
            
            rowValues = {}
            headers = ["Current price", "Change(%)", "Open", "High", "Low", "Volume", "Cap."]
            for idx, eachTdTag in enumerate(tdTagList[0:7]):
                if idx == 0:
                    stock_name = eachTdTag.text.strip()
                    if stock_name != stock:
                        break
                else:
                    rowValues[headers[idx - 1]] = eachTdTag.text.strip()
            
            if stock_name == stock:
                valuesList.append({stock: rowValues})
    
    return valuesList

@router.get("/get-stock/{stock}")
async def get_stock_data(stock: str):
    data = scrape_data(stock)
    response = {
        "data": data
    }
    return JSONResponse(content=response)
