# Küresel Borsa

Yedi ülkenin borsasını tek uygulamada takip eden, algoritmik teknik analiz ile
alım/satım fırsatlarını skorlayıp sıralayan Android uygulaması.

## Kapsanan piyasalar

| Ülke | Borsa | Örnek semboller |
|---|---|---|
| 🇺🇸 ABD | NASDAQ / NYSE | AAPL, MSFT, NVDA, TSLA |
| 🇩🇪 Almanya | XETRA (Frankfurt) | SAP.DE, SIE.DE, BMW.DE |
| 🇬🇧 İngiltere | London Stock Exchange | HSBA.L, BP.L, AZN.L |
| 🇹🇷 Türkiye | Borsa İstanbul | THYAO.IS, GARAN.IS, ASELS.IS |
| 🇫🇷 Fransa | Euronext Paris | MC.PA, OR.PA, TTE.PA |
| 🇨🇳 Çin | Shanghai / Shenzhen + ADR | 600519.SS, BABA, JD |
| 🇯🇵 Japonya | Tokyo Stock Exchange | 7203.T, 6758.T, 9984.T |

Toplamda ~465 şirket/kağıt/ETF kapsanır. Tam liste için
`app/src/main/java/com/metehanyl/borsa/data/StockCatalog.kt`. Listede olmayan
bir kağıt varsa Portföyüm ekranından "Sembolü elle girin" ile ekleyebilirsiniz;
uygulama o sembolü de otomatik izlemeye/analiz etmeye başlar.

## Veri kaynağı

Fiyat ve geçmiş grafik verisi, uygulamanın kendisi tarafından **cihazınızdan
doğrudan** Yahoo Finance'in genel (anahtarsız) `chart` uç noktasından
çekilir. Bu nedenle veri, bu depoyu derleyen ortamdan değil, uygulamayı
çalıştırdığınız telefonun internetinden gelir.

## Analiz motoru — nasıl çalışır?

`AnalysisEngine`, her sembol için son 1 yıllık günlük kapanış verisinden şu
göstergeleri hesaplar ve ağırlıklandırarak **-100 ile +100 arasında bir
"fırsat skoru"** üretir:

- **Trend** (30 puan): Fiyatın 50/200 günlük ortalamalara göre konumu (Golden/Death Cross)
- **RSI(14)** (25 puan): Aşırı alım/aşırı satım bölgesi
- **MACD(12,26,9)** (20 puan): Kısa vadeli momentum yönü
- **3 aylık getiri** (15 puan): Orta vadeli momentum
- **52 haftalık aralıktaki konum** (10 puan): Dip/zirveye yakınlık

Skor eşiklerine göre: **Güçlü Al ≥ 50**, **Al ≥ 20**, **Nötr** (-19..19),
**Sat ≤ -20**, **Güçlü Sat ≤ -50**.

Bu, canlı bir yapay zeka modeline (ör. bir LLM) her açılışta çağrı yapmaz —
tamamen kural tabanlı, şeffaf ve tekrarlanabilir bir hesaplamadır. Uygulama
içinde her hisse/fon detayında "Neden bu değerlendirme?" bölümünde skorun
gerekçesi madde madde açıklanır.

**Yatırım tavsiyesi değildir.** Skorlar eğitim/bilgilendirme amaçlıdır;
yatırım kararlarını kendi araştırmanıza ve risk toleransınıza göre verin.

## Fırsat sıralaması

Ana ekranda tüm hisseler, varsayılan olarak fırsat skoruna göre **düşükten
yükseğe** sıralanır; sağ üstteki sıralama simgesiyle yüksekten düşüğe de
çevrilebilir. Ülke sekmeleriyle tek bir piyasaya odaklanabilir, arama
kutusuyla sembol/şirket adına göre filtreleyebilirsiniz.

## APK nasıl üretiliyor?

Bu depo bir Android Studio (Kotlin + Jetpack Compose) projesidir.
`.github/workflows/build-apk.yml` her push'ta GitHub Actions üzerinde
otomatik olarak derlenir ve üretilen `.apk` dosyaları hem workflow'un
**Artifacts** bölümünde hem de depoya eklenen bir **Release**'de
`KureselBorsa-release.apk` adıyla yayınlanır.

Kendi bilgisayarınızda derlemek isterseniz:

```bash
./gradlew :app:assembleRelease
# çıktı: app/build/outputs/apk/release/app-release.apk
```

APK, Play Store dışından kurulacağı için telefonda **Ayarlar > Güvenlik >
Bilinmeyen kaynaklardan yükleme** iznini açmanız gerekir.
