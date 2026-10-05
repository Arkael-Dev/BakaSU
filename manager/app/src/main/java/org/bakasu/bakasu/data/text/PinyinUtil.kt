package org.bakasu.bakasu.data.text

import android.icu.text.Transliterator
import org.bakasu.bakasu.domain.text.TextTransliterator

class PinyinUtil : TextTransliterator {

    private val transliterator: Transliterator by lazy {
        Transliterator.getInstance("Han-Latin; Latin-ASCII; Lower")
    }

    @Synchronized
    override fun transliterate(value: String): String = transliterator.transliterate(value).replace(" ", "")
}
