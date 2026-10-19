# Quick Bill (React + Spring Boot, no database)

## Run the backend (Java 17+, Maven)
    cd backend
    mvn spring-boot:run          # http://localhost:8080

## Run the frontend (Node 18+)
    cd frontend
    npm install
    npm run dev                  # http://localhost:5173  (proxies /api to :8080)

## API
POST /api/bills/a4                  -> A4 invoice PDF
POST /api/bills/pos?widthMm=80|58   -> thermal receipt PDF (height fits content)

Sample body:
{
  "invoiceNo": "INV-1001",
  "shop": {"name":"Sharma General Store","gstin":"21ABCDE1234F1Z5","address":"Market Road, Bhubaneswar","phones":"9876543210"},
  "customer": {"name":"Ravi Kumar","mobile":"9123456789","address":null},
  "items": [{"name":"Rice 5kg","qty":2,"price":320},{"name":"Sugar 1kg","qty":3,"price":45}],
  "gstPercent": 18,
  "discount": {"type":"PERCENT","value":5}
}

## Deploy (single container)
    docker build -t quick-bill .
    docker run -p 8080:8080 quick-bill      # open http://localhost:8080
Push the repo to GitHub and create a "Web Service" from the Dockerfile on Render / Railway / Fly.io.
