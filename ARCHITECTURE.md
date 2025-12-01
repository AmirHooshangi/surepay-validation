# Architecture Overview

A Spring Boot service that validates bank transaction files. It checks two things:
1. **Uniqueness**: No duplicate transaction references in a file
2. **Balance**: End balance = start balance + mutation

## How It Works

You upload a CSV or JSON file, and the service streams through it one transaction at a time. This keeps memory usage constant even for huge files. Each transaction gets checked against both rules, and all errors are collected (we don't stop at the first error).

The service can run validations synchronously (you wait for the result) or asynchronously (you get a job ID and check back later). All reports are automatically saved to MongoDB, and if you upload the same file twice, it'll return the cached result without re-processing.

## The Layers

We use a simple layered architecture:

- **Controllers**: Handle HTTP requests, figure out file types, return JSON responses
- **Services**: Orchestrate the validation workflow and manage async jobs
- **Domain**: Core business objects (Transaction, ValidationError) with the actual business logic
- **Parsers**: Read CSV or JSON files and convert them to Transaction objects
- **Validators**: Check the rules (uniqueness, balance)
- **Reporter**: Generates the final report and saves it

Each layer has one job, which makes the code easy to test and extend.

## Key Design Decisions

**Streaming instead of loading everything into memory**
- Files are processed one transaction at a time
- Memory usage stays constant regardless of file size
- Can handle files larger than available RAM

**BigDecimal for money**
- Never use `double` for financial calculations
- Balance checks use a 0.01 tolerance to handle rounding

**Hash-based deduplication**
- Files are hashed using xxHash128 (fast, 10-20x faster than SHA-256)
- Same file content = same hash = same report ID
- If we've seen this file before, we return the cached report immediately

**Separate error storage**
- Report summaries (error counts) go in the main `validation_reports` collection
- Detailed errors go in a separate `validation_errors` collection
- This avoids MongoDB's 16MB document limit for files with tons of errors
- Errors are paginated when retrieved (1000 per page by default)

**Java Records for immutability**
- Transaction, ValidationError, and other core objects are immutable
- Thread-safe by default, prevents accidental bugs

**ScopedValue for thread-safe validation state**
- Java 25's ScopedValue is used to maintain per-validation uniqueness state
- Each validation run gets its own isolated scope, preventing cross-contamination
- No need for thread-local variables or synchronization
- Automatically cleaned up when validation completes
- Enables safe concurrent validation of multiple files

**Virtual Threads for async processing**
- Async validation jobs use Java 25 virtual threads (Project Loom)
- Millions of virtual threads can run concurrently with minimal overhead
- Much more efficient than traditional thread pools for I/O-bound operations
- Configured via Spring's TaskExecutor with configurable pool sizes

## API Endpoints

- `POST /api/v1/validation/validate` - Sync validation (max 250 MB, returns result immediately)
- `POST /api/v1/validation/validate/async` - Async validation (max 2.5 GB, returns job ID)
- `GET /api/v1/validation/jobs/{jobId}/status` - Check job status
- `GET /api/v1/validation/jobs/{jobId}/result` - Get job result
- `GET /api/v1/validation/reports/{reportId}?errors=true&page=0&size=1000` - Get stored report (errors optional, paginated)
- `GET /api/v1/validation/health` - Health check

All responses are JSON, even if you upload a CSV file. The report ID is the file's hash, so you can retrieve it later or get automatic deduplication.

## Technology Choices

- **Java 25**: Modern features like records and virtual threads
- **Spring Boot 3.5.8**: REST API, dependency injection, async support
- **MongoDB**: Stores jobs and reports (survives restarts, works in distributed setups)
- **OpenCSV**: Reliable CSV parsing
- **Jackson**: JSON parsing (built into Spring Boot)
- **xxHash128**: Fast file hashing for deduplication

## Error Handling

- **Parse errors**: Return 400 Bad Request with a clear message
- **Validation errors**: Collect all of them, don't stop at the first one
- **Unexpected errors**: Catch-all handler returns 500 with a user-friendly message (no stack traces)

## Scalability

The service is stateless (except for job/report storage in MongoDB), so you can run multiple instances behind a load balancer. Each file is validated independently, and uniqueness is only checked within a single file (not across files).

For very large files, use the async endpoint. It returns immediately (202 Accepted) and processes in the background using virtual threads.

**Performance Characteristics:**
- **Memory**: Constant memory usage regardless of file size (streaming)
- **CPU**: Single-threaded per file validation (can process multiple files concurrently)
- **I/O**: Streaming reads minimize disk I/O overhead
- **Database**: Batch writes for errors (1000 per batch by default)
- **Concurrency**: Virtual threads enable high concurrency for async jobs

**Scaling Considerations:**
- MongoDB can be scaled horizontally (replica sets, sharding)
- Application instances are stateless and can be horizontally scaled
- File deduplication works across instances (same hash = same report ID)
- Consider MongoDB connection pool sizing for high concurrency

## Adding New Features

**New file format?** Implement `TransactionParser`, register as a Spring component, done.

**New validation rule?** Implement `TransactionValidator`, register as a Spring component, it automatically runs.

**Different storage?** Implement `ReportRepository`, swap the bean, no other changes needed.

The architecture is designed to be extended without modifying existing code.
