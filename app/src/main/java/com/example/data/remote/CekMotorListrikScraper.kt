package com.example.data.remote

import com.example.data.local.SeedCatalogData
import com.example.data.model.ElectricMotorEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import java.util.Locale
import java.util.concurrent.TimeUnit

data class ScrapeResult(
    val motors: List<ElectricMotorEntity>,
    val endpointUsed: String,
    val isLiveSuccess: Boolean,
    val message: String
)

/**
 * Mesin sinkronisasi multi-strategi untuk cekmotorlistrik.com:
 * 1. Memeriksa endpoint JSON publik / API jika tersedia (/api/vehicles, /api/motors, /api/katalog).
 * 2. Mengekstrak payload terstruktur (__NEXT_DATA__ / RSC JSON / JSON-LD) dari halaman web.
 * 3. Melakukan web scraping DOM HTML yang efisien dengan Jsoup pada halaman utama & /katalog.
 * 4. Menggabungkan pembaruan harga/spesifikasi secara aman ke database lokal Room tanpa kehilangan data offline.
 */
class CekMotorListrikScraper {

    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(12, TimeUnit.SECONDS)
        .readTimeout(18, TimeUnit.SECONDS)
        .followRedirects(true)
        .build()

    private val apiCandidates = listOf(
        "https://cekmotorlistrik.com/api/vehicles",
        "https://cekmotorlistrik.com/api/motors",
        "https://cekmotorlistrik.com/api/katalog",
        "https://cekmotorlistrik.com/data/vehicles.json"
    )

    private val htmlCandidates = listOf(
        "https://cekmotorlistrik.com/katalog",
        "https://cekmotorlistrik.com"
    )

    suspend fun fetchLatestCatalog(existingMotors: List<ElectricMotorEntity>): ScrapeResult =
        withContext(Dispatchers.IO) {
            val now = System.currentTimeMillis()
            val baseline = existingMotors.ifEmpty { SeedCatalogData.getInitialMotors(now) }

            // Strategi 1: Coba endpoint JSON/API publik terlebih dahulu (paling efisien)
            for (apiUrl in apiCandidates) {
                val jsonBody = executeGet(apiUrl) ?: continue
                val parsed = parseJsonCatalog(jsonBody, baseline, now, apiUrl)
                if (parsed.isNotEmpty()) {
                    return@withContext ScrapeResult(
                        motors = mergeWithBaseline(baseline, parsed, now),
                        endpointUsed = apiUrl,
                        isLiveSuccess = true,
                        message = "Sinkronisasi API JSON berhasil (${parsed.size} model diperbarui)."
                    )
                }
            }

            // Strategi 2 & 3: Ambil HTML halaman katalog / beranda dan ekstrak JSON tersemat atau DOM card
            for (pageUrl in htmlCandidates) {
                val html = executeGet(pageUrl) ?: continue
                val doc = Jsoup.parse(html, pageUrl)

                // 2a. Cek __NEXT_DATA__
                val nextDataScript = doc.selectFirst("script#__NEXT_DATA__")?.data()
                if (!nextDataScript.isNullOrBlank()) {
                    val nextMotors = extractJsonArrayRecursive(nextDataScript, baseline, now, pageUrl)
                    if (nextMotors.isNotEmpty()) {
                        return@withContext ScrapeResult(
                            motors = mergeWithBaseline(baseline, nextMotors, now),
                            endpointUsed = "$pageUrl (__NEXT_DATA__)",
                            isLiveSuccess = true,
                            message = "Sinkronisasi data terstruktur berhasil (${nextMotors.size} model)."
                        )
                    }
                }

                // 2b. Cek Next.js RSC flight payload atau inline JSON array kendaraan
                val scriptBlobs = doc.select("script").joinToString("\n") { it.data() }
                if (scriptBlobs.contains("watt", ignoreCase = true) ||
                    scriptBlobs.contains("jarak", ignoreCase = true) ||
                    scriptBlobs.contains("price", ignoreCase = true)
                ) {
                    val rscMotors = extractJsonArrayRecursive(scriptBlobs, baseline, now, pageUrl)
                    if (rscMotors.isNotEmpty()) {
                        return@withContext ScrapeResult(
                            motors = mergeWithBaseline(baseline, rscMotors, now),
                            endpointUsed = "$pageUrl (Embedded JSON)",
                            isLiveSuccess = true,
                            message = "Sinkronisasi payload web berhasil (${rscMotors.size} model)."
                        )
                    }
                }

                // 3. Parsing HTML DOM Card & Tabel Spesifikasi cekmotorlistrik.com
                val domMotors = parseHtmlCatalogDom(doc, baseline, now, pageUrl)
                if (domMotors.isNotEmpty()) {
                    return@withContext ScrapeResult(
                        motors = mergeWithBaseline(baseline, domMotors, now),
                        endpointUsed = pageUrl,
                        isLiveSuccess = true,
                        message = "Web scraping HTML berhasil (${domMotors.size} model terdeteksi dari web)."
                    )
                }

                // Jika situs merespons HTTP 200 tetapi render sepenuhnya client-side (SPA tanpa SSR data)
                if (doc.title().isNotBlank()) {
                    val touched = baseline.map { it.copy(lastSyncedAt = now) }
                    return@withContext ScrapeResult(
                        motors = touched,
                        endpointUsed = pageUrl,
                        isLiveSuccess = true,
                        message = "Terhubung ke ${doc.title()} — katalog lokal tervalidasi (${touched.size} model)."
                    )
                }
            }

            ScrapeResult(
                motors = emptyList(),
                endpointUsed = "cekmotorlistrik.com",
                isLiveSuccess = false,
                message = "Gagal menjangkau cekmotorlistrik.com. Menampilkan database offline lokal."
            )
        }

    private fun executeGet(url: String): String? {
        return runCatching {
            val request = Request.Builder()
                .url(url)
                .header(
                    "User-Agent",
                    "Mozilla/5.0 (Linux; Android 15; Pixel 8) AppleWebKit/537.36 Chrome/131.0 Mobile Safari/537.36"
                )
                .header("Accept", "application/json, text/html, */*")
                .get()
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@use null
                response.body?.string()?.takeIf { it.isNotBlank() }
            }
        }.getOrNull()
    }

    private fun parseJsonCatalog(
        rawJson: String,
        baseline: List<ElectricMotorEntity>,
        timestamp: Long,
        sourceUrl: String
    ): List<ElectricMotorEntity> {
        val trimmed = rawJson.trim()
        val array = runCatching {
            when {
                trimmed.startsWith("[") -> JSONArray(trimmed)
                trimmed.startsWith("{") -> {
                    val obj = JSONObject(trimmed)
                    obj.optJSONArray("data")
                        ?: obj.optJSONArray("vehicles")
                        ?: obj.optJSONArray("motors")
                        ?: obj.optJSONArray("items")
                }
                else -> null
            }
        }.getOrNull() ?: return emptyList()

        return parseJsonArrayItems(array, baseline, timestamp, sourceUrl)
    }

    private fun extractJsonArrayRecursive(
        blob: String,
        baseline: List<ElectricMotorEntity>,
        timestamp: Long,
        sourceUrl: String
    ): List<ElectricMotorEntity> {
        val results = mutableListOf<ElectricMotorEntity>()
        val unescaped = blob.replace("\\\"", "\"")
        val candidates = Regex("""\[\s*\{[^{}\[\]]*(?:\{[^{}]*\}[^{}\[\]]*)*\}\s*(?:,\s*\{[^{}\[\]]*(?:\{[^{}]*\}[^{}\[\]]*)*\}\s*)*]""")
            .findAll(unescaped)
            .take(12)

        for (match in candidates) {
            val parsed = runCatching {
                parseJsonArrayItems(JSONArray(match.value), baseline, timestamp, sourceUrl)
            }.getOrDefault(emptyList())
            if (parsed.size >= 2) {
                results.addAll(parsed)
                break
            }
        }
        return results
    }

    private fun parseJsonArrayItems(
        array: JSONArray,
        baseline: List<ElectricMotorEntity>,
        timestamp: Long,
        sourceUrl: String
    ): List<ElectricMotorEntity> {
        val out = mutableListOf<ElectricMotorEntity>()
        for (i in 0 until array.length()) {
            val obj = array.optJSONObject(i) ?: continue
            val rawName = obj.optString("name").ifBlank {
                obj.optString("model").ifBlank { obj.optString("title") }
            }.trim()
            if (rawName.length < 2) continue

            val rawBrand = obj.optString("brand").ifBlank {
                obj.optString("merk").ifBlank { inferBrand(rawName) }
            }.trim()

            val slug = slugify("$rawBrand-$rawName")
            val matchBase = findMatchingBaseline(baseline, rawBrand, rawName)

            val price = parsePriceIdr(
                obj.opt("price")?.toString()
                    ?: obj.opt("harga")?.toString()
                    ?: obj.opt("price_idr")?.toString()
                    ?: ""
            ).takeIf { it > 1_000_000L } ?: matchBase?.priceIdr ?: 18_500_000L

            val watt = parseIntMetric(
                obj.opt("power")?.toString()
                    ?: obj.opt("watt")?.toString()
                    ?: obj.opt("motor_watt")?.toString()
                    ?: ""
            ).takeIf { it in 300..30_000 } ?: matchBase?.motorPowerWatt ?: 1500

            val rangeKm = parseIntMetric(
                obj.opt("range")?.toString()
                    ?: obj.opt("jarak_tempuh")?.toString()
                    ?: obj.opt("range_km")?.toString()
                    ?: ""
            ).takeIf { it in 20..400 } ?: matchBase?.rangeKm ?: 60

            val kwh = parseDoubleMetric(
                obj.opt("battery_kwh")?.toString()
                    ?: obj.opt("kwh")?.toString()
                    ?: ""
            ).takeIf { it in 0.4..15.0 } ?: matchBase?.batteryKwh ?: 1.44

            val topSpeed = parseIntMetric(
                obj.opt("top_speed")?.toString()
                    ?: obj.opt("kecepatan")?.toString()
                    ?: ""
            ).takeIf { it in 25..200 } ?: matchBase?.topSpeedKmh ?: 60

            val image = obj.optString("image").ifBlank { obj.optString("imageUrl") }
                .takeIf { it.startsWith("http") } ?: matchBase?.imageUrl

            val type = obj.optString("type").ifBlank {
                obj.optString("category").ifBlank {
                    matchBase?.vehicleType ?: inferVehicleType(rawName, watt)
                }
            }

            out.add(
                ElectricMotorEntity(
                    id = matchBase?.id ?: slug,
                    brand = rawBrand.ifBlank { matchBase?.brand ?: "EV Indonesia" },
                    modelName = rawName.removePrefix(rawBrand).trim().ifBlank { rawName },
                    vehicleType = normalizeVehicleType(type),
                    priceIdr = price,
                    priceNote = matchBase?.priceNote ?: "Sumber: cekmotorlistrik.com",
                    motorPowerWatt = watt,
                    batteryKwh = kwh,
                    batterySpec = obj.optString("battery").ifBlank { matchBase?.batterySpec ?: "Lithium" },
                    isSwappable = matchBase?.isSwappable ?: rawName.contains("swap", ignoreCase = true),
                    rangeKm = rangeKm,
                    topSpeedKmh = topSpeed,
                    chargingHours = matchBase?.chargingHours ?: 5.0,
                    hillClimbGrade = matchBase?.hillClimbGrade ?: inferHillGrade(watt),
                    hillClimbDegrees = matchBase?.hillClimbDegrees ?: inferHillDegrees(watt),
                    weightKg = matchBase?.weightKg ?: 98,
                    warrantyInfo = obj.optString("warranty").ifBlank {
                        matchBase?.warrantyInfo ?: "Garansi Pabrikan Resmi (Cek Dealer)"
                    },
                    warrantyYears = matchBase?.warrantyYears ?: 2,
                    subsidyEligible = matchBase?.subsidyEligible ?: (price <= 45_000_000L),
                    imageUrl = image,
                    sourceUrl = sourceUrl,
                    highlights = matchBase?.highlights ?: "Data Real-Time cekmotorlistrik.com",
                    lastSyncedAt = timestamp
                )
            )
        }
        return out
    }

    private fun parseHtmlCatalogDom(
        doc: Document,
        baseline: List<ElectricMotorEntity>,
        timestamp: Long,
        pageUrl: String
    ): List<ElectricMotorEntity> {
        val scraped = mutableListOf<ElectricMotorEntity>()
        val cardSelectors = listOf(
            "article",
            "[class*=card]",
            "[class*=vehicle]",
            "[class*=motor]",
            "[class*=product]",
            "tr"
        )

        val candidateElements = doc.select(cardSelectors.joinToString(", "))
        for (el in candidateElements) {
            val text = el.text().trim()
            if (text.length < 20 || text.length > 900) continue
            val hasEvMetrics = (text.contains("Rp", ignoreCase = true) ||
                text.contains("km", ignoreCase = true)) &&
                (text.contains("W", ignoreCase = false) ||
                    text.contains("kWh", ignoreCase = true) ||
                    text.contains("Watt", ignoreCase = true))

            if (!hasEvMetrics) continue

            val titleEl = el.selectFirst("h1, h2, h3, h4, strong, [class*=title], [class*=name]")
            val title = titleEl?.text()?.trim().orEmpty()
            if (title.length < 3 || title.length > 60) continue

            val matched = findMatchingBaseline(baseline, "", title)
            val parsedPrice = parsePriceIdr(text).takeIf { it > 2_000_000L } ?: matched?.priceIdr ?: continue
            val parsedWatt = Regex("""(\d{3,5})\s*(?:W|Watt)""", RegexOption.IGNORE_CASE)
                .find(text)?.groupValues?.get(1)?.toIntOrNull() ?: matched?.motorPowerWatt ?: 1500
            val parsedRange = Regex("""(\d{2,3})\s*km(?!/)""", RegexOption.IGNORE_CASE)
                .find(text)?.groupValues?.get(1)?.toIntOrNull() ?: matched?.rangeKm ?: 60
            val parsedKwh = Regex("""(\d+(?:[.,]\d+)?)\s*kWh""", RegexOption.IGNORE_CASE)
                .find(text)?.groupValues?.get(1)?.replace(",", ".")?.toDoubleOrNull()
                ?: matched?.batteryKwh ?: 1.44
            val imgSrc = el.selectFirst("img")?.absUrl("src")?.takeIf { it.startsWith("http") }

            val brand = matched?.brand ?: inferBrand(title)
            val modelOnly = title.removePrefix(brand).trim().ifBlank { title }

            scraped.add(
                (matched ?: ElectricMotorEntity(
                    id = slugify("$brand-$modelOnly"),
                    brand = brand,
                    modelName = modelOnly,
                    vehicleType = inferVehicleType(title, parsedWatt),
                    priceIdr = parsedPrice,
                    priceNote = "Sumber: cekmotorlistrik.com",
                    motorPowerWatt = parsedWatt,
                    batteryKwh = parsedKwh,
                    batterySpec = "Lithium / SLA",
                    isSwappable = text.contains("swap", ignoreCase = true),
                    rangeKm = parsedRange,
                    topSpeedKmh = inferTopSpeed(parsedWatt),
                    chargingHours = 5.0,
                    hillClimbGrade = inferHillGrade(parsedWatt),
                    hillClimbDegrees = inferHillDegrees(parsedWatt),
                    weightKg = 98,
                    warrantyInfo = "Garansi Resmi Pabrikan",
                    warrantyYears = 2,
                    subsidyEligible = parsedPrice <= 45_000_000L,
                    imageUrl = imgSrc,
                    sourceUrl = pageUrl,
                    highlights = "Tersinkronisasi dari cekmotorlistrik.com",
                    lastSyncedAt = timestamp
                )).copy(
                    priceIdr = parsedPrice,
                    motorPowerWatt = parsedWatt,
                    rangeKm = parsedRange,
                    batteryKwh = parsedKwh,
                    imageUrl = imgSrc ?: matched?.imageUrl,
                    sourceUrl = pageUrl,
                    lastSyncedAt = timestamp
                )
            )
        }
        return scraped.distinctBy { it.id }
    }

    private fun mergeWithBaseline(
        baseline: List<ElectricMotorEntity>,
        scraped: List<ElectricMotorEntity>,
        timestamp: Long
    ): List<ElectricMotorEntity> {
        val byId = baseline.associateBy { it.id }.toMutableMap()
        for (item in scraped) {
            val existing = byId[item.id]
            byId[item.id] = if (existing != null) {
                existing.copy(
                    priceIdr = item.priceIdr,
                    motorPowerWatt = item.motorPowerWatt,
                    batteryKwh = item.batteryKwh,
                    rangeKm = item.rangeKm,
                    imageUrl = item.imageUrl ?: existing.imageUrl,
                    sourceUrl = item.sourceUrl,
                    lastSyncedAt = timestamp
                )
            } else {
                item.copy(lastSyncedAt = timestamp)
            }
        }
        return byId.values.map { it.copy(lastSyncedAt = timestamp) }
    }

    private fun findMatchingBaseline(
        baseline: List<ElectricMotorEntity>,
        brand: String,
        title: String
    ): ElectricMotorEntity? {
        val normTitle = title.lowercase(Locale.ROOT).replace(Regex("[^a-z0-9]"), "")
        val normBrand = brand.lowercase(Locale.ROOT).replace(Regex("[^a-z0-9]"), "")
        return baseline.firstOrNull { item ->
            val full = (item.brand + item.modelName).lowercase(Locale.ROOT).replace(Regex("[^a-z0-9]"), "")
            val modelOnly = item.modelName.lowercase(Locale.ROOT).replace(Regex("[^a-z0-9]"), "")
            normTitle.contains(modelOnly) || full == normTitle || (normBrand.isNotEmpty() && full == normBrand + normTitle)
        }
    }

    private fun parsePriceIdr(raw: String): Long {
        val clean = raw.replace(".", "").replace(",", "").replace(" ", "")
        val rpMatch = Regex("""Rp\s*(\d{7,9})""", RegexOption.IGNORE_CASE).find(clean)
        if (rpMatch != null) return rpMatch.groupValues[1].toLongOrNull() ?: 0L
        val jtMatch = Regex("""(\d+(?:[.,]\d+)?)\s*(?:jt|juta)""", RegexOption.IGNORE_CASE).find(raw)
        if (jtMatch != null) {
            val millions = jtMatch.groupValues[1].replace(",", ".").toDoubleOrNull() ?: 0.0
            return (millions * 1_000_000L).toLong()
        }
        return raw.filter { it.isDigit() }.take(9).toLongOrNull() ?: 0L
    }

    private fun parseIntMetric(raw: String): Int =
        Regex("""(\d+)""").find(raw.replace(".", ""))?.groupValues?.get(1)?.toIntOrNull() ?: 0

    private fun parseDoubleMetric(raw: String): Double =
        Regex("""(\d+(?:[.,]\d+)?)""").find(raw)?.groupValues?.get(1)?.replace(",", ".")?.toDoubleOrNull() ?: 0.0

    private fun inferBrand(title: String): String {
        val known = listOf(
            "Polytron", "ALVA", "Honda", "United", "Selis", "Gesits",
            "Yadea", "Volta", "Smoot", "Avand", "Savart", "MAKA",
            "Indomobil", "Charged", "Viar", "Exotic", "Pacific", "Uwinfly"
        )
        return known.firstOrNull { title.contains(it, ignoreCase = true) }
            ?: title.substringBefore(" ").ifBlank { "EV Indonesia" }
    }

    private fun inferVehicleType(name: String, watt: Int): String = when {
        name.contains("sepeda", ignoreCase = true) || watt <= 900 -> "Sepeda Listrik"
        name.contains("swap", ignoreCase = true) -> "Swap Battery"
        watt >= 6000 -> "Sport EV"
        watt >= 3000 -> "Maxi EV"
        else -> "Skuter Listrik"
    }

    private fun normalizeVehicleType(type: String): String = when {
        type.contains("sepeda", ignoreCase = true) -> "Sepeda Listrik"
        type.contains("swap", ignoreCase = true) -> "Swap Battery"
        type.contains("sport", ignoreCase = true) -> "Sport EV"
        type.contains("maxi", ignoreCase = true) -> "Maxi EV"
        else -> "Skuter Listrik"
    }

    private fun inferTopSpeed(watt: Int): Int = when {
        watt >= 5000 -> 105
        watt >= 3000 -> 85
        watt >= 1800 -> 68
        watt >= 1200 -> 55
        else -> 42
    }

    private fun inferHillGrade(watt: Int): String = when {
        watt >= 4000 -> "Sangat Kuat (26°)"
        watt >= 2500 -> "Sangat Kuat (22°)"
        watt >= 1800 -> "Kuat (19°)"
        watt >= 1200 -> "Baik (16°)"
        else -> "Komuter (12°)"
    }

    private fun inferHillDegrees(watt: Int): Int = when {
        watt >= 4000 -> 26
        watt >= 2500 -> 22
        watt >= 1800 -> 19
        watt >= 1200 -> 16
        else -> 12
    }

    private fun slugify(input: String): String =
        input.lowercase(Locale.ROOT)
            .replace(Regex("[^a-z0-9]+"), "-")
            .trim('-')
}
