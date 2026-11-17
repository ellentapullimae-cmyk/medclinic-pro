# Modelo de datos

## Diagrama entidad-relación

```mermaid
erDiagram
    USER ||--o{ APPOINTMENT : "atiende"
    USER ||--o{ CLINICAL_RECORD : "firma"
    USER ||--o{ STOCK_MOVEMENT : "registra"
    USER ||--o{ PAYMENT : "cobra"

    PATIENT ||--o{ APPOINTMENT : "tiene"
    PATIENT ||--o{ CLINICAL_RECORD : "posee"
    PATIENT ||--o{ INVOICE : "recibe"

    INVOICE ||--|{ INVOICE_ITEM : "contiene"
    INVOICE ||--o{ PAYMENT : "recibe"

    INVENTORY_ITEM ||--o{ STOCK_MOVEMENT : "movimiento"

    USER {
        int id PK
        string email UK
        string password_hash
        string full_name
        string role "admin|medico|recepcion"
        string specialty "nullable"
        bool is_active
        datetime created_at
    }

    PATIENT {
        int id PK
        string file_number UK "HC-000001"
        string first_name
        string last_name
        string dni UK
        date birth_date
        string sex "M|F|O"
        string phone
        string email
        string address
        string insurance
        string allergies "resumen libre"
        text notes
        bool is_active
        datetime created_at
    }

    APPOINTMENT {
        int id PK
        int patient_id FK
        int doctor_id FK
        datetime start_time
        datetime end_time
        string status "pendiente|confirmada|atendida|cancelada"
        string reason
        text notes
        datetime created_at
    }

    CLINICAL_RECORD {
        int id PK
        int patient_id FK
        int doctor_id FK
        datetime visit_date
        string diagnosis
        string treatment
        text notes
        string vitals "JSON: tension, peso, temp..."
        datetime created_at
    }

    INVOICE {
        int id PK
        int patient_id FK
        int appointment_id FK "nullable"
        string number UK "FAC-000001"
        date issue_date
        string status "pendiente|pagada|anulada"
        decimal total
        datetime created_at
    }

    INVOICE_ITEM {
        int id PK
        int invoice_id FK
        string concept
        int quantity
        decimal unit_price
        decimal subtotal
    }

    PAYMENT {
        int id PK
        int invoice_id FK
        int cashier_id FK
        decimal amount
        string method "efectivo|tarjeta|transferencia|seguro"
        datetime paid_at
    }

    INVENTORY_ITEM {
        int id PK
        string sku UK
        string name
        string category "medicamento|insumo"
        string unit
        int stock
        int min_stock
        date expiry_date "nullable"
        decimal unit_cost
        bool is_active
    }

    STOCK_MOVEMENT {
        int id PK
        int item_id FK
        int user_id FK
        int change "positivo=entrada, negativo=salida"
        int stock_after
        string reason
        datetime created_at
    }
```

## Reglas de negocio clave

1. **Nº de historia clínica** (`file_number`) autogenerado: `HC-000001`, único.
2. **Nº de factura** autogenerado: `FAC-000001`, único, inmutable.
3. **Citas**: `end_time > start_time`; sin solape entre citas de mismo médico en estado `pendiente|confirmada`.
4. **Totales de factura**: `total = Σ(quantity × unit_price)`, recalculado en el service, nunca confiado al cliente.
5. **Inventario**: cada modificación de `stock` genera un `STOCK_MOVEMENT` (auditoría); no se permite stock negativo.
6. **Borrado lógico**: `is_active = false` en usuarios, pacientes e ítems; nunca `DELETE` de datos clínicos.
7. **Historia clínica**: solo `medico` crea/edita; `recepcion` solo lee ficha básica; auditoría de quién firmó.

## Estados

- Cita: `pendiente → confirmada → atendida`, con `cancelada` desde cualquier estado previo.
- Factura: `pendiente → pagada` (al cubrirse el total), `anulada` solo si no tiene pagos.
