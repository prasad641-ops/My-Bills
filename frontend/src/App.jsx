import { useEffect, useMemo, useState } from "react";
import "./App.css";

const newItem = () => ({ id: crypto.randomUUID(), name: "", qty: "1", price: "" });
const newInvoiceNo = () => "INV-" + Date.now().toString().slice(-8);
const n = (v) => (Number.isFinite(parseFloat(v)) ? parseFloat(v) : 0);
const inr = (v) =>
  v.toLocaleString("en-IN", { minimumFractionDigits: 2, maximumFractionDigits: 2 });

const loadShop = () => {
  try {
    return JSON.parse(localStorage.getItem("shop"));
  } catch {
    return null;
  }
};

function Field({ label, optional, children }) {
  return (
    <label className="field">
      <span>
        {label} {optional && <em>(optional)</em>}
      </span>
      {children}
    </label>
  );
}

function Card({ icon, title, children }) {
  return (
    <section className="card">
      <h2>
        <span className="icon">{icon}</span>
        {title}
      </h2>
      {children}
    </section>
  );
}

export default function App() {
  // Shop details are remembered in the browser only (no server storage).
  const [shop, setShop] = useState(loadShop() ?? { name: "", gstin: "", address: "", phones: "" });
  const [customer, setCustomer] = useState({ name: "", mobile: "", address: "" });
  const [items, setItems] = useState([newItem()]);
  const [gstPercent, setGstPercent] = useState("18");
  const [discountType, setDiscountType] = useState("PERCENT");
  const [discountValue, setDiscountValue] = useState("");
  const [invoiceNo, setInvoiceNo] = useState(newInvoiceNo);
  const [posWidth, setPosWidth] = useState(80);
  const [pdf, setPdf] = useState(null); // { url, filename }
  const [loading, setLoading] = useState("");
  const [error, setError] = useState("");

  useEffect(() => localStorage.setItem("shop", JSON.stringify(shop)), [shop]);

  const totals = useMemo(() => {
    const subtotal = items.reduce((s, i) => s + n(i.qty) * n(i.price), 0);
    let discount = 0;
    if (discountValue) {
      discount = discountType === "PERCENT" ? (subtotal * n(discountValue)) / 100 : n(discountValue);
    }
    discount = Math.min(discount, subtotal);
    const taxable = subtotal - discount;
    const gst = (taxable * n(gstPercent)) / 100;
    const raw = taxable + gst;
    const total = Math.round(raw);
    return { subtotal, discount, taxable, gst, roundOff: total - raw, total };
  }, [items, gstPercent, discountType, discountValue]);

  const setShopField = (k) => (e) => setShop({ ...shop, [k]: e.target.value });
  const setCustField = (k) => (e) => setCustomer({ ...customer, [k]: e.target.value });
  const setItemField = (id, k) => (e) =>
    setItems(items.map((i) => (i.id === id ? { ...i, [k]: e.target.value } : i)));
  const removeItem = (id) => setItems(items.length > 1 ? items.filter((i) => i.id !== id) : items);

  function newBill() {
    setCustomer({ name: "", mobile: "", address: "" });
    setItems([newItem()]);
    setDiscountValue("");
    setInvoiceNo(newInvoiceNo());
    setPdf(null);
    setError("");
  }

  async function generate(kind) {
    setError("");
    const filled = items.filter((i) => i.name.trim());
    if (!filled.length) return setError("Add at least one item with a name.");

    const payload = {
      invoiceNo,
      shop,
      customer: { ...customer, address: customer.address.trim() || null },
      items: filled.map((i) => ({ name: i.name.trim(), qty: n(i.qty), price: n(i.price) })),
      gstPercent: n(gstPercent),
      discount: discountValue ? { type: discountType, value: n(discountValue) } : null,
    };

    setLoading(kind);
    try {
      const url = kind === "a4" ? "/api/bills/a4" : `/api/bills/pos?widthMm=${posWidth}`;
      const res = await fetch(url, {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify(payload),
      });
      if (!res.ok) {
        const body = await res.json().catch(() => ({}));
        throw new Error(body.message || `Server error (${res.status})`);
      }
      const blob = await res.blob();
      if (pdf) URL.revokeObjectURL(pdf.url);
      setPdf({ url: URL.createObjectURL(blob), filename: `${invoiceNo}-${kind.toUpperCase()}.pdf`, kind });
    } catch (e) {
      setError(e.message === "Failed to fetch" ? "Cannot reach the backend. Is Spring Boot running on port 8080?" : e.message);
    } finally {
      setLoading("");
    }
  }

  return (
    <div className="app">
      <header className="hero">
        <div>
          <h1>🧾 Quick Bill</h1>
          <p>Create a GST invoice and print it on A4 or a POS roll. Nothing is saved.</p>
        </div>
        <div className="hero-right">
          <span className="pill">{invoiceNo}</span>
          <button className="ghost" onClick={newBill}>+ New bill</button>
        </div>
      </header>

      <main className="layout">
        <div className="left">
          <Card icon="🏪" title="Shop details">
            <div className="grid">
              <Field label="Shop name"><input value={shop.name} onChange={setShopField("name")} placeholder="Sharma General Store" /></Field>
              <Field label="GSTIN"><input value={shop.gstin} onChange={setShopField("gstin")} placeholder="21ABCDE1234F1Z5" maxLength={15} /></Field>
              <Field label="Contact number(s)"><input value={shop.phones} onChange={setShopField("phones")} placeholder="9876543210, 0674-2345678" /></Field>
              <Field label="Shop address"><input value={shop.address} onChange={setShopField("address")} placeholder="Market Road, Bhubaneswar - 751001" /></Field>
            </div>
          </Card>

          <Card icon="👤" title="Customer">
            <div className="grid">
              <Field label="Customer name"><input value={customer.name} onChange={setCustField("name")} placeholder="Ravi Kumar" /></Field>
              <Field label="Mobile number"><input value={customer.mobile} onChange={setCustField("mobile")} placeholder="10 digit number" inputMode="numeric" maxLength={10} /></Field>
              <Field label="Address" optional><input value={customer.address} onChange={setCustField("address")} placeholder="Customer address" /></Field>
            </div>
          </Card>

          <Card icon="🛒" title="Goods / items">
            <div className="items">
              <div className="item-row head">
                <span>Item</span><span>Qty</span><span>Rate</span><span>Amount</span><span />
              </div>
              {items.map((i) => (
                <div className="item-row" key={i.id}>
                  <input value={i.name} onChange={setItemField(i.id, "name")} placeholder="Item name" />
                  <input type="number" min="0" step="any" value={i.qty} onChange={setItemField(i.id, "qty")} />
                  <input type="number" min="0" step="any" value={i.price} onChange={setItemField(i.id, "price")} placeholder="0.00" />
                  <strong className="amt">{inr(n(i.qty) * n(i.price))}</strong>
                  <button className="icon-btn" title="Remove" onClick={() => removeItem(i.id)}>✕</button>
                </div>
              ))}
            </div>
            <button className="ghost" onClick={() => setItems([...items, newItem()])}>+ Add item</button>
          </Card>

          <Card icon="🏷️" title="GST & discount">
            <div className="grid">
              <Field label="GST %">
                <select value={gstPercent} onChange={(e) => setGstPercent(e.target.value)}>
                  {["0", "5", "12", "18", "28"].map((g) => <option key={g} value={g}>{g}%</option>)}
                </select>
              </Field>
              <Field label="Discount" optional>
                <div className="combo">
                  <select value={discountType} onChange={(e) => setDiscountType(e.target.value)}>
                    <option value="PERCENT">%</option>
                    <option value="FLAT">₹ Flat</option>
                  </select>
                  <input type="number" min="0" step="any" value={discountValue} onChange={(e) => setDiscountValue(e.target.value)} placeholder="0" />
                </div>
              </Field>
            </div>
          </Card>
        </div>

        <aside className="right">
          <section className="card summary">
            <h2><span className="icon">💰</span>Bill summary</h2>
            <dl>
              <div><dt>Subtotal</dt><dd>₹ {inr(totals.subtotal)}</dd></div>
              {totals.discount > 0 && <div><dt>Discount</dt><dd>− ₹ {inr(totals.discount)}</dd></div>}
              <div><dt>GST ({gstPercent}%)</dt><dd>₹ {inr(totals.gst)}</dd></div>
              {totals.roundOff !== 0 && <div><dt>Round off</dt><dd>₹ {inr(totals.roundOff)}</dd></div>}
            </dl>
            <div className="grand"><span>Total</span><span>₹ {inr(totals.total)}</span></div>

            {error && <div className="error">{error}</div>}

            <button className="primary" disabled={!!loading} onClick={() => generate("a4")}>
              {loading === "a4" ? "Generating…" : "📄 Generate A4 PDF"}
            </button>
            <div className="pos-row">
              <button className="primary alt" disabled={!!loading} onClick={() => generate("pos")}>
                {loading === "pos" ? "Generating…" : "🧾 Generate POS PDF"}
              </button>
              <select value={posWidth} onChange={(e) => setPosWidth(Number(e.target.value))} title="Paper width">
                <option value={80}>80 mm</option>
                <option value={58}>58 mm</option>
              </select>
            </div>
          </section>

          {pdf && (
            <section className="card preview">
              <h2><span className="icon">👁️</span>Preview</h2>
              <iframe title="Bill preview" src={pdf.url} className={pdf.kind === "pos" ? "tall" : ""} />
              <a className="primary link" href={pdf.url} download={pdf.filename}>⬇ Download {pdf.filename}</a>
            </section>
          )}
        </aside>
      </main>
    </div>
  );
}
