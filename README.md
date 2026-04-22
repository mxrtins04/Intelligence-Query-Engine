# Intelligence-Query-Engine

A queryable demographic intelligence API built with Spring Boot. Stores and exposes 2026 profiles enriched with gender, age, and nationality data. Supports advanced filtering, sorting, pagination, and natural language querying.

**Live URL:** `https://hng-tasks-production-0c18.up.railway.app`

---

## Tech Stack

- **Java 21** — OpenJDK 25 targeting Java 21
- **Spring Boot 4.0.5** — REST API framework
- **Spring Data JPA + Specifications** — dynamic query building
- **PostgreSQL** — hosted on Railway
- **Jackson 3.x** — JSON serialization
- **java-uuid-generator** — UUID v7 generation
- **Lombok** — boilerplate reduction
- **Maven** — build tool

---

## Database Schema

Table: `profiles`

| Field | Type | Notes |
|---|---|---|
| `id` | UUID v7 | Primary key, auto-generated |
| `name` | VARCHAR UNIQUE | Person's full name |
| `gender` | VARCHAR | `male` or `female` |
| `gender_probability` | FLOAT | Confidence score (0–1) |
| `age` | INT | Exact age |
| `age_group` | VARCHAR | `child`, `teenager`, `adult`, `senior` |
| `country_id` | VARCHAR(2) | ISO 3166-1 alpha-2 code (e.g. `NG`, `KE`) |
| `country_name` | VARCHAR | Full country name |
| `country_probability` | FLOAT | Confidence score (0–1) |
| `created_at` | TIMESTAMP | Auto-set on insert, UTC |

---

## Data Seeding

The database is pre-seeded with 2026 profiles from `seed_profiles.json` (placed in `src/main/resources`).

Seeding runs automatically on startup via a `CommandLineRunner` bean. It is fully idempotent — re-running the app will not create duplicate records. Records are skipped if a profile with the same `name` already exists.



## API Reference

### `GET /api/profiles`

Returns a paginated, filterable, sortable list of profiles.

**Query Parameters:**

| Parameter | Type | Description |
|---|---|---|
| `gender` | string | `male` or `female` |
| `age_group` | string | `child`, `teenager`, `adult`, `senior` |
| `country_id` | string | ISO code e.g. `NG`, `KE` |
| `min_age` | integer | Minimum age (inclusive) |
| `max_age` | integer | Maximum age (inclusive) |
| `min_gender_probability` | float | Minimum gender confidence score |
| `min_country_probability` | float | Minimum country confidence score |
| `sort_by` | string | `age`, `created_at`, `gender_probability` (default: `created_at`) |
| `order` | string | `asc` or `desc` (default: `asc`) |
| `page` | integer | Page number, 1-indexed (default: `1`) |
| `limit` | integer | Results per page, max 50 (default: `10`) |

All filters are optional and combinable. Results must match **all** supplied conditions.

**Example:**
```
GET /api/profiles?gender=male&country_id=NG&min_age=25&sort_by=age&order=desc&page=1&limit=10
```

**Response:**
```json
{
  "status": "success",
  "page": 1,
  "limit": 10,
  "total": 312,
  "data": [
    {
      "id": "019d9b01-16ec-768d-9f05-d20295fb60c8",
      "name": "Chukwuemeka Obi",
      "gender": "male",
      "gender_probability": 0.98,
      "age": 34,
      "age_group": "adult",
      "country_id": "NG",
      "country_name": "Nigeria",
      "country_probability": 0.81,
      "created_at": "2026-04-17T10:33:53.191325Z"
    }
  ]
}
```

---

### `GET /api/profiles/search`

Accepts a plain English query and converts it into filters automatically.

**Query Parameters:**

| Parameter | Type | Description |
|---|---|---|
| `q` | string | Natural language query (required) |
| `page` | integer | Page number (default: `1`) |
| `limit` | integer | Results per page, max 50 (default: `10`) |

**Example queries:**

| Query | Interpreted as |
|---|---|
| `young males from nigeria` | `gender=male`, `min_age=16`, `max_age=24`, `country_id=NG` |
| `females above 30` | `gender=female`, `min_age=30` |
| `people from angola` | `country_id=AO` |
| `adult males from kenya` | `gender=male`, `age_group=adult`, `country_id=KE` |
| `male and female teenagers above 17` | `age_group=teenager`, `min_age=17` |

> **Note:** `"young"` is a parsing keyword only — it maps to ages 16–24 and is not a stored age group.

**Example:**
```
GET /api/profiles/search?q=young males from nigeria&page=1&limit=10
```

**Response:** Same shape as `GET /api/profiles`.

**Error — unrecognisable query:**
```json
{ "status": "error", "message": "Unable to interpret query" }
```

---

### `GET /api/profiles/{id}`

Returns a single profile by UUID.

```
GET /api/profiles/019d9b01-16ec-768d-9f05-d20295fb60c8
```

**Response:**
```json
{
  "status": "success",
  "data": { ... }
}
```

**Error — not found:**
```json
{ "status": "error", "message": "Person not found" }
```

---

### `POST /api/profiles`

Creates a new profile by calling the Genderize, Agify, and Nationalize APIs to enrich the name with demographic data.

**Request body:**
```json
{ "name": "Amara" }
```

**Response (201 Created):**
```json
{
  "status": "success",
  "data": { ... }
}
```

**Response (200 OK — already exists):**
```json
{
  "status": "success",
  "data": { ... },
  "message": "Profile already exists"
}
```

---

### `DELETE /api/profiles/{id}`

Deletes a profile by UUID. Returns `204 No Content` on success.

---

## Error Responses

All errors follow this structure:

```json
{ "status": "error", "message": "<description>" }
```

| HTTP Status | Meaning |
|---|---|
| `400 Bad Request` | Missing or empty required parameter |
| `422 Unprocessable Entity` | Invalid parameter type or uninterpretable NL query |
| `404 Not Found` | Profile not found |
| `500` | Server error |

---

## Natural Language Query Parser

The `/api/profiles/search` endpoint uses a **rule-based parser** — no AI or LLMs involved.

It works by:
1. Scanning the query string for gender keywords (`male`, `female`, `men`, `women`, etc.)
2. Matching age group keywords (`child`, `teenager`, `adult`, `senior`) or the special keyword `young` (→ ages 16–24)
3. Extracting numeric age constraints via regex patterns (`above 30`, `under 25`, `between 20 and 40`)
4. Detecting country names after `"from"` or `"in"` and mapping them to ISO codes via a lookup table

If no recognisable tokens are found, the parser returns null and the endpoint responds with `"Unable to interpret query"`.

---

## CORS

All endpoints allow cross-origin requests:

```
Access-Control-Allow-Origin: *
```


src/main/resources/
└── seed_profiles.json                   # 2026 profiles seed data
```
