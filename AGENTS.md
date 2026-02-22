# AGENTS Guidelines for This Repository

This repository contains a Spring Boot backend application for a PFE project.  
When working on the project interactively with an agent (e.g., VS Code AI agent), please follow the guidelines below to ensure clean, stable, and production-level development.

---

## 1. Do Only the Requested Task

Always implement ONLY the exact task explicitly requested.

- Do NOT add extra features.
- Do NOT generate additional layers (controller, service, etc.) unless explicitly requested.
- Do NOT refactor unrelated code.
- Do NOT introduce speculative improvements.
- Keep implementations simple and clear.

Avoid over-engineering. This is a structured PFE project, not an experimental playground.

---

## 2. Preserve Project Structure

This project follows a clean layered architecture:

- controller  
- service  
- repository  
- model  
- dto  
- security  
- config  

Do NOT mix responsibilities between layers.  
Do NOT create new folders unless explicitly instructed.

Production-level structure only.

---

## 3. Models (Entities) Protection Rule

The models are the foundation of this project.

- NEVER modify, refactor, rename, or restructure any entity without explicit approval.
- If a model change seems necessary, ask for confirmation before applying it.
- Do NOT adjust relationships unless explicitly instructed.

Models stability is mandatory.

---

## 4. Coding Standards

### Java Naming Conventions
- Classes → PascalCase  
- Variables → camelCase  
- Enums → PascalCase  
- Enum values → UPPER_CASE  

### Database (PostgreSQL) Naming
- Tables → snake_case  
- Columns → snake_case  
- Foreign keys → snake_case  
- Never use quoted identifiers.

Keep code readable, minimal, and production-ready.

---

## 5. Output Style Rules

- Return only what is requested.
- No unnecessary explanations unless explicitly asked.
- No excessive comments explaining basic concepts.
- Keep responses concise.
- Maintain clean formatting.

---

## 6. Development Workflow

When modifying backend code:

- Do not change configuration files unless requested.
- Do not alter dependencies unless instructed.
- Do not generate markdown documentation files unless explicitly requested.

If unsure, ask before making structural changes.

---

Following these guidelines ensures a clean, organized, and professional PFE backend project with stable foundations and predictable agent behavior.
