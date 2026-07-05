export default function EstadoBadge({ estado }) {
  if (!estado) return null;
  return <span className={`badge ${estado}`}>{estado.replace('_', ' ')}</span>;
}
