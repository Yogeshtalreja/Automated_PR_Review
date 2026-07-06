# Project Context

## Overview
This is a Spring Boot 2.5.4 REST API backend built with Java 1.8.

## Architecture
- Controllers handle HTTP requests, never contain business logic
- Services contain all business logic, annotated with @Service
- Repositories extend JpaRepository for database access
- DTOs are used for request/response, never expose entities directly

## Coding Conventions
- All service methods that write to DB must have @Transactional
- Never return null — use Optional or throw a specific exception
- All public methods must have null checks on parameters
- Exceptions: use custom exceptions, never throw RuntimeException directly
- Logging: use @Slf4j, log at DEBUG for normal flow, ERROR for exceptions

## Known Fragile Areas
- Payment processing logic — any changes here need extra scrutiny
- User authentication — security sensitive, review carefully

## Tech Stack
- Spring Boot 2.5.4, Java 1.8
- MySQL with JPA/Hibernate
- RestTemplate for HTTP calls 