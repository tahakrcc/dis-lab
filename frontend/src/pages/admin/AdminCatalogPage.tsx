import { useEffect, useMemo, useState } from "react";
import { ApiError } from "../../api/client";
import { formatTutar } from "../../api/cases";
import { currentPrices, type BirimTipi, type Partnership, type PriceEntry, type ServiceItem } from "../../api/catalog";
import type { Organization } from "../../api/org";
import {
  createServiceItem,
  getPrices,
  listOrganizations,
  listPartnerships,
  listServiceItems,
  upsertPrice,
} from "../../api/admin";

const bugun = () => new Date().toISOString().slice(0, 10);
const birimEtiket = (b: BirimTipi) => (b === "DIS" ? "Diş" : "Adet");

export default function AdminCatalogPage() {
  const [orgs, setOrgs] = useState<Organization[]>([]);
  const [partnerships, setPartnerships] = useState<Partnership[]>([]);
  const [hata, setHata] = useState<string | null>(null);

  // katalog
  const [labId, setLabId] = useState("");
  const [items, setItems] = useState<ServiceItem[]>([]);
  const [yeniAd, setYeniAd] = useState("");
  const [yeniBirim, setYeniBirim] = useState<BirimTipi>("DIS");
  const [ekleBusy, setEkleBusy] = useState(false);

  // fiyat
  const [partnershipId, setPartnershipId] = useState("");
  const [prices, setPrices] = useState<PriceEntry[]>([]);
  const [priceItems, setPriceItems] = useState<ServiceItem[]>([]);
  const [fiyatInput, setFiyatInput] = useState<Record<string, string>>({});
  const [fiyatBusy, setFiyatBusy] = useState<string | null>(null);

  const labs = useMemo(() => orgs.filter((o) => o.tip === "LAB"), [orgs]);

  useEffect(() => {
    Promise.all([listOrganizations(), listPartnerships()])
      .then(([o, p]) => {
        setOrgs(o);
        setPartnerships(p);
        setLabId((c) => c || o.find((x) => x.tip === "LAB")?.id || "");
        setPartnershipId((c) => c || p[0]?.id || "");
      })
      .catch((e) => setHata(e instanceof ApiError ? e.message : "Yüklenemedi."));
  }, []);

  async function katalogYukle(id: string) {
    if (!id) return;
    setItems(await listServiceItems(id));
  }
  useEffect(() => {
    if (labId) katalogYukle(labId).catch(() => setItems([]));
  }, [labId]);

  // seçili ortaklık -> o labın kalemleri + fiyatlar
  useEffect(() => {
    if (!partnershipId) return;
    const p = partnerships.find((x) => x.id === partnershipId);
    if (!p) return;
    Promise.all([listServiceItems(p.labId), getPrices(partnershipId)])
      .then(([si, pr]) => {
        setPriceItems(si.filter((x) => x.aktif));
        setPrices(currentPrices(pr));
      })
      .catch(() => {
        setPriceItems([]);
        setPrices([]);
      });
  }, [partnershipId, partnerships]);

  const priceById = useMemo(() => {
    const m = new Map<string, PriceEntry>();
    prices.forEach((p) => m.set(p.serviceItemId, p));
    return m;
  }, [prices]);

  async function kalemEkle(e: React.FormEvent) {
    e.preventDefault();
    setHata(null);
    if (!yeniAd.trim() || !labId) return;
    setEkleBusy(true);
    try {
      await createServiceItem(labId, { ad: yeniAd.trim(), birim: yeniBirim });
      setYeniAd("");
      await katalogYukle(labId);
    } catch (err) {
      setHata(err instanceof ApiError ? err.message : "Kalem eklenemedi.");
    } finally {
      setEkleBusy(false);
    }
  }

  async function fiyatKaydet(serviceItemId: string) {
    const raw = fiyatInput[serviceItemId];
    if (!raw || !raw.trim()) return;
    const fiyat = Number(raw.replace(",", "."));
    if (Number.isNaN(fiyat) || fiyat < 0) return setHata("Geçerli bir fiyat gir.");
    setFiyatBusy(serviceItemId);
    setHata(null);
    try {
      await upsertPrice(partnershipId, { serviceItemId, fiyat, gecerliBaslangic: bugun() });
      setPrices(currentPrices(await getPrices(partnershipId)));
      setFiyatInput((s) => ({ ...s, [serviceItemId]: "" }));
    } catch (err) {
      setHata(err instanceof ApiError ? err.message : "Fiyat kaydedilemedi.");
    } finally {
      setFiyatBusy(null);
    }
  }

  return (
    <div className="page">
      <div className="page-head">
        <div className="eyebrow">Platform</div>
        <h1 className="page-title">Katalog &amp; fiyat</h1>
        <p className="page-desc">Laboratuvar kataloğunu yönet; her ortaklık (lab↔klinik) için ayrı fiyat belirle.</p>
      </div>

      {hata && <div className="alert alert-error">{hata}</div>}

      <section className="card block">
        <div className="card-title cardtitle-row">
          <span>Hizmet kataloğu</span>
          <label className="field inline-select tight">
            <span>Laboratuvar</span>
            <select value={labId} onChange={(e) => setLabId(e.target.value)}>
              {labs.map((o) => (
                <option key={o.id} value={o.id}>
                  {o.ad}
                </option>
              ))}
            </select>
          </label>
        </div>
        <div className="table-wrap">
          <table className="data-table">
            <thead>
              <tr>
                <th>Ad</th>
                <th>Birim</th>
                <th>Durum</th>
              </tr>
            </thead>
            <tbody>
              {items.map((it) => (
                <tr key={it.id}>
                  <td>{it.ad}</td>
                  <td className="muted-cell">{birimEtiket(it.birim)}</td>
                  <td>
                    <span className={`status-badge ${it.aktif ? "st-onay" : "st-taslak"}`}>
                      {it.aktif ? "Aktif" : "Pasif"}
                    </span>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
        <form className="add-catalog" onSubmit={kalemEkle}>
          <input className="add-ad" value={yeniAd} onChange={(e) => setYeniAd(e.target.value)} placeholder="Yeni hizmet adı" />
          <select value={yeniBirim} onChange={(e) => setYeniBirim(e.target.value as BirimTipi)}>
            <option value="DIS">Diş başına</option>
            <option value="ADET">Adet başına</option>
          </select>
          <button className="btn btn-primary" type="submit" disabled={ekleBusy}>
            {ekleBusy ? "…" : "+ Ekle"}
          </button>
        </form>
      </section>

      <section className="card block">
        <div className="card-title cardtitle-row">
          <span>Ortaklığa özel fiyat</span>
          <label className="field inline-select tight">
            <span>Ortaklık</span>
            <select value={partnershipId} onChange={(e) => setPartnershipId(e.target.value)}>
              {partnerships.map((p) => (
                <option key={p.id} value={p.id}>
                  {p.labAd} ↔ {p.clinicAd}
                </option>
              ))}
            </select>
          </label>
        </div>
        {priceItems.length === 0 ? (
          <div className="empty sm">Bu labın aktif kalemi yok.</div>
        ) : (
          <div className="table-wrap">
            <table className="data-table">
              <thead>
                <tr>
                  <th>Kalem</th>
                  <th>Birim</th>
                  <th className="num">Güncel</th>
                  <th>Yeni fiyat</th>
                  <th aria-label="kaydet"></th>
                </tr>
              </thead>
              <tbody>
                {priceItems.map((it) => {
                  const pe = priceById.get(it.id);
                  return (
                    <tr key={it.id}>
                      <td>{it.ad}</td>
                      <td className="muted-cell">{birimEtiket(it.birim)}</td>
                      <td className="num mono">{pe ? formatTutar(pe.fiyat) : "—"}</td>
                      <td>
                        <input
                          className="price-input"
                          inputMode="decimal"
                          placeholder={pe ? String(pe.fiyat) : "0"}
                          value={fiyatInput[it.id] ?? ""}
                          onChange={(e) => setFiyatInput((s) => ({ ...s, [it.id]: e.target.value }))}
                        />
                      </td>
                      <td className="num">
                        <button
                          className="mini-btn primary"
                          disabled={fiyatBusy === it.id || !(fiyatInput[it.id] ?? "").trim()}
                          onClick={() => fiyatKaydet(it.id)}
                        >
                          {fiyatBusy === it.id ? "…" : "Kaydet"}
                        </button>
                      </td>
                    </tr>
                  );
                })}
              </tbody>
            </table>
          </div>
        )}
      </section>
    </div>
  );
}
