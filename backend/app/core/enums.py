from enum import Enum


class Role(str, Enum):
    admin = "admin"
    medico = "medico"
    recepcion = "recepcion"
    paciente = "paciente"


class Sex(str, Enum):
    M = "M"
    F = "F"
    O = "O"


class AppointmentStatus(str, Enum):
    pendiente = "pendiente"
    confirmada = "confirmada"
    atendida = "atendida"
    cancelada = "cancelada"


class InvoiceStatus(str, Enum):
    pendiente = "pendiente"
    pagada = "pagada"
    anulada = "anulada"


class PaymentMethod(str, Enum):
    efectivo = "efectivo"
    tarjeta = "tarjeta"
    transferencia = "transferencia"
    seguro = "seguro"


class ItemCategory(str, Enum):
    medicamento = "medicamento"
    insumo = "insumo"
