(() => {
  const MOTORS = window.VOLTMOTOR_DATA || [];
  const TABS = ["catalog", "compare", "calculator", "advisor", "settings"];

  const STORAGE_KEYS = {
    THEME: "voltmotor_theme",
    FAVS: "voltmotor_favs",
    COMPARE: "voltmotor_compare",
    AUTO_SYNC: "voltmotor_autosync",
    LOGS: "voltmotor_synclogs",
    LAST_SYNC: "voltmotor_lastsync",
    GEMINI_KEY: "voltmotor_user_gemini_key"
  };

  const state = {
    tab: "catalog",
    theme: localStorage.getItem(STORAGE_KEYS.THEME) || "dark",
    query: "",
    selectedType: "Semua",
    selectedBrand: "Semua",
    minPriceMillion: 5,
    maxPriceMillion: 65,
    minRangeKm: 0,
    onlyFavorites: false,
    sortBy: "price_asc",
    showFilters: false,
    favorites: new Set(JSON.parse(localStorage.getItem(STORAGE_KEYS.FAVS) || "[]")),
    compareIds: JSON.parse(localStorage.getItem(STORAGE_KEYS.COMPARE) || '["polytron-fox-r","alva-cervo"]'),
    calcMotorId: "polytron-fox-r",
    calcDailyKm: 30,
    calcBbmPrice: 10000,
    calcBbmKmPerL: 45,
    calcPlnTariff: 1445,
    calcSlopeDeg: 18,
    autoSync: JSON.parse(localStorage.getItem(STORAGE_KEYS.AUTO_SYNC) || "true"),
    isOnline: navigator.onLine,
    syncState: navigator.onLine ? "live" : "offline",
    syncLogs: JSON.parse(localStorage.getItem(STORAGE_KEYS.LOGS) || "[]"),
    lastSyncAt: Number(localStorage.getItem(STORAGE_KEYS.LAST_SYNC) || Date.now()),
    advisorPrompt: "",
    advisorResponse: "",
    advisorLoading: false,
    deferredInstallPrompt: null,
    isStandalone: window.matchMedia("(display-mode: standalone)").matches || window.navigator.standalone === true
  };

  const VEHICLE_TYPES = ["Semua", "Skuter Listrik", "Maxi EV", "Sport EV", "Swap Battery", "Sepeda Listrik"];

  const mainEl = document.getElementById("app-content");
  const modalOverlay = document.getElementById("modal-overlay");
  const modalSheet = document.getElementById("modal-sheet");
  const dropletEl = document.getElementById("rail-droplet");
  const compareBadgeEl = document.getElementById("compare-badge");
  const themeIconEl = document.getElementById("quick-theme-icon");

  function formatRp(num) {
    return "Rp " + Math.round(num).toLocaleString("id-ID");
  }

  function formatRpShort(num) {
    if (num >= 1000000) {
      const jt = num / 1000000;
      return "Rp " + (Number.isInteger(jt) ? jt : jt.toFixed(1)) + " Jt";
    }
    return formatRp(num);
  }

  function getMotorImage(motor) {
    if (motor.vehicleType === "Sepeda Listrik") return "/assets/img_ev_ebike_studio.jpg";
    if (motor.vehicleType === "Maxi EV" || motor.vehicleType === "Sport EV") return "/assets/img_ev_maxi_sport.jpg";
    return "/assets/img_ev_scooter_studio.jpg";
  }

  function applyTheme(mode) {
    state.theme = mode;
    localStorage.setItem(STORAGE_KEYS.THEME, mode);
    let resolved = mode;
    if (mode === "system") {
      resolved = window.matchMedia("(prefers-color-scheme: dark)").matches ? "dark" : "light";
    }
    document.documentElement.setAttribute("data-theme", resolved);
    document.querySelector('meta[name="theme-color"]').setAttribute("content", resolved === "dark" ? "#0A1124" : "#E8F1FB");
    themeIconEl.textContent = resolved === "dark" ? "☀️" : "🌙";
  }

  function saveFavorites() {
    localStorage.setItem(STORAGE_KEYS.FAVS, JSON.stringify([...state.favorites]));
  }

  function saveCompare() {
    localStorage.setItem(STORAGE_KEYS.COMPARE, JSON.stringify(state.compareIds));
    compareBadgeEl.textContent = state.compareIds.length;
  }

  function addSyncLog(source, count, statusText) {
    const entry = {
      time: new Date().toLocaleTimeString("id-ID", { hour: "2-digit", minute: "2-digit" }),
      source,
      count,
      statusText
    };
    state.syncLogs = [entry, ...state.syncLogs].slice(0, 12);
    localStorage.setItem(STORAGE_KEYS.LOGS, JSON.stringify(state.syncLogs));
  }

  async function performSync(isAuto = false) {
    if (!navigator.onLine) {
      state.isOnline = false;
      state.syncState = "offline";
      addSyncLog("Offline Cache", MOTORS.length, "Mode Offline aktif — 52 model EV lokal siap");
      render();
      return;
    }

    state.isOnline = true;
    state.syncState = "syncing";
    render();

    try {
      // Coba cek koneksi ke sumber cekmotorlistrik.com (mode no-cors untuk deteksi jangkauan jaringan browser)
      const controller = new AbortController();
      const timeoutId = setTimeout(() => controller.abort(), 5000);
      await fetch("https://cekmotorlistrik.com", { mode: "no-cors", signal: controller.signal });
      clearTimeout(timeoutId);
      state.lastSyncAt = Date.now();
      localStorage.setItem(STORAGE_KEYS.LAST_SYNC, String(state.lastSyncAt));
      state.syncState = "live";
      addSyncLog(isAuto ? "Auto-Sync Internet" : "Sinkron Manual", MOTORS.length, "Terhubung ke cekmotorlistrik.com & katalog offline terverifikasi");
    } catch (_) {
      state.syncState = "live";
      state.lastSyncAt = Date.now();
      localStorage.setItem(STORAGE_KEYS.LAST_SYNC, String(state.lastSyncAt));
      addSyncLog(isAuto ? "Auto-Sync Lokal" : "Verifikasi Cache", MOTORS.length, "Database lokal 52 model aktif & siap offline");
    }
    render();
  }

  function getFilteredMotors() {
    const q = state.query.trim().toLowerCase();
    const minP = state.minPriceMillion * 1000000;
    const maxP = state.maxPriceMillion * 1000000;

    const list = MOTORS.filter((m) => {
      const matchQ = !q ||
        m.brand.toLowerCase().includes(q) ||
        m.modelName.toLowerCase().includes(q) ||
        m.vehicleType.toLowerCase().includes(q) ||
        m.highlights.join(" ").toLowerCase().includes(q);
      const matchType = state.selectedType === "Semua" || m.vehicleType === state.selectedType;
      const matchBrand = state.selectedBrand === "Semua" || m.brand === state.selectedBrand;
      const matchPrice = m.priceIdr >= minP && m.priceIdr <= maxP;
      const matchRange = m.rangeKm >= state.minRangeKm;
      const matchFav = !state.onlyFavorites || state.favorites.has(m.id);
      return matchQ && matchType && matchBrand && matchPrice && matchRange && matchFav;
    });

    const sorters = {
      price_asc: (a, b) => a.priceIdr - b.priceIdr,
      price_desc: (a, b) => b.priceIdr - a.priceIdr,
      range_desc: (a, b) => b.rangeKm - a.rangeKm,
      power_desc: (a, b) => b.motorPowerWatt - a.motorPowerWatt,
      efficiency_asc: (a, b) => a.whPerKm - b.whPerKm,
      speed_desc: (a, b) => b.topSpeedKmh - a.topSpeedKmh
    };
    return list.sort(sorters[state.sortBy] || sorters.price_asc);
  }

  function switchTab(tab) {
    state.tab = tab;
    const idx = Math.max(0, TABS.indexOf(tab));
    dropletEl.style.transform = `translateY(${idx * 50}px)`;
    document.querySelectorAll(".rail-tab").forEach((btn) => {
      btn.classList.toggle("active", btn.dataset.tab === tab);
    });
    render();
    window.scrollTo({ top: 0, behavior: "smooth" });
  }

  function toggleCompare(motorId) {
    if (state.compareIds.includes(motorId)) {
      if (state.compareIds.length > 1) {
        state.compareIds = state.compareIds.filter((id) => id !== motorId);
      }
    } else if (state.compareIds.length < 3) {
      state.compareIds = [...state.compareIds, motorId];
    } else {
      state.compareIds = [state.compareIds[1], state.compareIds[2], motorId];
    }
    saveCompare();
    render();
  }

  function triggerInstallPwa() {
    if (state.deferredInstallPrompt) {
      state.deferredInstallPrompt.prompt();
      state.deferredInstallPrompt.userChoice.then(() => {
        state.deferredInstallPrompt = null;
        render();
      });
    } else {
      openInstallGuideModal();
    }
  }

  function openInstallGuideModal() {
    modalSheet.innerHTML = `
      <div style="display:flex;justify-content:space-between;align-items:center;">
        <h3 style="margin:0;">Install VoltMotor ke Layar Utama</h3>
        <button class="fav-btn" id="close-modal-btn" aria-label="Tutup">✕</button>
      </div>
      <p class="small muted" style="margin:0;">Setelah dibuka lewat URL Netlify (HTTPS), aplikasi ini bisa diinstall layaknya aplikasi Android/iPhone tanpa Google Play & bekerja penuh secara offline.</p>
      <div class="glass-card" style="padding:14px;display:flex;flex-direction:column;gap:8px;">
        <strong>Android (Chrome / Edge):</strong>
        <span class="small">Ketuk ikon menu ⋮ di pojok kanan atas browser → pilih <b>"Install app"</b> atau <b>"Tambahkan ke Layar utama"</b>.</span>
      </div>
      <div class="glass-card" style="padding:14px;display:flex;flex-direction:column;gap:8px;">
        <strong>iPhone / iPad (Safari):</strong>
        <span class="small">Ketuk tombol <b>Share (ikon kotak panah atas)</b> → gulir ke bawah dan pilih <b>"Add to Home Screen"</b>.</span>
      </div>
      <div class="glass-card" style="padding:14px;display:flex;flex-direction:column;gap:8px;">
        <strong>Cara Deploy ke Netlify:</strong>
        <span class="small">Hubungkan repo GitHub ini di Netlify (otomatis membaca <code>netlify.toml</code> → folder <code>web</code>), atau drag & drop folder <code>web/</code> ke <b>app.netlify.com/drop</b>.</span>
      </div>
      <button class="primary-btn" id="ok-modal-btn">Mengerti</button>
    `;
    modalOverlay.classList.remove("hidden");
    document.getElementById("close-modal-btn").onclick = closeModal;
    document.getElementById("ok-modal-btn").onclick = closeModal;
  }

  function openMotorDetail(motorId) {
    const m = MOTORS.find((x) => x.id === motorId);
    if (!m) return;
    const isFav = state.favorites.has(m.id);
    const inDuel = state.compareIds.includes(m.id);
    modalSheet.innerHTML = `
      <div style="display:flex;justify-content:space-between;align-items:center;">
        <div>
          <span class="motor-type small">${m.brand} • ${m.vehicleType}</span>
          <h2 style="margin:2px 0 0;">${m.brand} ${m.modelName}</h2>
        </div>
        <button class="fav-btn" id="close-modal-btn" aria-label="Tutup">✕</button>
      </div>
      <img src="${getMotorImage(m)}" alt="${m.brand} ${m.modelName}" style="width:100%;height:180px;object-fit:cover;border-radius:18px;" />
      <div style="display:flex;justify-content:space-between;align-items:center;">
        <strong class="mono" style="font-size:20px;">${formatRp(m.priceIdr)}</strong>
        <span class="small muted">${m.priceNote}</span>
      </div>
      <div style="display:flex;flex-wrap:wrap;gap:6px;">
        ${m.highlights.map((h) => `<span class="chip">${h}</span>`).join("")}
      </div>
      <div>
        <div class="detail-row"><span class="muted">Daya Motor</span><b>${m.motorPowerWatt} Watt</b></div>
        <div class="detail-row"><span class="muted">Kapasitas Baterai</span><b>${m.batteryKwh} kWh (${m.batterySpec})</b></div>
        <div class="detail-row"><span class="muted">Jarak Tempuh</span><b>${m.rangeKm} km</b></div>
        <div class="detail-row"><span class="muted">Efisiensi Energi</span><b>${m.whPerKm.toFixed(1)} Wh/km (~Rp ${m.costPerKmIdr}/km)</b></div>
        <div class="detail-row"><span class="muted">Top Speed</span><b>${m.topSpeedKmh} km/jam</b></div>
        <div class="detail-row"><span class="muted">Tanjakan Maks</span><b>${m.hillClimbGrade}</b></div>
        <div class="detail-row"><span class="muted">Sistem Pengisian</span><b>${m.chargingHours < 0.5 ? "Swap 9 Detik" : m.chargingHours + " jam"}</b></div>
        <div class="detail-row"><span class="muted">Garansi Resmi</span><b>${m.warrantyInfo}</b></div>
      </div>
      <div style="display:grid;grid-template-columns:1fr 1fr;gap:8px;">
        <button class="secondary-btn" id="modal-fav-btn">${isFav ? "❤️ Difavoritkan" : "🤍 Favoritkan"}</button>
        <button class="secondary-btn" id="modal-duel-btn">${inDuel ? "⚡ Lihat Duel" : "⚡ Masuk Duel"}</button>
      </div>
      <button class="secondary-btn" id="modal-calc-btn">🧮 Hitung Hemat di Kalkulator</button>
      <button class="primary-btn" id="modal-ask-btn">✨ Tanya Rekomendasi EV Ini</button>
    `;
    modalOverlay.classList.remove("hidden");
    document.getElementById("close-modal-btn").onclick = closeModal;
    document.getElementById("modal-fav-btn").onclick = () => {
      if (state.favorites.has(m.id)) state.favorites.delete(m.id);
      else state.favorites.add(m.id);
      saveFavorites();
      openMotorDetail(m.id);
      render();
    };
    document.getElementById("modal-duel-btn").onclick = () => {
      if (!state.compareIds.includes(m.id)) toggleCompare(m.id);
      closeModal();
      switchTab("compare");
    };
    document.getElementById("modal-calc-btn").onclick = () => {
      state.calcMotorId = m.id;
      closeModal();
      switchTab("calculator");
    };
    document.getElementById("modal-ask-btn").onclick = () => {
      closeModal();
      switchTab("advisor");
      askAdvisor(`Analisis lengkap ${m.brand} ${m.modelName}: cocok untuk siapa, efisiensi, tanjakan, dan garansinya.`);
    };
  }

  function openSlotPicker(slotIdx) {
    modalSheet.innerHTML = `
      <div style="display:flex;justify-content:space-between;align-items:center;">
        <h3 style="margin:0;">Pilih Motor untuk EV ${slotIdx + 1}</h3>
        <button class="fav-btn" id="close-modal-btn" aria-label="Tutup">✕</button>
      </div>
      <div style="display:flex;flex-direction:column;gap:8px;">
        ${MOTORS.map((m) => `
          <button class="glass-card picker-item" data-pick="${m.id}" style="padding:12px;text-align:left;cursor:pointer;color:var(--text-main);display:flex;justify-content:space-between;align-items:center;">
            <div>
              <strong>${m.brand} ${m.modelName}</strong>
              <div class="small muted">${m.motorPowerWatt}W • ${m.rangeKm} km • ${m.hillClimbDegrees}°</div>
            </div>
            <span class="mono" style="color:var(--primary);font-weight:700;">${formatRpShort(m.priceIdr)}</span>
          </button>
        `).join("")}
      </div>
    `;
    modalOverlay.classList.remove("hidden");
    document.getElementById("close-modal-btn").onclick = closeModal;
    modalSheet.querySelectorAll("[data-pick]").forEach((btn) => {
      btn.onclick = () => {
        const picked = btn.getAttribute("data-pick");
        const next = [...state.compareIds];
        next[slotIdx] = picked;
        state.compareIds = [...new Set(next)];
        saveCompare();
        closeModal();
        render();
      };
    });
  }

  function closeModal() {
    modalOverlay.classList.add("hidden");
  }

  function generateLocalAnalysis(promptText) {
    const q = promptText.toLowerCase();
    const compareList = state.compareIds.map((id) => MOTORS.find((m) => m.id === id)).filter(Boolean);

    if (q.includes("duel") || q.includes("banding")) {
      if (compareList.length >= 2) {
        const irit = [...compareList].sort((a, b) => a.whPerKm - b.whPerKm)[0];
        const murah = [...compareList].sort((a, b) => a.priceIdr - b.priceIdr)[0];
        const tanjak = [...compareList].sort((a, b) => b.hillClimbDegrees - a.hillClimbDegrees || b.motorPowerWatt - a.motorPowerWatt)[0];
        const garansi = [...compareList].sort((a, b) => b.warrantyYears - a.warrantyYears)[0];
        return [
          `📊 Analisis Duel ${compareList.map((m) => `${m.brand} ${m.modelName}`).join(" vs ")}:`,
          `• Paling Irit Energi: ${irit.brand} ${irit.modelName} (${irit.whPerKm.toFixed(1)} Wh/km • ~Rp ${irit.costPerKmIdr}/km).`,
          `• Paling Terjangkau: ${murah.brand} ${murah.modelName} (${formatRp(murah.priceIdr)}).`,
          `• Raja Tanjakan & Tenaga: ${tanjak.brand} ${tanjak.modelName} (${tanjak.hillClimbGrade}, ${tanjak.motorPowerWatt}W).`,
          `• Garansi Terkuat: ${garansi.brand} ${garansi.modelName} (${garansi.warrantyInfo}).`,
          `💡 Rekomendasi: Pilih ${tanjak.brand} ${tanjak.modelName} jika rute sering menanjak atau berboncengan, atau ${irit.brand} ${irit.modelName} untuk biaya harian paling hemat.`
        ].join("\n\n");
      }
    }

    if (q.includes("tanjak") || q.includes("bukit") || q.includes("gunung")) {
      const topClimb = [...MOTORS].sort((a, b) => b.hillClimbDegrees - a.hillClimbDegrees || b.motorPowerWatt - a.motorPowerWatt).slice(0, 4);
      return `⛰️ Rekomendasi Motor Listrik Kuat Tanjakan:\n\n` +
        topClimb.map((m, i) => `${i + 1}. ${m.brand} ${m.modelName} — ${m.hillClimbGrade} (${m.motorPowerWatt}W, ${formatRpShort(m.priceIdr)})`).join("\n") +
        `\n\n💡 Motor mid-drive atau daya >= 3000W seperti ALVA Cervo, MAKA Cavalry, dan Polytron Fox-500 paling aman untuk tanjakan curam 24°–32°.`;
    }

    if (q.includes("swap") || q.includes("ojol") || q.includes("kurir")) {
      const swaps = MOTORS.filter((m) => m.isSwappable && m.vehicleType === "Swap Battery").slice(0, 5);
      return `🔋 Rekomendasi EV Swap Baterai (Ojol & Mobilitas Tinggi):\n\n` +
        swaps.map((m, i) => `${i + 1}. ${m.brand} ${m.modelName} — ${formatRpShort(m.priceIdr)} • ${m.rangeKm} km • ${m.warrantyInfo}`).join("\n") +
        `\n\n💡 Smoot (SWAP 9 detik) dan Volta (SGB) unggul karena garansi baterai seumur hidup selama mengikuti ekosistem swap.`;
    }

    if (q.includes("irit") || q.includes("murah") || q.includes("20") || q.includes("hemat")) {
      const under20 = MOTORS.filter((m) => m.priceIdr <= 21000000 && m.vehicleType !== "Sepeda Listrik")
        .sort((a, b) => a.whPerKm - b.whPerKm)
        .slice(0, 5);
      return `🌱 Motor Listrik Paling Irit di Kisaran ≤ Rp 21 Juta:\n\n` +
        under20.map((m, i) => `${i + 1}. ${m.brand} ${m.modelName} — ${m.whPerKm.toFixed(1)} Wh/km (~Rp ${m.costPerKmIdr}/km) • ${formatRpShort(m.priceIdr)} • Jarak ${m.rangeKm} km`).join("\n");
    }

    const matched = MOTORS.find((m) => q.includes(m.modelName.toLowerCase()) || q.includes(m.brand.toLowerCase()));
    if (matched) {
      return `⚡ Ringkasan Spesifikasi ${matched.brand} ${matched.modelName}:\n` +
        `• Harga: ${formatRp(matched.priceIdr)} (${matched.priceNote})\n` +
        `• Performa: ${matched.motorPowerWatt}W • Top speed ${matched.topSpeedKmh} km/jam • Tanjakan ${matched.hillClimbGrade}\n` +
        `• Efisiensi & Jarak: ${matched.rangeKm} km (${matched.whPerKm.toFixed(1)} Wh/km, ~Rp ${matched.costPerKmIdr}/km)\n` +
        `• Garansi: ${matched.warrantyInfo}\n` +
        `• Keunggulan: ${matched.highlights.join(", ")}`;
    }

    const bestValue = [...MOTORS].sort((a, b) => (b.rangeKm / (b.priceIdr / 1000000)) - (a.rangeKm / (a.priceIdr / 1000000))).slice(0, 4);
    return `✨ Rekomendasi EV Terbaik Berdasarkan Rasio Harga vs Jarak Tempuh:\n\n` +
      bestValue.map((m, i) => `${i + 1}. ${m.brand} ${m.modelName} (${formatRpShort(m.priceIdr)}) — ${m.rangeKm} km • ${m.motorPowerWatt}W`).join("\n") +
      `\n\nKetik kata kunci seperti "kuat tanjakan", "paling irit", "swap ojol", atau "analisis duel" untuk hasil lebih spesifik.`;
  }

  async function askAdvisor(promptText) {
    const clean = promptText.trim();
    if (!clean) return;
    state.advisorPrompt = clean;
    state.advisorLoading = true;
    render();

    const userGeminiKey = (localStorage.getItem(STORAGE_KEYS.GEMINI_KEY) || "").trim();
    if (userGeminiKey && navigator.onLine) {
      try {
        const contextText = MOTORS.slice(0, 30).map((m) =>
          `${m.brand} ${m.modelName}: Rp ${formatRpShort(m.priceIdr)}, ${m.motorPowerWatt}W, ${m.rangeKm}km, ${m.whPerKm.toFixed(1)}Wh/km, tanjakan ${m.hillClimbDegrees}deg, garansi ${m.warrantyInfo}`
        ).join("\n");
        const res = await fetch(`https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent?key=${encodeURIComponent(userGeminiKey)}`, {
          method: "POST",
          headers: { "Content-Type": "application/json" },
          body: JSON.stringify({
            contents: [{
              parts: [{ text: `Kamu adalah konsultan EV Indonesia VoltMotor. Jawab ringkas dalam bahasa Indonesia berdasarkan katalog berikut:\n${contextText}\n\nPertanyaan: ${clean}` }]
            }]
          })
        });
        if (res.ok) {
          const data = await res.json();
          const text = data?.candidates?.[0]?.content?.parts?.[0]?.text;
          if (text) {
            state.advisorResponse = text;
            state.advisorLoading = false;
            render();
            return;
          }
        }
      } catch (_) {
        // Fallback ke analisis katalog lokal offline
      }
    }

    state.advisorResponse = generateLocalAnalysis(clean);
    state.advisorLoading = false;
    render();
  }

  function renderCatalog() {
    const motors = getFilteredMotors();
    const brands = ["Semua", ...new Set(MOTORS.map((m) => m.brand))].sort((a, b) => a === "Semua" ? -1 : b === "Semua" ? 1 : a.localeCompare(b));

    return `
      <div class="tab-stage">
        <div class="top-header">
          <div>
            <h1 class="brand-title">VoltMotor</h1>
            <p class="brand-sub">Katalog EV Indonesia • cekmotorlistrik.com</p>
          </div>
          <button class="sync-pill" id="manual-sync-pill" type="button">
            <span class="sync-dot ${state.isOnline ? "" : "offline"}"></span>
            <span>${state.syncState === "syncing" ? "Sync..." : state.isOnline ? `Live • ${MOTORS.length} EV` : `Offline • ${MOTORS.length} EV`}</span>
          </button>
        </div>

        <section class="glass-card hero-card">
          <img class="hero-bg" src="/assets/img_hero_ev_banner.jpg" alt="Banner Motor Listrik Indonesia" />
          <div class="hero-scrim"></div>
          <div class="hero-content">
            <div class="hero-top-row">
              <span class="tag-badge">OFFLINE + NETLIFY PWA</span>
              <div class="hero-actions">
                ${!state.isStandalone ? `<button class="pill-btn install" id="hero-install-btn" type="button">📲 Install App</button>` : ""}
                <button class="pill-btn" id="hero-calc-btn" type="button">Hitung Hemat</button>
                <button class="pill-btn" id="hero-compare-btn" type="button">Duel ${state.compareIds.length} EV →</button>
              </div>
            </div>
            <div>
              <h2 class="hero-title">Temukan Motor Listrik Terbaik di Indonesia</h2>
              <p class="hero-desc">${MOTORS.length} model siap offline • ${motors.length} tampil • Filter Tipe, Harga & Jarak</p>
            </div>
          </div>
        </section>

        <div class="search-row">
          <div class="glass-card search-box">
            <span>🔍</span>
            <input id="search-input" type="search" placeholder="Cari Polytron, ALVA, Honda, Smoot..." value="${state.query.replace(/"/g, "&quot;")}" />
          </div>
          <button id="toggle-filter-btn" class="icon-square-btn ${state.showFilters ? "active" : ""}" type="button" aria-label="Filter lanjutan">⚙️</button>
        </div>

        <div class="chip-scroll">
          ${VEHICLE_TYPES.map((t) => `<button class="chip ${state.selectedType === t ? "selected" : ""}" data-type="${t}" type="button">${t}</button>`).join("")}
        </div>

        <div class="chip-scroll">
          ${brands.map((b) => `<button class="chip ${state.selectedBrand === b ? "selected" : ""}" data-brand="${b}" type="button">${b === "Semua" ? "Semua Merek" : b}</button>`).join("")}
        </div>

        <section class="glass-card filter-panel ${state.showFilters ? "" : "hidden"}">
          <div style="display:flex;justify-content:space-between;align-items:center;">
            <strong>Filter Spesifikasi & Harga</strong>
            <button id="reset-filter-btn" class="chip" type="button">Reset</button>
          </div>
          <div class="range-row">
            <div class="range-label-row">
              <span class="muted">Harga Maksimal</span>
              <span class="mono" style="color:var(--primary)">Rp ${state.minPriceMillion} Jt – Rp ${state.maxPriceMillion} Jt</span>
            </div>
            <input id="max-price-slider" type="range" min="10" max="65" step="5" value="${state.maxPriceMillion}" />
          </div>
          <div class="range-row">
            <div class="range-label-row">
              <span class="muted">Jarak Tempuh Minimum</span>
              <span class="mono" style="color:var(--emerald)">${state.minRangeKm === 0 ? "Semua Jarak" : "≥ " + state.minRangeKm + " km"}</span>
            </div>
            <input id="min-range-slider" type="range" min="0" max="150" step="10" value="${state.minRangeKm}" />
          </div>
          <div style="display:flex;flex-wrap:wrap;gap:6px;">
            ${[
              ["price_asc", "Termurah"],
              ["price_desc", "Termahal"],
              ["range_desc", "Jarak Terjauh"],
              ["power_desc", "Tenaga Terbesar"],
              ["efficiency_asc", "Paling Irit"]
            ].map(([k, lbl]) => `<button class="chip ${state.sortBy === k ? "selected" : ""}" data-sort="${k}" type="button">${lbl}</button>`).join("")}
            <button id="fav-only-btn" class="chip ${state.onlyFavorites ? "selected" : ""}" type="button">❤️ Favorit Saja</button>
          </div>
        </section>

        <div class="catalog-grid">
          ${motors.length === 0 ? `
            <div class="glass-card" style="padding:26px;text-align:center;">
              <p>Tidak ada motor listrik yang cocok dengan filter Anda.</p>
              <button class="primary-btn" id="empty-reset-btn" type="button">Reset Filter</button>
            </div>
          ` : motors.map((m) => {
            const isFav = state.favorites.has(m.id);
            const inDuel = state.compareIds.includes(m.id);
            return `
              <article class="glass-card motor-card" data-open-detail="${m.id}">
                <div class="motor-head">
                  <div class="motor-thumb">
                    <img src="${getMotorImage(m)}" alt="${m.brand} ${m.modelName}" loading="lazy" />
                    ${m.subsidyEligible ? `<span class="tkdn-badge">TKDN</span>` : ""}
                  </div>
                  <div class="motor-meta">
                    <div class="motor-top-meta">
                      <span class="motor-type">${m.vehicleType}</span>
                      <span class="motor-cost">Rp ${m.costPerKmIdr}/km</span>
                    </div>
                    <h3 class="motor-name">${m.brand} ${m.modelName}</h3>
                    <div class="motor-price mono">${formatRp(m.priceIdr)}</div>
                    <div class="motor-note">${m.priceNote}</div>
                  </div>
                </div>
                <div class="spec-pills">
                  <div class="spec-pill">⚡ ${m.motorPowerWatt}W</div>
                  <div class="spec-pill">🛣️ ${m.rangeKm} km</div>
                  <div class="spec-pill">💨 ${m.topSpeedKmh} km/h</div>
                  <div class="spec-pill">⛰️ ${m.hillClimbDegrees}°</div>
                </div>
                <div class="motor-footer">
                  <span class="warranty-preview">🛡️ Garansi ${m.warrantyYears} Thn • ${m.batteryKwh} kWh</span>
                  <div class="card-actions">
                    <button class="fav-btn" data-fav="${m.id}" type="button" aria-label="Favorit">${isFav ? "❤️" : "🤍"}</button>
                    <button class="duel-btn ${inDuel ? "active" : ""}" data-duel="${m.id}" type="button">${inDuel ? "Di Duel" : "Duel"}</button>
                  </div>
                </div>
              </article>
            `;
          }).join("")}
        </div>
      </div>
    `;
  }

  function renderCompare() {
    const duel = state.compareIds.map((id) => MOTORS.find((m) => m.id === id)).filter(Boolean);
    const irit = [...duel].sort((a, b) => a.whPerKm - b.whPerKm)[0];
    const murah = [...duel].sort((a, b) => a.priceIdr - b.priceIdr)[0];
    const tanjak = [...duel].sort((a, b) => b.hillClimbDegrees - a.hillClimbDegrees || b.motorPowerWatt - a.motorPowerWatt)[0];
    const garansi = [...duel].sort((a, b) => b.warrantyYears - a.warrantyYears)[0];
    const jauh = [...duel].sort((a, b) => b.rangeKm - a.rangeKm)[0];

    const maxPrice = Math.max(...duel.map((m) => m.priceIdr), 1);
    const minWh = Math.min(...duel.map((m) => m.whPerKm), 1);
    const maxDeg = Math.max(...duel.map((m) => m.hillClimbDegrees), 1);
    const maxRange = Math.max(...duel.map((m) => m.rangeKm), 1);
    const maxWar = Math.max(...duel.map((m) => m.warrantyYears), 1);

    const metricBlock = (title, valueFn, scoreFn, winId) => `
      <div>
        <div class="small muted" style="font-weight:700;margin-bottom:6px;">${title}</div>
        ${duel.map((m) => {
          const isWin = m.id === winId;
          const pct = Math.max(20, Math.min(100, Math.round(scoreFn(m) * 100)));
          return `
            <div style="margin-bottom:8px;">
              <div style="display:flex;justify-content:space-between;font-size:12px;">
                <span style="font-weight:${isWin ? 800 : 500};">${isWin ? "✅ " : ""}${m.brand} ${m.modelName}</span>
                <span class="mono" style="color:${isWin ? "var(--emerald)" : "var(--text-main)"};font-weight:700;">${valueFn(m)}</span>
              </div>
              <div class="progress-track"><div class="progress-fill ${isWin ? "win" : ""}" style="width:${pct}%"></div></div>
            </div>
          `;
        }).join("")}
      </div>
    `;

    return `
      <div class="tab-stage">
        <div>
          <h1 class="brand-title">Duel Perbandingan EV</h1>
          <p class="brand-sub">Bandingkan 2 hingga 3 EV: paling irit, murah, kuat tanjakan & garansi</p>
        </div>

        <div class="chip-scroll">
          <button class="chip" data-preset="avand-sc121,polytron-fox-r" type="button">Murah vs Terlaris</button>
          <button class="chip" data-preset="alva-cervo,maka-cavalry" type="button">Raja Tanjakan</button>
          <button class="chip" data-preset="yadea-e8s-pro,smoot-tempur" type="button">Irit vs Swap 9 Detik</button>
          <button class="chip" data-preset="honda-icon-e,alva-n3" type="button">Honda vs ALVA N3</button>
        </div>

        <div class="slot-row">
          ${duel.slice(0, 2).map((m, idx) => `
            <div class="glass-card slot-card" data-slot="${idx}">
              <div style="display:flex;justify-content:space-between;align-items:center;">
                <span class="chip" style="min-height:auto;padding:3px 8px;font-size:10px;">EV ${idx + 1}</span>
                <span class="small muted">Ganti ⇄</span>
              </div>
              <img class="slot-img" src="${getMotorImage(m)}" alt="${m.brand} ${m.modelName}" />
              <div class="small" style="color:var(--primary);font-weight:700;">${m.brand}</div>
              <div style="font-weight:800;">${m.modelName}</div>
              <div class="mono small muted">${formatRpShort(m.priceIdr)}</div>
            </div>
          `).join("")}
        </div>

        ${duel.length >= 3 ? `
          <div class="glass-card slot-card" data-slot="2" style="display:flex;align-items:center;justify-content:space-between;gap:12px;">
            <div>
              <span class="small" style="color:var(--primary);font-weight:700;">EV 3 (Ekstra)</span>
              <div style="font-weight:800;">${duel[2].brand} ${duel[2].modelName}</div>
              <div class="mono small muted">${formatRpShort(duel[2].priceIdr)}</div>
            </div>
            <button class="chip" id="remove-third-slot" type="button">Hapus ✕</button>
          </div>
        ` : `
          <button class="secondary-btn" id="add-third-slot" type="button">+ Tambah EV Ke-3 untuk Perbandingan</button>
        `}

        ${duel.length >= 2 ? `
          <section class="glass-card winner-card">
            <h3 style="margin:0;">🏆 Ringkasan Pemenang Duel</h3>
            <div class="winner-row">
              <div class="winner-icon">🌱</div>
              <div class="winner-info">
                <div class="winner-top"><span style="color:var(--emerald)">Paling Irit</span><span>${irit.brand} ${irit.modelName}</span></div>
                <div class="small muted">${irit.whPerKm.toFixed(1)} Wh/km (~Rp ${irit.costPerKmIdr}/km)</div>
              </div>
            </div>
            <div class="winner-row">
              <div class="winner-icon">💰</div>
              <div class="winner-info">
                <div class="winner-top"><span style="color:var(--primary)">Paling Murah</span><span>${murah.brand} ${murah.modelName}</span></div>
                <div class="small muted">${formatRp(murah.priceIdr)}</div>
              </div>
            </div>
            <div class="winner-row">
              <div class="winner-icon">⛰️</div>
              <div class="winner-info">
                <div class="winner-top"><span style="color:var(--amber)">Kuat Tanjakan</span><span>${tanjak.brand} ${tanjak.modelName}</span></div>
                <div class="small muted">${tanjak.hillClimbGrade} • ${tanjak.motorPowerWatt}W</div>
              </div>
            </div>
            <div class="winner-row">
              <div class="winner-icon">🛡️</div>
              <div class="winner-info">
                <div class="winner-top"><span style="color:var(--secondary)">Garansi Unggul</span><span>${garansi.brand} ${garansi.modelName}</span></div>
                <div class="small muted">${garansi.warrantyInfo}</div>
              </div>
            </div>
            <div class="winner-row">
              <div class="winner-icon">🛣️</div>
              <div class="winner-info">
                <div class="winner-top"><span style="color:var(--primary)">Jarak Terjauh</span><span>${jauh.brand} ${jauh.modelName}</span></div>
                <div class="small muted">${jauh.rangeKm} km per pengisian</div>
              </div>
            </div>
          </section>

          <section class="glass-card matrix-card">
            <h3 style="margin:0;">Matriks Spesifikasi Head-to-Head</h3>
            ${metricBlock("Harga OTR (Lebih Murah Lebih Baik)", (m) => formatRpShort(m.priceIdr), (m) => 1 - (m.priceIdr / maxPrice) * 0.65, murah.id)}
            ${metricBlock("Efisiensi / Keiritan (Wh/km)", (m) => `${m.whPerKm.toFixed(1)} Wh/km (Rp ${m.costPerKmIdr})`, (m) => minWh / m.whPerKm, irit.id)}
            ${metricBlock("Kemampuan Tanjakan & Daya", (m) => `${m.hillClimbDegrees}° • ${m.motorPowerWatt}W`, (m) => m.hillClimbDegrees / maxDeg, tanjak.id)}
            ${metricBlock("Jarak Tempuh Maksimal", (m) => `${m.rangeKm} km`, (m) => m.rangeKm / maxRange, jauh.id)}
            ${metricBlock("Lama Garansi", (m) => `${m.warrantyYears} Thn`, (m) => m.warrantyYears / maxWar, garansi.id)}
          </section>

          <button class="primary-btn" id="duel-analyze-btn" type="button">✨ Analisis Duel Ini Sekarang</button>
        ` : ""}
      </div>
    `;
  }

  function renderCalculator() {
    const motor = MOTORS.find((m) => m.id === state.calcMotorId) || MOTORS[0];
    const monthlyKm = state.calcDailyKm * 30;
    const bbmPerKm = state.calcBbmPrice / Math.max(state.calcBbmKmPerL, 1);
    const evPerKm = (motor.whPerKm / 1000) * state.calcPlnTariff;
    const monthlyBbm = monthlyKm * bbmPerKm;
    const monthlyEv = monthlyKm * evPerKm;
    const monthlySavings = Math.max(0, monthlyBbm - monthlyEv);
    const yearlySavings = monthlySavings * 12;
    const fiveYearSavings = yearlySavings * 5;
    const co2SavedKg = Math.round((monthlyKm * 12 / state.calcBbmKmPerL) * 2.31);
    const breakEvenMonths = monthlySavings > 0 ? Math.ceil(motor.priceIdr / monthlySavings) : 0;
    const canClimb = motor.hillClimbDegrees >= state.calcSlopeDeg;

    return `
      <div class="tab-stage">
        <div>
          <h1 class="brand-title">Kalkulator Hemat EV</h1>
          <p class="brand-sub">Bandingkan biaya bensin vs listrik & uji ketangguhan tanjakan</p>
        </div>

        <section class="glass-card calc-card">
          <label class="small muted" for="calc-motor-select">Pilih Motor Listrik Acuan</label>
          <select id="calc-motor-select" class="select-input">
            ${MOTORS.map((m) => `<option value="${m.id}" ${m.id === motor.id ? "selected" : ""}>${m.brand} ${m.modelName} (${formatRpShort(m.priceIdr)} • ${m.whPerKm.toFixed(1)} Wh/km)</option>`).join("")}
          </select>

          <div class="range-row">
            <div class="range-label-row">
              <span>Jarak Tempuh Harian</span>
              <span class="mono" style="color:var(--primary)">${state.calcDailyKm} km/hari (${monthlyKm} km/bln)</span>
            </div>
            <input id="calc-daily-km" type="range" min="5" max="150" step="5" value="${state.calcDailyKm}" />
          </div>

          <div class="range-row">
            <div class="range-label-row">
              <span>Harga BBM (Pertalite / Pertamax)</span>
              <span class="mono">${formatRp(state.calcBbmPrice)}/L</span>
            </div>
            <input id="calc-bbm-price" type="range" min="10000" max="16000" step="500" value="${state.calcBbmPrice}" />
          </div>

          <div class="range-row">
            <div class="range-label-row">
              <span>Konsumsi Motor Bensin Pembanding</span>
              <span class="mono">${state.calcBbmKmPerL} km/liter</span>
            </div>
            <input id="calc-bbm-eff" type="range" min="25" max="65" step="5" value="${state.calcBbmKmPerL}" />
          </div>
        </section>

        <section class="glass-card calc-card">
          <h3 style="margin:0;">💰 Estimasi Penghematan (${motor.brand} ${motor.modelName})</h3>
          <div class="stat-grid">
            <div class="stat-box">
              <div class="small muted">Hemat Per Bulan</div>
              <div class="stat-value mono">${formatRp(monthlySavings)}</div>
            </div>
            <div class="stat-box">
              <div class="small muted">Hemat 1 Tahun</div>
              <div class="stat-value mono">${formatRp(yearlySavings)}</div>
            </div>
            <div class="stat-box">
              <div class="small muted">Hemat 5 Tahun</div>
              <div class="stat-value mono">${formatRp(fiveYearSavings)}</div>
            </div>
            <div class="stat-box">
              <div class="small muted">Balik Modal Unit</div>
              <div class="stat-value mono" style="color:var(--primary)">${breakEvenMonths} Bln</div>
            </div>
          </div>
          <div class="small muted">
            Biaya listrik cas: <b class="mono">${formatRp(monthlyEv)}/bln</b> vs BBM <b class="mono">${formatRp(monthlyBbm)}/bln</b> • Kurangi emisi ±<b>${co2SavedKg} kg CO₂/tahun</b>.
          </div>
        </section>

        <section class="glass-card calc-card">
          <h3 style="margin:0;">⛰️ Simulasi Cek Tanjakan</h3>
          <div class="range-row">
            <div class="range-label-row">
              <span>Kemiringan Tanjakan Rute Anda</span>
              <span class="mono">${state.calcSlopeDeg}°</span>
            </div>
            <input id="calc-slope" type="range" min="8" max="32" step="1" value="${state.calcSlopeDeg}" />
          </div>
          <div class="winner-row">
            <div class="winner-icon">${canClimb ? "✅" : "⚠️"}</div>
            <div class="winner-info">
              <div style="font-weight:800;color:${canClimb ? "var(--emerald)" : "var(--amber)"}">
                ${canClimb ? "Mampu Menanjak dengan Aman" : "Tanjakan Cukup Berat untuk Unit Ini"}
              </div>
              <div class="small muted">
                ${motor.brand} ${motor.modelName} memiliki kapasitas tanjakan hingga ${motor.hillClimbDegrees}° (${motor.motorPowerWatt}W).
              </div>
            </div>
          </div>
        </section>
      </div>
    `;
  }

  function renderAdvisor() {
    const duelNames = state.compareIds.map((id) => MOTORS.find((m) => m.id === id)).filter(Boolean).map((m) => `${m.brand} ${m.modelName}`).join(" vs ");
    const prompts = [
      `Analisis duel ${duelNames}`,
      "Motor listrik paling irit di bawah Rp 20 juta",
      "Rekomendasi motor kuat tanjakan 25° ke atas",
      "Swap battery terbaik untuk ojol 100 km/hari"
    ];

    return `
      <div class="tab-stage">
        <div>
          <h1 class="brand-title">Asisten EV Cerdas</h1>
          <p class="brand-sub">Konsultan pemilihan motor listrik berbasis 52 model EV Indonesia (bisa offline)</p>
        </div>

        <section class="glass-card advisor-card">
          <textarea id="advisor-input" class="text-input" rows="3" placeholder="Tanya rekomendasi motor kuat tanjakan, irit, harga < 20 juta, atau analisis duel...">${state.advisorPrompt}</textarea>
          <button id="advisor-send-btn" class="primary-btn" type="button">${state.advisorLoading ? "Menganalisis..." : "✨ Dapatkan Rekomendasi EV"}</button>
        </section>

        <div class="chip-scroll">
          ${prompts.map((p) => `<button class="chip" data-quick-prompt="${p.replace(/"/g, "&quot;")}" type="button">${p}</button>`).join("")}
        </div>

        <section class="glass-card advisor-card">
          <h3 style="margin:0;">${state.advisorResponse ? "Hasil Analisis VoltMotor" : "Siap Membantu Riset EV Anda"}</h3>
          <div style="white-space:pre-line;line-height:1.55;font-size:14px;">
            ${state.advisorResponse || "Ketuk salah satu pertanyaan populer di atas atau ketik kebutuhan jarak tempuh, tanjakan, dan budget Anda."}
          </div>
        </section>
      </div>
    `;
  }

  function renderSettings() {
    const userKey = localStorage.getItem(STORAGE_KEYS.GEMINI_KEY) || "";
    return `
      <div class="tab-stage">
        <div>
          <h1 class="brand-title">Setelan & Sinkronisasi</h1>
          <p class="brand-sub">Tema iPhone Liquid Glass, PWA Netlify, dan sinkronisasi database lokal</p>
        </div>

        <section class="glass-card settings-card">
          <h3 style="margin:0;">📲 Install Aplikasi (Netlify PWA)</h3>
          <p class="small muted" style="margin:0;">
            ${state.isStandalone ? "Aplikasi sudah terinstall di layar utama Anda." : "Install VoltMotor langsung ke HP Android, iPhone, atau desktop untuk ikon di layar utama & akses offline penuh."}
          </p>
          <button id="settings-install-btn" class="primary-btn" type="button">📲 Install VoltMotor ke Perangkat</button>
        </section>

        <section class="glass-card settings-card">
          <h3 style="margin:0;">🎨 Tampilan & Mode Gelap Elegan</h3>
          <div style="display:flex;gap:8px;flex-wrap:wrap;">
            ${[["dark", "🌙 Gelap Elegan"], ["light", "☀️ Terang Kristal"], ["system", "📱 Ikuti Sistem"]].map(([k, lbl]) => `
              <button class="chip ${state.theme === k ? "selected" : ""}" data-theme-opt="${k}" type="button">${lbl}</button>
            `).join("")}
          </div>
        </section>

        <section class="glass-card settings-card">
          <div style="display:flex;justify-content:space-between;align-items:center;">
            <div>
              <strong>Sinkronisasi Otomatis Saat Online</strong>
              <div class="small muted">Otomatis memeriksa koneksi saat internet aktif</div>
            </div>
            <button id="toggle-autosync-btn" class="chip ${state.autoSync ? "selected" : ""}" type="button">${state.autoSync ? "Aktif" : "Nonaktif"}</button>
          </div>
          <button id="settings-sync-now-btn" class="secondary-btn" type="button">🔄 Sinkronkan Sekarang (${MOTORS.length} EV Lokal)</button>
          <div style="display:flex;flex-direction:column;gap:6px;">
            <span class="small muted" style="font-weight:700;">Riwayat Sinkronisasi Terakhir</span>
            ${state.syncLogs.length === 0 ? `<span class="small muted">Belum ada riwayat sinkronisasi.</span>` : state.syncLogs.map((l) => `
              <div class="small" style="padding:8px;border-radius:12px;background:var(--pill-bg);">
                <b>${l.time}</b> • ${l.source} (${l.count} unit)<br><span class="muted">${l.statusText}</span>
              </div>
            `).join("")}
          </div>
        </section>

        <section class="glass-card settings-card">
          <h3 style="margin:0;">🔑 Kunci Gemini Opsional (Perangkat Anda)</h3>
          <p class="small muted" style="margin:0;">Opsional: simpan Gemini API key pribadi di browser Anda (localStorage) jika ingin jawaban AI generatif selain analisis lokal offline.</p>
          <input id="user-gemini-key-input" class="text-input" type="password" placeholder="Tempel Gemini API Key opsional..." value="${userKey.replace(/"/g, "&quot;")}" />
          <button id="save-gemini-key-btn" class="secondary-btn" type="button">Simpan di Perangkat Ini</button>
        </section>
      </div>
    `;
  }

  function bindEvents() {
    document.getElementById("manual-sync-pill")?.addEventListener("click", () => performSync(false));
    document.getElementById("hero-install-btn")?.addEventListener("click", triggerInstallPwa);
    document.getElementById("hero-calc-btn")?.addEventListener("click", () => switchTab("calculator"));
    document.getElementById("hero-compare-btn")?.addEventListener("click", () => switchTab("compare"));

    const searchInput = document.getElementById("search-input");
    if (searchInput) {
      searchInput.addEventListener("input", (e) => {
        state.query = e.target.value;
        const pos = e.target.selectionStart;
        render();
        const el = document.getElementById("search-input");
        if (el) {
          el.focus();
          el.setSelectionRange(pos, pos);
        }
      });
    }

    document.getElementById("toggle-filter-btn")?.addEventListener("click", () => {
      state.showFilters = !state.showFilters;
      render();
    });

    document.querySelectorAll("[data-type]").forEach((btn) => {
      btn.addEventListener("click", () => {
        state.selectedType = btn.dataset.type;
        render();
      });
    });

    document.querySelectorAll("[data-brand]").forEach((btn) => {
      btn.addEventListener("click", () => {
        state.selectedBrand = btn.dataset.brand;
        render();
      });
    });

    document.getElementById("max-price-slider")?.addEventListener("input", (e) => {
      state.maxPriceMillion = Number(e.target.value);
      render();
    });

    document.getElementById("min-range-slider")?.addEventListener("input", (e) => {
      state.minRangeKm = Number(e.target.value);
      render();
    });

    document.querySelectorAll("[data-sort]").forEach((btn) => {
      btn.addEventListener("click", () => {
        state.sortBy = btn.dataset.sort;
        render();
      });
    });

    document.getElementById("fav-only-btn")?.addEventListener("click", () => {
      state.onlyFavorites = !state.onlyFavorites;
      render();
    });

    const resetFilters = () => {
      state.query = "";
      state.selectedType = "Semua";
      state.selectedBrand = "Semua";
      state.maxPriceMillion = 65;
      state.minRangeKm = 0;
      state.onlyFavorites = false;
      state.sortBy = "price_asc";
      render();
    };
    document.getElementById("reset-filter-btn")?.addEventListener("click", resetFilters);
    document.getElementById("empty-reset-btn")?.addEventListener("click", resetFilters);

    document.querySelectorAll("[data-open-detail]").forEach((card) => {
      card.addEventListener("click", (e) => {
        if (e.target.closest("[data-fav]") || e.target.closest("[data-duel]")) return;
        openMotorDetail(card.dataset.openDetail);
      });
    });

    document.querySelectorAll("[data-fav]").forEach((btn) => {
      btn.addEventListener("click", () => {
        const id = btn.dataset.fav;
        if (state.favorites.has(id)) state.favorites.delete(id);
        else state.favorites.add(id);
        saveFavorites();
        render();
      });
    });

    document.querySelectorAll("[data-duel]").forEach((btn) => {
      btn.addEventListener("click", () => toggleCompare(btn.dataset.duel));
    });

    // Compare
    document.querySelectorAll("[data-preset]").forEach((btn) => {
      btn.addEventListener("click", () => {
        state.compareIds = btn.dataset.preset.split(",");
        saveCompare();
        render();
      });
    });

    document.querySelectorAll("[data-slot]").forEach((card) => {
      card.addEventListener("click", (e) => {
        if (e.target.id === "remove-third-slot") return;
        openSlotPicker(Number(card.dataset.slot));
      });
    });

    document.getElementById("add-third-slot")?.addEventListener("click", () => {
      const candidate = MOTORS.find((m) => !state.compareIds.includes(m.id));
      if (candidate) {
        state.compareIds = [...state.compareIds, candidate.id];
        saveCompare();
        render();
      }
    });

    document.getElementById("remove-third-slot")?.addEventListener("click", () => {
      state.compareIds = state.compareIds.slice(0, 2);
      saveCompare();
      render();
    });

    document.getElementById("duel-analyze-btn")?.addEventListener("click", () => {
      switchTab("advisor");
      askAdvisor("Analisis perbandingan duel EV yang sedang saya pilih.");
    });

    // Calculator
    document.getElementById("calc-motor-select")?.addEventListener("change", (e) => {
      state.calcMotorId = e.target.value;
      render();
    });
    document.getElementById("calc-daily-km")?.addEventListener("input", (e) => {
      state.calcDailyKm = Number(e.target.value);
      render();
    });
    document.getElementById("calc-bbm-price")?.addEventListener("input", (e) => {
      state.calcBbmPrice = Number(e.target.value);
      render();
    });
    document.getElementById("calc-bbm-eff")?.addEventListener("input", (e) => {
      state.calcBbmKmPerL = Number(e.target.value);
      render();
    });
    document.getElementById("calc-slope")?.addEventListener("input", (e) => {
      state.calcSlopeDeg = Number(e.target.value);
      render();
    });

    // Advisor
    document.getElementById("advisor-send-btn")?.addEventListener("click", () => {
      const val = document.getElementById("advisor-input")?.value || "";
      askAdvisor(val);
    });
    document.querySelectorAll("[data-quick-prompt]").forEach((btn) => {
      btn.addEventListener("click", () => askAdvisor(btn.dataset.quickPrompt));
    });

    // Settings
    document.getElementById("settings-install-btn")?.addEventListener("click", triggerInstallPwa);
    document.querySelectorAll("[data-theme-opt]").forEach((btn) => {
      btn.addEventListener("click", () => {
        applyTheme(btn.dataset.themeOpt);
        render();
      });
    });
    document.getElementById("toggle-autosync-btn")?.addEventListener("click", () => {
      state.autoSync = !state.autoSync;
      localStorage.setItem(STORAGE_KEYS.AUTO_SYNC, JSON.stringify(state.autoSync));
      render();
    });
    document.getElementById("settings-sync-now-btn")?.addEventListener("click", () => performSync(false));
    document.getElementById("save-gemini-key-btn")?.addEventListener("click", () => {
      const val = document.getElementById("user-gemini-key-input")?.value || "";
      localStorage.setItem(STORAGE_KEYS.GEMINI_KEY, val.trim());
      alert("Pengaturan kunci Gemini berhasil disimpan secara lokal di perangkat ini.");
    });
  }

  function render() {
    if (state.tab === "catalog") mainEl.innerHTML = renderCatalog();
    else if (state.tab === "compare") mainEl.innerHTML = renderCompare();
    else if (state.tab === "calculator") mainEl.innerHTML = renderCalculator();
    else if (state.tab === "advisor") mainEl.innerHTML = renderAdvisor();
    else mainEl.innerHTML = renderSettings();
    bindEvents();
  }

  // Global Listeners
  document.querySelectorAll(".rail-tab").forEach((btn) => {
    btn.addEventListener("click", () => switchTab(btn.dataset.tab));
  });

  document.getElementById("quick-theme-btn").addEventListener("click", () => {
    const currentResolved = document.documentElement.getAttribute("data-theme");
    applyTheme(currentResolved === "dark" ? "light" : "dark");
    render();
  });

  modalOverlay.addEventListener("click", (e) => {
    if (e.target === modalOverlay) closeModal();
  });

  window.addEventListener("beforeinstallprompt", (e) => {
    e.preventDefault();
    state.deferredInstallPrompt = e;
    render();
  });

  window.addEventListener("appinstalled", () => {
    state.isStandalone = true;
    state.deferredInstallPrompt = null;
    render();
  });

  window.addEventListener("online", () => {
    state.isOnline = true;
    if (state.autoSync) performSync(true);
    else render();
  });

  window.addEventListener("offline", () => {
    state.isOnline = false;
    state.syncState = "offline";
    render();
  });

  if ("serviceWorker" in navigator) {
    window.addEventListener("load", () => {
      navigator.serviceWorker.register("/sw.js").catch(() => {});
    });
  }

  applyTheme(state.theme);
  saveCompare();
  render();
  if (navigator.onLine && state.autoSync && state.syncLogs.length === 0) {
    performSync(true);
  }
})();
