package com.example.weather.data

import com.example.weather.model.City
import java.util.Locale

enum class Region(val label: String) {
    MARMARA("Marmara"),
    EGE("Ege"),
    AKDENIZ("Akdeniz"),
    IC_ANADOLU("İç Anadolu"),
    KARADENIZ("Karadeniz"),
    DOGU_ANADOLU("Doğu Anadolu"),
    GUNEYDOGU_ANADOLU("Güneydoğu Anadolu"),
}

data class Province(val plate: Int, val name: String, val region: Region)

/** The 81 provinces of Türkiye with their licence plate codes and geographic regions. */
object TurkishProvinces {
    val all: List<Province> = listOf(
        Province(1, "Adana", Region.AKDENIZ),
        Province(2, "Adıyaman", Region.GUNEYDOGU_ANADOLU),
        Province(3, "Afyonkarahisar", Region.EGE),
        Province(4, "Ağrı", Region.DOGU_ANADOLU),
        Province(5, "Amasya", Region.KARADENIZ),
        Province(6, "Ankara", Region.IC_ANADOLU),
        Province(7, "Antalya", Region.AKDENIZ),
        Province(8, "Artvin", Region.KARADENIZ),
        Province(9, "Aydın", Region.EGE),
        Province(10, "Balıkesir", Region.MARMARA),
        Province(11, "Bilecik", Region.MARMARA),
        Province(12, "Bingöl", Region.DOGU_ANADOLU),
        Province(13, "Bitlis", Region.DOGU_ANADOLU),
        Province(14, "Bolu", Region.KARADENIZ),
        Province(15, "Burdur", Region.AKDENIZ),
        Province(16, "Bursa", Region.MARMARA),
        Province(17, "Çanakkale", Region.MARMARA),
        Province(18, "Çankırı", Region.IC_ANADOLU),
        Province(19, "Çorum", Region.KARADENIZ),
        Province(20, "Denizli", Region.EGE),
        Province(21, "Diyarbakır", Region.GUNEYDOGU_ANADOLU),
        Province(22, "Edirne", Region.MARMARA),
        Province(23, "Elazığ", Region.DOGU_ANADOLU),
        Province(24, "Erzincan", Region.DOGU_ANADOLU),
        Province(25, "Erzurum", Region.DOGU_ANADOLU),
        Province(26, "Eskişehir", Region.IC_ANADOLU),
        Province(27, "Gaziantep", Region.GUNEYDOGU_ANADOLU),
        Province(28, "Giresun", Region.KARADENIZ),
        Province(29, "Gümüşhane", Region.KARADENIZ),
        Province(30, "Hakkari", Region.DOGU_ANADOLU),
        Province(31, "Hatay", Region.AKDENIZ),
        Province(32, "Isparta", Region.AKDENIZ),
        Province(33, "Mersin", Region.AKDENIZ),
        Province(34, "İstanbul", Region.MARMARA),
        Province(35, "İzmir", Region.EGE),
        Province(36, "Kars", Region.DOGU_ANADOLU),
        Province(37, "Kastamonu", Region.KARADENIZ),
        Province(38, "Kayseri", Region.IC_ANADOLU),
        Province(39, "Kırklareli", Region.MARMARA),
        Province(40, "Kırşehir", Region.IC_ANADOLU),
        Province(41, "Kocaeli", Region.MARMARA),
        Province(42, "Konya", Region.IC_ANADOLU),
        Province(43, "Kütahya", Region.EGE),
        Province(44, "Malatya", Region.DOGU_ANADOLU),
        Province(45, "Manisa", Region.EGE),
        Province(46, "Kahramanmaraş", Region.AKDENIZ),
        Province(47, "Mardin", Region.GUNEYDOGU_ANADOLU),
        Province(48, "Muğla", Region.EGE),
        Province(49, "Muş", Region.DOGU_ANADOLU),
        Province(50, "Nevşehir", Region.IC_ANADOLU),
        Province(51, "Niğde", Region.IC_ANADOLU),
        Province(52, "Ordu", Region.KARADENIZ),
        Province(53, "Rize", Region.KARADENIZ),
        Province(54, "Sakarya", Region.MARMARA),
        Province(55, "Samsun", Region.KARADENIZ),
        Province(56, "Siirt", Region.GUNEYDOGU_ANADOLU),
        Province(57, "Sinop", Region.KARADENIZ),
        Province(58, "Sivas", Region.IC_ANADOLU),
        Province(59, "Tekirdağ", Region.MARMARA),
        Province(60, "Tokat", Region.KARADENIZ),
        Province(61, "Trabzon", Region.KARADENIZ),
        Province(62, "Tunceli", Region.DOGU_ANADOLU),
        Province(63, "Şanlıurfa", Region.GUNEYDOGU_ANADOLU),
        Province(64, "Uşak", Region.EGE),
        Province(65, "Van", Region.DOGU_ANADOLU),
        Province(66, "Yozgat", Region.IC_ANADOLU),
        Province(67, "Zonguldak", Region.KARADENIZ),
        Province(68, "Aksaray", Region.IC_ANADOLU),
        Province(69, "Bayburt", Region.KARADENIZ),
        Province(70, "Karaman", Region.IC_ANADOLU),
        Province(71, "Kırıkkale", Region.IC_ANADOLU),
        Province(72, "Batman", Region.GUNEYDOGU_ANADOLU),
        Province(73, "Şırnak", Region.GUNEYDOGU_ANADOLU),
        Province(74, "Bartın", Region.KARADENIZ),
        Province(75, "Ardahan", Region.DOGU_ANADOLU),
        Province(76, "Iğdır", Region.DOGU_ANADOLU),
        Province(77, "Yalova", Region.MARMARA),
        Province(78, "Karabük", Region.KARADENIZ),
        Province(79, "Kilis", Region.GUNEYDOGU_ANADOLU),
        Province(80, "Osmaniye", Region.AKDENIZ),
        Province(81, "Düzce", Region.KARADENIZ),
    )

    private val turkish = Locale.forLanguageTag("tr")
    private val byName = all.associateBy { key(it.name) }

    /** Looks up a province by the API's admin1 value, which is sometimes suffixed ("Bursa İli"). */
    fun find(admin1: String?): Province? =
        admin1?.let { byName[key(it.removeSuffix(" İli"))] }

    private fun key(name: String) = name.trim().lowercase(turkish)
}

/** The Turkish province of this city, or null for cities outside Türkiye. */
val City.province: Province?
    get() = if (countryCode == "TR") TurkishProvinces.find(admin1) else null
