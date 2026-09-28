const API_BASE = (localStorage.getItem("tableturnApiBase") || (location.port === "8080" ? "/api" : "http://localhost:8080/api")).replace(/\/$/, "");
const MENU = [["Masala Tea",15],["Filter Coffee",30],["Veg Biryani",180],["Paneer Butter Masala",220],["Butter Naan",40],["Masala Dosa",90],["Fried Rice",150],["Gulab Jamun",60]];
const $ = s => document.querySelector(s);
let tables = [], cart = {}, page = 0, lastPage = true;

async function api(path, method = "GET", body) {
  const r = await fetch(API_BASE + path, {
    method,
    headers: { "Content-Type": "application/json" },
    body: body ? JSON.stringify(body) : undefined
  });
  const d = r.status === 204 ? null : await r.json().catch(() => null);
  if (!r.ok) {
    const detail = d && d.errors ? " " + Object.values(d.errors).join("; ") : "";
    throw new Error((d && d.message || "Request failed") + detail);
  }
  return d;
}

function toast(msg, err) {
  const t = $("#toast");
  t.textContent = msg;
  t.className = err ? "err" : "";
  t.style.display = "block";
  clearTimeout(toast.h);
  toast.h = setTimeout(() => t.style.display = "none", 3800);
}

async function act(fn, ok) {
  try {
    await fn();
    if (ok) toast(ok);
  } catch (e) {
    toast(e.message, true);
  }
}

const money = n => "₹" + Number(n).toFixed(2);

document.querySelectorAll("nav button").forEach(b => b.onclick = () => show(b.dataset.tab));

function show(id) {
  document.querySelectorAll("nav button").forEach(x => x.classList.toggle("on", x.dataset.tab === id));
  document.querySelectorAll("section").forEach(x => x.classList.toggle("on", x.id === id));
  if (id === "floor") loadTables();
  if (id === "res") { loadAvailable(); loadReservations(); }
  if (id === "ord") loadOrderTables();
}

/* ---------- Floor ---------- */
async function loadTables() {
  tables = await api("/tables");
  $("#tables").innerHTML = tables.map(t => `<div class="card table ${t.status}">
    <div class="no">Table ${t.tableNumber}</div>
    <div class="muted">Seats ${t.capacity}</div>
    <span class="badge ${t.status}">${t.status === "FREE" ? "Free" : "Occupied"}</span>
    <div class="row">${t.status === "FREE"
      ? `<button class="b" onclick="walkIn(${t.id},${t.capacity})">Seat walk-in</button>`
      : `<button class="b alt" onclick="goOrders(${t.id})">Orders</button><button class="b warn" onclick="vacate(${t.id})">Vacate</button>`}</div>
  </div>`).join("");
}

function walkIn(id, cap) {
  const name = prompt("Guest name?");
  if (!name) return;
  const size = parseInt(prompt("Party size? (max " + cap + ")", "2"));
  if (!size) return;
  act(async () => {
    await api("/tables/walk-in", "POST", { tableId: id, customerName: name, partySize: size });
    await loadTables();
    await loadAvailable();
    await loadOrderTables();
  }, "Walk-in seated");
}

function vacate(id) {
  act(async () => {
    await api(`/tables/${id}/vacate`, "PUT");
    await loadTables();
    await loadAvailable();
    await loadOrderTables();
  }, "Table is free again");
}

async function goOrders(id) {
  show("ord");
  await loadOrderTables();
  $("#ordTable").value = id;
  await loadOrders();
}

/* ---------- Reservations ---------- */
const f = $("#resForm");

function initDate() {
  const now = new Date();
  const y = now.getFullYear();
  const m = String(now.getMonth() + 1).padStart(2, "0");
  const d = String(now.getDate()).padStart(2, "0");
  return `${y}-${m}-${d}`;
}

if (f && f.date) {
  f.date.value = initDate();
  ["partySize", "date", "startTime", "durationMinutes"].forEach(n => {
    if (f[n]) {
      f[n].addEventListener("input", loadAvailable);
      f[n].addEventListener("change", loadAvailable);
    }
  });

  f.onsubmit = e => {
    e.preventDefault();
    act(async () => {
      await api("/reservations", "POST", {
        customerName: f.customerName.value,
        phone: f.phone.value,
        partySize: +f.partySize.value,
        date: f.date.value,
        startTime: f.startTime.value,
        durationMinutes: +f.durationMinutes.value,
        tableId: +f.tableId.value
      });
      await loadAvailable();
      await loadReservations();
    }, "Table reserved");
  };
}

async function loadAvailable() {
  if (!f || !f.tableId) return;
  if (!f.date.value || !f.startTime.value || !f.partySize.value) {
    f.tableId.innerHTML = '<option value="">Fill party, date & time</option>';
    return;
  }
  try {
    const list = await api(`/tables/available?date=${f.date.value}&startTime=${f.startTime.value}&partySize=${f.partySize.value}&durationMinutes=${f.durationMinutes.value || 90}`);
    f.tableId.innerHTML = list && list.length
      ? list.map(t => `<option value="${t.id}">Table ${t.tableNumber} (seats ${t.capacity})</option>`).join("")
      : `<option value="">No table free for this slot</option>`;
  } catch (e) {
    f.tableId.innerHTML = '<option value="">No table free for this slot</option>';
    toast(e.message, true);
  }
}

async function loadReservations() {
  const d = $("#filterDate").value;
  const p = await api(`/reservations?page=${page}&size=8&sort=reservationDate,desc&sort=startTime,desc${d ? "&date=" + d : ""}`);
  lastPage = p.last;
  $("#pageInfo").textContent = `Page ${p.number + 1} of ${Math.max(p.totalPages, 1)}`;
  $("#resRows").innerHTML = p.content.length ? p.content.map(r => `<tr><td>${r.id}</td><td>${r.table.tableNumber}</td>
    <td>${r.customerName}${r.type === "WALK_IN" ? ' <span class="muted">(walk-in)</span>' : ""}</td><td>${r.partySize}</td>
    <td>${r.reservationDate}</td><td>${r.startTime.slice(0, 5)}–${r.endTime.slice(0, 5)}</td>
    <td><span class="badge ${r.status}">${r.status.toLowerCase()}</span></td>
    <td>${r.status === "BOOKED" ? `<button class="b" onclick="seatRes(${r.id})">Seat</button> <button class="b alt" onclick="cancelRes(${r.id})">Cancel</button>` : ""}</td></tr>`).join("")
    : `<tr><td colspan="8" class="muted">No reservations yet. Book one above.</td></tr>`;
}

$("#filterDate").onchange = () => { page = 0; loadReservations(); };
$("#clearFilter").onclick = () => { $("#filterDate").value = ""; page = 0; loadReservations(); };
$("#prev").onclick = () => { if (page > 0) { page--; loadReservations(); } };
$("#next").onclick = () => { if (!lastPage) { page++; loadReservations(); } };

const seatRes = id => act(async () => {
  await api(`/reservations/${id}/seat`, "PUT");
  await loadReservations();
  await loadTables();
  await loadAvailable();
  await loadOrderTables();
}, "Guests seated");

const cancelRes = id => act(async () => {
  await api(`/reservations/${id}/cancel`, "PUT");
  await loadReservations();
  await loadTables();
  await loadAvailable();
  await loadOrderTables();
}, "Reservation cancelled");

/* ---------- Orders & bill ---------- */
$("#menu").innerHTML = MENU.map(([n, p], i) => `<button class="b alt" onclick="addItem(${i})">${n} · ₹${p}</button>`).join("");

function addItem(i) {
  const [n, p] = MENU[i];
  cart[n] = cart[n] || { itemName: n, unitPrice: p, quantity: 0 };
  cart[n].quantity++;
  drawCart();
}

function chg(n, d) {
  cart[n].quantity += d;
  if (cart[n].quantity <= 0) delete cart[n];
  drawCart();
}

function drawCart() {
  const items = Object.values(cart);
  $("#cart").innerHTML = items.length ? items.map(c => `<tr><td>${c.itemName}</td><td class="r">${c.quantity}</td><td class="r">${money(c.quantity * c.unitPrice)}</td>
   <td class="r"><button class="b alt" onclick="chg('${c.itemName}',-1)">−</button> <button class="b alt" onclick="chg('${c.itemName}',1)">+</button></td></tr>`).join("")
   : `<tr><td colspan="4" class="muted">Tap a menu item to add it.</td></tr>`;
}

async function loadOrderTables() {
  tables = await api("/tables");
  const busy = tables.filter(t => t.status === "OCCUPIED");
  const keep = $("#ordTable").value;
  $("#ordTable").innerHTML = busy.length ? busy.map(t => `<option value="${t.id}">Table ${t.tableNumber}</option>`).join("") : `<option value="">No table is seated</option>`;
  if (keep) $("#ordTable").value = keep;
  drawCart();
  loadOrders();
}

$("#ordTable").onchange = () => { $("#billCard").style.display = "none"; loadOrders(); };

async function loadOrders() {
  const id = $("#ordTable").value;
  if (!id) { $("#orders").textContent = "Choose a seated table."; return; }
  try {
    const list = await api(`/orders/table/${id}`);
    $("#orders").innerHTML = list.length ? list.map(o => `<div style="margin-bottom:10px"><strong>Order #${o.id}</strong> <span class="badge ${o.status === "BILLED" ? "COMPLETED" : "BOOKED"}">${o.status.toLowerCase()}</span>
      <table class="t">${o.items.map(i => `<tr><td>${i.itemName} × ${i.quantity}</td><td class="r">${money(i.lineTotal)}</td></tr>`).join("")}
      <tr><td class="r"><strong>Subtotal</strong></td><td class="r"><strong>${money(o.subtotal)}</strong></td></tr></table></div>`).join("") : "No orders yet for this sitting.";
  } catch (e) { $("#orders").textContent = e.message; }
}

$("#placeOrder").onclick = () => act(async () => {
  const items = Object.values(cart);
  await api("/orders", "POST", { tableId: +$("#ordTable").value, items });
  cart = {};
  drawCart();
  await loadOrders();
}, "Order placed");

$("#billBtn").onclick = () => act(async () => {
  const b = await api(`/bills/table/${$("#ordTable").value}`, "POST");
  const c = $("#billCard");
  c.style.display = "block";
  c.innerHTML = `<h2>Bill #${b.id} · Table ${b.table.tableNumber}</h2>
   <table class="t">${b.orders.flatMap(o => o.items).map(i => `<tr><td>${i.itemName} × ${i.quantity}</td><td class="r">${money(i.lineTotal)}</td></tr>`).join("")}</table>
   <p class="total">Total ${money(b.totalAmount)}</p>
   <button class="b warn" onclick="vacate(${b.table.id});$('#billCard').style.display='none'">Guests paid · vacate table</button>`;
  await loadOrders();
}, "Bill generated");

/* Initial Load */
loadTables();
loadAvailable();
loadOrderTables();
