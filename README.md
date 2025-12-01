# Transaction Validation Service

Validates bank transaction files (CSV or JSON) and reports errors.

## Features

- Supports CSV and JSON files (extensible)
- Validates duplicate references and balance calculations
- Streams large files to save memory
- REST API for file validation
- Uses BigDecimal for precise financial math
- Stores errors separately (use `errors=true` to retrieve)
- File size limits: 250 MB for synchronous validation, 2.5 GB for asynchronous validation

## Architecture

For detailed architectural decisions and design rationale, see [ARCHITECTURE.md](ARCHITECTURE.md).

## Technology Stack

- Java 25 with ScopedValue for thread-safe validation state
- Spring Boot 3.5.8 for REST API and dependency injection
- MongoDB for persistent storage
- Maven for build management
- Docker for containerization

## Building and Running

### Prerequisites

- Java 25 or higher
- Maven 3.8+
- MongoDB 7.0+ (for persistent storage)

### Local Development

#### 1. Start MongoDB

You can use Docker Compose to start MongoDB:

```bash
docker-compose up -d mongodb
```

Or run MongoDB locally and configure the connection in `application.yml`.

#### 2. Build

```bash
mvn clean install
```

#### 3. Run

```bash
mvn spring-boot:run
```

The service will start on `http://localhost:8080`

### Docker Deployment

#### Using Docker Compose (Recommended)

```bash
# Build and start all services (MongoDB + Validation Service)
docker-compose up -d

# View logs
docker-compose logs -f validation-service

# Stop services
docker-compose down
```

The service will be available at `http://localhost:8080`

#### Using Docker directly

```bash
# Build image
docker build -t transaction-validation-service .

# Run with MongoDB connection
docker run -p 8080:8080 \
  -e SPRING_DATA_MONGODB_URI=mongodb://admin:admin123@host.docker.internal:27017/validation_db?authSource=admin \
  transaction-validation-service
```

### Configuration

The service can be configured via `application.yml` or environment variables:

**Key Configuration Options:**

- `validation.balance.tolerance` (default: 0.01) - Tolerance for balance mismatch checks
- `validation.error.batch-size` (default: 1000) - Batch size for error storage
- `validation.pagination.default-page-size` (default: 1000) - Default pagination size
- `validation.pagination.max-page-size` (default: 10000) - Maximum pagination size
- `spring.servlet.multipart.max-file-size` (default: 2560MB) - Maximum file size for async validation
- `spring.task.execution.pool.core-size` (default: 5) - Async thread pool core size
- `spring.task.execution.pool.max-size` (default: 10) - Async thread pool max size

**MongoDB Connection:**

Set `SPRING_DATA_MONGODB_URI` environment variable or configure in `application.yml`:

```yaml
spring:
  data:
    mongodb:
      uri: mongodb://username:password@host:27017/database?authSource=admin
```

### Testing

Run all tests:

```bash
mvn test
```

Run tests with coverage:

```bash
mvn test jacoco:report
```

## API Endpoints

All endpoints return JSON. Reports are automatically stored for later retrieval.


### <span style="color: #2563eb">Validate File (Synchronous)</span>

```bash
POST /api/v1/validation/validate
Content-Type: multipart/form-data

file: <transaction-file>
```

**Response**: JSON validation report

The report ID is in the `X-Report-Id` header.

### <span style="color: #16a34a">Validate File (Asynchronous)</span>

```bash
POST /api/v1/validation/validate/async
Content-Type: multipart/form-data

file: <transaction-file>
```

**Response**: JSON with job info (202 Accepted)

Returns immediately. Check job status endpoints for progress and results.

### Get Job Status

```bash
GET /api/v1/validation/jobs/{jobId}/status
```

**Response**: JSON with job status and result if completed

### Get Job Result

```bash
GET /api/v1/validation/jobs/{jobId}/result
```

**Response**: JSON validation report if job is completed

### Get Stored Report

```bash
GET /api/v1/validation/reports/{reportId}?errors=true&page=0&size=1000
```

**Query Parameters:**
- `errors` (optional, default: `false`): Set to `true` to include detailed errors
- `page` (optional, default: `0`): Page number (0-indexed) - only used when `errors=true`
- `size` (optional, default: `1000`): Number of errors per page - only used when `errors=true`

**Response**: JSON validation report

By default, returns summary only (empty errors list). Use `errors=true` for detailed errors.

**Pagination:**
When `errors=true`, the response is paginated to handle large error sets efficiently:
- Default page size is 1000 errors per page
- Use `page` parameter to navigate through pages (0-indexed)
- Use `size` parameter to control page size
- For reports with many errors, pagination prevents connection timeouts and memory issues

### Health Check

```bash
GET /api/v1/validation/health
```

**Response**: JSON with health status

## Example Usage

### Using curl

```bash
# Validate CSV file
curl -X POST http://localhost:8080/api/v1/validation/validate \
  -F "file=@records.csv"

# Validate JSON file
curl -X POST http://localhost:8080/api/v1/validation/validate \
  -F "file=@records.json"

# Get report ID from response header
curl -X POST http://localhost:8080/api/v1/validation/validate \
  -F "file=@records.csv" \
  -i | grep X-Report-Id

# Validate asynchronously
curl -X POST http://localhost:8080/api/v1/validation/validate/async \
  -F "file=@records.csv"

# Get stored report (summary only)
curl http://localhost:8080/api/v1/validation/reports/{reportId}

# Get stored report with detailed errors (first 1000 errors)
curl http://localhost:8080/api/v1/validation/reports/{reportId}?errors=true

# Get stored report with pagination (page 0, 1000 errors per page)
curl http://localhost:8080/api/v1/validation/reports/{reportId}?errors=true&page=0&size=1000

# Get next page of errors (page 1, 1000 errors per page)
curl http://localhost:8080/api/v1/validation/reports/{reportId}?errors=true&page=1&size=1000
```

### Example Response

```json
{
  "valid": false,
  "errorCount": 2,
  "duplicateReferenceCount": 1,
  "balanceMismatchCount": 1,
  "errors": [
    {
      "transactionReference": "112806",
      "description": "Book Peter de Vries",
      "errorType": "DUPLICATE_REFERENCE",
      "errorMessage": "Duplicate transaction reference"
    },
    {
      "transactionReference": "167875",
      "description": "Toy Greg Alysha",
      "errorType": "BALANCE_MISMATCH",
      "errorMessage": "End balance does not match calculated balance"
    }
  ]
}
```

### Example Error Response

```json
{
  "error": "PARSE_ERROR",
  "message": "Failed to parse file: Invalid CSV format"
}
```

## File Formats

### CSV Format

```csv
Reference,AccountNumber,Description,Start Balance,Mutation,End Balance
194261,NL91RABO0315273637,Book John Smith,21.6,-41.83,-20.23
112806,NL27SNSB0917829871,Clothes Irma Steven,91.23,+15.57,106.8
```

### JSON Format

```json
[
  {
    "reference": "130498",
    "accountNumber": "NL69ABNA0433647324",
    "description": "Book Jan Theuß",
    "startBalance": 26.9,
    "mutation": -18.78,
    "endBalance": 8.12
  }
]
```



## Performance Considerations

- **Streaming Processing**: Files are processed one transaction at a time, keeping memory usage constant
- **Hash-based Deduplication**: Files are hashed using xxHash128 for fast duplicate detection
- **Virtual Threads**: Async validation uses Java 25 virtual threads for efficient concurrency
- **Separate Error Storage**: Errors are stored separately to avoid MongoDB's 16MB document limit
- **Pagination**: Large error sets are paginated to prevent memory issues

## Troubleshooting

**MongoDB Connection Issues:**
- Ensure MongoDB is running and accessible
- Check connection string format and credentials
- Verify network connectivity (firewall, Docker networking)

**File Size Limits:**
- Synchronous validation: 250 MB (hardcoded limit)
- Asynchronous validation: 2.5 GB (configurable via `spring.servlet.multipart.max-file-size`)

**Memory Issues:**
- For very large files, use the async endpoint
- Adjust JVM heap size: `-Xmx8g` (default in Docker)
- Monitor MongoDB connection pool settings

## Future Enhancements

- **Security**: Securing the REST endpoints (authentication, authorization)
- **Documentation**: Code-level documentation (JavaDoc)
- **Scalability**: Horizontal scalability in Kubernetes or resource-based scaling in AWS
- **File Formats**: Support for more file formats (XML, Excel)
- **Observability**: Metrics and observability (Prometheus, Grafana, distributed tracing)
- **Validation**: IBAN validation for account numbers
- **Batch Processing**: Batch processing API for multiple files
- **Job Management**: Retry failed job API endpoint to retry a failed validation job without resubmitting the file
- **Rate Limiting**: API rate limiting and throttling
- **Webhooks**: Webhook notifications for async job completion



