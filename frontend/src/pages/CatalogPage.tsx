import { useEffect, useMemo, useState } from "react";
import { useAuth } from "../auth/AuthContext";
import { ApiError } from "../api/client";
import { formatTutar } from "../api/cases";
import {
  createServiceItem,
  currentPrices,
  getPartnerships,
  getPartnershipPrices,
  getServiceItems,
  updateServiceItem,
  upsertPrice,
  type BirimTipi,
  type Partnership,
  type PriceEntry,
  type ServiceItem,
} from "../api/catalog";

const bugun = () => new Date().toISOString().slice(0, 10);
const birimEtiket = (b: BirimTipi) => (b === "DIS" ? "Diş" : "Adet");

export default function CatalogPage() {
  const { activeMembership, activeOrgId } = useAuth();
  const labAdmin = activeMembership?.rol === "LAB_ADMIN";

  const [items, setItems] = useState<ServiceItem[]>([]);
  const [partnerships, setPartnerships] = useState<Partnership[]>([]);
  const [partnershipId, setPartnershipId] = useState("");
  const [prices, setPrices] = useState<PriceEntry[]>([]);
  const [hata, setHata] = useState<string | null>(null);

  const [yeniAd, setYeniAd] = useState("");
  const [yeniBirim, setYeniBirim] = useState<BirimTipi>("DIS");
  const [ekleBusy, setEkleBusy] = useState(false);

  const [fiyatInput, setFiyatInput] = useState<Record<string, string>>({});
  const [fiyatBusy, setFiyatBusy] = useState<string | null>(null);

  async function katalogYukle() {
    setItems(await getServiceItems());
  }

  useEffect(() => {
    if (!labAdmin) return;
    let iptal = false;
    Promise.all([getServiceItems(), getPartnerships()])
      .then(([si, ps]) => {
        if (iptal) return;
        setItems(si);
        setPartnerships(ps);
        setPartnershipId((cur) => cur || ps[0]?.id || "");
      })
      .catch((e) => !iptal && setHata(e instanceof ApiError ? e.message : "Veriler yüklenemedi."));
    return () => {
      iptal = true;
    };
  }, [activeOrgId, labAdmin]);

  useEffect(() => {
    if (!partnershipId) return;
    getPartnershipPrices(partnershipId)
      .then((p) => setPrices(currentPrices(p)))
      .catch(() => setPrices([]));
  }, [partnershipId]);

  const priceById = useMemo(() => {
    const m = new Map<string, PriceEntry>();
    prices.forEach((p) => m.set(p.serviceItemId, p));
    return m;
  }, [prices]);

  async function kalemEkle(e: React.FormEvent) {
    e.preventDefault();
    setHata(null);
    if (!yeniAd.trim()) return;
    setEkleBusy(true);
    try {
      await createServiceItem({ ad: yeniAd.trim(), birim: yeniBirim });
      setYeniAd("");
      await katalogYukle();
    } catch (err) {
      setHata(err instanceof ApiError ? err.message : "Kalem eklenemedi.");
    } finally {
      setEkleBusy(false);
    }
  }

  async function aktiflikDegis(it: ServiceItem) {
    try {
      await updateServiceItem(it.id, { aktif: !it.aktif });
      await katalogYukle();
    } catch (err) {
      setHata(err instanceof ApiError ? err.message : "Güncellenemedi.");
    }
  }

  async function fiyatKaydet(serviceItemId: string) {
    const raw = fiyatInput[serviceItemId];
    if (raw == null || raw === "") return;
    const fiyat = Number(raw.replace(",", "."));
    if (Number.isNaN(fiyat) || fiyat < 0) {
      setHata("Geçerli bir fiyat gir.");
      return;
    }
    setFiyatBusy(serviceItemId);
    setHata(null);
    try {
      await upsertPrice(partnershipId, { serviceItemId, fiyat, gecerliBaslangic: bugun() });
      const p = await getPartnershipPrices(partnershipId);
      setPrices(currentPrices(p));
      setFiyatInput((s) => ({ ...s, [serviceItemId]: "" }));
    } catch (err) {
      setHata(err instanceof ApiError ? err.message : "Fiyat kaydedilemedi.");
    } finally {
      setFiyatBusy(null);
    }
  }

  if (!labAdmin) {
    return (
      <div className="page">
        <div className="empty">Katalog ve fiyat yönetimi yalnızca laboratuvar yöneticisine açıktır.</div>
      </div>
    );
  }

  const aktifItems = items.filter((i) => i.aktif);

  return (
    <div className="page">
      <div className="page-head">
        <div className="eyebrow">Katalog &amp; fiyat</div>
        <h1 className="page-title">Katalog &amp; fiyat</h1>
        <p className="page-desc">
          Hizmet kalemlerini tanımla; her klinik için ayrı fiyat belirle. Fiyat değişiklikleri
          sürümlenir — geçmiş vakalar etkilenmez.
        </p>
      </div>

      {hata && <div className="alert alert-error">{hata}</div>}

      <section className="card block">
        <div className="card-title">Hizmet kataloğu ({items.length})</div>
        <div className="table-wrap">
          <table className="data-table">
            <thead>
              <tr>
                <th>Ad</th>
                <th>Birim</th>
                <th>Durum</th>
                <th aria-label="işlem"></th>
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
                  <td className="num">
                    <button className="mini-btn" onClick={() => aktiflikDegis(it)}>
                      {it.aktif ? "Pasifleştir" : "Aktifleştir"}
                    </button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>

        <form className="add-catalog" onSubmit={kalemEkle}>
          <input
            className="add-ad"
            value={yeniAd}
            onChange={(e) => setYeniAd(e.target.value)}
            placeholder="Yeni hizmet adı (ör. İmplant Üstü Kron)"
          />
          <select value={yeniBirim} onChange={(e) => setYeniBirim(e.target.value as BirimTipi)}>
            <option value="DIS">Diş başına</option>
            <option value="ADET">Adet başına</option>
          </select>
          <button className="btn btn-primary" type="submit" disabled={ekleBusy}>
            {ekleBusy ? "Ekleniyor…" : "+ Ekle"}
          </button>
        </form>
      </section>

      <section className="card block">
        <div className="card-title cardtitle-row">
          <span>Kliniğe özel fiyat</span>
          <label className="field inline-select tight">
            <span>Klinik</span>
            <select value={partnershipId} onChange={(e) => setPartnershipId(e.target.value)}>
              {partnerships.map((p) => (
                <option key={p.id} value={p.id}>
                  {p.clinicAd}
                </option>
              ))}
            </select>
          </label>
        </div>

        {aktifItems.length === 0 ? (
          <div className="empty sm">Önce katalog kalemi ekle.</div>
        ) : (
          <div className="table-wrap">
            <table className="data-table">
              <thead>
                <tr>
                  <th>Kalem</th>
                  <th>Birim</th>
                  <th className="num">Güncel fiyat</th>
                  <th>Yeni fiyat</th>
                  <th aria-label="kaydet"></th>
                </tr>
              </thead>
              <tbody>
                {aktifItems.map((it) => {
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
