package com.example.hoode_app.data.model

data class DuaItem(
    val id: String,
    val title: String,
    val category: String,
    val arabic: String,
    val transliteration: String,
    val translation: String,
    val reference: String
)

object EssentialDuas {
    val list = listOf(
        DuaItem(
            id = "dua_before_eating",
            title = "Dua Before Eating",
            category = "Daily Habits • کھانے سے پہلے کی دعا",
            arabic = "بِسْمِ اللَّهِ وَعَلَى بَرَكَةِ اللَّهِ",
            transliteration = "Bismillahi wa 'ala barakatillah",
            translation = "In the name of Allah and upon the blessings of Allah.",
            reference = "Abu Dawud & Al-Hakim"
        ),
        DuaItem(
            id = "dua_after_eating",
            title = "Dua After Eating",
            category = "Daily Habits • کھانے کے بعد کی دعا",
            arabic = "الْحَمْدُ لِلَّهِ الَّذِي أَطْعَمَنَا وَسَقَانَا وَجَعَلَنَا مُسْلِمِينَ",
            transliteration = "Alhamdu lillahilladhi at'amana wa saqana wa ja'alana Muslimeen",
            translation = "Praise be to Allah Who has fed us and given us drink, and made us Muslims.",
            reference = "Sunan Abi Dawud (3850), Tirmidhi"
        ),
        DuaItem(
            id = "dua_janaza_adult",
            title = "Janaza Namaz Dua (Adult)",
            category = "Funeral Prayer • نمازِ جنازہ (بالغ)",
            arabic = "اللَّهُمَّ اغْفِرْ لِحَيِّنَا وَمَيِّتِنَا وَشَاهِدِنَا وَغَائِبِنَا وَصَغِيرِنَا وَكَبِيرِنَا وَذَكَرِنَا وَأُنْثَانَا ۖ اللَّهُمَّ مَنْ أَحْيَيْتَهُ مِنَّا فَأَحْيِهِ عَلَى الْإِسْلَامِ وَمَنْ تَوَفَّيْتَهُ مِنَّا فَتَوَفَّهُ عَلَى الْإِيمَانِ ۖ اللَّهُمَّ لَا تَحْرِمْنَا أَجْرَهُ وَلَا تُضِلَّنَا بَعْدَهُ",
            transliteration = "Allahummaghfir lihayyina wa mayyitina, wa shahidina wa gha'ibina, wa saghirina wa kabirina, wa dhakarina wa unthana. Allahumma man ahyaytahu minna fa-ahyihi 'alal-Islam, wa man tawaffaytahu minna fatawaffahu 'alal-Iman. Allahumma la tahrimna ajrahu wa la tudillana ba'dah.",
            translation = "O Allah, forgive our living and our dead, those present and those absent, our young and our old, our males and our females. O Allah, whomsoever You keep alive among us, keep him alive upon Islam, and whomsoever You cause to die, cause him to die upon Faith. O Allah, do not deprive us of his reward, and do not let us go astray after him.",
            reference = "Sunan Abi Dawud, Ibn Majah, Tirmidhi"
        ),
        DuaItem(
            id = "dua_janaza_child",
            title = "Janaza Namaz Dua (Child / Non-Adult)",
            category = "Funeral Prayer • نمازِ جنازہ (نابالغ)",
            arabic = "اللَّهُمَّ اجْعَلْهُ لَنَا فَرَطًا وَاجْعَلْهُ لَنَا أَجْرًا وَذُخْرًا وَاجْعَلْهُ لَنَا شَافِعًا وَمُشَفَّعًا",
            transliteration = "Allahummaj'alhu lana faratan wa-j'alhu lana ajran wa dhukhran, wa-j'alhu lana shafi'an wa mushaffa'a.",
            translation = "O Allah, make him/her for us a predecessor, and make him/her for us a reward and a treasure, and make him/her an intercessor and one whose intercession is accepted.",
            reference = "Sahih al-Bukhari (Ta'liq), Al-Sunan al-Kubra"
        ),
        DuaItem(
            id = "ayatul_kursi",
            title = "Ayat al-Kursi (The Throne Verse)",
            category = "Supreme Protection • آیت الکرسی",
            arabic = "اللَّهُ لَا إِلَٰهَ إِلَّا هُوَ الْحَيُّ الْقَيُّومُ ۚ لَا تَأْخُذُهُ سِنَةٌ وَلَا نَوْمٌ ۚ لَّهُ مَا فِي السَّمَاوَاتِ وَمَا فِي الْأَرْضِ ۗ مَن ذَا الَّذِي يَشْفَعُ عِندَهُ إِلَّا بِإِذْنِهِ ۚ يَعْلَمُ مَا بَيْنَ أَيْدِيهِمْ وَمَا خَلْفَهُمْ ۖ وَلَا يُحِيطُونَ بِشَيْءٍ مِّنْ عِلْمِهِ إِلَّا بِمَا شَاءَ ۚ وَسِعَ كُرْسِيُّهُ السَّمَاوَاتِ وَالْأَرْضَ ۖ وَلَا يَئُودُهُ حِفْظُهُمَا ۚ وَهُوَ الْعَلِيُّ الْعَظِيمُ",
            transliteration = "Allahu la ilaha illa huwal-Hayyul-Qayyum. La ta'khudhuhu sinatuw-wa la nawm. Lahu ma fis-samawati wa ma fil-ard. Man dhal-ladhi yashfa'u 'indahu illa bi-idhnih. Ya'lamu ma bayna aydihim wa ma khalfahum, wa la yuheetoona bi-shay'im-min 'ilmihi illa bima sha'a. Wasi'a kursiyyuhus-samawati wal-ard, wa la ya'uduhu hifdhuhuma, wa huwal-'Aliyyul-'Adheem.",
            translation = "Allah! There is no deity except Him, the Ever-Living, the Sustainer of all existence. Neither drowsiness overtakes Him nor sleep. To Him belongs whatever is in the heavens and whatever is on the earth. Who is it that can intercede with Him except by His permission? He knows what is before them and what will be after them, and they encompass not a thing of His knowledge except for what He wills. His Kursi extends over the heavens and the earth, and their preservation tires Him not. And He is the Most High, the Most Great.",
            reference = "Surah Al-Baqarah (2:255)"
        ),
        DuaItem(
            id = "dua_parents",
            title = "Dua for Parents",
            category = "Family & Gratitude • والدین کے لیے دعا",
            arabic = "رَّبِّ ارْحَمْهُمَا كَمَا رَبَّيَانِي صَغِيرًا",
            transliteration = "Rabbir-hamhuma kama rabbayani sagheera.",
            translation = "My Lord, have mercy upon them as they brought me up when I was small.",
            reference = "Surah Al-Isra (17:24)"
        ),
        DuaItem(
            id = "dua_before_sleep",
            title = "Dua Before Sleeping",
            category = "Night Routine • سونے سے پہلے کی دعا",
            arabic = "اللَّهُمَّ بِاسْمِكَ أَمُوتُ وَأَحْيَا",
            transliteration = "Allahumma bismika amootu wa ahya.",
            translation = "O Allah, in Your Name I die and I live.",
            reference = "Sahih al-Bukhari (6324)"
        ),
        DuaItem(
            id = "dua_waking_up",
            title = "Dua Upon Waking Up",
            category = "Morning Sunnah • نیند سے بیدار ہونے پر",
            arabic = "الْحَمْدُ لِلَّهِ الَّذِي أَحْيَانَا بَعْدَ مَا أَمَاتَنَا وَإِلَيْهِ النُّشُورُ",
            transliteration = "Alhamdu lillahilladhi ahyana ba'da ma amatana wa ilayhin-nushoor.",
            translation = "All praise belongs to Allah, Who gave us life after having caused us to die, and unto Him is the resurrection.",
            reference = "Sahih al-Bukhari (6312)"
        ),
        DuaItem(
            id = "dua_entering_mosque",
            title = "Dua Entering the Mosque",
            category = "Mosque Etiquette • مسجد میں داخل ہونے کی دعا",
            arabic = "اللَّهُمَّ افْتَحْ لِي أَبْوَابَ رَحْمَتِكَ",
            transliteration = "Allahummaftah li abwaba rahmatik.",
            translation = "O Allah, open for me the gates of Your mercy.",
            reference = "Sahih Muslim (713)"
        ),
        DuaItem(
            id = "dua_leaving_mosque",
            title = "Dua Leaving the Mosque",
            category = "Mosque Etiquette • مسجد سے نکلنے کی دعا",
            arabic = "اللَّهُمَّ إِنِّي أَسْأَلُكَ مِنْ فَضْلِكَ",
            transliteration = "Allahumma inni as'aluka min fadlik.",
            translation = "O Allah, I ask You from Your bounty.",
            reference = "Sahih Muslim (713)"
        ),
        DuaItem(
            id = "dua_distress_yunus",
            title = "Dua in Distress & Difficulty (Ayat-e-Kareema)",
            category = "Relief from Hardship • پریشانی اور نجات کی دعا",
            arabic = "لَّا إِلَٰهَ إِلَّا أَنتَ سُبْحَانَكَ إِنِّي كُنتُ مِنَ الظَّالِمِينَ",
            transliteration = "La ilaha illa anta subhanaka inni kuntu minaz-zalimeen.",
            translation = "There is no deity except You; exalted are You. Indeed, I have been of the wrongdoers.",
            reference = "Surah Al-Anbiya (21:87) • Sunan at-Tirmidhi"
        ),
        DuaItem(
            id = "sayyidul_istighfar",
            title = "Sayyidul Istighfar (Chief of Forgiveness)",
            category = "Master Du'a • سید الاستغفار",
            arabic = "اللَّهُمَّ أَنْتَ رَبِّي لَا إِلَهَ إِلَّا أَنْتَ ۖ خَلَقْتَنِي وَأَنَا عَبْدُكَ ۖ وَأَنَا عَلَى عَهْدِكَ وَوَعْدِكَ مَا اسْتَطَعْتُ ۖ أَعُوذُ بِكَ مِنْ شَرِّ مَا صَنَعْتُ ۖ أَبُوءُ لَكَ بِنِعْمَتِكَ عَلَيَّ وَأَبُوءُ بِذَنْبِي فَاغْفِرْ لِي فَإِنَّهُ لَا يَغْفِرُ الذُّنُوبَ إِلَّا أَنْتَ",
            transliteration = "Allahumma Anta Rabbi la ilaha illa Anta, khalaqtani wa ana 'abduka, wa ana 'ala 'ahdika wa wa'dika mastata'tu, a'oodhu bika min sharri ma sana'tu, aboo'u laka bini'matika 'alayya wa aboo'u bidhanbi faghfir li, fa-innahu la yaghfirudh-dhunooba illa Anta.",
            translation = "O Allah, You are my Lord; none has the right to be worshipped but You. You created me and I am Your servant, and I abide by Your covenant and promise as best I can. I seek refuge in You from the evil of what I have done. I acknowledge Your favor upon me, and I acknowledge my sin, so forgive me, for none forgives sins but You.",
            reference = "Sahih al-Bukhari (6306)"
        )
    )
}
