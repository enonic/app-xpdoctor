package me.myklebust.xpdoctor.validator.nodevalidator.nfcname;

import java.util.List;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class Xp8NodeNamesTest
{
    @Test
    public void decomposed_name_is_invalid_until_normalized()
    {
        final String decomposed = "blåbær";

        assertFalse( Xp8NodeNames.isValid( decomposed ) );
        assertEquals( "blåbær", Xp8NodeNames.normalize( decomposed ) );
        assertTrue( Xp8NodeNames.isValid( Xp8NodeNames.normalize( decomposed ) ) );
    }

    @Test
    public void composed_name_is_valid()
    {
        assertTrue( Xp8NodeNames.isValid( "blåbær" ) );
    }

    @Test
    public void letters_with_canonical_decomposition_are_invalid_until_normalized()
    {
        // Angstrom sign, Ohm sign, CJK compatibility ideograph, decomposed Hangul syllable
        for ( final String name : List.of( "Å", "Ω", "豈", "가" ) )
        {
            assertEquals( name, "name is not Unicode NFC normalized", Xp8NodeNames.invalidReason( name ) );
            assertTrue( name, Xp8NodeNames.isValid( Xp8NodeNames.normalize( name ) ) );
        }
        assertTrue( Xp8NodeNames.isValid( "가" ) );
    }

    @Test
    public void nfc_name_with_marks_is_valid()
    {
        // q + combining tilde has no composed form, so the NFC form still contains the mark
        assertTrue( Xp8NodeNames.isValid( "q̃" ) );
        // Thai: letter + vowel sign (non-spacing mark)
        assertTrue( Xp8NodeNames.isValid( "กิ" ) );
    }

    @Test
    public void devanagari_is_valid()
    {
        for ( final String name : List.of( "हमें-आपकी-सहमति-चाहिए",
                                           "कुकीज़", "ट्रैकिंग",
                                           "पर्यावरण", "हिन्दी",
                                           "नमस्ते" ) )
        {
            assertTrue( name, Xp8NodeNames.isValid( name ) );
            assertEquals( name, Xp8NodeNames.normalize( name ) );
        }
    }

    @Test
    public void mark_attached_to_letter_digit_or_mark_is_valid()
    {
        // digit + grave, Arabic tatweel + fatha, Devanagari letter + spacing mark + non-spacing mark, Thai letter + 2 marks
        for ( final String name : List.of( "1̀", "ـَ", "कां", "ที่" ) )
        {
            assertTrue( name, Xp8NodeNames.isValid( name ) );
        }
    }

    @Test
    public void unattached_mark_is_invalid()
    {
        for ( final String name : List.of( "̀q", "q ̀", "q-̀", "q.̀", "€̀", "😀̀" ) )
        {
            assertEquals( name, "name contains unattached mark 'U+0300'", Xp8NodeNames.invalidReason( name ) );
        }
    }

    @Test
    public void invisible_mark_is_invalid()
    {
        assertEquals( "name contains invisible mark 'U+034F'", Xp8NodeNames.invalidReason( "q͏q" ) );
        // red heart emoji with variation selector 16
        assertEquals( "name contains invisible mark 'U+FE0F'", Xp8NodeNames.invalidReason( "❤️" ) );
    }

    @Test
    public void enclosing_mark_is_invalid()
    {
        final String enclosed = "a⃝";

        assertEquals( "name contains character 'U+20DD' not allowed in XP 8", Xp8NodeNames.invalidReason( enclosed ) );
        assertFalse( Xp8NodeNames.isValid( Xp8NodeNames.normalize( enclosed ) ) );
    }

    @Test
    public void non_spacing_marks_per_base()
    {
        final String fourMarks = "q̀́̂̃";
        assertTrue( Xp8NodeNames.isValid( fourMarks ) );
        assertTrue( Xp8NodeNames.isValid( fourMarks + fourMarks ) );

        final String fiveMarks = fourMarks + "̄";
        assertEquals( "name contains more than 4 non-spacing marks per character", Xp8NodeNames.invalidReason( fiveMarks ) );
        assertFalse( Xp8NodeNames.isValid( Xp8NodeNames.normalize( fiveMarks ) ) );

        // A spacing mark does not reset the count
        assertEquals( "name contains more than 4 non-spacing marks per character",
                      Xp8NodeNames.invalidReason( fourMarks + "ा̀" ) );
    }

    @Test
    public void invalid_char_is_reported_before_mark_after_it()
    {
        assertEquals( "name contains character 'U+200D' not allowed in XP 8", Xp8NodeNames.invalidReason( "a‍́" ) );
    }

    @Test
    public void emoji_is_valid()
    {
        assertTrue( Xp8NodeNames.isValid( "cat😀" ) );
        assertFalse( Xp8NodeNames.isValid( "cat\uD83D" ) );
    }

    @Test
    public void whitespace_rules()
    {
        assertTrue( Xp8NodeNames.isValid( "my file.jpg" ) );
        assertFalse( Xp8NodeNames.isValid( " file" ) );
        assertFalse( Xp8NodeNames.isValid( "file " ) );
        assertFalse( Xp8NodeNames.isValid( "my\tfile" ) );
    }

    @Test
    public void illegal_characters_are_invalid()
    {
        assertFalse( Xp8NodeNames.isValid( "a|b" ) );
        assertFalse( Xp8NodeNames.isValid( "a?b" ) );
    }

    @Test
    public void invalid_reason()
    {
        assertNull( Xp8NodeNames.invalidReason( "my file.jpg" ) );
        assertEquals( "name is empty", Xp8NodeNames.invalidReason( "" ) );
        assertEquals( "name starts with a space", Xp8NodeNames.invalidReason( " file" ) );
        assertEquals( "name ends with a space", Xp8NodeNames.invalidReason( "file " ) );
        assertEquals( "name is not Unicode NFC normalized", Xp8NodeNames.invalidReason( "blåbær" ) );
        assertEquals( "name contains character '|' not allowed in XP 8", Xp8NodeNames.invalidReason( "a|b" ) );
        assertEquals( "name contains character 'U+0009' not allowed in XP 8", Xp8NodeNames.invalidReason( "my\tfile" ) );
    }
}