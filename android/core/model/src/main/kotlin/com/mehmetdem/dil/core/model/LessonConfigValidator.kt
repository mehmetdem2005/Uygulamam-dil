package com.mehmetdem.dil.core.model

data class ValidationIssue(
    val field: String,
    val message: String,
)

object LessonConfigValidator {
    fun validate(config: LessonSessionConfig): List<ValidationIssue> = buildList {
        if (config.source.locator.isBlank()) {
            add(ValidationIssue("source", "Bir YouTube bağlantısı veya PDF seçilmelidir."))
        }

        when (config.source.kind) {
            SourceKind.YOUTUBE -> {
                if (YouTubeVideoIdParser.parse(config.source.locator) == null) {
                    add(ValidationIssue("source", "Geçerli bir YouTube video bağlantısı girilmelidir."))
                }
                if (config.source.range !is ContentRange.Time) {
                    add(ValidationIssue("range", "YouTube kaynağı zaman aralığı kullanmalıdır."))
                }
            }

            SourceKind.PDF -> {
                if (config.source.range !is ContentRange.Pages) {
                    add(ValidationIssue("range", "PDF kaynağı sayfa aralığı kullanmalıdır."))
                }
            }
        }

        val format = config.format
        if (format.title.isBlank()) {
            add(ValidationIssue("title", "Ders adı boş bırakılamaz."))
        }
        if (format.instruction.trim().length < 10) {
            add(ValidationIssue("instruction", "Öğretim talimatı en az 10 karakter olmalıdır."))
        }
        if (format.fields.size !in 1..8) {
            add(ValidationIssue("fields", "Formatta 1 ile 8 arasında kart alanı bulunmalıdır."))
        }
        if (format.fields.map { it.key }.distinct().size != format.fields.size) {
            add(ValidationIssue("fields", "Kart alanlarının anahtarları benzersiz olmalıdır."))
        }
        if (format.fields.map { it.position }.distinct().size != format.fields.size) {
            add(ValidationIssue("fields", "Kart alanlarının sıraları benzersiz olmalıdır."))
        }
        if (format.totalBlockCount !in 1..100) {
            add(ValidationIssue("totalBlockCount", "Kart sayısı 1 ile 100 arasında olmalıdır."))
        }
        if (format.blocksPerRequest !in 1..10) {
            add(ValidationIssue("blocksPerRequest", "İstek başına kart sayısı 1 ile 10 arasında olmalıdır."))
        }
        if (format.blocksPerRequest > format.totalBlockCount) {
            add(ValidationIssue("blocksPerRequest", "İstek başına kart sayısı toplam kart sayısını aşamaz."))
        }
        if (format.requestIntervalSeconds !in 2..120) {
            add(ValidationIssue("requestIntervalSeconds", "İstek aralığı 2 ile 120 saniye arasında olmalıdır."))
        }
        if (format.cardWidthFraction !in 0.72f..1f) {
            add(ValidationIssue("cardWidthFraction", "Kart genişliği %72 ile %100 arasında olmalıdır."))
        }
        if (format.playback.continueWhenScreenOff && !format.playback.ttsEnabled) {
            add(ValidationIssue("continueWhenScreenOff", "Ekran kapalı ders için sesli okuma açık olmalıdır."))
        }
    }
}
