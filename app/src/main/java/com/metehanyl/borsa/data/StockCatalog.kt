package com.metehanyl.borsa.data

import com.metehanyl.borsa.data.model.Market
import com.metehanyl.borsa.data.model.StockInfo

/**
 * Uygulamanın izlediği 7 ülke borsasındaki seçilmiş büyük şirket/hisse listesi.
 * Semboller Yahoo Finance sembol biçimindedir (ör. Almanya için ".DE", Türkiye için ".IS").
 */
object StockCatalog {

    val all: List<StockInfo> = buildList {
        // Amerika Birleşik Devletleri (NASDAQ / NYSE)
        add(StockInfo("AAPL", "Apple Inc.", Market.US, "Teknoloji"))
        add(StockInfo("MSFT", "Microsoft Corporation", Market.US, "Teknoloji"))
        add(StockInfo("GOOGL", "Alphabet Inc. (Google)", Market.US, "Teknoloji"))
        add(StockInfo("AMZN", "Amazon.com Inc.", Market.US, "Perakende / Bulut"))
        add(StockInfo("NVDA", "NVIDIA Corporation", Market.US, "Yarı İletken"))
        add(StockInfo("META", "Meta Platforms Inc.", Market.US, "Teknoloji"))
        add(StockInfo("TSLA", "Tesla Inc.", Market.US, "Otomotiv / Enerji"))
        add(StockInfo("JPM", "JPMorgan Chase & Co.", Market.US, "Bankacılık"))
        add(StockInfo("JNJ", "Johnson & Johnson", Market.US, "Sağlık"))
        add(StockInfo("XOM", "Exxon Mobil Corporation", Market.US, "Enerji"))
        add(StockInfo("ARM", "Arm Holdings plc (ADR)", Market.US, "Yarı İletken"))
        add(StockInfo("UMC", "United Microelectronics Corp (ADR)", Market.US, "Yarı İletken"))

        // Almanya (XETRA / Frankfurt)
        add(StockInfo("SAP.DE", "SAP SE", Market.GERMANY, "Yazılım"))
        add(StockInfo("SIE.DE", "Siemens AG", Market.GERMANY, "Sanayi"))
        add(StockInfo("ALV.DE", "Allianz SE", Market.GERMANY, "Sigorta"))
        add(StockInfo("DTE.DE", "Deutsche Telekom AG", Market.GERMANY, "Telekomünikasyon"))
        add(StockInfo("BAS.DE", "BASF SE", Market.GERMANY, "Kimya"))
        add(StockInfo("BMW.DE", "Bayerische Motoren Werke AG", Market.GERMANY, "Otomotiv"))
        add(StockInfo("VOW3.DE", "Volkswagen AG", Market.GERMANY, "Otomotiv"))
        add(StockInfo("MBG.DE", "Mercedes-Benz Group AG", Market.GERMANY, "Otomotiv"))
        add(StockInfo("MUV2.DE", "Munich Re", Market.GERMANY, "Sigorta"))
        add(StockInfo("ADS.DE", "Adidas AG", Market.GERMANY, "Tüketici Ürünleri"))

        // İngiltere (London Stock Exchange)
        add(StockInfo("HSBA.L", "HSBC Holdings plc", Market.UK, "Bankacılık"))
        add(StockInfo("BP.L", "BP plc", Market.UK, "Enerji"))
        add(StockInfo("SHEL.L", "Shell plc", Market.UK, "Enerji"))
        add(StockInfo("GSK.L", "GSK plc", Market.UK, "Sağlık"))
        add(StockInfo("AZN.L", "AstraZeneca plc", Market.UK, "Sağlık"))
        add(StockInfo("ULVR.L", "Unilever plc", Market.UK, "Tüketici Ürünleri"))
        add(StockInfo("RIO.L", "Rio Tinto plc", Market.UK, "Madencilik"))
        add(StockInfo("DGE.L", "Diageo plc", Market.UK, "İçecek"))
        add(StockInfo("VOD.L", "Vodafone Group plc", Market.UK, "Telekomünikasyon"))
        add(StockInfo("BATS.L", "British American Tobacco plc", Market.UK, "Tüketici Ürünleri"))

        // Türkiye (Borsa İstanbul)
        add(StockInfo("THYAO.IS", "Türk Hava Yolları", Market.TURKEY, "Ulaştırma"))
        add(StockInfo("GARAN.IS", "Garanti BBVA", Market.TURKEY, "Bankacılık"))
        add(StockInfo("AKBNK.IS", "Akbank", Market.TURKEY, "Bankacılık"))
        add(StockInfo("ASELS.IS", "Aselsan", Market.TURKEY, "Savunma Sanayi"))
        add(StockInfo("BIMAS.IS", "BİM Birleşik Mağazalar", Market.TURKEY, "Perakende"))
        add(StockInfo("KCHOL.IS", "Koç Holding", Market.TURKEY, "Holding"))
        add(StockInfo("SISE.IS", "Şişecam", Market.TURKEY, "Sanayi"))
        add(StockInfo("EREGL.IS", "Ereğli Demir Çelik", Market.TURKEY, "Demir-Çelik"))
        add(StockInfo("TUPRS.IS", "Tüpraş", Market.TURKEY, "Enerji"))
        add(StockInfo("SAHOL.IS", "Sabancı Holding", Market.TURKEY, "Holding"))

        // Fransa (Euronext Paris)
        add(StockInfo("MC.PA", "LVMH Moët Hennessy Louis Vuitton", Market.FRANCE, "Lüks Tüketim"))
        add(StockInfo("OR.PA", "L'Oréal S.A.", Market.FRANCE, "Tüketici Ürünleri"))
        add(StockInfo("TTE.PA", "TotalEnergies SE", Market.FRANCE, "Enerji"))
        add(StockInfo("SAN.PA", "Sanofi S.A.", Market.FRANCE, "Sağlık"))
        add(StockInfo("AIR.PA", "Airbus SE", Market.FRANCE, "Havacılık"))
        add(StockInfo("BNP.PA", "BNP Paribas S.A.", Market.FRANCE, "Bankacılık"))
        add(StockInfo("AI.PA", "Air Liquide S.A.", Market.FRANCE, "Kimya"))
        add(StockInfo("DG.PA", "Vinci S.A.", Market.FRANCE, "İnşaat"))
        add(StockInfo("SU.PA", "Schneider Electric SE", Market.FRANCE, "Sanayi"))
        add(StockInfo("CS.PA", "AXA S.A.", Market.FRANCE, "Sigorta"))

        // Çin (Shanghai / Shenzhen + ABD'de işlem gören ADR'ler)
        add(StockInfo("600519.SS", "Kweichow Moutai", Market.CHINA, "İçecek"))
        add(StockInfo("601398.SS", "Industrial and Commercial Bank of China", Market.CHINA, "Bankacılık"))
        add(StockInfo("601988.SS", "Bank of China", Market.CHINA, "Bankacılık"))
        add(StockInfo("000858.SZ", "Wuliangye Yibin", Market.CHINA, "İçecek"))
        add(StockInfo("600036.SS", "China Merchants Bank", Market.CHINA, "Bankacılık"))
        add(StockInfo("601318.SS", "Ping An Insurance", Market.CHINA, "Sigorta"))
        add(StockInfo("BABA", "Alibaba Group (ADR)", Market.CHINA, "E-ticaret"))
        add(StockInfo("JD", "JD.com Inc. (ADR)", Market.CHINA, "E-ticaret"))
        add(StockInfo("PDD", "PDD Holdings (ADR)", Market.CHINA, "E-ticaret"))
        add(StockInfo("NIO", "NIO Inc. (ADR)", Market.CHINA, "Otomotiv"))

        // Japonya (Tokyo Stock Exchange)
        add(StockInfo("7203.T", "Toyota Motor Corporation", Market.JAPAN, "Otomotiv"))
        add(StockInfo("6758.T", "Sony Group Corporation", Market.JAPAN, "Elektronik"))
        add(StockInfo("9984.T", "SoftBank Group Corp.", Market.JAPAN, "Telekomünikasyon"))
        add(StockInfo("6501.T", "Hitachi Ltd.", Market.JAPAN, "Sanayi"))
        add(StockInfo("8306.T", "Mitsubishi UFJ Financial Group", Market.JAPAN, "Bankacılık"))
        add(StockInfo("9432.T", "Nippon Telegraph and Telephone", Market.JAPAN, "Telekomünikasyon"))
        add(StockInfo("7267.T", "Honda Motor Co.", Market.JAPAN, "Otomotiv"))
        add(StockInfo("6367.T", "Daikin Industries", Market.JAPAN, "Sanayi"))
        add(StockInfo("8035.T", "Tokyo Electron Ltd.", Market.JAPAN, "Yarı İletken"))
        add(StockInfo("6902.T", "Denso Corporation", Market.JAPAN, "Otomotiv Yan Sanayi"))
    }

    fun byMarket(market: Market): List<StockInfo> = all.filter { it.market == market }
}
