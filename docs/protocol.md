# Seriput Protocol [Draft]

This document describes the **initial binary protocol** for the cache. The goal is to keep the protocol simple for now, but **extensible** so it can evolve without breaking compatibility.

## 1. Design Principles

1. **Binary and compact** — messages are framed as binary to reduce overhead.
2. **Extensible** — serializer formats are encoded explicitly so new formats can be added later.
3. **Simple first mindset** — keys are always `String` and values `JSON` for the first version.

## 2. Request Protocol

### 2.1 Frame Structure

Each request is wrapped in a binary frame:

`[ op ][ keyTypeId ][ valueTypeId ][ keyLen ][ valueLen ][ keyBytes ][ valueBytes ]`

| Field           | Size            | Description                                   |
|-----------------|-----------------|-----------------------------------------------|
| **op**          | 1 byte          | Operation code (`GET`, `PUT`, `DELETE`, etc.) |
| **keyTypeId**   | 1 byte          | Specifies how the key is serialized           |
| **valueTypeId** | 1 byte          | Specifies how the value is serialized         |
| **keyLen**      | 4 bytes (int32) | Length of `keyBytes`                          |
| **valueLen**    | 4 bytes (int32) | Length of `valueBytes`                        |
| **keyBytes**    | variable        | Serialized key                                |
| **valueBytes**  | variable        | Serialized value (only for `PUT`)             |

### 2.2 Operation Codes

Current plan (subject to change):

| op value | Meaning |
|----------|---------|
| `0x01`   | GET     |
| `0x02`   | PUT     |
| `0x03`   | DELETE  |

### 2.3 Type IDs

Keys and values use **separate serializers**, allowing independent evolution.

#### Key Serializers

| Type ID | Meaning                | Serializer                                    |
|---------|------------------------|-----------------------------------------------|
| `0x01`  | UTF-8 String (Key)     | `key.getBytes(StandardCharsets.UTF_8)`        |

#### Value Serializers

| Type ID | Meaning                | Serializer                                    |
|---------|------------------------|-----------------------------------------------|
| `0x01`  | JSON (Value as String) | `jsonString.getBytes(StandardCharsets.UTF_8)` |

### 2.4 Example Messages

#### PUT key="user:1", value="{\"name\":\"Alice\",\"age\":30}"

- `op` = 0x02
- `keyTypeId` = 0x01 (UTF-8 String)
- `valueTypeId` = 0x10 (JSON)
- `keyLen` = 6
- `valueLen` = 27
- `keyBytes` = "user:1"
- `valueBytes` = "{"name":"Alice","age":30}"

---

#### GET key="user:1"

- `op` = 0x01
- `keyTypeId` = 0x01 (UTF-8 String)
- `valueTypeId` = 0x00 (ignored in GET request)
- `keyLen` = 6
- `valueLen` = 0
- `keyBytes` = "user:1"

---

#### DELETE key="user:1"

- `op` = 0x03
- `keyTypeId` = 0x01
- `valueTypeId` = 0x00
- `keyLen` = 6
- `valueLen` = 0
- `keyBytes` = "user:1"

---

## 3. Response Protocol

### 3.1 Frame Structure

Each server response is wrapped in a binary frame:

`[ status ][ valueTypeId ][ valueLen ][ valueBytes ]`

| Field           | Size            | Description                                |
|-----------------|-----------------|--------------------------------------------|
| **status**      | 1 byte          | Result of the operation                    |
| **valueTypeId** | 1 byte          | Serializer used for the returned value     |
| **valueLen**    | 4 bytes (int32) | Length of `valueBytes`                     |
| **valueBytes**  | variable        | Serialized response value (only for `GET`) |

### 3.2 Status Codes

| Status | Meaning         |
|--------|-----------------|
| `0x00` | OK              |
| `0x01` | INVALID_REQUEST |
| `0x02` | NOT_FOUND       |
| `0x03` | INTERNAL_ERROR  |

### 3.3 Type IDs

#### Value Deserializers

| Type ID | Meaning                    | Deserializer                   |
|---------|----------------------------|--------------------------------|
| `0x01`  | JSON (UTF-8 encoded bytes) | Jackson `ObjectMapper` ↔ UTF-8 |

### 3.4 Example Responses

#### PUT

- Success: `[00][00][00000000]`

- If internal error: `[03][01][valueLen][valueBytes]`

---

#### GET

- If key exists: `[00][01][valueLen][valueBytes]`

- If key does not exist: `[02][00][0]`

- If internal error: `[03][01][valueLen][valueBytes]`

---

#### DELETE

- Success: `[00][00][00000000]`

- If internal error: `[03][01][valueLen][valueBytes]`

## 4. Compatibility Rules

1. **New serializers** must be assigned new type IDs.
2. Existing type IDs must never change meaning.
3. The frame layout **must never change** — only the meaning of `typeId` values may evolve.  
