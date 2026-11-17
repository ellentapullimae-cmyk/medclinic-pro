import { useCallback, useEffect, useState } from 'react';
import { api, apiErrorText } from '../api/client';
import { useAuth } from '../context/AuthContext';
import { Icon } from '../components/Icons';
import { Badge, Card, EmptyState, ErrorBox, Loading, PageHead, StatCard } from '../components/ui';

interface Item {
  id: number;
  sku: string;
  name: string;
  stock: number;
  unit_cost: string;
}

interface Movement {
  id: number;
  item_id: number;
  change: number;
  stock_after: number;
  reason?: string | null;
  created_at?: string | null;
}

interface ExpenseLine {
  id: string;
  itemName: string;
  sku: string;
  change: number;
  value: number;
  reason: string;
  createdAt: string;
}

const money = (n: number) =>
  n.toLocaleString('es-PE', { style: 'currency', currency: 'USD', minimumFractionDigits: 2 });

export default function Expenses() {
  const { user: me } = useAuth();
  const isAdmin = me?.role === 'admin';
  const [lines, setLines] = useState<ExpenseLine[]>([]);
  const [inventoryValue, setInventoryValue] = useState(0);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  const load = useCallback(() => {
    if (!isAdmin) {
      setLoading(false);
      setError('');
      return;
    }
    setLoading(true);
    setError('');
    api
      .get('/api/v1/inventory', { params: { page_size: 200 } })
      .then(async (r) => {
        const items: Item[] = r.data.items ?? [];
        setInventoryValue(items.reduce((sum, it) => sum + it.stock * (Number(it.unit_cost) || 0), 0));

        const results = await Promise.all(
          items.map((item) =>
            api
              .get(`/api/v1/inventory/${item.id}/movements`)
              .then((m) => ({ item, movements: (m.data.items ?? []) as Movement[] }))
              .catch(() => ({ item, movements: [] as Movement[] })),
          ),
        );

        const outflows: ExpenseLine[] = results
          .flatMap(({ item, movements }) =>
            movements
              .filter((mv) => mv.change < 0)
              .map((mv) => ({
                id: `${item.id}-${mv.id}`,
                itemName: item.name,
                sku: item.sku,
                change: mv.change,
                value: Math.abs(mv.change) * (Number(item.unit_cost) || 0),
                reason: mv.reason || 'Salida de almacén',
                createdAt: mv.created_at || '',
              })),
          )
          .sort((a, b) => (b.createdAt > a.createdAt ? 1 : -1));

        setLines(outflows);
      })
      .catch((e) => setError(apiErrorText(e, 'No se pudieron calcular los gastos.')))
      .finally(() => setLoading(false));
  }, [isAdmin]);

  useEffect(() => {
    load();
  }, [load]);

  if (!isAdmin) {
    return (
      <>
        <PageHead title="Gastos" subtitle="Consumo real de insumos" />
        <Card>
          <EmptyState
            title="Sin permisos"
            hint="El detalle de movimientos de inventario requiere rol administrador."
          />
        </Card>
      </>
    );
  }

  const consumption = lines.reduce((sum, l) => sum + l.value, 0);

  return (
    <>
      <PageHead
        title="Gastos"
        subtitle="Consumo real de insumos derivado de movimientos de inventario"
        actions={
          <button className="btn btn--soft" onClick={load}>
            <Icon name="trend" size={16} />
            Recalcular
          </button>
        }
      />

      {error && <ErrorBox message={error} />}

      <div className="stat-grid">
        <StatCard label="Valor de inventario" value={money(inventoryValue)} icon="box" tone="blue" />
        <StatCard label="Consumo acumulado" value={money(consumption)} icon="receipt" tone="amber" />
        <StatCard label="Salidas registradas" value={lines.length} icon="trend" tone="violet" />
      </div>

      <Card>
        <div className="card__head">
          <div>
            <h3>Movimientos de salida</h3>
            <span>Cada línea corresponde a un movimiento negativo real de stock</span>
          </div>
        </div>

        {loading ? (
          <Loading label="Calculando gastos…" />
        ) : lines.length === 0 ? (
          <EmptyState
            title="Sin consumos registrados"
            hint="Registra salidas de inventario para ver el gasto reflejado aquí."
          />
        ) : (
          <div className="table-wrap">
            <table className="table">
              <thead>
                <tr>
                  <th>Fecha</th>
                  <th>Insumo</th>
                  <th>SKU</th>
                  <th>Cantidad</th>
                  <th>Motivo</th>
                  <th style={{ textAlign: 'right' }}>Valor</th>
                </tr>
              </thead>
              <tbody>
                {lines.map((l) => (
                  <tr key={l.id}>
                    <td className="cell-muted">{l.createdAt ? l.createdAt.slice(0, 10) : '—'}</td>
                    <td className="cell-strong">{l.itemName}</td>
                    <td className="cell-muted">{l.sku}</td>
                    <td>
                      <Badge label={`${l.change}`} tone="red" />
                    </td>
                    <td className="cell-muted">{l.reason}</td>
                    <td style={{ textAlign: 'right' }} className="cell-strong">
                      {money(l.value)}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </Card>
    </>
  );
}
