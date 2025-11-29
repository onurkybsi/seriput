# Protocol [Draft]

This document describes the **initial binary protocol** for the cache.  
The goal is to keep the protocol simple for now, but **extensible** so it can evolve without breaking compatibility.

---

## 1. Design Principles

1. **Binary and compact** — messages are framed as binary to reduce overhead.
2. **Extensible** — serializer formats are encoded explicitly so new formats can be added later.
3. **Simple first mindset** — keys are always `String`, values are always `JSON`.

---

## 2. Message Frame Structure

Each request or response is wrapped in a binary frame:

`[ op ][ keyTypeId ][ valueTypeId ][ keyLen ][ valueLen ][ keyBytes ][ valueBytes ]`

### Field Description

| Field           | Size            | Description                                   |
|-----------------|-----------------|-----------------------------------------------|
| **op**          | 1 byte          | Operation code (`GET`, `PUT`, `DELETE`, etc.) |
| **keyTypeId**   | 1 byte          | Specifies how the key is serialized           |
| **valueTypeId** | 1 byte          | Specifies how the value is serialized         |
| **keyLen**      | 4 bytes (int32) | Length of `keyBytes`                          |
| **valueLen**    | 4 bytes (int32) | Length of `valueBytes`                        |
| **keyBytes**    | variable        | Serialized key                                |
| **valueBytes**  | variable        | Serialized value (only for `PUT`)             |

---

## 3. Operation Codes

Current plan (subject to change):

| op value | Meaning |
|----------|---------|
| `0x01`   | GET     |
| `0x02`   | PUT     |
| `0x03`   | DELETE  |

---

## 4. Type IDs and Serialization

Keys and values use **separate serializers**, allowing independent evolution.

### Current Supported Types (v1)

For the first version of the protocol:

| Type ID | Meaning                | Serializer                                    |
|---------|------------------------|-----------------------------------------------|
| `0x01`  | UTF-8 String (Key)     | `key.getBytes(StandardCharsets.UTF_8)`        |
| `0x01`  | JSON (Value as String) | `jsonString.getBytes(StandardCharsets.UTF_8)` |

## 5. Example Messages

### PUT key="user:1", value="{\"name\":\"Alice\",\"age\":30}"

- `op` = 0x02
- `keyTypeId` = 0x01 (UTF-8 String)
- `valueTypeId` = 0x10 (JSON)
- `keyLen` = 6
- `valueLen` = 27
- `keyBytes` = "user:1"
- `valueBytes` = "{"name":"Alice","age":30}"

---

### GET key="user:1"

- `op` = 0x01
- `keyTypeId` = 0x01 (UTF-8 String)
- `valueTypeId` = 0x00 (ignored in GET request)
- `keyLen` = 6
- `valueLen` = 0
- `keyBytes` = "user:1"

---

### DELETE key="user:1"

- `op` = 0x03
- `keyTypeId` = 0x01
- `valueTypeId` = 0x00
- `keyLen` = 6
- `valueLen` = 0
- `keyBytes` = "user:1"

---

## 6. Compatibility Rules

1. **New serializers** must be assigned new type IDs.
2. Existing type IDs must never change meaning.
3. The frame layout **must never change** — only the meaning of `typeId` values may evolve.  
